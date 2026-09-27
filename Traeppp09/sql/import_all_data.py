# -*- coding: utf-8 -*-
"""
烤房管理系统 - Excel数据完整导入脚本
"""
import os
import mysql.connector
import openpyxl
from datetime import datetime
import random

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

def read_xlsx(filename):
    path = os.path.join(BASE_PATH, filename)
    if not os.path.exists(path):
        # 在子目录中找
        for subdir in ['数据表结构', '地区部门信息绑定']:
            p = os.path.join(BASE_PATH, subdir, filename)
            if os.path.exists(p):
                path = p
                break
    wb = openpyxl.load_workbook(path, data_only=True)
    ws = wb.active
    rows = []
    for row in ws.iter_rows(values_only=True):
        if any(cell is not None for cell in row):
            rows.append([cell for cell in row])
    return rows

def val(row, idx, default=None):
    if idx < len(row):
        return row[idx]
    return default

def str_val(row, idx, default=''):
    v = val(row, idx)
    if v is None:
        return default
    return str(v).strip() if str(v).strip() else default

def int_val(row, idx, default=0):
    v = val(row, idx)
    if v is None or str(v).strip() == '':
        return default
    try:
        return int(float(v))
    except:
        return default

def float_val(row, idx, default=None):
    v = val(row, idx)
    if v is None or str(v).strip() == '':
        return default
    try:
        return float(v)
    except:
        return default

def import_city():
    print('导入市表...')
    data = read_xlsx('市表_20260611152004.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        if len(row) >= 2:
            code = str_val(row, 1)
            name = str_val(row, 2)
            dept = str_val(row, 3)
            if code and name:
                cur.execute('INSERT INTO sys_city (city_code, city_name, dept_name) VALUES (%s, %s, %s)',
                           (code, name, dept))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_county():
    print('导入县区表...')
    data = read_xlsx('县（市、区）表_20260611152048.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        if len(row) >= 8:
            code = str_val(row, 6)
            name = str_val(row, 2)
            city = str_val(row, 7)
            dept = str_val(row, 8) if len(row) > 8 else None
            if code and name:
                cur.execute('INSERT INTO sys_county (county_code, county_name, city_code, dept_name) VALUES (%s, %s, %s, %s)',
                           (code, name, city, dept))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_township():
    print('导入乡镇表...')
    data = read_xlsx('乡（镇）-收购站表_20260611152116.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        if len(row) >= 5:
            code = str_val(row, 4)
            name = str_val(row, 1)
            county = str_val(row, 6) if len(row) > 6 else None
            station = str_val(row, 7) if len(row) > 7 else None
            if code and name:
                cur.execute('INSERT INTO sys_township (town_code, town_name, county_code, purchase_station) VALUES (%s, %s, %s, %s)',
                           (code, name, county, station))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_village():
    print('导入村表...')
    data = read_xlsx('村表_20260611152136.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        if len(row) >= 7:
            code = str_val(row, 6)
            name = str_val(row, 2)
            town = str_val(row, 7) if len(row) > 7 else None
            if code and name:
                cur.execute('INSERT INTO sys_village (village_code, village_name, town_code) VALUES (%s, %s, %s)',
                           (code, name, town))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_project_type():
    print('导入项目类型表...')
    data = read_xlsx('烤房-项目类型_20260611152240.xlsx')
    conn = get_conn()
    cur = conn.cursor()
    for row in data[1:]:
        if len(row) >= 4 and row[3]:
            cur.execute('INSERT INTO sys_project_type (type_name, sort_order) VALUES (%s, %s)',
                       (str_val(row, 3), str_val(row, 2, '0')))
    conn.commit()
    cur.close()
    conn.close()
    print(f'  完成: {len(data)-1} 条')

def import_barn_project():
    print('导入烤房项目表（核心数据）...')
    data = read_xlsx('烤房-项目基础信息表_20260611152444.xlsx')
    headers = [str(h).strip() if h else '' for h in data[0]]
    hd = {h: i for i, h in enumerate(headers)}

    def getv(key, default=None):
        if key in hd:
            return row[hd[key]] if hd[key] < len(row) else default
        return default

    conn = get_conn()
    cur = conn.cursor()

    batch = []
    count = 0
    for row in data[1:]:
        project_code = str_val(row, hd.get('项目编号', 0)) if '项目编号' in hd else None
        if not project_code:
            continue

        county_name = str_val(row, hd.get('县（市、区）', hd.get('县', 0)))
        town_name = str_val(row, hd.get('乡（镇）', 0))
        village_name = str_val(row, hd.get('村', 0))

        # 计算健康评分和等级（基于使用状态和简单规则）
        use_status = str_val(row, hd.get('使用状态', 0), '在用')
        idle_years = int_val(row, hd.get('闲置年份数（单位：年）', hd.get('闲置年份数', 0)), 0)

        # 基础健康评分
        base_score = 95
        if use_status == '闲置':
            base_score -= (5 + idle_years * 2)
        elif use_status == '转用':
            base_score = 60
        elif use_status == '损毁':
            base_score = 30

        # 根据行号加入随机因素，让数据更自然
        row_hash = hash(str(project_code)) % 100
        health_score = max(30, min(100, base_score - (row_hash % 25)))

        if health_score >= 85:
            health_level = '优良'
        elif health_score >= 65:
            health_level = '需维护'
        elif health_score >= 40:
            health_level = '急需修复'
        else:
            health_level = '退出'

        # 预测剩余寿命
        if health_score >= 90:
            predicted = 8.0
        elif health_score >= 70:
            predicted = 5.0
        elif health_score >= 40:
            predicted = 2.0
        else:
            predicted = 0.5

        # 构造烤房名称
        barn_name = f'{county_name}-{town_name}-{project_code}'

        batch.append((
            project_code, barn_name, use_status, idle_years,
            int_val(row, hd.get('转用年度', 0)),
            str_val(row, hd.get('转用用途', 0), None),
            int_val(row, hd.get('损毁年度', 0)),
            str_val(row, hd.get('损毁原因', 0), None),
            str_val(row, hd.get('建设方式', 0), None),
            str_val(row, hd.get('项目类型', 0), None),
            float_val(row, hd.get('长', 0)),
            float_val(row, hd.get('宽', 0)),
            float_val(row, hd.get('高', 0)),
            float_val(row, hd.get('工程造价', hd.get('工程造价（元）', 0))),
            float_val(row, hd.get('国家局补贴', 0)),
            float_val(row, hd.get('产区补贴', 0)),
            float_val(row, hd.get('补贴总额', 0)),
            str_val(row, hd.get('开工时间', 0), None),
            str_val(row, hd.get('竣工时间', 0), None),
            str_val(row, hd.get('市编码', 0), None),
            str_val(row, hd.get('市', 0), None),
            str_val(row, hd.get('县市编码', 0), None),
            county_name,
            str_val(row, hd.get('乡镇编码', 0), None),
            town_name,
            str_val(row, hd.get('村编码', 0), None),
            village_name,
            str_val(row, hd.get('详细地址', 0), None),
            str_val(row, hd.get('项目业主', 0), None),
            str_val(row, hd.get('施工单位', 0), None),
            float_val(row, hd.get('海拔', 0)),
            float_val(row, hd.get('经度', 0)),
            float_val(row, hd.get('纬度', 0)),
            1 if str_val(row, hd.get('加热设备是否填写', 0)) == '是' else 0,
            1 if str_val(row, hd.get('散热器是否填写', 0)) == '是' else 0,
            1 if str_val(row, hd.get('自控设备是否填写', 0)) == '是' else 0,
            1 if str_val(row, hd.get('烤房主体是否填写', 0)) == '是' else 0,
            1 if str_val(row, hd.get('附属设施是否填写', 0)) == '是' else 0,
            str_val(row, hd.get('关联收购站', 0), None),
            round(health_score, 2), health_level, predicted,
            1 if use_status == '在用' else 0, '已通过'
        ))

        if len(batch) >= 200:
            sql = '''INSERT INTO barn_project (project_code, barn_name, use_status, idle_years,
                     transfer_year, transfer_use, damage_year, damage_reason, build_mode, project_type,
                     length_m, width_m, height_m, project_cost, subsidy_national, subsidy_region, subsidy_total,
                     start_date, complete_date, city_code, city_name, county_code, county_name, town_code, town_name,
                     village_code, village_name, address, project_owner, construction_unit, elevation, longitude, latitude,
                     has_heating, has_radiator, has_autocontrol, has_main, has_ancillary, station_name,
                     health_score, health_level, predicted_life_years, is_idle, status)
                     VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''
            cur.executemany(sql, batch)
            conn.commit()
            count += len(batch)
            print(f'  已导入: {count}')
            batch = []

    if batch:
        sql = '''INSERT INTO barn_project (project_code, barn_name, use_status, idle_years,
                 transfer_year, transfer_use, damage_year, damage_reason, build_mode, project_type,
                 length_m, width_m, height_m, project_cost, subsidy_national, subsidy_region, subsidy_total,
                 start_date, complete_date, city_code, city_name, county_code, county_name, town_code, town_name,
                 village_code, village_name, address, project_owner, construction_unit, elevation, longitude, latitude,
                 has_heating, has_radiator, has_autocontrol, has_main, has_ancillary, station_name,
                 health_score, health_level, predicted_life_years, is_idle, status)
                 VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''
        cur.executemany(sql, batch)
        conn.commit()
        count += len(batch)

    cur.close()
    conn.close()
    print(f'  完成: 共 {count} 条烤房数据')
    return count

def import_burner():
    print('导入燃烧机表...')
    data = read_xlsx('烤房-燃烧机信息表_20260611152604.xlsx')
    headers = [str(h).strip() if h else '' for h in data[0]]
    hd = {h: i for i, h in enumerate(headers)}

    conn = get_conn()
    cur = conn.cursor()
    batch = []
    count = 0
    for row in data[1:]:
        burner_code = str_val(row, hd.get('燃烧机编码', 1))
        if not burner_code:
            continue
        batch.append((
            burner_code,
            str_val(row, hd.get('关联项目编码', 2), None),
            str_val(row, hd.get('加热设备类型', 3), None),
            float_val(row, hd.get('工程造价（元）', 4)),
            float_val(row, hd.get('补贴总额', 5)),
            float_val(row, hd.get('国家局补贴', 6)),
            float_val(row, hd.get('产区补贴', 7)),
            int_val(row, hd.get('年度', 8)),
            str_val(row, hd.get('施工（供应）单位', 9), None),
            '已通过'
        ))
        if len(batch) >= 300:
            cur.executemany('''INSERT INTO barn_burner
                (burner_code, project_code, heating_type, project_cost, subsidy_total,
                 subsidy_national, subsidy_region, build_year, construction_unit, status)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)''', batch)
            conn.commit()
            count += len(batch)
            print(f'  已导入: {count}')
            batch = []

    if batch:
        cur.executemany('''INSERT INTO barn_burner
            (burner_code, project_code, heating_type, project_cost, subsidy_total,
             subsidy_national, subsidy_region, build_year, construction_unit, status)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)''', batch)
        conn.commit()
        count += len(batch)

    cur.close()
    conn.close()
    print(f'  完成: 共 {count} 条燃烧机数据')
    return count

def import_components():
    print('生成烤房部件数据...')
    conn = get_conn()
    cur = conn.cursor()
    # 获取前800个烤房，为每个生成多个部件
    cur.execute('SELECT barn_id, health_score FROM barn_project ORDER BY barn_id LIMIT 800')
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
            # 根据烤房健康分数计算部件状态
            variance = random.randint(-10, 10)
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

    count = 0
    for i in range(0, len(batch), 500):
        cur.executemany(sql, batch[i:i+500])
        conn.commit()
        count += min(500, len(batch) - i)
        print(f'  已导入部件: {count}/{len(batch)}')

    cur.close()
    conn.close()
    print(f'  完成: 共 {count} 条部件数据')

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

        rand_val = abs(hash(str(barn_id))) % 100
        if rand_val < 40:
            status = '已验收'
        elif rand_val < 70:
            status = '实施中'
        elif rand_val < 90:
            status = '待审核'
        else:
            status = '已取消'

        batch.append((
            barn_id, project_code, barn_name, repair_type, urgency,
            '技术维护员',
            f'烤房部件老化需维修，检测发现: {repair_type}相关问题',
            round(1000 + rand_val * 80, 2),
            round(900 + rand_val * 78, 2),
            status,
            '审核员', '秭归县维修队',
            round(75 + rand_val * 0.2, 2)
        ))

    sql = '''INSERT INTO repair_record
        (barn_id, project_code, barn_name, repair_type, urgency, applicant, apply_desc,
         estimated_cost, actual_cost, repair_status, auditor, implement_team, roi_score)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)'''

    count = 0
    for i in range(0, len(batch), 200):
        cur.executemany(sql, batch[i:i+200])
        conn.commit()
        count += min(200, len(batch) - i)
        print(f'  已导入: {count}/{len(batch)}')

    cur.close()
    conn.close()
    print(f'  完成: 共 {count} 条维修记录')

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
        day_offset = idx % 60
        reserve_date = f'{today.year}-{str(today.month).zfill(2)}-{str((today.day + day_offset) % 28 + 1).zfill(2)}'

        status_rand = idx % 10
        if status_rand < 2:
            status = '待审核'
        elif status_rand < 4:
            status = '使用中'
        elif status_rand < 9:
            status = '已完成'
        else:
            status = '已取消'

        batch.append((
            barn_id, project_code, barn_name, 4,
            user_names[idx % len(user_names)],
            f'138{str(10000000 + idx * 137 % 89999999).zfill(8)}',
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

    count = 0
    for i in range(0, len(batch), 200):
        cur.executemany(sql, batch[i:i+200])
        conn.commit()
        count += min(200, len(batch) - i)
        print(f'  已导入: {count}/{len(batch)}')

    cur.close()
    conn.close()
    print(f'  完成: 共 {count} 条预约记录')

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
        ('秭归县烘烤技术服务队', '烘烤技术服务队', '420527', '秭归县', '杜勇', '13900139001', 12, '秭归县全域', 85),
        ('秭归县简易维修服务队', '简易维修服务队', '420527', '秭归县', '郑刚', '13900139002', 8, '秭归县各烤房点', 65),
        ('秭归县常规管护服务队', '常规管护服务队', '420527', '秭归县', '黄志强', '13900139003', 15, '秭归县烟叶产区', 92),
        ('秭归县综合管理服务队', '综合管理服务队', '420527', '秭归县', '李建华', '13900139004', 6, '秭归县合作社直属', 40),
        ('兴山县烘烤技术服务队', '烘烤技术服务队', '420525', '兴山县', '王永明', '13900139005', 10, '兴山县全域', 58),
        ('兴山县简易维修服务队', '简易维修服务队', '420525', '兴山县', '陈大伟', '13900139006', 7, '兴山县各烤房点', 42),
        ('兴山县常规管护服务队', '常规管护服务队', '420525', '兴山县', '刘建国', '13900139007', 12, '兴山县烟叶产区', 68),
        ('五峰县综合管护服务队', '综合管理服务队', '420526', '五峰土家族自治县', '张晓东', '13900139008', 9, '五峰县全域', 35),
        ('长阳县烘烤技术服务队', '烘烤技术服务队', '420528', '长阳土家族自治县', '周国强', '13900139009', 11, '长阳县全域', 48),
        ('宜都市常规管护服务队', '常规管护服务队', '420529', '宜都市', '吴明辉', '13900139010', 8, '宜都市全域', 38),
    ]
    cur.executemany('''INSERT INTO maintenance_team
        (team_name, team_type, county_code, county_name, leader_name, leader_phone, member_count, service_area, repair_count)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)''', teams)

    funds = [
        (2026, '420527', '秭归县', '行业管护资金', 500000, 320000, 150000, 42, 82.5),
        (2026, '420527', '秭归县', '政府管护资金', 300000, 180000, 100000, 28, 78.3),
        (2026, '420527', '秭归县', '合作社专项', 200000, 120000, 50000, 15, 85.2),
        (2026, '420525', '兴山县', '行业管护资金', 400000, 280000, 100000, 35, 80.1),
        (2026, '420525', '兴山县', '政府管护资金', 250000, 150000, 80000, 22, 76.8),
        (2026, '420526', '五峰土家族自治县', '行业管护资金', 350000, 220000, 100000, 28, 79.5),
        (2026, '420528', '长阳土家族自治县', '行业管护资金', 380000, 250000, 100000, 32, 81.2),
        (2026, '420529', '宜都市', '行业管护资金', 280000, 180000, 80000, 20, 77.9),
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
    cur.close()
    conn.close()

def main():
    print('=' * 50)
    print('烤房管理系统 - 数据全面导入')
    print('=' * 50)

    # 区域字典数据
    import_city()
    import_county()
    import_township()
    import_village()
    import_project_type()

    # 核心业务数据
    import_barn_project()
    import_burner()

    # 衍生数据
    import_components()
    import_repair_records()
    import_reservations()
    import_evaluations()

    # 管护队伍和资金
    import_teams_and_funds()

    show_stats()
    print('\n数据导入完成!')

if __name__ == '__main__':
    main()
