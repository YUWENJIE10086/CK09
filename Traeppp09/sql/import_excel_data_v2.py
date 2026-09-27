# -*- coding: utf-8 -*-
"""
烤房管理系统 - Excel数据导入脚本（修复版）
"""
import os
import mysql.connector
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', '3306')),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'database': os.getenv('DB_NAME', 'barn_management'),
    'charset': 'utf8mb4'
}

BASE_PATH = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), '基础数据'))

def get_connection():
    return mysql.connector.connect(**DB_CONFIG)

def execute_sql(conn, sql, data=None):
    cursor = conn.cursor()
    try:
        if data:
            cursor.executemany(sql, data)
        else:
            cursor.execute(sql)
        conn.commit()
        return cursor.rowcount
    except Exception as e:
        print(f"  SQL错误: {e}")
        conn.rollback()
        return 0
    finally:
        cursor.close()

def safe_int(val, default=0):
    """安全转换为整数"""
    if val is None or val == '':
        return default
    try:
        return int(val)
    except:
        return default

def safe_float(val, default=None):
    """安全转换为浮点数"""
    if val is None or val == '':
        return default
    try:
        return float(val)
    except:
        return default

def safe_str(val, default=''):
    """安全转换为字符串"""
    if val is None:
        return default
    return str(val).strip()

def read_excel_simple(file_path):
    """读取Excel文件"""
    import openpyxl
    try:
        wb = openpyxl.load_workbook(file_path, data_only=True)
        ws = wb.active
        data = []
        for row in ws.iter_rows(values_only=True):
            if any(cell is not None for cell in row):
                data.append([cell for cell in row])
        return data
    except Exception as e:
        print(f"读取Excel失败 {os.path.basename(file_path)}: {e}")
        return []

def import_city(conn):
    """导入市表数据"""
    print("正在导入市表...")
    file_path = os.path.join(BASE_PATH, '地区部门信息绑定', '市表_20260611152004.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("市表数据为空")
        return 0
    
    records = []
    for row in data[1:]:
        if len(row) >= 2:
            records.append((
                safe_str(row[2] if len(row) > 2 else row[0]),  # 市名称
                safe_str(row[1] if len(row) > 1 else row[0]),   # 市编码
                safe_str(row[3] if len(row) > 3 else None, None),  # 部门
                '启用',  # 状态
                safe_str(row[6] if len(row) > 6 else None, None),
                safe_str(row[7] if len(row) > 7 else None, None),
                safe_str(row[8] if len(row) > 8 else None, None)
            ))
    
    # 先清空表
    execute_sql(conn, "DELETE FROM t_city")
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
        if len(row) >= 8:
            records.append((
                safe_str(row[6]),   # 县区编码
                safe_str(row[2] if len(row) > 2 else row[6]),  # 县区名称
                safe_str(row[7]),   # 关联市编码
                safe_str(row[4] if len(row) > 4 else None, None),  # 首字母
                safe_str(row[8] if len(row) > 8 else None, None),  # 部门
                '启用',
                safe_str(row[10] if len(row) > 10 else None, None),
                safe_str(row[11] if len(row) > 11 else None, None),
                safe_str(row[12] if len(row) > 12 else None, None)
            ))
    
    execute_sql(conn, "DELETE FROM t_county")
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
        if len(row) >= 5:
            records.append((
                safe_str(row[4]),   # 乡镇编码
                safe_str(row[1]),   # 乡镇名称
                safe_str(row[6] if len(row) > 6 else None, None),  # 县区编码
                safe_str(row[3] if len(row) > 3 else None, None),  # 首字母
                safe_str(row[7] if len(row) > 7 else None, None),  # 收购站
                '启用',
                safe_str(row[9] if len(row) > 9 else None, None),
                safe_str(row[10] if len(row) > 10 else None, None),
                safe_str(row[11] if len(row) > 11 else None, None)
            ))
    
    execute_sql(conn, "DELETE FROM t_township")
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
        if len(row) >= 7:
            records.append((
                safe_str(row[6]),   # 村编码
                safe_str(row[2] if len(row) > 2 else row[6]),  # 村名称
                safe_str(row[7] if len(row) > 7 else None, None),  # 乡镇编码
                safe_str(row[5] if len(row) > 5 else None, None),  # 首字母
                '启用',
                safe_str(row[9] if len(row) > 9 else None, None),
                safe_str(row[10] if len(row) > 10 else None, None),
                safe_str(row[11] if len(row) > 11 else None, None)
            ))
    
    execute_sql(conn, "DELETE FROM t_village")
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
        if len(row) >= 4 and row[3]:
            records.append((
                safe_str(row[3]),   # 项目类型
                safe_str(row[2] if len(row) > 2 else None, None),
                '启用',
                safe_str(row[5] if len(row) > 5 else None, None),
                safe_str(row[6] if len(row) > 6 else None, None),
                safe_str(row[7] if len(row) > 7 else None, None)
            ))
    
    execute_sql(conn, "DELETE FROM t_project_type")
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
    idx = {str(h).strip(): i for i, h in enumerate(headers) if h}
    print(f"烤房项目表共 {len(data)-1} 条数据")
    
    records = []
    for row in data[1:]:
        try:
            def get_val(key, default=None):
                i = idx.get(key, -1)
                if i >= 0 and i < len(row):
                    return row[i]
                return default
            
            project_code = get_val('项目编号')
            if not project_code or str(project_code).strip() == '':
                continue
            
            record = (
                safe_str(project_code),
                safe_str(get_val('使用状态'), '在用'),
                safe_int(get_val('闲置年份数（单位：年）'), 0),
                safe_int(get_val('转用年度')),
                safe_str(get_val('转用用途'), None),
                safe_int(get_val('损毁年度')),
                safe_str(get_val('损毁原因'), None),
                safe_str(get_val('建设方式'), None),
                safe_str(get_val('项目类型'), None),
                safe_float(get_val('长')),
                safe_float(get_val('宽')),
                safe_float(get_val('高')),
                safe_float(get_val('工程造价（元）') or get_val('工程造价')),
                safe_float(get_val('国家局补贴')),
                safe_float(get_val('产区补贴')),
                safe_float(get_val('补贴总额')),
                safe_str(get_val('开工时间'), None),
                safe_str(get_val('竣工时间'), None),
                safe_str(get_val('市'), None),
                safe_str(get_val('市编码'), None),
                safe_str(get_val('县（市、区）'), None),
                safe_str(get_val('县市编码'), None),
                safe_str(get_val('乡（镇）'), None),
                safe_str(get_val('乡镇编码'), None),
                safe_str(get_val('村'), None),
                safe_str(get_val('村编码'), None),
                safe_str(get_val('详细地址'), None),
                safe_str(get_val('项目业主'), None),
                safe_str(get_val('施工单位'), None),
                safe_float(get_val('海拔')),
                safe_float(get_val('经度')),
                safe_float(get_val('纬度')),
                1 if get_val('加热设备是否填写') == '是' else 0,
                1 if get_val('散热器是否填写') == '是' else 0,
                1 if get_val('自控设备是否填写') == '是' else 0,
                1 if get_val('烤房主体是否填写') == '是' else 0,
                1 if get_val('附属设施是否填写') == '是' else 0,
                safe_str(get_val('关联收购站'), None),
                safe_str(get_val('当前流程状态'), '待审核'),
                safe_str(get_val('申请人'), None),
                safe_str(get_val('申请时间'), None),
                safe_str(get_val('更新时间'), None),
            )
            records.append(record)
        except Exception as e:
            continue
    
    execute_sql(conn, "DELETE FROM t_barn_project")
    sql = """INSERT INTO t_barn_project (
        project_code, use_status, idle_years, transfer_year, transfer_use,
        damage_year, damage_reason, build_mode, project_type, length_m, width_m, height_m,
        project_cost, subsidy_national, subsidy_region, subsidy_total, start_date, complete_date,
        city_name, city_code, county_name, county_code, township_name, township_code,
        village_name, village_code, address, project_owner, construction_unit, elevation,
        longitude, latitude, has_heating, has_radiator, has_autocontrol, has_main, has_ancillary,
        station_code, status, applicant, apply_time, update_time
    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"""
    
    batch_size = 500
    total = 0
    for i in range(0, len(records), batch_size):
        batch = records[i:i+batch_size]
        count = execute_sql(conn, sql, batch)
        total += count
        print(f"  已导入: {min(i+batch_size, len(records))}/{len(records)}")
    
    print(f"烤房项目基础信息表导入完成: {total} 条")
    return total

def import_burner(conn):
    """导入燃烧机信息表"""
    print("正在导入燃烧机信息表...")
    file_path = os.path.join(BASE_PATH, '数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx')
    data = read_excel_simple(file_path)
    if len(data) < 2:
        print("燃烧机信息表数据为空")
        return 0
    
    headers = data[0]
    idx = {str(h).strip(): i for i, h in enumerate(headers) if h}
    print(f"燃烧机表共 {len(data)-1} 条数据")
    
    records = []
    for row in data[1:]:
        try:
            burner_code = get_val_local(idx, row, '燃烧机编码')
            if not burner_code:
                continue
            
            record = (
                safe_str(burner_code),
                safe_str(get_val_local(idx, row, '关联项目编码'), None),
                safe_str(get_val_local(idx, row, '加热设备类型'), None),
                safe_float(get_val_local(idx, row, '工程造价（元）')),
                safe_float(get_val_local(idx, row, '补贴总额')),
                safe_float(get_val_local(idx, row, '国家局补贴')),
                safe_float(get_val_local(idx, row, '产区补贴')),
                safe_int(get_val_local(idx, row, '年度')),
                safe_str(get_val_local(idx, row, '施工（供应）单位'), None),
                safe_str(get_val_local(idx, row, '是否已关联项目编码'), '否'),
                safe_str(get_val_local(idx, row, '当前流程状态'), '待审核'),
                safe_str(get_val_local(idx, row, '申请人'), None),
                safe_str(get_val_local(idx, row, '申请时间'), None),
                safe_str(get_val_local(idx, row, '更新时间'), None),
            )
            records.append(record)
        except:
            continue
    
    def get_val_local(idx_dict, row_data, key):
        i = idx_dict.get(key, -1)
        if i >= 0 and i < len(row_data):
            return row_data[i]
        return None
    
    execute_sql(conn, "DELETE FROM t_burner")
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
        print(f"  已导入: {min(i+batch_size, len(records))}/{len(records)}")
    
    print(f"燃烧机信息表导入完成: {total} 条")
    return total

def main():
    print("=" * 50)
    print("烤房管理系统 - Excel数据导入（修复版）")
    print("=" * 50)
    
    try:
        conn = get_connection()
        print("数据库连接成功!")
        
        import_city(conn)
        import_county(conn)
        import_township(conn)
        import_village(conn)
        import_project_type(conn)
        import_barn_project(conn)
        import_burner(conn)
        
        print("=" * 50)
        print("所有数据导入完成!")
        
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

if __name__ == '__main__':
    main()
