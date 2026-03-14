#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ChatBI 业务数据生成脚本
生成6大业务场景的测试数据
"""

import random
import datetime
from datetime import timedelta

# 配置
START_DATE = datetime.date(2023, 1, 1)
END_DATE = datetime.date(2026, 3, 10)

# 基础数据
REGIONS = ['华东', '华南', '华北', '华中', '西南', '西北', '东北']
CITIES = {
    '华东': ['上海', '杭州', '南京', '苏州', '宁波'],
    '华南': ['广州', '深圳', '东莞', '佛山', '珠海'],
    '华北': ['北京', '天津', '石家庄', '太原', '呼和浩特'],
    '华中': ['武汉', '长沙', '郑州', '南昌', '合肥'],
    '西南': ['成都', '重庆', '昆明', '贵阳', '西安'],
    '西北': ['兰州', '银川', '西宁', '乌鲁木齐'],
    '东北': ['沈阳', '长春', '哈尔滨', '大连']
}

PRODUCTS = [
    ('智能手机', '电子产品', 2999, 1800),
    ('笔记本电脑', '电子产品', 5999, 4200),
    ('平板电脑', '电子产品', 3499, 2400),
    ('智能手表', '电子产品', 1999, 1200),
    ('无线耳机', '电子产品', 899, 500),
    ('智能音箱', '智能家居', 599, 350),
    ('扫地机器人', '智能家居', 2499, 1600),
    ('空气净化器', '智能家居', 1899, 1100),
    ('智能门���', '智能家居', 1299, 800),
    ('智能摄像头', '智能家居', 399, 200)
]

DEPARTMENTS = ['销售部', '市场部', '研发部', '产品部', '运营部', '财务部', '人力资源部', '行政部']
POSITIONS = ['总监', '经理', '主管', '专员', '助理']

def generate_date_range(start, end, count):
    """生成日期范围"""
    delta = (end - start).days
    return [start + timedelta(days=random.randint(0, delta)) for _ in range(count)]

def generate_sales_data():
    """生成销售数据"""
    print("-- ============================================")
    print("-- 销售订单数据（2000条）")
    print("-- ============================================")
    print("INSERT INTO `sales_order` (`order_no`, `customer_id`, `customer_name`, `product_id`, `product_name`, `product_category`, `region`, `sales_person_id`, `sales_person_name`, `quantity`, `unit_price`, `sales_amount`, `cost_amount`, `profit_amount`, `discount_amount`, `order_date`, `delivery_date`, `status`) VALUES")

    orders = []
    for i in range(1, 2001):
        order_date = START_DATE + timedelta(days=random.randint(0, (END_DATE - START_DATE).days))
        delivery_date = order_date + timedelta(days=random.randint(1, 7))

        region = random.choice(REGIONS)
        product = random.choice(PRODUCTS)
        quantity = random.randint(1, 20)
        unit_price = product[2]
        cost_price = product[3]
        discount = random.choice([0, 0, 0, 50, 100, 200])

        sales_amount = unit_price * quantity - discount
        cost_amount = cost_price * quantity
        profit_amount = sales_amount - cost_amount

        status = random.choices(['DELIVERED', 'SHIPPED', 'CONFIRMED', 'CANCELLED'], weights=[70, 15, 10, 5])[0]

        customer_id = random.randint(1, 500)
        sales_person_id = random.randint(1, 50)

        order = f"('{order_date.strftime('%Y%m%d')}{i:06d}', {customer_id}, '客户{customer_id}', {random.randint(1, 10)}, '{product[0]}', '{product[1]}', '{region}', {sales_person_id}, '销售{sales_person_id}', {quantity}, {unit_price}, {sales_amount}, {cost_amount}, {profit_amount}, {discount}, '{order_date}', '{delivery_date}', '{status}')"
        orders.append(order)

    print(',\n'.join(orders) + ';')
    print()

def generate_customer_data():
    """生成客户数据"""
    print("-- ============================================")
    print("-- 客户数据（500条）")
    print("-- ============================================")
    print("INSERT INTO `customer` (`customer_no`, `customer_name`, `customer_type`, `industry`, `region`, `level`, `contact_person`, `contact_phone`, `email`, `first_purchase_date`, `last_purchase_date`, `total_purchase_amount`, `purchase_count`, `status`) VALUES")

    customers = []
    industries = ['互联网', '金融', '制造业', '零售', '教育', '医疗', '房地产', '物流']

    for i in range(1, 501):
        customer_type = random.choices(['ENTERPRISE', 'SMB', 'INDIVIDUAL'], weights=[30, 50, 20])[0]
        region = random.choice(REGIONS)
        level = random.choices(['VIP', 'A', 'B', 'C'], weights=[10, 20, 30, 40])[0]
        first_date = START_DATE + timedelta(days=random.randint(0, 700))
        last_date = first_date + timedelta(days=random.randint(30, 400))
        total_amount = random.randint(10000, 500000)
        purchase_count = random.randint(5, 100)

        customer = f"('C{i:06d}', '客户{i}', '{customer_type}', '{random.choice(industries)}', '{region}', '{level}', '联系人{i}', '138{random.randint(10000000, 99999999)}', 'customer{i}@example.com', '{first_date}', '{last_date}', {total_amount}, {purchase_count}, 'ACTIVE')"
        customers.append(customer)

    print(',\n'.join(customers) + ';')
    print()

def generate_user_behavior_data():
    """生成用户行为数据"""
    print("-- ============================================")
    print("-- 用户行为数据（3000条）")
    print("-- ============================================")
    print("INSERT INTO `user_behavior` (`user_id`, `session_id`, `event_type`, `page_url`, `page_title`, `device_type`, `os`, `browser`, `channel`, `duration`, `city`, `province`, `event_time`) VALUES")

    behaviors = []
    event_types = ['PAGE_VIEW', 'CLICK', 'SEARCH', 'REGISTER', 'LOGIN', 'PURCHASE']
    devices = ['PC', 'MOBILE', 'TABLET']
    os_list = ['Windows', 'macOS', 'iOS', 'Android', 'Linux']
    browsers = ['Chrome', 'Safari', 'Firefox', 'Edge']
    channels = ['ORGANIC', 'PAID', 'SOCIAL', 'EMAIL', 'DIRECT']

    for i in range(1, 3001):
        event_time = START_DATE + timedelta(days=random.randint(0, (END_DATE - START_DATE).days),
                                            hours=random.randint(0, 23),
                                            minutes=random.randint(0, 59))
        region = random.choice(REGIONS)
        city = random.choice(CITIES[region])

        behavior = f"({random.randint(1, 1000)}, 'SESSION{i}', '{random.choice(event_types)}', '/page{random.randint(1, 50)}', '页面{random.randint(1, 50)}', '{random.choice(devices)}', '{random.choice(os_list)}', '{random.choice(browsers)}', '{random.choice(channels)}', {random.randint(10, 600)}, '{city}', '{region}', '{event_time}')"
        behaviors.append(behavior)

    print(',\n'.join(behaviors) + ';')
    print()

def generate_app_user_data():
    """生成用户数据"""
    print("-- ============================================")
    print("-- 用户数据（1000条）")
    print("-- ============================================")
    print("INSERT INTO `app_user` (`user_no`, `username`, `nickname`, `gender`, `age`, `city`, `province`, `register_channel`, `register_date`, `last_login_date`, `login_count`, `status`) VALUES")

    users = []
    genders = ['MALE', 'FEMALE']
    channels = ['APP', 'WEB', 'WECHAT', 'ALIPAY', 'REFERRAL']

    for i in range(1, 1001):
        register_date = START_DATE + timedelta(days=random.randint(0, 700))
        last_login = register_date + timedelta(days=random.randint(1, 400))
        region = random.choice(REGIONS)
        city = random.choice(CITIES[region])

        user = f"('U{i:08d}', 'user{i}', '用户{i}', '{random.choice(genders)}', {random.randint(18, 60)}, '{city}', '{region}', '{random.choice(channels)}', '{register_date}', '{last_login}', {random.randint(10, 500)}, 'ACTIVE')"
        users.append(user)

    print(',\n'.join(users) + ';')
    print()

def generate_financial_data():
    """生成财务数据"""
    print("-- ============================================")
    print("-- 财务记录数据（1500条）")
    print("-- ============================================")
    print("INSERT INTO `financial_record` (`record_no`, `record_date`, `category`, `sub_category`, `amount`, `type`, `department`, `project_name`, `description`, `status`) VALUES")

    records = []
    categories = {
        'REVENUE': ['产品销售', '服务收入', '投资收益'],
        'COST': ['原材料', '生产成本', '物流成本'],
        'EXPENSE': ['人力成本', '办公费用', '营销费用', '研发费用', '管理费用']
    }

    for i in range(1, 1501):
        record_date = START_DATE + timedelta(days=random.randint(0, (END_DATE - START_DATE).days))

        if random.random() < 0.4:  # 40% 收入
            category = 'REVENUE'
            type_val = 'INCOME'
            amount = random.randint(50000, 500000)
        else:  # 60% 支出
            category = random.choice(['COST', 'EXPENSE'])
            type_val = 'EXPENSE'
            amount = random.randint(10000, 200000)

        sub_category = random.choice(categories[category])
        department = random.choice(DEPARTMENTS)

        record = f"('FR{record_date.strftime('%Y%m%d')}{i:06d}', '{record_date}', '{category}', '{sub_category}', {amount}, '{type_val}', '{department}', '项目{random.randint(1, 50)}', '财务记录{i}', 'CONFIRMED')"
        records.append(record)

    print(',\n'.join(records) + ';')
    print()

def generate_employee_data():
    """生成员工数据"""
    print("-- ============================================")
    print("-- 员工数据（300条）")
    print("-- ============================================")
    print("INSERT INTO `employee` (`employee_no`, `name`, `gender`, `birth_date`, `age`, `department`, `position`, `level`, `hire_date`, `resign_date`, `status`, `salary`, `performance_score`) VALUES")

    employees = []
    levels = ['P1', 'P2', 'P3', 'P4', 'P5', 'P6', 'M1', 'M2', 'M3']

    for i in range(1, 301):
        birth_date = datetime.date(random.randint(1980, 2000), random.randint(1, 12), random.randint(1, 28))
        age = 2026 - birth_date.year
        hire_date = START_DATE + timedelta(days=random.randint(0, 700))

        # 10% 已离职
        if random.random() < 0.1:
            resign_date = hire_date + timedelta(days=random.randint(180, 800))
            status = 'RESIGNED'
        else:
            resign_date = 'NULL'
            status = 'ACTIVE'

        department = random.choice(DEPARTMENTS)
        position = random.choice(POSITIONS)
        level = random.choice(levels)
        salary = random.randint(8000, 50000)
        performance = round(random.uniform(3.0, 5.0), 2)

        employee = f"('E{i:06d}', '员工{i}', '{random.choice(['男', '女'])}', '{birth_date}', {age}, '{department}', '{position}', '{level}', '{hire_date}', {resign_date}, '{status}', {salary}, {performance})"
        employees.append(employee)

    print(',\n'.join(employees) + ';')
    print()

def generate_inventory_data():
    """生成库存数据"""
    print("-- ============================================")
    print("-- 库存数据（100条）")
    print("-- ============================================")
    print("INSERT INTO `inventory` (`product_id`, `product_name`, `product_category`, `warehouse`, `quantity`, `unit_cost`, `total_value`, `safety_stock`, `last_in_date`, `last_out_date`, `status`) VALUES")

    inventories = []
    warehouses = ['北京仓', '上海仓', '广州仓', '成都仓', '武汉仓']

    for i in range(1, 101):
        product = random.choice(PRODUCTS)
        warehouse = random.choice(warehouses)
        quantity = random.randint(50, 2000)
        unit_cost = product[3]
        total_value = quantity * unit_cost
        safety_stock = random.randint(100, 500)

        last_in = END_DATE - timedelta(days=random.randint(1, 30))
        last_out = END_DATE - timedelta(days=random.randint(1, 15))

        if quantity < safety_stock:
            status = 'LOW'
        else:
            status = 'NORMAL'

        inventory = f"({i}, '{product[0]}', '{product[1]}', '{warehouse}', {quantity}, {unit_cost}, {total_value}, {safety_stock}, '{last_in}', '{last_out}', '{status}')"
        inventories.append(inventory)

    print(',\n'.join(inventories) + ';')
    print()

def generate_service_ticket_data():
    """生成客服工单数据"""
    print("-- ============================================")
    print("-- 客服工单数据（800条）")
    print("-- ============================================")
    print("INSERT INTO `service_ticket` (`ticket_no`, `customer_id`, `customer_name`, `ticket_type`, `priority`, `status`, `channel`, `subject`, `assignee_id`, `assignee_name`, `created_date`, `first_response_date`, `resolved_date`, `response_time_minutes`, `resolution_time_hours`, `satisfaction_score`) VALUES")

    tickets = []
    ticket_types = ['COMPLAINT', 'INQUIRY', 'SUGGESTION', 'TECHNICAL']
    priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
    statuses = ['CLOSED', 'RESOLVED', 'IN_PROGRESS', 'OPEN']
    channels = ['PHONE', 'EMAIL', 'CHAT', 'APP']

    for i in range(1, 801):
        created_date = START_DATE + timedelta(days=random.randint(0, (END_DATE - START_DATE).days),
                                              hours=random.randint(0, 23))
        response_time = random.randint(5, 120)
        first_response = created_date + timedelta(minutes=response_time)
        resolution_time = round(random.uniform(0.5, 48.0), 2)
        resolved_date = created_date + timedelta(hours=resolution_time)

        status = random.choices(statuses, weights=[50, 30, 15, 5])[0]
        satisfaction = random.randint(3, 5) if status in ['CLOSED', 'RESOLVED'] else 'NULL'

        ticket = f"('TK{created_date.strftime('%Y%m%d')}{i:06d}', {random.randint(1, 500)}, '客户{random.randint(1, 500)}', '{random.choice(ticket_types)}', '{random.choice(priorities)}', '{status}', '{random.choice(channels)}', '工单主题{i}', {random.randint(1, 30)}, '客服{random.randint(1, 30)}', '{created_date}', '{first_response}', '{resolved_date}', {response_time}, {resolution_time}, {satisfaction})"
        tickets.append(ticket)

    print(',\n'.join(tickets) + ';')
    print()

if __name__ == '__main__':
    print("-- ============================================")
    print("-- ChatBI 业务分析测试数据")
    print("-- 生成日期: 2026-03-10")
    print("-- 数据范围: 2023-01-01 至 2026-03-10")
    print("-- ============================================")
    print()

    generate_sales_data()
    generate_customer_data()
    generate_user_behavior_data()
    generate_app_user_data()
    generate_financial_data()
    generate_employee_data()
    generate_inventory_data()
    generate_service_ticket_data()

    print("-- ============================================")
    print("-- 数据生成完成")
    print("-- ============================================")
