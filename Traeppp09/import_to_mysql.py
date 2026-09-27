# -*- coding: utf-8 -*-
"""
烤房基础数据导入MySQL数据库脚本
功能：
1. 读取所有Excel文件
2. 自动创建数据库和表结构
3. 导入数据到MySQL
4. 提供实时同步功能
"""

import os
import sys
import pandas as pd
import pymysql
from pymysql import Error
from datetime import datetime
import json
import hashlib

# 数据库配置
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'charset': 'utf8mb4',
    'port': int(os.getenv('DB_PORT', '3306'))
}

DATABASE_NAME = os.getenv('DB_NAME', 'tobacco_infrastructure')

# 数据文件路径
BASE_DATA_PATH = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.abspath(__file__)), '基础数据'))

# Excel文件列表
EXCEL_FILES = {
    'city': os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '市表_20260611152004.xlsx'),
    'county': os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '县（市、区）表_20260611152048.xlsx'),
    'town': os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '乡（镇）-收购站表_20260611152116.xlsx'),
    'village': os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '村表_20260611152136.xlsx'),
    'county_dept': os.path.join(BASE_DATA_PATH, '地区部门信息绑定', '县级—部门对应表_20260611151703.xlsx'),
    'project_type': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-项目类型_20260611152240.xlsx'),
    'main_body': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-主体表_20260611152423.xlsx'),
    'project_info': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-项目基础信息表_20260611152444.xlsx'),
    'control_device': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-自控设备表_20260611152447.xlsx'),
    'heating_device': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-加热设备表_20260611152543.xlsx'),
    ' radiator': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-散热器表_20260611152524.xlsx'),
    'burner': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx'),
    'auxiliary': os.path.join(BASE_DATA_PATH, '数据表结构', '烤房-附属设施表.xlsx'),
}

# 表名映射
TABLE_NAMES = {
    'city': 'sys_city',
    'county': 'sys_county',
    'town': 'sys_town',
    'village': 'sys_village',
    'county_dept': 'sys_county_department',
    'project_type': 'kf_project_type',
    'main_body': 'kf_main_body',
    'project_info': 'kf_project_info',
    'control_device': 'kf_control_device',
    'heating_device': 'kf_heating_device',
    ' radiator': 'kf_radiator',
    'burner': 'kf_burner',
    'auxiliary': 'kf_auxiliary_facility',
}


def create_database():
    """创建数据库"""
    connection = pymysql.connect(**DB_CONFIG)
    try:
        with connection.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DATABASE_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
            print(f"✓ 数据库 '{DATABASE_NAME}' 创建成功")
    except Error as e:
        print(f"✗ 创建数据库失败: {e}")
    finally:
        connection.close()


def get_mysql_type(pandas_type, max_length=None):
    """将Pandas数据类型转换为MySQL数据类型"""
    if pd.api.types.is_integer_dtype(pandas_type):
        return 'INT'
    elif pd.api.types.is_float_dtype(pandas_type):
        return 'DECIMAL(15,2)'
    elif pd.api.types.is_datetime64_dtype(pandas_type):
        return 'DATETIME'
    elif pd.api.types.is_bool_dtype(pandas_type):
        return 'TINYINT(1)'
    else:
        if max_length and max_length > 255:
            return 'TEXT'
        elif max_length and max_length > 100:
            return f'VARCHAR({min(int(max_length * 1.5), 500)})'
        else:
            return 'VARCHAR(255)'


def sanitize_column_name(col_name):
    """清理列名，使其适合MySQL"""
    # 移除特殊字符，替换为下划线
    sanitized = col_name.replace('（', '_').replace('）', '')
    sanitized = sanitized.replace(' ', '_').replace('-', '_')
    sanitized = sanitized.replace('/', '_').replace('\\', '_')
    sanitized = sanitized.replace('%', '_pct_')
    # 移除其他特殊字符
    import re
    sanitized = re.sub(r'[^\w]', '_', sanitized)
    # 移除连续下划线
    sanitized = re.sub(r'_+', '_', sanitized)
    # 移除开头和结尾的下划线
    sanitized = sanitized.strip('_')
    return sanitized.lower()


def create_table_from_dataframe(df, table_name, connection):
    """根据DataFrame结构创建MySQL表"""
    with connection.cursor() as cursor:
        # 先删除已存在的表
        cursor.execute(f"DROP TABLE IF EXISTS `{table_name}`")
        
        # 构建CREATE TABLE语句
        columns_sql = []
        column_mapping = {}
        
        for col in df.columns:
            sanitized_col = sanitize_column_name(col)
            column_mapping[col] = sanitized_col
            
            # 计算最大长度（对于字符串类型）
            max_len = None
            if pd.api.types.is_object_dtype(df[col].dtype):
                try:
                    max_len = df[col].astype(str).str.len().max()
                except:
                    max_len = 255
            
            mysql_type = get_mysql_type(df[col].dtype, max_len)
            columns_sql.append(f"`{sanitized_col}` {mysql_type}")
        
        # 添加id主键和同步相关字段
        create_sql = f"""
        CREATE TABLE `{table_name}` (
            `id` INT AUTO_INCREMENT PRIMARY KEY,
            {', '.join(columns_sql)},
            `sync_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据同步哈希值',
            `sync_status` VARCHAR(20) DEFAULT 'synced' COMMENT '同步状态',
            `last_sync_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后同步时间',
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """
        
        cursor.execute(create_sql)
        print(f"✓ 表 '{table_name}' 创建成功，包含 {len(df.columns)} 个字段")
        
        return column_mapping


def calculate_row_hash(row):
    """计算行数据的哈希值，用于同步检测"""
    row_str = '|'.join([str(v) if pd.notna(v) else 'NULL' for v in row.values])
    return hashlib.sha256(row_str.encode('utf-8')).hexdigest()


def import_data_to_table(df, table_name, column_mapping, connection):
    """将DataFrame数据导入到MySQL表"""
    # 清理数据
    df_clean = df.copy()
    
    # 重命名列
    df_clean.columns = [column_mapping[col] for col in df_clean.columns]
    
    # 处理NaN值 - 将所有NaN转换为None（MySQL NULL）
    for col in df_clean.columns:
        df_clean[col] = df_clean[col].apply(lambda x: None if pd.isna(x) else x)
    
    # 计算每行的哈希值（使用原始数据）
    sync_hashes = [calculate_row_hash(row) for _, row in df.iterrows()]
    
    with connection.cursor() as cursor:
        # 构建INSERT语句
        columns = list(df_clean.columns)
        columns_with_hash = columns + ['sync_hash']
        
        placeholders = ', '.join(['%s'] * len(columns_with_hash))
        columns_str = ', '.join([f"`{col}`" for col in columns_with_hash])
        
        insert_sql = f"INSERT INTO `{table_name}` ({columns_str}) VALUES ({placeholders})"
        
        # 批量插入
        rows_to_insert = []
        for idx, row in df_clean.iterrows():
            row_values = [None if pd.isna(v) else v for v in row.values] + [sync_hashes[idx]]
            rows_to_insert.append(row_values)
        
        # 执行批量插入
        cursor.executemany(insert_sql, rows_to_insert)
        connection.commit()
        
        print(f"✓ 表 '{table_name}' 导入 {len(rows_to_insert)} 条数据")


def process_excel_file(file_key, file_path, connection):
    """处理单个Excel文件"""
    try:
        print(f"\n处理文件: {os.path.basename(file_path)}")
        
        # 读取Excel文件
        df = pd.read_excel(file_path)
        
        # 显示基本信息
        print(f"  - 数据行数: {len(df)}")
        print(f"  - 列数: {len(df.columns)}")
        print(f"  - 列名: {list(df.columns)[:5]}...")  # 只显示前5列
        
        # 创建表
        table_name = TABLE_NAMES.get(file_key, f'table_{file_key}')
        column_mapping = create_table_from_dataframe(df, table_name, connection)
        
        # 导入数据
        import_data_to_table(df, table_name, column_mapping, connection)
        
        return True
        
    except Exception as e:
        print(f"✗ 处理文件失败: {e}")
        return False


def create_sync_log_table(connection):
    """创建同步日志表"""
    with connection.cursor() as cursor:
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS `sync_log` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `table_name` VARCHAR(100) NOT NULL,
                `file_path` VARCHAR(500) NOT NULL,
                `sync_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                `records_count` INT DEFAULT 0,
                `sync_status` VARCHAR(20) DEFAULT 'success',
                `error_message` TEXT,
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """)
        print("✓ 同步日志表创建成功")


def log_sync(connection, table_name, file_path, records_count, status, error_msg=None):
    """记录同步日志"""
    with connection.cursor() as cursor:
        cursor.execute("""
            INSERT INTO sync_log (table_name, file_path, records_count, sync_status, error_message)
            VALUES (%s, %s, %s, %s, %s)
        """, (table_name, file_path, records_count, status, error_msg))
        connection.commit()


def check_and_sync_changes(file_key, file_path, connection):
    """检查文件变化并同步更新"""
    try:
        df = pd.read_excel(file_path)
        table_name = TABLE_NAMES.get(file_key, f'table_{file_key}')
        
        # 计算当前数据的哈希集合
        current_hashes = set([calculate_row_hash(row) for _, row in df.iterrows()])
        
        with connection.cursor() as cursor:
            # 获取数据库中的哈希集合
            cursor.execute(f"SELECT sync_hash FROM `{table_name}` WHERE sync_hash IS NOT NULL")
            db_hashes = set([row[0] for row in cursor.fetchall()])
            
            # 检测新增的数据
            new_hashes = current_hashes - db_hashes
            if new_hashes:
                print(f"  发现 {len(new_hashes)} 条新增数据")
                # 这里可以添加新增数据的逻辑
            
            # 检测删除的数据
            deleted_hashes = db_hashes - current_hashes
            if deleted_hashes:
                print(f"  发现 {len(deleted_hashes)} 条删除数据")
                # 标记为删除或实际删除
                for hash_val in deleted_hashes:
                    cursor.execute(f"UPDATE `{table_name}` SET sync_status='deleted' WHERE sync_hash=%s", (hash_val,))
                connection.commit()
            
            if not new_hashes and not deleted_hashes:
                print(f"  数据无变化，同步完成")
        
        return True
        
    except Exception as e:
        print(f"  同步检查失败: {e}")
        return False


def main():
    """主函数"""
    print("=" * 60)
    print("烤房基础数据导入MySQL数据库")
    print("=" * 60)
    
    # 创建数据库
    create_database()
    
    # 连接到新创建的数据库
    DB_CONFIG['database'] = DATABASE_NAME
    connection = pymysql.connect(**DB_CONFIG)
    
    try:
        # 创建同步日志表
        create_sync_log_table(connection)
        
        # 处理所有Excel文件
        success_count = 0
        for file_key, file_path in EXCEL_FILES.items():
            if os.path.exists(file_path):
                if process_excel_file(file_key, file_path, connection):
                    success_count += 1
                    log_sync(connection, TABLE_NAMES.get(file_key), file_path, 
                            len(pd.read_excel(file_path)), 'success')
            else:
                print(f"\n✗ 文件不存在: {file_path}")
                log_sync(connection, TABLE_NAMES.get(file_key), file_path, 0, 'failed', '文件不存在')
        
        print("\n" + "=" * 60)
        print(f"导入完成！成功处理 {success_count}/{len(EXCEL_FILES)} 个文件")
        print("=" * 60)
        
        # 显示数据库表列表
        with connection.cursor() as cursor:
            cursor.execute(f"SHOW TABLES")
            tables = cursor.fetchall()
            print("\n数据库表列表:")
            for table in tables:
                cursor.execute(f"SELECT COUNT(*) FROM `{table[0]}`")
                count = cursor.fetchone()[0]
                print(f"  - {table[0]}: {count} 条记录")
        
    except Error as e:
        print(f"\n✗ 数据库操作失败: {e}")
    finally:
        connection.close()


def run_sync():
    """运行实时同步检查"""
    print("\n" + "=" * 60)
    print("执行实时同步检查...")
    print("=" * 60)
    
    DB_CONFIG['database'] = DATABASE_NAME
    connection = pymysql.connect(**DB_CONFIG)
    
    try:
        for file_key, file_path in EXCEL_FILES.items():
            if os.path.exists(file_path):
                print(f"\n检查: {os.path.basename(file_path)}")
                check_and_sync_changes(file_key, file_path, connection)
    finally:
        connection.close()


if __name__ == '__main__':
    main()
    
    # 如果需要实时同步，可以运行
    if len(sys.argv) > 1 and sys.argv[1] == '--sync':
        run_sync()
