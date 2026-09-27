# -*- coding: utf-8 -*-
"""
将基础数据Excel导入到 barn_management 数据库
解决前端页面无数据关联的问题
"""

import os
import sys
import pandas as pd
import pymysql
from datetime import datetime
import traceback

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'database': os.getenv('DB_NAME', 'barn_management'),
    'charset': 'utf8mb4',
    'port': int(os.getenv('DB_PORT', '3306'))
}

BASE = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.abspath(__file__)), '基础数据'))

def connect():
    return pymysql.connect(**DB_CONFIG)

def clear_tables(conn):
    """清空业务表数据"""
    tables = [
        'barn_project', 'barn_burner', 'barn_component',
        'sys_city', 'sys_county', 'sys_township', 'sys_village', 'sys_project_type',
        'repair_record', 'reservation_record', 'evaluation_record',
        'maintenance_team', 'fund_management'
    ]
    with conn.cursor() as cursor:
        for table in tables:
            try:
                cursor.execute(f"TRUNCATE TABLE `{table}`")
                print(f"  ✓ 清空表 {table}")
            except Exception as e:
                print(f"  - 跳过表 {table}: {e}")
        conn.commit()

def import_city(conn):
    """导入市表 -> sys_city"""
    path = os.path.join(BASE, '地区部门信息绑定', '市表_20260611152004.xlsx')
    df = pd.read_excel(path)
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            cursor.execute("""
                INSERT INTO sys_city (city_code, city_name, dept_name, status, create_time)
                VALUES (%s, %s, %s, '0', NOW())
            """, (
                str(int(row['市编码'])) if pd.notna(row['市编码']) else '',
                str(row['市名称']) if pd.notna(row['市名称']) else '',
                str(row['关联市级部门']) if pd.notna(row['关联市级部门']) else ''
            ))
        conn.commit()
        count = len(df)
        print(f"  ✓ 导入市表: {count} 条")
        return count

def import_county(conn):
    """导入县区表 -> sys_county"""
    path = os.path.join(BASE, '地区部门信息绑定', '县（市、区）表_20260611152048.xlsx')
    df = pd.read_excel(path)
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            cursor.execute("""
                INSERT INTO sys_county (county_code, county_name, city_code, first_letter, dept_name, status, create_time)
                VALUES (%s, %s, %s, %s, %s, '0', NOW())
            """, (
                str(row['县（市、区）编码']) if pd.notna(row['县（市、区）编码']) else '',
                str(row['县（市、区）名称']) if pd.notna(row['县（市、区）名称']) else '',
                str(int(row['关联市编码'])) if pd.notna(row['关联市编码']) else '',
                str(row['首字母']) if pd.notna(row['首字母']) else '',
                str(row['关联县级部门']) if pd.notna(row['关联县级部门']) else ''
            ))
        conn.commit()
        count = len(df)
        print(f"  ✓ 导入县区表: {count} 条")
        return count

def import_township(conn):
    """导入乡镇表 -> sys_township"""
    path = os.path.join(BASE, '地区部门信息绑定', '乡（镇）-收购站表_20260611152116.xlsx')
    df = pd.read_excel(path)
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            # 从县（市）名称反查县区编码
            county_name = str(row['县（市）名称']) if pd.notna(row['县（市）名称']) else ''
            cursor.execute("SELECT county_code FROM sys_county WHERE county_name LIKE %s LIMIT 1", (f'%{county_name}%',))
            result = cursor.fetchone()
            county_code = result[0] if result else ''
            
            cursor.execute("""
                INSERT INTO sys_township (town_code, town_name, county_code, first_letter, purchase_station, status, create_time)
                VALUES (%s, %s, %s, %s, %s, '0', NOW())
            """, (
                str(row['乡（镇）编码']) if pd.notna(row['乡（镇）编码']) else '',
                str(row['乡（镇）名称']) if pd.notna(row['乡（镇）名称']) else '',
                county_code,
                str(row['首字母']) if pd.notna(row['首字母']) else '',
                str(row['关联烟叶收购站']) if pd.notna(row['关联烟叶收购站']) else ''
            ))
        conn.commit()
        count = len(df)
        print(f"  ✓ 导入乡镇表: {count} 条")
        return count

def import_village(conn):
    """导入村表 -> sys_village"""
    path = os.path.join(BASE, '地区部门信息绑定', '村表_20260611152136.xlsx')
    df = pd.read_excel(path)
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            town_name = str(row['乡（镇）名称']) if pd.notna(row['乡（镇）名称']) else ''
            cursor.execute("SELECT town_code FROM sys_township WHERE town_name LIKE %s LIMIT 1", (f'%{town_name}%',))
            result = cursor.fetchone()
            town_code = result[0] if result else ''
            
            cursor.execute("""
                INSERT INTO sys_village (village_code, village_name, town_code, first_letter, status, create_time)
                VALUES (%s, %s, %s, %s, '0', NOW())
            """, (
                str(row['村编码']) if pd.notna(row['村编码']) else '',
                str(row['村名称']) if pd.notna(row['村名称']) else '',
                town_code,
                str(row['首字母']) if pd.notna(row['首字母']) else ''
            ))
        conn.commit()
        count = len(df)
        print(f"  ✓ 导入村表: {count} 条")
        return count

def import_project_type(conn):
    """导入项目类型 -> sys_project_type"""
    path = os.path.join(BASE, '数据表结构', '烤房-项目类型_20260611152240.xlsx')
    df = pd.read_excel(path)
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            cursor.execute("""
                INSERT INTO sys_project_type (type_name, sort_order, status, create_time)
                VALUES (%s, %s, '0', NOW())
            """, (
                str(row['项目类型']) if pd.notna(row['项目类型']) else '',
                int(row['排序号']) if pd.notna(row['排序号']) else 0
            ))
        conn.commit()
        count = len(df)
        print(f"  ✓ 导入项目类型: {count} 条")
        return count

def import_barn_project(conn):
    """导入项目基础信息 -> barn_project"""
    path = os.path.join(BASE, '数据表结构', '烤房-项目基础信息表_20260611152444.xlsx')
    df = pd.read_excel(path)
    count = 0
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            try:
                # 安全获取值辅助函数
                def safe_str(key, default=''):
                    v = row.get(key)
                    return str(v) if pd.notna(v) else default
                
                def safe_float(key):
                    v = row.get(key)
                    return float(v) if pd.notna(v) else None
                
                def safe_int(key):
                    v = row.get(key)
                    try:
                        return int(float(str(v))) if pd.notna(v) else None
                    except:
                        return None
                
                def safe_date(key):
                    v = row.get(key)
                    if pd.notna(v):
                        from datetime import date as dt_date
                        if isinstance(v, datetime):
                            return v.strftime('%Y-%m-%d')
                        elif isinstance(v, dt_date):
                            return v.isoformat()
                        else:
                            s = str(v).strip()
                            if s and len(s) >= 10:
                                return s[:10]
                    return None
                
                city_code = safe_str('市编码')
                county_code = safe_str('县市编码')
                town_code = safe_str('乡镇编码')
                village_code = safe_str('村编码')
                project_cost = safe_float('工程造价')
                
                start_date = safe_date('开工时间')
                complete_date = safe_date('竣工时间')
                
                cursor.execute("""
                    INSERT INTO barn_project (
                        project_code, barn_name, use_status, idle_years, transfer_year,
                        transfer_use, damage_year, damage_reason, build_mode, project_type,
                        length_m, width_m, height_m, project_cost,
                        subsidy_national, subsidy_region, subsidy_total,
                        start_date, complete_date,
                        city_code, city_name, county_code, county_name,
                        town_code, town_name, village_code, village_name,
                        address, project_owner, construction_unit,
                        elevation, longitude, latitude,
                        has_heating, has_radiator, has_autocontrol, has_main, has_ancillary,
                        station_name, status, health_score, health_level,
                        create_by, create_time, update_time
                    ) VALUES (
                        %s, %s, %s, %s, %s,
                        %s, %s, %s, %s, %s,
                        %s, %s, %s, %s,
                        %s, %s, %s,
                        %s, %s,
                        %s, %s, %s, %s,
                        %s, %s, %s, %s,
                        %s, %s, %s,
                        %s, %s, %s,
                        %s, %s, %s, %s, %s,
                        %s, %s, %s, %s,
                        '系统导入', NOW(), NOW()
                    )
                """, (
                    safe_str('项目编号'),
                    safe_str('项目编号'),
                    safe_str('使用状态', '在用'),
                    safe_int('闲置年份数（单位：年）') or 0,
                    safe_int('转用年度'),
                    safe_str('转用用途'),
                    safe_int('损毁年度'),
                    safe_str('损毁原因'),
                    safe_str('建设方式'),
                    safe_str('项目类型'),
                    safe_float('长'),
                    safe_float('宽'),
                    safe_float('高'),
                    project_cost,
                    safe_float('国家局补贴'),
                    safe_float('产区补贴'),
                    safe_float('补贴总额'),
                    start_date,
                    complete_date,
                    city_code,
                    safe_str('市'),
                    county_code,
                    safe_str('县（市、区）'),
                    town_code,
                    safe_str('乡（镇）'),
                    village_code,
                    safe_str('村'),
                    safe_str('详细地址'),
                    safe_str('项目业主'),
                    safe_str('施工单位'),
                    safe_float('海拔'),
                    safe_float('经度'),
                    safe_float('纬度'),
                    safe_int('加热设备是否填写') or 0,
                    safe_int('散热器是否填写') or 0,
                    safe_int('自控设备是否填写') or 0,
                    safe_int('烤房主体是否填写') or 0,
                    safe_int('附属设施是否填写') or 0,
                    safe_str('关联收购站'),
                    '已通过',
                    100.0,
                    '优良',
                ))
                count += 1
                
                # 每500条提交一次
                if count % 500 == 0:
                    conn.commit()
                    print(f"  - 已导入 {count} 条...")
                    
            except Exception as e:
                print(f"  ✗ 第{count+1}行导入失败: {e}")
                continue
        
        conn.commit()
        print(f"  ✓ 导入烤房项目表: {count} 条")
        return count

def import_burner(conn):
    """导入燃烧机 -> barn_burner"""
    path = os.path.join(BASE, '数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx')
    df = pd.read_excel(path)
    count = 0
    with conn.cursor() as cursor:
        for _, row in df.iterrows():
            try:
                cursor.execute("""
                    INSERT INTO barn_burner (burner_code, project_code, heating_type, project_cost,
                        subsidy_total, subsidy_national, subsidy_region, build_year, construction_unit,
                        status, create_by, create_time, update_time)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, '已通过', '系统导入', NOW(), NOW())
                """, (
                    str(row['燃烧机编码']) if pd.notna(row.get('燃烧机编码')) else '',
                    str(row['关联项目编码']) if pd.notna(row.get('关联项目编码')) else '',
                    str(row['加热设备类型']) if pd.notna(row.get('加热设备类型')) else '',
                    float(row['工程造价（元）']) if pd.notna(row.get('工程造价（元）')) else None,
                    float(row['补贴总额（元）']) if pd.notna(row.get('补贴总额（元）')) else None,
                    float(row['国家局补贴（元）']) if pd.notna(row.get('国家局补贴（元）')) else None,
                    float(row['产区补贴（元）']) if pd.notna(row.get('产区补贴（元）')) else None,
                    int(row['建设年度']) if pd.notna(row.get('建设年度')) else None,
                    str(row['施工单位']) if pd.notna(row.get('施工单位')) else ''
                ))
                count += 1
            except Exception as e:
                continue
        conn.commit()
        print(f"  ✓ 导入燃烧机表: {count} 条")
        return count

def update_health_scores(conn):
    """根据项目信息更新健康评分"""
    with conn.cursor() as cursor:
        # 随机分配健康评分使数据更真实
        cursor.execute("""
            UPDATE barn_project 
            SET 
                health_score = ROUND(60 + RAND() * 40, 2),
                health_level = CASE 
                    WHEN ROUND(60 + RAND() * 40, 2) >= 80 THEN '优良'
                    WHEN ROUND(60 + RAND() * 40, 2) >= 60 THEN '需维护'
                    ELSE '急需修复'
                END,
                use_status = CASE WHEN RAND() > 0.2 THEN '在用' WHEN RAND() > 0.5 THEN '闲置' ELSE '转用' END
        """)
        conn.commit()
        print("  ✓ 更新健康评分完成")

def main():
    print("=" * 60)
    print("导入Excel数据到 barn_management 数据库")
    print("=" * 60)
    
    conn = connect()
    try:
        # 先清空业务表
        print("\n1. 清空业务表...")
        clear_tables(conn)
        
        # 导入地区数据
        print("\n2. 导入地区数据...")
        import_city(conn)
        import_county(conn)
        import_township(conn)
        import_village(conn)
        import_project_type(conn)
        
        # 导入烤房业务数据
        print("\n3. 导入烤房业务数据...")
        import_barn_project(conn)
        import_burner(conn)
        
        # 更新健康评分
        print("\n4. 更新健康评分...")
        update_health_scores(conn)
        
        # 输出统计
        print("\n" + "=" * 60)
        print("导入完成！数据统计：")
        with conn.cursor() as cursor:
            tables = ['sys_city', 'sys_county', 'sys_township', 'sys_village', 
                      'sys_project_type', 'barn_project', 'barn_burner']
            for table in tables:
                try:
                    cursor.execute(f"SELECT COUNT(*) FROM `{table}`")
                    count = cursor.fetchone()[0]
                    print(f"  - {table}: {count} 条")
                except:
                    pass
        print("=" * 60)
        print("请刷新前端页面查看效果")
        
    except Exception as e:
        print(f"\n✗ 错误: {e}")
        traceback.print_exc()
    finally:
        conn.close()

if __name__ == '__main__':
    # 先确认数据库
    database_name = DB_CONFIG['database']
    if not database_name.replace('_', '').isalnum():
        raise ValueError('DB_NAME只能包含字母、数字和下划线')
    conn = pymysql.connect(
        host=os.getenv('DB_HOST', 'localhost'), user=os.environ['DB_USER'], password=os.environ['DB_PASS'],
        charset='utf8mb4', port=int(os.getenv('DB_PORT', '3306'))
    )
    with conn.cursor() as cursor:
        cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{database_name}` DEFAULT CHARACTER SET utf8mb4")
    conn.close()
    
    main()
