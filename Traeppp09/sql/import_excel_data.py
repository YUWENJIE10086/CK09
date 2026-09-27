# -*- coding: utf-8 -*-
"""
烤房管理系统 - Excel数据导入脚本
读取基础数据Excel文件，导入到MySQL数据库
"""
import os
import sys
import mysql.connector
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# 数据库连接配置
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', '3306')),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'database': os.getenv('DB_NAME', 'barn_management'),
    'charset': 'utf8mb4'
}

BASE_PATH = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), '基础数据'))
BASE_PATH_ENCODED = BASE_PATH.encode('unicode_escape').decode('utf-8')

def get_connection():
    """获取数据库连接"""
    return mysql.connector.connect(**DB_CONFIG)

def execute_sql(conn, sql, data=None):
    """执行SQL"""
    cursor = conn.cursor()
    try:
        if data:
            cursor.executemany(sql, data)
        else:
            cursor.execute(sql)
        conn.commit()
        return cursor.rowcount
    except Exception as e:
        print(f"执行SQL出错: {e}")
        conn.rollback()
        return 0
    finally:
        cursor.close()

def read_excel_simple(file_path):
    """读取Excel文件（简化版，使用csv或直接解析）"""
    import openpyxl
    try:
        wb = openpyxl.load_workbook(file_path, data_only=True)
        ws = wb.active
        data = []
        for row in ws.iter_rows(values_only=True):
            # 过滤空行
            if any(cell is not None for cell in row):
                data.append(row)
        return data
    except Exception as e:
        print(f"读取Excel失败 {file_path}: {e}")
        return []

def import_city(conn):
    """导入市表数据"""
    print("正在导入市表...")
    file_path = os.path.join(BASE_PATH, '地区部门信息绑定', '市表_20260611152004.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("市表数据为空")
        return 0
    
    headers = data[0]
    records = []
    for row in data[1:]:
        if len(row) >= 4:
            records.append((
                row[2] if row[2] else '',  # 市名称
                row[1] if row[1] else '',  # 市编码
                row[3] if len(row) > 3 and row[3] else None,  # 部门
                row[6] if len(row) > 6 and row[6] else '启用',  # 状态
                row[7] if len(row) > 7 and row[7] else None,
                row[8] if len(row) > 8 and row[8] else None,
                row[9] if len(row) > 9 and row[9] else None
            ))
    
    sql = """INSERT INTO t_city (city_name, city_code, dept_name, status, applicant, apply_time, update_time)
             VALUES (%s, %s, %s, %s, %s, %s, %s)"""
    count = execute_sql(conn, sql, records)
    print(f"市表导入完成: {count} 条")
    return count

def import_county(conn):
    """导入县区表数据"""
    print("正在导入县区表...")
    file_path = os.path.join(BASE_PATH, '地区部门信息绑定', '县（市、区）表_20260611152048.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("县区表数据为空")
        return 0
    
    records = []
    for row in data[1:]:
        if len(row) >= 8 and row[6]:  # 县区编码
            records.append((
                row[6],  # 县区编码
                row[2] if row[2] else '',  # 县区名称
                row[7] if row[7] else '',  # 关联市编码
                row[4] if len(row) > 4 and row[4] else None,  # 首字母
                row[8] if len(row) > 8 and row[8] else None,  # 部门
                row[9] if len(row) > 9 and row[9] else '启用',
                row[10] if len(row) > 10 and row[10] else None,
                row[11] if len(row) > 11 and row[11] else None,
                row[12] if len(row) > 12 and row[12] else None
            ))
    
    sql = """INSERT INTO t_county (county_code, county_name, city_code, first_letter, dept_name, status, applicant, apply_time, update_time)
             VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"""
    count = execute_sql(conn, sql, records)
    print(f"县区表导入完成: {count} 条")
    return count

def import_township(conn):
    """导入乡镇表数据"""
    print("正在导入乡镇表...")
    file_path = os.path.join(BASE_PATH, '地区部门信息绑定', '乡（镇）-收购站表_20260611152116.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("乡镇表数据为空")
        return 0
    
    records = []
    for row in data[1:]:
        if len(row) >= 5 and row[4]:  # 乡镇编码
            records.append((
                row[4],  # 乡镇编码
                row[1] if row[1] else '',  # 乡镇名称
                row[6] if len(row) > 6 and row[6] else '',  # 关联县区编码
                row[3] if len(row) > 3 and row[3] else None,  # 首字母
                row[7] if len(row) > 7 and row[7] else None,  # 收购站
                row[8] if len(row) > 8 and row[8] else '启用',
                row[9] if len(row) > 9 and row[9] else None,
                row[10] if len(row) > 10 and row[10] else None,
                row[11] if len(row) > 11 and row[11] else None
            ))
    
    sql = """INSERT INTO t_township (township_code, township_name, county_code, first_letter, purchase_station, status, applicant, apply_time, update_time)
             VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"""
    count = execute_sql(conn, sql, records)
    print(f"乡镇表导入完成: {count} 条")
    return count

def import_village(conn):
    """导入村表数据"""
    print("正在导入村表...")
    file_path = os.path.join(BASE_PATH, '地区部门信息绑定', '村表_20260611152136.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("村表数据为空")
        return 0
    
    records = []
    for row in data[1:]:
        if len(row) >= 7 and row[6]:  # 村编码
            records.append((
                row[6],  # 村编码
                row[2] if row[2] else '',  # 村名称
                row[7] if len(row) > 7 and row[7] else '',  # 关联乡镇编码
                row[5] if len(row) > 5 and row[5] else None,  # 首字母
                row[8] if len(row) > 8 and row[8] else '启用',
                row[9] if len(row) > 9 and row[9] else None,
                row[10] if len(row) > 10 and row[10] else None,
                row[11] if len(row) > 11 and row[11] else None
            ))
    
    sql = """INSERT INTO t_village (village_code, village_name, township_code, first_letter, status, applicant, apply_time, update_time)
             VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"""
    count = execute_sql(conn, sql, records)
    print(f"村表导入完成: {count} 条")
    return count

def import_project_type(conn):
    """导入项目类型表"""
    print("正在导入项目类型表...")
    file_path = os.path.join(BASE_PATH, '数据表结构', '烤房-项目类型_20260611152240.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("项目类型表数据为空")
        return 0
    
    records = []
    for row in data[1:]:
        if len(row) >= 4 and row[3]:  # 项目类型
            records.append((
                row[3],  # 项目类型
                row[2] if len(row) > 2 and row[2] else None,  # 排序号
                row[4] if len(row) > 4 and row[4] else '启用',
                row[5] if len(row) > 5 and row[5] else None,
                row[6] if len(row) > 6 and row[6] else None,
                row[7] if len(row) > 7 and row[7] else None
            ))
    
    sql = """INSERT INTO t_project_type (type_name, sort_order, status, applicant, apply_time, update_time)
             VALUES (%s, %s, %s, %s, %s, %s)"""
    count = execute_sql(conn, sql, records)
    print(f"项目类型表导入完成: {count} 条")
    return count

def import_barn_project(conn):
    """导入烤房项目基础信息表"""
    print("正在导入烤房项目基础信息表（这可能需要几分钟）...")
    file_path = os.path.join(BASE_PATH, '数据表结构', '烤房-项目基础信息表_20260611152444.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("烤房项目基础信息表数据为空")
        return 0
    
    headers = data[0]
    print(f"烤房项目表共 {len(data)-1} 条数据")
    
    # 找到各列索引
    idx = {h: i for i, h in enumerate(headers) if h}
    
    records = []
    for row in data[1:]:
        try:
            record = (
                row[idx.get('项目编号', 0)] if idx.get('项目编号', 0) < len(row) else None,  # 项目编号
                row[idx.get('使用状态', 1)] if idx.get('使用状态', 1) < len(row) else '在用',  # 使用状态
                row[idx.get('闲置年份数（单位：年）', 2)] if idx.get('闲置年份数（单位：年）', 2) < len(row) else 0,  # 闲置年份
                row[idx.get('转用年度', 3)] if idx.get('转用年度', 3) < len(row) and row[idx.get('转用年度', 3)] else None,
                row[idx.get('转用用途', 4)] if idx.get('转用用途', 4) < len(row) else None,
                row[idx.get('损毁年度', 5)] if idx.get('损毁年度', 5) < len(row) and row[idx.get('损毁年度', 5)] else None,
                row[idx.get('损毁原因', 6)] if idx.get('损毁原因', 6) < len(row) else None,
                row[idx.get('建设方式', 7)] if idx.get('建设方式', 7) < len(row) else None,
                row[idx.get('项目类型', 8)] if idx.get('项目类型', 8) < len(row) else None,
                row[idx.get('长', 9)] if idx.get('长', 9) < len(row) and row[idx.get('长', 9)] else None,
                row[idx.get('宽', 10)] if idx.get('宽', 10) < len(row) and row[idx.get('宽', 10)] else None,
                row[idx.get('高', 11)] if idx.get('高', 11) < len(row) and row[idx.get('高', 11)] else None,
                row[idx.get('工程造价', 12)] if idx.get('工程造价', 12) < len(row) and row[idx.get('工程造价', 12)] else None,
                row[idx.get('国家局补贴', 13)] if idx.get('国家局补贴', 13) < len(row) and row[idx.get('国家局补贴', 13)] else None,
                row[idx.get('产区补贴', 14)] if idx.get('产区补贴', 14) < len(row) and row[idx.get('产区补贴', 14)] else None,
                row[idx.get('补贴总额', 15)] if idx.get('补贴总额', 15) < len(row) and row[idx.get('补贴总额', 15)] else None,
                row[idx.get('开工时间', 16)] if idx.get('开工时间', 16) < len(row) else None,
                row[idx.get('竣工时间', 17)] if idx.get('竣工时间', 17) < len(row) else None,
                row[idx.get('市', 18)] if idx.get('市', 18) < len(row) else None,
                row[idx.get('市编码', 19)] if idx.get('市编码', 19) < len(row) else None,
                row[idx.get('县（市、区）', 20)] if idx.get('县（市、区）', 20) < len(row) else None,
                row[idx.get('县市编码', 21)] if idx.get('县市编码', 21) < len(row) else None,
                row[idx.get('乡（镇）', 22)] if idx.get('乡（镇）', 22) < len(row) else None,
                row[idx.get('乡镇编码', 23)] if idx.get('乡镇编码', 23) < len(row) else None,
                row[idx.get('村', 24)] if idx.get('村', 24) < len(row) else None,
                row[idx.get('村编码', 25)] if idx.get('村编码', 25) < len(row) else None,
                row[idx.get('详细地址', 26)] if idx.get('详细地址', 26) < len(row) else None,
                row[idx.get('项目业主', 27)] if idx.get('项目业主', 27) < len(row) else None,
                row[idx.get('施工单位', 28)] if idx.get('施工单位', 28) < len(row) else None,
                row[idx.get('海拔', 29)] if idx.get('海拔', 29) < len(row) and row[idx.get('海拔', 29)] else None,
                row[idx.get('经度', 30)] if idx.get('经度', 30) < len(row) and row[idx.get('经度', 30)] else None,
                row[idx.get('纬度', 31)] if idx.get('纬度', 31) < len(row) and row[idx.get('纬度', 31)] else None,
                row[idx.get('加热设备是否填写', 32)] if idx.get('加热设备是否填写', 32) < len(row) else 0,
                row[idx.get('散热器是否填写', 33)] if idx.get('散热器是否填写', 33) < len(row) else 0,
                row[idx.get('自控设备是否填写', 34)] if idx.get('自控设备是否填写', 34) < len(row) else 0,
                row[idx.get('烤房主体是否填写', 35)] if idx.get('烤房主体是否填写', 35) < len(row) else 0,
                row[idx.get('附属设施是否填写', 36)] if idx.get('附属设施是否填写', 36) < len(row) else 0,
                row[idx.get('关联收购站', 37)] if idx.get('关联收购站', 37) < len(row) else None,
                row[idx.get('当前流程状态', 0)] if idx.get('当前流程状态', 0) < len(row) else '待审核',  # 状态
                row[idx.get('申请人', 41)] if idx.get('申请人', 41) < len(row) else None,
                row[idx.get('申请时间', 42)] if idx.get('申请时间', 42) < len(row) else None,
                row[idx.get('更新时间', 43)] if idx.get('更新时间', 43) < len(row) else None,
            )
            records.append(record)
        except Exception as e:
            continue
    
    sql = """INSERT INTO t_barn_project (
        project_code, use_status, idle_years, transfer_year, transfer_use,
        damage_year, damage_reason, build_mode, project_type, length_m, width_m, height_m,
        project_cost, subsidy_national, subsidy_region, subsidy_total, start_date, complete_date,
        city_name, city_code, county_name, county_code, township_name, township_code,
        village_name, village_code, address, project_owner, construction_unit, elevation,
        longitude, latitude, has_heating, has_radiator, has_autocontrol, has_main, has_ancillary,
        station_code, status, applicant, apply_time, update_time
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""
    
    # 批量插入，每500条提交一次
    batch_size = 500
    total = 0
    for i in range(0, len(records), batch_size):
        batch = records[i:i+batch_size]
        count = execute_sql(conn, sql, batch)
        total += count
        print(f"已导入: {min(i+batch_size, len(records))}/{len(records)}")
    
    print(f"烤房项目基础信息表导入完成: {total} 条")
    return total

def import_burner(conn):
    """导入燃烧机信息表"""
    print("正在导入燃烧机信息表（这可能需要几分钟）...")
    file_path = os.path.join(BASE_PATH, '数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("燃烧机信息表数据为空")
        return 0
    
    headers = data[0]
    idx = {h: i for i, h in enumerate(headers) if h}
    print(f"燃烧机表共 {len(data)-1} 条数据")
    
    records = []
    for row in data[1:]:
        try:
            record = (
                row[idx.get('燃烧机编码', 0)] if idx.get('燃烧机编码', 0) < len(row) and row[idx.get('燃烧机编码', 0)] else None,
                row[idx.get('关联项目编码', 1)] if idx.get('关联项目编码', 1) < len(row) and row[idx.get('关联项目编码', 1)] else None,
                row[idx.get('加热设备类型', 2)] if idx.get('加热设备类型', 2) < len(row) else None,
                row[idx.get('工程造价（元）', 3)] if idx.get('工程造价（元）', 3) < len(row) and row[idx.get('工程造价（元）', 3)] else None,
                row[idx.get('补贴总额', 4)] if idx.get('补贴总额', 4) < len(row) and row[idx.get('补贴总额', 4)] else None,
                row[idx.get('国家局补贴', 5)] if idx.get('国家局补贴', 5) < len(row) and row[idx.get('国家局补贴', 5)] else None,
                row[idx.get('产区补贴', 6)] if idx.get('产区补贴', 6) < len(row) and row[idx.get('产区补贴', 6)] else None,
                row[idx.get('年度', 7)] if idx.get('年度', 7) < len(row) and row[idx.get('年度', 7)] else None,
                row[idx.get('施工（供应）单位', 8)] if idx.get('施工（供应）单位', 8) < len(row) else None,
                row[idx.get('是否已关联项目编码', 9)] if idx.get('是否已关联项目编码', 9) < len(row) else '否',
                row[idx.get('当前流程状态', 0)] if idx.get('当前流程状态', 0) < len(row) else '待审核',
                row[idx.get('申请人', 11)] if idx.get('申请人', 11) < len(row) else None,
                row[idx.get('申请时间', 12)] if idx.get('申请时间', 12) < len(row) else None,
                row[idx.get('更新时间', 13)] if idx.get('更新时间', 13) < len(row) else None,
            )
            if record[0]:  # 必须有燃烧机编码
                records.append(record)
        except:
            continue
    
    sql = """INSERT INTO t_burner (
        burner_code, project_code, heating_type, project_cost, subsidy_total,
        subsidy_national, subsidy_region, build_year, construction_unit, has_project_link,
        status, applicant, apply_time, update_time
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""
    
    batch_size = 500
    total = 0
    for i in range(0, len(records), batch_size):
        batch = records[i:i+batch_size]
        count = execute_sql(conn, sql, batch)
        total += count
        print(f"已导入: {min(i+batch_size, len(records))}/{len(records)}")
    
    print(f"燃烧机信息表导入完成: {total} 条")
    return total

def main():
    """主函数"""
    print("=" * 50)
    print("烤房管理系统 - Excel数据导入")
    print("=" * 50)
    
    try:
        conn = get_connection()
        print("数据库连接成功!")
        
        # 导入顺序很重要，先导字典表，再导业务表
        import_city(conn)
        import_county(conn)
        import_township(conn)
        import_village(conn)
        import_project_type(conn)
        import_barn_project(conn)
        import_burner(conn)
        
        print("=" * 50)
        print("所有数据导入完成!")
        
        # 显示统计
        cursor = conn.cursor()
        tables = ['t_city', 't_county', 't_township', 't_village', 't_project_type', 't_barn_project', 't_burner']
        for table in tables:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            print(f"  {table}: {count} 条")
        cursor.close()
        
    except Exception as e:
        print(f"错误: {e}")
    finally:
        if 'conn' in dir():
            conn.close()
        print("数据库连接已关闭")

if __name__ == '__main__':
    main()
