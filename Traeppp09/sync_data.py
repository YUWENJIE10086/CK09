# -*- coding: utf-8 -*-
"""
实时数据同步脚本
功能：
1. 监控Excel文件变化
2. 自动同步新增/修改/删除的数据到MySQL
3. 支持定时同步和手动触发同步
"""

import os
import sys
import time
import pandas as pd
import pymysql
from pymysql import Error
import hashlib
from datetime import datetime
import json

# 数据库配置
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'charset': 'utf8mb4',
    'database': os.getenv('DB_NAME', 'tobacco_infrastructure'),
    'port': int(os.getenv('DB_PORT', '3306'))
}

# 数据文件路径
BASE_DATA_PATH = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.abspath(__file__)), '基础数据'))

# Excel文件列表
EXCEL_FILES = {
    'city': ('sys_city', os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '市表_20260611152004.xlsx')),
    'county': ('sys_county', os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '县（市、区）表_20260611152048.xlsx')),
    'town': ('sys_town', os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '乡（镇）-收购站表_20260611152116.xlsx')),
    'village': ('sys_village', os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '村表_20260611152136.xlsx')),
    'county_dept': ('sys_county_department', os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '县级—部门对应表_20260611151703.xlsx')),
    'project_type': ('kf_project_type', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-项目类型_20260611152240.xlsx')),
    'main_body': ('kf_main_body', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-主体表_20260611152423.xlsx')),
    'project_info': ('kf_project_info', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-项目基础信息表_20260611152444.xlsx')),
    'control_device': ('kf_control_device', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-自控设备表_20260611152447.xlsx')),
    'heating_device': ('kf_heating_device', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-加热设备表_20260611152543.xlsx')),
    ' radiator': ('kf_radiator', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-散热器表_20260611152524.xlsx')),
    'burner': ('kf_burner', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx')),
    'auxiliary': ('kf_auxiliary_facility', os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-附属设施表.xlsx')),
}

# 文件修改时间记录
FILE_MOD_TIMES = {}


def sanitize_column_name(col_name):
    """清理列名"""
    import re
    sanitized = col_name.replace('（', '_').replace('）', '')
    sanitized = sanitized.replace(' ', '_').replace('-', '_')
    sanitized = sanitized.replace('/', '_').replace('\\', '_')
    sanitized = sanitized.replace('%', '_pct_')
    sanitized = re.sub(r'[^\w]', '_', sanitized)
    sanitized = re.sub(r'_+', '_', sanitized)
    sanitized = sanitized.strip('_')
    return sanitized.lower()


def calculate_row_hash(row):
    """计算行数据的哈希值"""
    row_str = '|'.join([str(v) if pd.notna(v) else 'NULL' for v in row.values])
    return hashlib.sha256(row_str.encode('utf-8')).hexdigest()


def get_file_mod_time(file_path):
    """获取文件最后修改时间"""
    try:
        return os.path.getmtime(file_path)
    except:
        return None


def check_file_changes(file_path):
    """检查文件是否有变化"""
    current_time = get_file_mod_time(file_path)
    if file_path not in FILE_MOD_TIMES:
        FILE_MOD_TIMES[file_path] = current_time
        return True  # 首次检查，认为有变化
    
    if current_time != FILE_MOD_TIMES[file_path]:
        FILE_MOD_TIMES[file_path] = current_time
        return True
    
    return False


def get_column_mapping(connection, table_name):
    """从数据库获取列名映射"""
    with connection.cursor() as cursor:
        cursor.execute(f"SHOW COLUMNS FROM `{table_name}`")
        columns = cursor.fetchall()
        # 排除系统字段
        system_fields = ['id', 'sync_hash', 'sync_status', 'last_sync_time', 'created_at', 'updated_at']
        return [col[0] for col in columns if col[0] not in system_fields]


def sync_table(table_name, file_path, connection):
    """同步单个表的数据"""
    try:
        print(f"\n同步表: {table_name}")
        print(f"  文件: {os.path.basename(file_path)}")
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            print(f"  ✗ 文件不存在")
            return False
        
        # 读取Excel文件
        df = pd.read_excel(file_path)
        
        if len(df) == 0:
            print(f"  文件为空，跳过")
            return True
        
        # 清理列名
        column_mapping = {col: sanitize_column_name(col) for col in df.columns}
        
        # 计算当前数据的哈希集合
        current_hashes = {}
        for idx, row in df.iterrows():
            hash_val = calculate_row_hash(row)
            current_hashes[hash_val] = (idx, row)
        
        # 获取数据库中的现有数据
        with connection.cursor() as cursor:
            # 获取所有列名
            db_columns = get_column_mapping(connection, table_name)
            
            # 获取数据库中的哈希值
            cursor.execute(f"SELECT id, sync_hash FROM `{table_name}` WHERE sync_status != 'deleted'")
            db_records = cursor.fetchall()
            db_hashes = {rec[1]: rec[0] for rec in rec if rec[1]}
            
            # 检测新增数据
            new_hashes = set(current_hashes.keys()) - set(db_hashes.keys())
            if new_hashes:
                print(f"  发现 {len(new_hashes)} 条新增数据")
                
                # 插入新数据
                for hash_val in new_hashes:
                    idx, row = current_hashes[hash_val]
                    
                    # 构建INSERT语句
                    columns = [column_mapping[col] for col in df.columns]
                    values = [None if pd.isna(v) else v for v in row.values]
                    
                    columns_str = ', '.join([f"`{col}`" for col in columns])
                    placeholders = ', '.join(['%s'] * len(columns))
                    
                    insert_sql = f"""
                        INSERT INTO `{table_name}` ({columns_str}, sync_hash, sync_status)
                        VALUES ({placeholders}, %s, 'synced')
                    """
                    
                    cursor.execute(insert_sql, values + [hash_val])
                
                connection.commit()
                print(f"  ✓ 新增数据已同步")
            
            # 检测删除数据
            deleted_hashes = set(db_hashes.keys()) - set(current_hashes.keys())
            if deleted_hashes:
                print(f"  发现 {len(deleted_hashes)} 条删除数据")
                
                for hash_val in deleted_hashes:
                    record_id = db_hashes[hash_val]
                    cursor.execute(f"""
                        UPDATE `{table_name}` 
                        SET sync_status='deleted', updated_at=NOW()
                        WHERE id=%s
                    """, (record_id,))
                
                connection.commit()
                print(f"  ✓ 删除数据已标记")
            
            # 更新同步时间
            cursor.execute(f"""
                UPDATE `{table_name}` 
                SET last_sync_time=NOW(), sync_status='synced'
                WHERE sync_status != 'deleted'
            """)
            connection.commit()
            
            # 记录同步日志
            cursor.execute("""
                INSERT INTO sync_log (table_name, file_path, records_count, sync_status)
                VALUES (%s, %s, %s, 'synced')
            """, (table_name, file_path, len(df)))
            connection.commit()
            
            if not new_hashes and not deleted_hashes:
                print(f"  数据无变化")
            
            return True
    
    except Exception as e:
        print(f"  ✗ 同步失败: {e}")
        
        # 记录错误日志
        with connection.cursor() as cursor:
            cursor.execute("""
                INSERT INTO sync_log (table_name, file_path, records_count, sync_status, error_message)
                VALUES (%s, %s, 0, 'failed', %s)
            """, (table_name, file_path, str(e)))
            connection.commit()
        
        return False


def run_full_sync():
    """执行全量同步"""
    print("=" * 60)
    print("开始全量数据同步")
    print("=" * 60)
    
    connection = pymysql.connect(**DB_CONFIG)
    
    try:
        success_count = 0
        
        for file_key, (table_name, file_path) in EXCEL_FILES.items():
            if sync_table(table_name, file_path, connection):
                success_count += 1
        
        print("\n" + "=" * 60)
        print(f"同步完成！成功: {success_count}/{len(EXCEL_FILES)}")
        print("=" * 60)
        
        # 显示同步统计
        with connection.cursor() as cursor:
            cursor.execute("""
                SELECT table_name, COUNT(*) as count, sync_status
                FROM sync_log
                WHERE sync_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
                GROUP BY table_name, sync_status
            """)
            stats = cursor.fetchall()
            
            print("\n同步统计:")
            for stat in stats:
                print(f"  - {stat[0]}: {stat[1]} 条 ({stat[2]})")
    
    finally:
        connection.close()


def run_incremental_sync():
    """执行增量同步（只同步有变化的文件）"""
    print("=" * 60)
    print("开始增量数据同步")
    print("=" * 60)
    
    connection = pymysql.connect(**DB_CONFIG)
    
    try:
        synced_count = 0
        
        for file_key, (table_name, file_path) in EXCEL_FILES.items():
            if check_file_changes(file_path):
                print(f"\n检测到文件变化: {os.path.basename(file_path)}")
                if sync_table(table_name, file_path, connection):
                    synced_count += 1
            else:
                print(f"\n文件无变化: {os.path.basename(file_path)}")
        
        print("\n" + "=" * 60)
        print(f"增量同步完成！同步了 {synced_count} 个表")
        print("=" * 60)
    
    finally:
        connection.close()


def watch_and_sync(interval=60):
    """持续监控并同步"""
    print("=" * 60)
    print("启动实时监控同步")
    print(f"监控间隔: {interval} 秒")
    print("=" * 60)
    
    # 初始化文件修改时间
    for file_key, (table_name, file_path) in EXCEL_FILES.items():
        FILE_MOD_TIMES[file_path] = get_file_mod_time(file_path)
    
    while True:
        try:
            run_incremental_sync()
            print(f"\n下次检查时间: {datetime.now().strftime('%H:%M:%S')} + {interval}秒")
            time.sleep(interval)
        except KeyboardInterrupt:
            print("\n\n监控已停止")
            break
        except Exception as e:
            print(f"\n监控出错: {e}")
            time.sleep(interval)


def show_sync_status():
    """显示同步状态"""
    print("=" * 60)
    print("数据同步状态")
    print("=" * 60)
    
    connection = pymysql.connect(**DB_CONFIG)
    
    try:
        with connection.cursor() as cursor:
            # 显示各表数据统计
            cursor.execute("""
                SELECT 
                    TABLE_NAME as table_name,
                    TABLE_ROWS as row_count
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = 'tobacco_infrastructure'
                ORDER BY TABLE_NAME
            """)
            tables = cursor.fetchall()
            
            print("\n数据库表统计:")
            for table in tables:
                if table[0] != 'sync_log':
                    # 获取同步状态统计
                    cursor.execute(f"""
                        SELECT sync_status, COUNT(*) 
                        FROM `{table[0]}` 
                        GROUP BY sync_status
                    """)
                    status_stats = cursor.fetchall()
                    status_str = ', '.join([f"{s[0]}:{s[1]}" for s in status_stats])
                    print(f"  - {table[0]}: {table[1]} 条记录 ({status_str})")
            
            # 显示最近的同步日志
            cursor.execute("""
                SELECT table_name, sync_time, records_count, sync_status
                FROM sync_log
                ORDER BY sync_time DESC
                LIMIT 10
            """)
            logs = cursor.fetchall()
            
            print("\n最近同步记录:")
            for log in logs:
                print(f"  - {log[0]}: {log[1]} ({log[2]}条, {log[3]})")
    
    finally:
        connection.close()


def main():
    """主函数"""
    if len(sys.argv) > 1:
        command = sys.argv[1]
        
        if command == '--full':
            run_full_sync()
        elif command == '--incremental':
            run_incremental_sync()
        elif command == '--watch':
            interval = int(sys.argv[2]) if len(sys.argv) > 2 else 60
            watch_and_sync(interval)
        elif command == '--status':
            show_sync_status()
        else:
            print("用法:")
            print("  python sync_data.py --full        # 全量同步")
            print("  python sync_data.py --incremental # 增量同步")
            print("  python sync_data.py --watch [间隔秒数] # 持续监控同步")
            print("  python sync_data.py --status      # 显示同步状态")
    else:
        # 默认执行增量同步
        run_incremental_sync()


if __name__ == '__main__':
    main()
