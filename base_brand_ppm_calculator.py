# -*- coding: utf-8 -*-
"""
基地品牌PPM计算程序
功能：从MySQL数据库读取可疑物料统计和供货量数据，计算基地品牌PPM
"""

import os
import re
import pymysql
from sqlalchemy import create_engine
import pandas as pd
import warnings
warnings.filterwarnings('ignore')


class BaseBrandPPMCalculator:
    """基地品牌PPM计算器"""
    
    def __init__(self, data_dir, source_db_config=None, result_db_config=None):
        """
        初始化计算器
        
        Args:
            data_dir: 数据文件所在目录（用于输出Excel文件）
            source_db_config: 原始数据数据库配置（可疑物料、供货量、供应商档案）
            result_db_config: PPM结果数据库配置
        """
        self.data_dir = data_dir
        
        # 原始数据数据库配置（从ppm_analysis读取）
        self.source_db_config = source_db_config or {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': 'fuqiuyang030828',
            'database': 'ppm_analysis',
            'charset': 'utf8mb4'
        }
        
        # PPM结果数据库配置（写入ppm_result）
        self.result_db_config = result_db_config or {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': 'fuqiuyang030828',
            'database': 'ppm_result',
            'charset': 'utf8mb4'
        }
        
        # 兼容旧的db_config属性
        self.db_config = self.source_db_config
        
        self.source_conn = None  # 原始数据数据库连接
        self.result_conn = None  # 结果数据库连接
        self.suspicious_data = pd.DataFrame()  # 可疑物料数据
        self.supply_data = pd.DataFrame()       # 供货量数据
        self.supplier_profile = pd.DataFrame()   # 供应商档案数据
        self.result_data = pd.DataFrame()       # PPM计算结果
        
        # 基地映射：plant_id -> 基地名称
        self.base_mapping = {
            1000: '河西',
            3000: '青岛',
            8000: '宝骏',
            8200: '重庆'
        }
        
        # 从可疑物料统计中提取基地的映射
        self.region_to_base = {
            '河西': '河西',
            '宝骏': '宝骏',
            '青岛': '青岛',
            '重庆': '重庆'
        }
    
    def connect(self):
        """连接数据库（原始数据数据库和结果数据库）"""
        try:
            # 连接原始数据数据库（ppm_analysis）
            self.source_conn = pymysql.connect(**self.source_db_config)
            source_db_url = f"mysql+pymysql://{self.source_db_config['user']}:{self.source_db_config['password']}@{self.source_db_config['host']}:{self.source_db_config['port']}/{self.source_db_config['database']}?charset={self.source_db_config['charset']}"
            self.source_engine = create_engine(source_db_url)
            print(f"原始数据库连接成功: {self.source_db_config['host']}/{self.source_db_config['database']}")
            
            # 连接结果数据库（ppm_result）
            self.result_conn = pymysql.connect(**self.result_db_config)
            result_db_url = f"mysql+pymysql://{self.result_db_config['user']}:{self.result_db_config['password']}@{self.result_db_config['host']}:{self.result_db_config['port']}/{self.result_db_config['database']}?charset={self.result_db_config['charset']}"
            self.result_engine = create_engine(result_db_url)
            print(f"结果数据库连接成功: {self.result_db_config['host']}/{self.result_db_config['database']}")
            
            # 兼容旧的conn和engine属性
            self.conn = self.source_conn
            self.engine = self.source_engine
            
            return self.source_conn
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return None
    
    def close(self):
        """关闭数据库连接"""
        if self.source_conn:
            self.source_conn.close()
        if self.result_conn:
            self.result_conn.close()
        if hasattr(self, 'source_engine'):
            self.source_engine.dispose()
        if hasattr(self, 'result_engine'):
            self.result_engine.dispose()
        print("数据库连接已关闭")
    
    def extract_base_from_region(self, region):
        """
        从区域工厂名称提取基地名称
        
        Args:
            region: 区域工厂名称，如 '重庆整车8200'
            
        Returns:
            基地名称
        """
        for key, base in self.region_to_base.items():
            if key in str(region):
                return base
        return None
    
    def load_suspicious_data(self):
        """从原始数据库（ppm_analysis）加载可疑物料数据"""
        if not self.source_conn:
            print("原始数据库未连接")
            return pd.DataFrame()
        
        try:
            # 从原始数据库读取可疑物料数据
            sql = """
                SELECT 
                    region_factory, entry_date, entry_person, part_code, part_name,
                    function_module, supplier_name, supplier_code, vehicle_model,
                    failure_description, failure_category, quantity, order_date,
                    shift_section, production_area, is_supplier_responsible, remark,
                    supplier_part, suspicious_quantity, supply_quantity, brand,
                    month, base
                FROM suspicious_data
            """
            self.suspicious_data = pd.read_sql(sql, self.source_engine)
            
            # 从区域工厂提取基地（如果没有base字段）
            if 'base' not in self.suspicious_data.columns or self.suspicious_data['base'].isna().all():
                self.suspicious_data['base'] = self.suspicious_data['region_factory'].apply(self.extract_base_from_region)
            
            # 统一供应商列名
            if 'supplier_name' in self.suspicious_data.columns:
                self.suspicious_data.rename(columns={'supplier_name': '供应商'}, inplace=True)
            
            print(f"可疑物料数据加载完成，共 {len(self.suspicious_data)} 行")
            return self.suspicious_data
            
        except Exception as e:
            print(f"加载可疑物料数据失败: {e}")
            return pd.DataFrame()
    
    def load_supplier_profile(self):
        """从原始数据库（ppm_analysis）加载供应商档案数据"""
        if not self.source_conn:
            print("原始数据库未连接")
            return pd.DataFrame()
        
        try:
            sql = """
                SELECT 
                    supplier_code, supplier_name, supplier_abbrev, supplier_abbrev2
                FROM supplier_profile
            """
            self.supplier_profile = pd.read_sql(sql, self.source_engine)
            print(f"供应商档案加载完成，共 {len(self.supplier_profile)} 条记录")
            
            # 确保供应商代码为字符串类型
            self.supplier_profile['supplier_code'] = self.supplier_profile['supplier_code'].astype(str)
            
            return self.supplier_profile
        except Exception as e:
            print(f"加载供应商档案失败: {e}")
            return pd.DataFrame()
    
    def load_supply_data(self):
        """从原始数据库（ppm_analysis）加载供货量数据"""
        if not self.source_conn:
            print("原始数据库未连接")
            return pd.DataFrame()
        
        try:
            # 从原始数据库读取供货量数据
            sql = """
                SELECT 
                    fiscal_year, supplier_code, supplier_name, part_code, part_name,
                    plant_id, month_1_no, month_2_no, month_3_no, month_4_no,
                    month_5_no, month_6_no, month_7_no, month_8_no, month_9_no,
                    month_10_no, month_11_no, month_12_no, supplier_part_code
                FROM supply_data
            """
            self.supply_data = pd.read_sql(sql, self.source_engine)
            
            # 统一供应商列名
            if 'supplier_name' in self.supply_data.columns:
                self.supply_data.rename(columns={'supplier_name': '供应商'}, inplace=True)
            
            print(f"供货量数据加载完成，共 {len(self.supply_data)} 行")
            return self.supply_data
            
        except Exception as e:
            print(f"加载供货量数据失败: {e}")
            return pd.DataFrame()
    
    def process_suspicious_data(self):
        """
        处理可疑物料数据，按供应商、基地、月份汇总
        
        只统计供应商责任的可疑物料（是否供应商责任 = "Y"）
        """
        if self.suspicious_data.empty:
            return pd.DataFrame()
        
        # 筛选供应商责任的可疑物料（是否供应商责任 = "Y"）
        df = self.suspicious_data[self.suspicious_data['is_supplier_responsible'] == 'Y'].copy()
        
        # 确保可疑物料数量为数值型
        # 优先使用suspicious_quantity，如果为0则使用quantity
        df['可疑物料数量'] = pd.to_numeric(
            df['suspicious_quantity'], errors='coerce'
        ).fillna(0)
        
        # 如果可疑物料数量为0但quantity有值，使用quantity
        if 'quantity' in df.columns:
            df['quantity'] = pd.to_numeric(df['quantity'], errors='coerce').fillna(0)
            # 当suspicious_quantity为0时，使用quantity
            df.loc[df['可疑物料数量'] == 0, '可疑物料数量'] = df.loc[df['可疑物料数量'] == 0, 'quantity']
        
        # 解析品牌字段（可能有多个品牌，用逗号分隔）
        def parse_brands(brand_str):
            if pd.isna(brand_str):
                return []
            # 去除空格并分割
            brands = str(brand_str).replace(' ', '').split(',')
            return [b for b in brands if b]
        
        df['品牌列表'] = df['brand'].apply(parse_brands)
        
        # 展开品牌（一个记录可能有多个品牌）
        rows = []
        for _, row in df.iterrows():
            brands = row['品牌列表']
            if not brands:
                # 无品牌信息，记录为"未知"
                rows.append({
                    '供应商': row.get('supplier_name') or row.get('供应商'),
                    '供应商代码': row.get('supplier_code'),
                    '基地': row.get('base'),
                    '月份': row.get('month'),
                    '可疑物料数量': row['可疑物料数量'],
                    '品牌': '未知'
                })
            else:
                # 每个品牌一条记录
                for brand in brands:
                    rows.append({
                        '供应商': row.get('supplier_name') or row.get('供应商'),
                        '供应商代码': row.get('supplier_code'),
                        '基地': row.get('base'),
                        '月份': row.get('month'),
                        '可疑物料数量': row['可疑物料数量'],
                        '品牌': brand
                    })
        
        result = pd.DataFrame(rows)
        
        # 将月份转换为数字和年份+月份格式
        def convert_month(m):
            if pd.isna(m):
                return None, None
            if isinstance(m, str) and '-' in m:
                parts = m.split('-')
                return int(parts[1]), parts[0] + parts[1]  # (月份数字, 年月如202507)
            return m, str(m)
        
        # 转换月份
        result['月份数字'] = result['月份'].apply(lambda x: convert_month(x)[0])
        result['年月'] = result['月份'].apply(lambda x: convert_month(x)[1])
        
        # 按供应商代码、月份数字、年月、品牌汇总（合并所有基地的供货量）
        summary = result.groupby(['供应商代码', '月份数字', '年月', '品牌']).agg({
            '可疑物料数量': 'sum'
        }).reset_index()
        
        # 使用年月作为主要月份标识
        summary['月份'] = summary['年月']
        summary = summary.drop(columns=['月份数字', '年月'])
        
        print(f"可疑物料数据处理完成，共 {len(summary)} 条记录")
        return summary
    
    def process_supply_data(self):
        """
        处理供货量数据
        
        将 month_1_no ~ month_12_no 转换为长格式，然后按供应商代码和月份汇总
        注意：需要排除零件名中包含"螺栓"、"螺母"、"卡扣"的记录
        """
        if self.supply_data.empty:
            return pd.DataFrame()
        
        # 复制数据并过滤：排除零件名中包含"螺栓"、"螺母"、"卡扣"的记录
        supply_df = self.supply_data.copy()
        
        # 检查part_name列是否存在
        if 'part_name' in supply_df.columns:
            # 排除零件名中包含螺栓、螺母、卡扣的记录
            exclude_keywords = ['螺栓', '螺母', '卡扣']
            mask = ~supply_df['part_name'].astype(str).str.contains('|'.join(exclude_keywords), regex=True, na=False)
            supply_df = supply_df[mask]
            print(f"过滤前: {len(self.supply_data)} 行, 过滤后: {len(supply_df)} 行 (排除了螺栓/螺母/卡扣)")
        
        # 获取月份列
        month_cols = [f'month_{i}_no' for i in range(1, 13)]
        
        # 转换为长格式
        dfs = []
        for col in month_cols:
            if col in supply_df.columns:
                month_num = int(col.split('_')[1])
                # 注意：需要使用每条记录的fiscal_year
                supplier_col = '供应商' if '供应商' in supply_df.columns else 'supplier_name'
                temp = supply_df[['supplier_code', supplier_col, 'fiscal_year', col]].copy()
                # 使用每条记录的fiscal_year生成年月
                temp['月份'] = temp['fiscal_year'].astype(str) + f'{month_num:02d}'
                temp.rename(columns={col: '供货量', supplier_col: 'supplier_name'}, inplace=True)
                dfs.append(temp)
        
        if not dfs:
            print("未找到供货量月份列")
            return pd.DataFrame()
        
        # 合并所有月份数据
        supply_long = pd.concat(dfs, ignore_index=True)
        
        # 确保供货量为数值型
        supply_long['供货量'] = pd.to_numeric(supply_long['供货量'], errors='coerce').fillna(0)
        
        # 按供应商代码和月份汇总（合并所有基地）
        summary = supply_long.groupby(['supplier_code', '月份']).agg({
            '供货量': 'sum'
        }).reset_index()
        
        summary.rename(columns={
            'supplier_code': '供应商代码'
        }, inplace=True)
        
        print(f"供货量数据处理完成，共 {len(summary)} 条记录")
        return summary
    
    def calculate_base_brand_ppm(self):
        """
        计算基地品牌PPM
        
        PPM = (可疑物料数量 / 供货量) * 1000000
        
        供货量 = 四个基地供货量的总和（按供应商代码汇总）
        """
        suspicious_summary = self.process_suspicious_data()
        supply_summary = self.process_supply_data()
        
        if suspicious_summary.empty or supply_summary.empty:
            print("数据不足，无法计算PPM")
            return pd.DataFrame()
        
        # 合并数据（按供应商代码和月份）
        # 确保月份列为同一类型
        supply_summary['月份'] = supply_summary['月份'].astype(str)
        suspicious_summary['月份'] = suspicious_summary['月份'].astype(str)
        
        merged = pd.merge(
            supply_summary,
            suspicious_summary,
            on=['供应商代码', '月份'],
            how='outer'
        )
        
        # 填充缺失值
        merged['可疑物料数量'] = merged['可疑物料数量'].fillna(0)
        merged['供货量'] = merged['供货量'].fillna(0)
        merged['品牌'] = merged['品牌'].fillna('未知')
        
        # 加载供应商档案获取供应商名称
        self.load_supplier_profile()
        if not self.supplier_profile.empty:
            code_to_name = dict(zip(
                self.supplier_profile['supplier_code'].astype(str),
                self.supplier_profile['supplier_name']
            ))
            merged['供应商名称'] = merged['供应商代码'].astype(str).map(code_to_name)
        
        # 计算PPM
        merged['PPM'] = merged.apply(
            lambda row: (row['可疑物料数量'] / row['供货量'] * 1000000) 
            if row['供货量'] > 0 else 0,
            axis=1
        )
        
        # 保留有效记录
        merged = merged[merged['供货量'] > 0]
        
        self.result_data = merged
        print(f"PPM计算完成，共 {len(self.result_data)} 条记录")
        
        return self.result_data
    
    def save_results_to_db(self):
        """将计算结果保存到结果数据库（ppm_result）"""
        if self.result_data.empty:
            print("结果数据为空，跳过保存")
            return False
        
        if not self.result_conn:
            print("结果数据库未连接")
            return False
        
        try:
            cursor = self.result_conn.cursor()
            
            # 先清空ppm_result_data表
            cursor.execute("TRUNCATE TABLE ppm_result_data")
            
            # 准备数据
            df = self.result_data.copy()
            
            # 映射供应商简称
            if not self.supplier_profile.empty:
                code_to_abbrev = dict(zip(
                    self.supplier_profile['supplier_code'].astype(str),
                    self.supplier_profile['supplier_abbrev']
                ))
                df['supplier_abbrev'] = df['供应商代码'].astype(str).map(code_to_abbrev)
            else:
                df['supplier_abbrev'] = None
            
            # 插入数据
            sql = """
                INSERT INTO ppm_result_data 
                (supplier_code, supplier_name, supplier_abbrev, brand, month, 
                 suspicious_quantity, supply_quantity, ppm)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            values = []
            for _, row in df.iterrows():
                # 处理NaN值
                supplier_name = row.get('供应商名称') if pd.notna(row.get('供应商名称')) else None
                supplier_abbrev = row.get('supplier_abbrev') if pd.notna(row.get('supplier_abbrev')) else None
                brand = row.get('品牌') if pd.notna(row.get('品牌')) else None
                
                values.append((
                    str(row['供应商代码']),
                    supplier_name,
                    supplier_abbrev,
                    brand,
                    str(row['月份']),
                    float(row['可疑物料数量']),
                    float(row['供货量']),
                    float(row['PPM'])
                ))
            
            cursor.executemany(sql, values)
            self.conn.commit()
            
            print(f"成功保存 {len(values)} 条PPM计算结果到数据库")
            return True
            
        except Exception as e:
            self.conn.rollback()
            print(f"保存PPM结果到数据库失败: {e}")
            return False
    
    def build_supplier_ppm_table(self):
        """
        构建供应商PPM明细表
        """
        if self.suspicious_data.empty or self.supply_data.empty:
            return pd.DataFrame()

        bases = ['河西', '宝骏', '青岛', '重庆']

        # 1) 可疑物料：只取供应商责任=Y 的数据
        sus_df = self.suspicious_data.copy()
        sus_df = sus_df[sus_df['is_supplier_responsible'] == 'Y'].copy()

        sus_df['可疑物料数量'] = pd.to_numeric(
            sus_df['suspicious_quantity'], errors='coerce'
        ).fillna(0)
        
        # 如果可疑物料数量为0但quantity有值，使用quantity
        if 'quantity' in sus_df.columns:
            sus_df['quantity'] = pd.to_numeric(sus_df['quantity'], errors='coerce').fillna(0)
            sus_df.loc[sus_df['可疑物料数量'] == 0, '可疑物料数量'] = sus_df.loc[sus_df['可疑物料数量'] == 0, 'quantity']
        
        # 确保supplier_name列存在
        if 'supplier_name' not in sus_df.columns:
            if '供应商' in sus_df.columns:
                sus_df['supplier_name'] = sus_df['供应商']
            else:
                sus_df['supplier_name'] = ''

        sus_g = sus_df.groupby(
            ['supplier_code', 'supplier_name', 'month', 'base']
        )['可疑物料数量'].sum().reset_index()

        # 2) 供货量：按基地汇总
        # 注意：供货量数据没有base字段，需要从plant_id映射
        # 先添加base字段
        self.supply_data['base'] = self.supply_data['plant_id'].map({
            1000: '河西',
            3000: '青岛',
            8000: '宝骏',
            8200: '重庆'
        })
        
        # 过滤：排除零件名中包含"螺栓"、"螺母"、"卡扣"的记录
        supply_df = self.supply_data.copy()
        if 'part_name' in supply_df.columns:
            exclude_keywords = ['螺栓', '螺母', '卡扣']
            mask = ~supply_df['part_name'].astype(str).str.contains('|'.join(exclude_keywords), regex=True, na=False)
            supply_df = supply_df[mask]
        
        # 先转换为长格式
        month_cols = [f'month_{i}_no' for i in range(1, 13)]
        
        dfs = []
        for col in month_cols:
            if col in supply_df.columns:
                month_num = int(col.split('_')[1])
                # 使用重命名后的列名 '供应商' 或者原始 'supplier_name'
                supplier_col = '供应商' if '供应商' in supply_df.columns else 'supplier_name'
                temp = supply_df[['supplier_code', supplier_col, 'base', col]].copy()
                temp['month'] = month_num
                temp.rename(columns={col: '供货量', supplier_col: 'supplier_name'}, inplace=True)
                dfs.append(temp)
        
        if not dfs:
            return pd.DataFrame()
        
        supply_long = pd.concat(dfs, ignore_index=True)
        supply_long['供货量'] = pd.to_numeric(supply_long['供货量'], errors='coerce').fillna(0)
        
        # 按供应商代码+名称+月份+基地汇总
        sup_g = supply_long.groupby(
            ['supplier_code', 'supplier_name', 'month', 'base']
        )['供货量'].sum().reset_index()

        # 3) 合并可疑物料 & 供货量
        # 确保月份列为同一类型
        sup_g['month'] = sup_g['month'].astype(str)
        sus_g['month'] = sus_g['month'].astype(str)
        
        merged = pd.merge(
            sup_g,
            sus_g,
            left_on=['supplier_code', 'supplier_name', 'month', 'base'],
            right_on=['supplier_code', 'supplier_name', 'month', 'base'],
            how='left',
            suffixes=('_supply', '_sus')
        )

        merged['可疑物料数量'] = merged['可疑物料数量'].fillna(0)
        merged['供货量'] = merged['供货量'].fillna(0)

        # 单基地供应商PPM
        merged['供应商PPM'] = merged.apply(
            lambda r: (r['可疑物料数量'] / r['供货量'] * 1_000_000) if r['供货量'] > 0 else 0,
            axis=1
        )

        # 4) 计算总计
        total_g = merged.groupby(
            ['month', 'supplier_code', 'supplier_name']
        ).agg(
            月不合格数=('可疑物料数量', 'sum'),
            月供货量=('供货量', 'sum')
        ).reset_index()

        total_g['供应商总PPM'] = total_g.apply(
            lambda r: (r['月不合格数'] / r['月供货量'] * 1_000_000) if r['月供货量'] > 0 else 0,
            axis=1
        )

        # 5) 按基地展开成宽表
        wide = merged.pivot_table(
            index=['month', 'supplier_code', 'supplier_name'],
            columns='base',
            values=['可疑物料数量', '供货量', '供应商PPM'],
            aggfunc='sum',
            fill_value=0
        )
        
        # 扁平化列名
        flat_cols = []
        for metric, base in wide.columns:
            if metric == '可疑物料数量':
                new = f'{base}可疑物料'
            elif metric == '供货量':
                new = f'{base}供货量'
            else:
                new = f'{base}供应商PPM'
            flat_cols.append(new)
        
        wide.columns = flat_cols
        wide = wide.reset_index()
        wide = wide.reset_index(drop=True) if isinstance(wide.index, pd.MultiIndex) else wide

        # 补齐缺失的基地列
        for b in bases:
            for suffix in ['可疑物料', '供货量', '供应商PPM']:
                col = f'{b}{suffix}'
                if col not in wide.columns:
                    wide[col] = 0

        # 6) 合并总计信息
        final = pd.merge(
            total_g,
            wide,
            on=['month', 'supplier_code', 'supplier_name'],
            how='left'
        )

        # PPM月份
        final['PPM月份'] = final['month'].astype(str).str.replace('-', '', regex=False)

        # 映射供应商简称
        self.load_supplier_profile()
        if not self.supplier_profile.empty:
            code_to_abbrev = dict(zip(
                self.supplier_profile['supplier_code'].astype(str),
                self.supplier_profile['supplier_abbrev']
            ))
            final['供应商'] = final['supplier_code'].astype(str).map(code_to_abbrev)
            final['供应商'] = final['供应商'].fillna(final['supplier_name'])
        else:
            final['供应商'] = final['supplier_name']

        # 整理列顺序
        col_order = [
            'PPM月份', 'supplier_code', '供应商', '月不合格数', '月供货量', '供应商总PPM',
            '河西可疑物料', '河西供货量', '河西供应商PPM',
            '宝骏可疑物料', '宝骏供货量', '宝骏供应商PPM',
            '青岛可疑物料', '青岛供货量', '青岛供应商PPM',
            '重庆可疑物料', '重庆供货量', '重庆供应商PPM',
        ]
        
        # 先添加缺失列（使用supplier_code）
        if 'supplier_code' not in final.columns:
            final['supplier_code'] = ''
        
        for c in col_order:
            if c not in final.columns:
                final[c] = 0
        
        # 使用supplier_code排序
        final = final[col_order].sort_values(['PPM月份', 'supplier_code'])

        return final
    
    def save_supplier_ppm_detail_to_db(self):
        """保存供应商PPM明细到结果数据库（ppm_result）"""
        if not self.result_conn:
            print("结果数据库未连接")
            return False
        
        try:
            df = self.build_supplier_ppm_table()
            if df.empty:
                print("供应商PPM明细为空，跳过保存")
                return False
            
            cursor = self.result_conn.cursor()
            
            # 清空表
            cursor.execute("TRUNCATE TABLE supplier_ppm_detail")
            
            # 插入数据
            sql = """
                INSERT INTO supplier_ppm_detail 
                (ppm_month, supplier_code, supplier_abbrev, monthly_defect_count, 
                 monthly_supply_count, supplier_total_ppm,
                 hexi_suspicious, hexi_supply, hexi_ppm,
                 baojun_suspicious, baojun_supply, baojun_ppm,
                 qingdao_suspicious, qingdao_supply, qingdao_ppm,
                 chongqing_suspicious, chongqing_supply, chongqing_ppm)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            values = []
            for _, row in df.iterrows():
                values.append((
                    str(row['PPM月份']),
                    str(row['供应商代码']),
                    str(row['供应商']) if row['供应商代码'] else None,
                    float(row['月不合格数']),
                    float(row['月供货量']),
                    float(row['供应商总PPM']),
                    float(row.get('河西可疑物料', 0)),
                    float(row.get('河西供货量', 0)),
                    float(row.get('河西供应商PPM', 0)),
                    float(row.get('宝骏可疑物料', 0)),
                    float(row.get('宝骏供货量', 0)),
                    float(row.get('宝骏供应商PPM', 0)),
                    float(row.get('青岛可疑物料', 0)),
                    float(row.get('青岛供货量', 0)),
                    float(row.get('青岛供应商PPM', 0)),
                    float(row.get('重庆可疑物料', 0)),
                    float(row.get('重庆供货量', 0)),
                    float(row.get('重庆供应商PPM', 0)),
                ))
            
            cursor.executemany(sql, values)
            self.conn.commit()
            
            print(f"成功保存 {len(values)} 条供应商PPM明细到数据库")
            return True
            
        except Exception as e:
            self.conn.rollback()
            print(f"保存供应商PPM明细到数据库失败: {e}")
            return False
    
    def export_supplier_ppm_files(self):
        """导出供应商PPM明细到Excel文件"""
        df = self.build_supplier_ppm_table()
        if df.empty:
            print("供应商PPM表为空，未导出。")
            return

        for ppm_month, sub in df.groupby('PPM月份'):
            sub = sub.sort_values(['supplier_code'])

            file_name = f'PPM可疑物料分析_供应商PPM{ppm_month}.xlsx'
            sheet_name = f'供应商PPM{ppm_month}'

            out_path = os.path.join(self.data_dir, file_name)
            
            if os.path.exists(out_path):
                try:
                    os.remove(out_path)
                except PermissionError:
                    print(f"文件被占用，无法删除: {out_path}")
                    continue
            
            with pd.ExcelWriter(out_path, engine='openpyxl') as writer:
                sub.to_excel(writer, sheet_name=sheet_name, index=False)

            print(f'已导出：{out_path}')

    def calculate_monthly_summary(self):
        """计算月度汇总PPM"""
        if self.result_data.empty:
            return pd.DataFrame()
        
        summary = self.result_data.groupby(['月份']).agg({
            '可疑物料数量': 'sum',
            '供货量': 'sum'
        }).reset_index()
        
        summary['PPM'] = summary.apply(
            lambda row: (row['可疑物料数量'] / row['供货量'] * 1000000) 
            if row['供货量'] > 0 else 0,
            axis=1
        )
        
        return summary
    
    def save_monthly_summary_to_db(self):
        """保存月度汇总到结果数据库（ppm_result）"""
        if not self.result_conn:
            print("结果数据库未连接")
            return False
        
        try:
            summary = self.calculate_monthly_summary()
            if summary.empty:
                return False
            
            cursor = self.result_conn.cursor()
            cursor.execute("TRUNCATE TABLE monthly_ppm_summary")
            
            sql = """
                INSERT INTO monthly_ppm_summary 
                (month, total_suspicious, total_supply, total_ppm)
                VALUES (%s, %s, %s, %s)
            """
            
            values = []
            for _, row in summary.iterrows():
                values.append((
                    str(row['月份']),
                    float(row['可疑物料数量']),
                    float(row['供货量']),
                    float(row['PPM'])
                ))
            
            cursor.executemany(sql, values)
            self.conn.commit()
            print(f"成功保存 {len(values)} 条月度汇总到数据库")
            return True
            
        except Exception as e:
            self.conn.rollback()
            print(f"保存月度汇总到数据库失败: {e}")
            return False
    
    def calculate_brand_summary(self):
        """计算品牌汇总PPM"""
        if self.result_data.empty:
            return pd.DataFrame()
        
        summary = self.result_data.groupby(['品牌', '月份']).agg({
            '可疑物料数量': 'sum',
            '供货量': 'sum'
        }).reset_index()
        
        summary['PPM'] = summary.apply(
            lambda row: (row['可疑物料数量'] / row['供货量'] * 1000000) 
            if row['供货量'] > 0 else 0,
            axis=1
        )
        
        return summary
    
    def save_brand_summary_to_db(self):
        """保存品牌汇总到结果数据库（ppm_result）"""
        if not self.result_conn:
            print("结果数据库未连接")
            return False
        
        try:
            summary = self.calculate_brand_summary()
            if summary.empty:
                return False
            
            cursor = self.result_conn.cursor()
            cursor.execute("TRUNCATE TABLE brand_ppm_summary")
            
            sql = """
                INSERT INTO brand_ppm_summary 
                (brand, month, suspicious_quantity, supply_quantity, ppm)
                VALUES (%s, %s, %s, %s, %s)
            """
            
            values = []
            for _, row in summary.iterrows():
                values.append((
                    str(row['品牌']),
                    str(row['月份']),
                    float(row['可疑物料数量']),
                    float(row['供货量']),
                    float(row['PPM'])
                ))
            
            cursor.executemany(sql, values)
            self.conn.commit()
            print(f"成功保存 {len(values)} 条品牌汇总到数据库")
            return True
            
        except Exception as e:
            self.conn.rollback()
            print(f"保存品牌汇总到数据库失败: {e}")
            return False
    
    def calculate_overall_summary(self):
        """计算总体PPM"""
        if self.result_data.empty:
            return {}
        
        total_suspicious = self.result_data['可疑物料数量'].sum()
        total_supply = self.result_data['供货量'].sum()
        
        overall_ppm = (total_suspicious / total_supply * 1000000) if total_supply > 0 else 0
        
        print(f"总体PPM: {overall_ppm:.2f}")
        print(f"  可疑物料总数: {total_suspicious}")
        print(f"  供货量总数: {total_supply}")
        
        return {
            '总体PPM': overall_ppm,
            '可疑物料总数': total_suspicious,
            '供货量总数': total_supply
        }
    
    def export_results(self, output_filename='supplier_ppm_result.xlsx'):
        """导出PPM计算结果到Excel"""
        if self.result_data.empty:
            print("没有可导出的数据")
            return
        
        output_path = os.path.join(self.data_dir, output_filename)
        
        monthly_summary = self.calculate_monthly_summary()
        brand_summary = self.calculate_brand_summary()
        
        with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
            self.result_data.to_excel(writer, sheet_name='详细数据', index=False)
            
            if not monthly_summary.empty:
                monthly_summary.to_excel(writer, sheet_name='月度PPM', index=False)
            
            if not brand_summary.empty:
                brand_summary.to_excel(writer, sheet_name='品牌PPM', index=False)
        
        print(f"结果已导出到: {output_path}")
        
        return output_path
    
    def run(self):
        """运行完整的PPM计算流程"""
        print("=" * 50)
        print("开始计算基地品牌PPM（从数据库读取）")
        print("=" * 50)
        
        # 连接数据库
        if not self.connect():
            return None
        
        # 1. 从数据库加载数据
        print("\n[1/6] 从数据库加载可疑物料数据...")
        self.load_suspicious_data()
        
        print("\n[2/6] 从数据库加载供货量数据...")
        self.load_supply_data()
        
        # 2. 计算PPM
        print("\n[3/6] 计算供应商PPM...")
        self.calculate_base_brand_ppm()
        
        # 3. 保存PPM结果到数据库
        print("\n[4/6] 保存PPM结果到数据库...")
        self.save_results_to_db()
        
        # 4. 保存供应商PPM明细到数据库
        print("\n[5/6] 保存供应商PPM明细到数据库...")
        self.save_supplier_ppm_detail_to_db()
        
        # 5. 保存月度汇总到数据库
        self.save_monthly_summary_to_db()
        
        # 6. 保存品牌汇总到数据库
        self.save_brand_summary_to_db()
        
        # 7. 输出汇总
        print("\n[6/6] 汇总统计...")
        self.calculate_overall_summary()
        
        # 关闭数据库连接
        self.close()
        
        print("\n" + "=" * 50)
        print("计算完成!")
        print("=" * 50)
        
        return self.result_data


def main():
    """主函数"""
    # 数据目录（用于输出Excel）
    data_dir = r'D:\codespace\ppm\ppm'
    
    # 原始数据数据库配置（可疑物料、供货量、供应商档案）
    source_db_config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': 'fuqiuyang030828',
        'database': 'ppm_analysis',
        'charset': 'utf8mb4'
    }
    
    # 结果数据库配置（PPM计算结果）
    result_db_config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': 'fuqiuyang030828',
        'database': 'ppm_result',
        'charset': 'utf8mb4'
    }
    
    # 创建计算器并运行
    calculator = BaseBrandPPMCalculator(data_dir, source_db_config, result_db_config)
    result = calculator.run()
    
    if result is not None:
        summary = calculator.calculate_overall_summary()
        print(f"\n总体PPM: {summary.get('总体PPM', 0):.2f}")


if __name__ == "__main__":
    main()
