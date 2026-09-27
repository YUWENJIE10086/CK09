# -*- coding: utf-8 -*-
"""
烤房管理系统 - 数据完整导入（修正版）
"""
import os
import mysql.connector
import openpyxl
import random
from datetime import datetime

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', '3306')),
    'user': os.environ['DB_USER'],
    'password': os.environ['DB_PASS'],
    'database': os.getenv('DB_NAME', 'barn_management'),
    'charset': 'utf8mb4'
}

BASE_PATH = os.getenv('BASE_DATA_PATH', os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), '基础数据'))

def get_conn():
    return mysql.connector.connect(**DB_CONFIG)

def read_xlsx(subdir, filename):
    path = os.path.join(BASE_PATH, subdir, filename)
    wb = openpyxl.load_workbook(path, data_only=True)
    ws = wb.active
    rows = []
    for row in ws.iter_rows(values_only=True):
        if any(cell is not None for cell in row):
            rows.append(list(row))
    return rows

def v(row, idx):
    if idx < len(row):
        return row[idx]
    return None

def sv(row, idx, default=''):
    val = v(row, idx)
    if val is None or str(val).strip() == '':
        return default
    return str(val).strip()

def iv(row, idx, default=0):
    val = v(row, idx)
    if val is None or str(val).strip() == '':
        return default
    try:
        return int(float(val))
    except:
        return default

def fv(row, idx, default=None):
    val = v(row, idx)
    if val is None or str(val).strip() == '':
        return default
    try:
        return float(val)
    except:
        return default

def clean_tables():
    print('清空业务表...')
    conn = get_conn()
    cur = conn.cursor()
    for t in ['evaluation_record', 'reservation_record', 'repair_record', 'barn_component',
              'barn_burner', 'barn_project', 'maintenance_team', 'fund_management',
              'sys_project_type', 'sys_village', 'sys_township', 'sys_county', 'sys_city']:
        try:
            cur.execute(f'TRUNCATE TABLE {t}')
        except Exception as e:
            try:
                cur.execute(f'DELETE FROM {t}')
            except:
                pass
    conn.commit()
    cur.close()
    conn.close()

def import_city():
    print('导入市表...')
    data = read_xlsx('地区部门信息绑定', '市表_20260611152004.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        code = sv(row, 2)  # 市编码
        name = sv(row, 1)  # 市名称
        dept = sv(row, 3, None)  # 关联市级部门
        if code and name:
            cur.execute('INSERT INTO sys_city (city_code, city_name, dept_name) VALUES (%s, %s, %s)',
                       (code, name, dept))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_county():
    print('导入县区表...')
    data = read_xlsx('地区部门信息绑定', '县（市、区）表_20260611152048.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        code = sv(row, 6)  # 县（市、区）编码
        name = sv(row, 2)  # 县（市、区）名称
        city_code = sv(row, 7)  # 关联市编码
        dept = sv(row, 8, None)  # 关联县级部门
        if code and name:
            cur.execute('INSERT INTO sys_county (county_code, county_name, city_code, dept_name) VALUES (%s, %s, %s, %s)',
                       (code, name, city_code, dept))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_township():
    print('导入乡镇表...')
    data = read_xlsx('地区部门信息绑定', '乡（镇）-收购站表_20260611152116.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        code = sv(row, 5)  # 乡（镇）编码
        name = sv(row, 1)  # 乡（镇）名称
        county_code = sv(row, 6)  # 关联县（市）编码
        station = sv(row, 7, None)  # 关联烟叶收购站
        if code and name:
            cur.execute('INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES (%s, %s, %s, %s)',
                       (code, name, county_code, station))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_village():
    print('导入村表...')
    data = read_xlsx('地区部门信息绑定', '村表_20260611152136.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    batch = []
    for row in data[1:]:
        code = sv(row, 6)  # 村编码
        name = sv(row, 2)  # 村名称
        town_code = sv(row, 7)  # 关联乡（镇）编码
        if code and name:
            batch.append((code, name, town_code))

    cur.executemany('INSERT INTO sys_village (village_code, village_name, town_code) VALUES (%s, %s, %s)', batch)
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(batch)} 条')

def import_project_type():
    print('导入项目类型表...')
    data = read_xlsx('数据表结构', '烤房-项目类型_20260611152240.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        name = sv(row, 3)  # 项目类型
        sort = sv(row, 2, '0')  # 排序号
        if name:
            cur.execute('INSERT INTO sys_project_type (type_name, sort_order) VALUES (%s, %s)',
                       (name, sort))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_barn_project():
    print('导入烤房项目表（核心数据，2642条）...')
    data = read_xlsx('数据表结构', '烤房-项目基础信息表_20260611152444.xlsx')
    conn = get_conn()
    cur = conn.cursor()

    batch = []
    total = 0
    for row in data[1:]:
        project_code = sv(row, 1)  # 项目编号
        if not project_code:
            continue

        use_status = sv(row, 2, '在用')  # 使用状态
        idle_years = iv(row, 3, 0)  # 闲置年份数
        county_name = sv(row, 20)  # 县（市、区）
        town_name = sv(row, 21)  # 乡（镇）
        village_name = sv(row, 22)  # 村

        # 计算健康评分
        base_score = 95
        if use_status == '闲置':
            base_score -= (5 + idle_years * 2)
        elif use_status == '转用':
            base_score = 60
        elif use_status == '损毁':
            base_score = 25

        # 基于项目编号hash产生随机偏移
        h = abs(hash(project_code)) % 100
        health_score = max(20, min(100, base_score - (h % 25)))

        if health_score >= 85:
            health_level = '优良'
        elif health_score >= 65:
            health_level = '需维护'
        elif health_score >= 40:
            health_level = '急需修复'
        else:
            health_level = '退出'

        if health_score >= 90:
            predicted = 8.0
        elif health_score >= 70:
            predicted = 5.0
        elif health_score >= 40:
            predicted = 2.0
        else:
            predicted = 0.5

        barn_name = f'{county_name}-{town_name}-{village_name}-{project_code}'

        batch.append((
            project_code, barn_name, use_status, idle_years,
            iv(row, 4), sv(row, 5, None), iv(row, 6), sv(row, 7, None),
            sv(row, 8, None), sv(row, 9, None),
            fv(row, 10), fv(row, 11), fv(row, 12),
            fv(row, 13), fv(row, 14), fv(row, 15), fv(row, 16),
            sv(row, 17, None), sv(row, 18, None),
            sv(row, 34, None), sv(row, 19, None),  # 市编码, 市
            sv(row, 35, None), county_name,           # 县市编码, 县名称
            sv(row, 36, None), town_name,             # 乡镇编码, 乡镇名称
            sv(row, 37, None), village_name,          # 村编码, 村名称
            sv(row, 23, None),                        # 详细地址
            sv(row, 24, None),                        # 项目业主
            sv(row, 25, None),                        # 施工单位
            fv(row, 26), fv(row, 27), fv(row, 28),    # 海拔, 经度, 纬度
            1 if sv(row, 29) == '是' else 0,          # has_heating
            1 if sv(row, 30) == '是' else 0,          # has_radiator
            1 if sv(row, 31) == '是' else 0,          # has_autocontrol
            1 if sv(row, 32) == '是' else 0,          # has_main
            1 if sv(row, 33) == '是' else 0,          # has_ancillary
            sv(row, 38, None),                        # 收购站名称
            round(health_score, 2), health_level, predicted,
            1 if use_status == '在用' else 0, '已通过'
        ))

        if len(batch) >= 200:
            # 44 列，对应 44 个值
            sql = '''INSERT INTO barn_project (
                project_code, barn_name, use_status, idle_years,
                transfer_year, transfer_use, damage_year, damage_reason,
                build_mode, project_type, length_m, width_m, height_m,
                project_cost, subsidy_national, subsidy_region, subsidy_total,
                start_date, complete_date, city_code, city_name, county_code, county_name,
                town_code, town_name, village_code, village_name, address, project_owner,
                construction_unit, elevation, longitude, latitude,
                has_heating, has_radiator, has_autocontrol, has_main, has_ancillary,
                station_name, health_score, health_level, predicted_life_years, is_idle, status
            ) VALUES (
                %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
            )'''
            try:
                cur.executemany(sql, batch)
                conn.commit()
                total += len(batch)
                print(f'  已导入: {total}')
            except mysql.connector.Error as e:
                print(f'  错误: {e}, batch大小: {len(batch)}, 字段数: {len(batch[0])}')
                raise
            batch = []

    if batch:
        sql = '''INSERT INTO barn_project (
            project_code, barn_name, use_status, idle_years,
            transfer_year, transfer_use, damage_year, damage_reason,
            build_mode, project_type, length_m, width_m, height_m,
            project_cost, subsidy_national, subsidy_region, subsidy_total,
            start_date, complete_date, city_code, city_name, county_code, county_name,
            town_code, town_name, village_code, village_name, address, project_owner,
            construction_unit, elevation, longitude, latitude,
            has_heating, has_radiator, has_autocontrol, has_main, has_ancillary,
            station_name, health_score, health_level, predicted_life_years, is_idle, status
        ) VALUES (
            %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
        )'''
        cur.executemany(sql, batch)
        conn.commit()
        total += len(batch)

    cur.close()
    conn.close()
    print(f'  完成: 共 {total} 条烤房数据')

def import_burner():
    print('导入燃烧机表（1109条）...')
    data = read_xlsx('数据表结构', '烤房-燃烧机信息表_20260611152604.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    batch = []
    total = 0
    for row in data[1:]:
        burner_code = sv(row, 1)
        if not burner_code:
            continue
        batch.append((
            burner_code, sv(row, 2, None), sv(row, 3, None),
            fv(row, 4), fv(row, 5), fv(row, 6), fv(row, 7),
            iv(row, 8), sv(row, 9, None), '已通过'
        ))

        if len(batch) >= 300:
            cur.executemany('''INSERT INTO barn_burner
                (burner_code, project_code, heating_type, project_cost, subsidy_total,
                 subsidy_national, subsidy_region, build_year, construction_unit, status)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)''', batch)
            conn.commit()
            total += len(batch)
            print(f'  已导入: {total}')
            batch = []

    if batch:
        cur.executemany('''INSERT INTO barn_burner
            (burner_code, project_code, heating_type, project_cost, subsidy_total,
             subsidy_national, subsidy_region, build_year, construction_unit, status)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)''', batch)
        conn.commit()
        total += len(batch)

    cur.close()
    conn.close()
    print(f'  完成: 共 {total} 条燃烧机数据')

def import_components():
    print('生成烤房部件评分数据...')
    conn = get_conn()
    cur = conn.cursor()
    cur.execute('SELECT barn_id, health_score FROM barn_project ORDER BY barn_id')
    barns = cur.fetchall()

    components_def = [
        ('加热设备', '加热炉', 0.20),
        ('散热器', '散热管', 0.15),
        ('自控设备', '温度传感器', 0.15),
        ('自控设备', '循环风机', 0.15),
        ('主体结构', '墙体屋顶', 0.15),
        ('主体结构', '挂烟梁柱', 0.10),
        ('附属设施', '烟棚', 0.10),
    ]

    batch = []
    for barn_id, health_score in barns:
        for comp_type, comp_name, weight in components_def:
            variance = (hash(str(barn_id) + comp_name) % 21) - 10
            comp_score = max(20, min(100, health_score + variance))

            if comp_score >= 80:
                status = '正常'
                damage = '无'
            elif comp_score >= 60:
                status = '轻微损坏'
                damage = '轻度'
            elif comp_score >= 40:
                status = '严重损坏'
                damage = '重度'
            else:
                status = '无法使用'
                damage = '严重'

            batch.append((barn_id, comp_type, comp_name, status, damage, round(comp_score, 2), weight))

    sql = '''INSERT INTO barn_component (barn_id, component_type, component_name, component_status, damage_level, score, weight)
             VALUES (%s,%s,%s,%s,%s,%s,%s)'''

    total = 0
    for i in range(0, len(batch), 500):
        cur.executemany(sql, batch[i:i+500])
        conn.commit()
        total += min(500, len(batch) - i)
        if total % 2000 == 0:
            print(f'  已导入部件: {total}/{len(batch)}')

    cur.close()
    conn.close()
    print(f'  完成: 共 {total} 条部件数据')

def import_repair_records():
    print('生成维修记录数据...')
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("SELECT barn_id, project_code, barn_name, health_score, county_name FROM barn_project WHERE health_score < 85 ORDER BY health_score ASC LIMIT 350")
    barns = cur.fetchall()

    batch = []
    for barn_id, project_code, barn_name, health_score, county_name in barns:
        if health_score < 50:
            repair_type = '紧急维修'
            urgency = '紧急'
        elif health_score < 70:
            repair_type = '常规维修'
            urgency = '正常'
        else:
            repair_type = '保养维护'
            urgency = '低'

        r = abs(hash(str(barn_id))) % 100
        if r < 40:
            status = '已验收'
        elif r < 70:
            status = '实施中'
        elif r < 90:
            status = '待审核'
        else:
            status = '已取消'

        batch.append((
            barn_id, project_code, barn_name, repair_type, urgency,
            '技术维护员',
            f'烤房检测发现部件老化，需要进行{repair_type}',
            round(1000 + r * 80, 2), round(900 + r * 78, 2),
            status, '审核员', f'{county_name}维修队',
            round(75 + r * 0.2, 2)
        ))

    sql = '''INSERT INTO repair_record
        (barn_id, project_code, barn_name, repair_type, urgency, applicant, apply_desc,
         estimated_cost, actual_cost, repair_status, auditor, implement_team, roi_score)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''

    cur.executemany(sql, batch)
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: 共 {len(batch)} 条维修记录')

def import_reservations():
    print('生成预约记录数据...')
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("SELECT barn_id, project_code, barn_name, county_name FROM barn_project WHERE is_idle = 1 AND use_status = '在用' ORDER BY barn_id LIMIT 300")
    barns = cur.fetchall()

    user_names = ['张烟农', '李烟农', '王烟农', '陈烟农', '赵烟农', '刘烟农', '周烟农', '黄烟农']
    batch = []
    today = datetime.now()

    for idx, (barn_id, project_code, barn_name, county_name) in enumerate(barns):
        day = (idx % 55) + 1
        month = ((idx // 28) % 3) + today.month
        if month > 12: month = 12
        year = today.year
        day_str = min(day, 28)

        reserve_date = f'{year}-{str(month).zfill(2)}-{str(day_str).zfill(2)}'

        sr = idx % 10
        if sr < 2:
            status = '待审核'
        elif sr < 4:
            status = '使用中'
        elif sr < 9:
            status = '已完成'
        else:
            status = '已取消'

        batch.append((
            barn_id, project_code, barn_name, 4,
            user_names[idx % len(user_names)],
            f'138{str(10000000 + (idx * 137) % 89999999).zfill(8)}',
            reserve_date,
            f'{reserve_date} 08:00:00',
            f'{reserve_date} 20:00:00',
            round(200 + (idx * 17) % 300, 2),
            status, '审核员', '同意预约使用',
            f'{reserve_date} 08:00:00',
            f'{reserve_date} 19:30:00',
            1 if idx % 15 == 0 else 0
        ))

    sql = '''INSERT INTO reservation_record
        (barn_id, project_code, barn_name, user_id, user_name, user_phone, reserve_date,
         reserve_start, reserve_end, leaf_weight, reserve_status, reviewer, review_opinion,
         actual_start, actual_end, is_timeout)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''

    cur.executemany(sql, batch)
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: 共 {len(batch)} 条预约记录')

def import_evaluations():
    print('生成评价数据...')
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("SELECT reservation_id, barn_id, project_code, barn_name, user_name FROM reservation_record WHERE reserve_status = '已完成' ORDER BY reservation_id LIMIT 200")
    reservations = cur.fetchall()

    baker_names = ['刘师傅', '张师傅', '王师傅', '陈师傅', '赵师傅', '李师傅']
    barn_comments = ['烤房运行稳定，温控精准', '排湿稍慢，整体良好', '设备正常，烘烤质量好',
                     '密封性一般，需要改进', '整体状况良好', '新建烤房性能优秀', '风机噪音偏大，需维护']
    baker_comments = ['操作熟练，经验丰富', '服务态度好', '时间观念强', '沟通顺畅，配合度高', '需加强培训']

    batch = []
    for idx, (res_id, barn_id, project_code, barn_name, user_name) in enumerate(reservations):
        barn_score = 4 + (idx % 2)
        baker_score = 4 + ((idx + 1) % 2)
        batch.append((
            res_id, barn_id, project_code, barn_name, 4, user_name,
            baker_names[idx % len(baker_names)],
            barn_score, baker_score,
            barn_comments[idx % len(barn_comments)],
            baker_comments[idx % len(baker_comments)]
        ))

    sql = '''INSERT INTO evaluation_record
        (reservation_id, barn_id, project_code, barn_name, evaluator_id, evaluator_name,
         baker_name, barn_score, baker_score, barn_comment, baker_comment)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''

    cur.executemany(sql, batch)
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: 共 {len(batch)} 条评价记录')

def import_teams_and_funds():
    print('导入管护队伍和资金管理数据...')
    conn = get_conn()
    cur = conn.cursor()

    teams = [
        ('秭归县烘烤技术服务队', '烘烤技术服务队', 'XS-ZGX-00005', '秭归县', '杜勇', '13900139001', 12, '秭归县全域', 85),
        ('秭归县简易维修服务队', '简易维修服务队', 'XS-ZGX-00005', '秭归县', '郑刚', '13900139002', 8, '秭归县各烤房点', 65),
        ('秭归县常规管护服务队', '常规管护服务队', 'XS-ZGX-00005', '秭归县', '黄志强', '13900139003', 15, '秭归县烟叶产区', 92),
        ('秭归县综合管理服务队', '综合管理服务队', 'XS-ZGX-00005', '秭归县', '李建华', '13900139004', 6, '秭归县合作社直属', 40),
        ('兴山县烘烤技术服务队', '烘烤技术服务队', 'XS-XSX-00002', '兴山县', '王永明', '13900139005', 10, '兴山县全域', 58),
        ('兴山县简易维修服务队', '简易维修服务队', 'XS-XSX-00002', '兴山县', '陈大伟', '13900139006', 7, '兴山县各烤房点', 42),
        ('兴山县常规管护服务队', '常规管护服务队', 'XS-XSX-00002', '兴山县', '刘建国', '13900139007', 12, '兴山县烟叶产区', 68),
    ]
    cur.executemany('''INSERT INTO maintenance_team
        (team_name, team_type, county_code, county_name, leader_name, leader_phone, member_count, service_area, repair_count)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)''', teams)

    funds = [
        (2026, 'XS-ZGX-00005', '秭归县', '行业管护资金', 500000.00, 320000.00, 150000.00, 42, 82.5),
        (2026, 'XS-ZGX-00005', '秭归县', '政府管护资金', 300000.00, 180000.00, 100000.00, 28, 78.3),
        (2026, 'XS-ZGX-00005', '秭归县', '合作社专项', 200000.00, 120000.00, 50000.00, 15, 85.2),
        (2026, 'XS-XSX-00002', '兴山县', '行业管护资金', 400000.00, 280000.00, 100000.00, 35, 80.1),
    ]
    cur.executemany('''INSERT INTO fund_management
        (fund_year, county_code, county_name, fund_source, total_amount, used_amount, allocated_amount, repair_count, avg_roi)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)''', funds)
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(teams)} 支队伍，{len(funds)} 条资金记录')

def show_stats():
    print('\n' + '=' * 50)
    print('数据导入统计:')
    print('=' * 50)
    conn = get_conn()
    cur = conn.cursor()
    tables = ['sys_user', 'sys_city', 'sys_county', 'sys_township', 'sys_village',
              'sys_project_type', 'barn_project', 'barn_burner', 'barn_component',
              'repair_record', 'reservation_record', 'evaluation_record',
              'maintenance_team', 'fund_management']
    for t in tables:
        cur.execute(f'SELECT COUNT(*) FROM {t}')
        c = cur.fetchone()[0]
        print(f'  {t}: {c}')

    # 额外统计
    cur.execute("SELECT health_level, COUNT(*) FROM barn_project GROUP BY health_level")
    print('\n健康等级分布:')
    for level, cnt in cur.fetchall():
        print(f'  {level}: {cnt}')

    cur.execute("SELECT county_name, COUNT(*) FROM barn_project GROUP BY county_name ORDER BY COUNT(*) DESC LIMIT 5")
    print('\nTOP 5 县区烤房数量:')
    for name, cnt in cur.fetchall():
        print(f'  {name}: {cnt}')

    cur.close()
    conn.close()

def main():
    print('=' * 50)
    print('烤房管理系统 - 数据完整导入')
    print('=' * 50)

    clean_tables()
    import_city()
    import_county()
    import_township()
    import_village()
    import_project_type()
    import_barn_project()
    import_burner()
    import_components()
    import_repair_records()
    import_reservations()
    import_evaluations()
    import_teams_and_funds()
    show_stats()
    print('\n=== 全部数据导入完成! ===')

if __name__ == '__main__':
    main()
