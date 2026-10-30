"""Builds app/src/main/assets/catalog/foods.json.

Source: https://github.com/Sanotsu/china-food-composition-data
(《中国食物成分表 标准版 第6版》 energy and general nutrients, folder
json_data_v3_20260825_qwen38max_kimi_k3_fixed), plus supplement_foods.tsv (raw ingredients the
table lacks) and food_aliases.tsv (everyday names appended in ［］ so search finds them).

Usage: python -I build_foods.py <folder with merged_*.json> <output foods.json>
Values are per 100 g edible portion. "Tr" (trace) counts as 0; rows without energy are dropped.
Only raw ingredients are kept (see is_raw): meals are cooked at home and weighed raw, so cooked
dishes, snacks, drinks, cured meats and brand variants are left out.
"""
import glob
import json
import os
import sys


def num(v):
    if v is None:
        return None
    s = str(v).strip().rstrip('*')
    if s in ('', '—', '-', '…'):
        return None
    if s.lower() == 'tr':
        return 0.0
    try:
        return float(s)
    except ValueError:
        return None


def category(file_name):
    # "merged_禽肉类及其制品-鸡.json" -> "禽肉类"
    head = os.path.basename(file_name)[len('merged_'):-len('.json')].split('-')[0]
    return head.replace('及其制品', '')


def tsv(path):
    for line in open(path, encoding='utf-8'):
        line = line.rstrip('\n')
        if line and not line.startswith('#'):
            yield line.split('\t')


def with_aliases(name, aliases):
    if name.endswith('］'):
        old = name[name.rindex('［') + 1:-1].split('、')
        extra = [a for a in aliases if a not in old]
        return name[:-1] + ''.join('、' + a for a in extra) + '］'
    return name + '［' + '、'.join(aliases) + '］'


NOT_RAW_CATEGORIES = {'菜肴（估算）', '主食（估算）', '小吃（估算）', '零食甜点（估算）', '快餐（估算）', '饮料', '加工食品'}

# A name containing any of these is cooked, cured, canned, a snack or a sauce.
NOT_RAW_WORDS = [
    '熟', '煮', '蒸', '炸', '烤', '炒', '煎', '卤', '酱', '罐', '腊', '熏', '肠', '丸', '咸', '腌', '糟', '风干',
    '烧', '扒', '焖', '炖', '五香', '香辣', '手抓', '清香', '香叶', '咖哩', '肉松', '肉酥', '肉脯', '肉干', '果脯',
    '蜜饯', '蜜枣', '糖', '糕', '饼', '酥油茶', '粥', '饭', '包子', '馒头', '花卷', '面包', '方便', '油条', '馅',
    '豆沙', '奶片', '炼乳', '巧克力', '果冻', '汁', '饮料', '醉鸡', '醉蟹', '泡菜', '泡发', '干酪', '奶酪', '调制乳',
    '配方', '人乳', '驴奶', '驼奶', '果料', '果粒', '调味', '橘味', '食品工业', '焦克', '鸡汤', '鸡精', '味精', '底料',
    '膏', '火腿', '方腿', '午餐', '素鸡', '素什锦', '炸素虾', '烤麸', '豆腐卷', '油面筋', '浆粉', '豉', '蟹足棒',
    '油浸', '盐水浸', '瓶装', '鱼片干', '鱼排', '鱼奇油', '虾滑', '牌',
]

# Sauces and staples a home cook weighs, plus raw rows the words above would catch.
RAW_ANYWAY = {
    '白砂糖［白糖、砂糖］', '蜂蜜', '番茄酱', '芝麻酱', '花生酱', '奶酪［干酪、芝士］', '希腊酸奶',
    '牛大肠', '猪大肠', '猪小肠', '羊大肠', '鸭肠', '肥牛卷［肥牛片］',
}

# 其他类 is mostly medicinal (阿胶, 燕窝, 龟甲); only these belong in a kitchen.
OTHER_KEPT = {'牛蛙', '乳清蛋白粉［蛋白粉］', '白砂糖［白糖、砂糖］', '蜂蜜', '番茄酱', '蚝油', '椰浆［椰奶］', '芝麻酱', '花生酱'}


def is_raw(item):
    name, cat = item['n'], item['c']
    if name in RAW_ANYWAY:
        return True
    if cat in NOT_RAW_CATEGORIES or (cat == '其他类' and name not in OTHER_KEPT):
        return False
    base = name.split('［')[0]
    if any(w in base for w in NOT_RAW_WORDS):
        return False
    if cat == '乳类' and '（' in base:
        # "纯牛奶（全脂，光明牌）": keep the table's 代表值 rows, drop the brand variants.
        detail = base[base.index('（') + 1:]
        if '代表值' not in detail and ('，' in detail or '奶粉' in base):
            return False
    return True


def raw_only(rows):
    """Raw ingredients, first row of each name (the table repeats a few names with other values)."""
    seen, out = set(), []
    for item in rows:
        if is_raw(item) and item['n'] not in seen:
            seen.add(item['n'])
            out.append(item)
    return out


def check_energy(item):
    # Catches typos: energy should roughly match 4/9/4 kcal per gram (alcohol and fibre aside).
    est = 4 * item['p'] + 9 * item['f'] + 4 * item['h']
    if item['c'] != '饮料' and abs(est - item['k']) > max(0.2 * item['k'], 15):
        raise SystemExit(f"energy mismatch for {item['n']}: {item['k']} vs {est:.0f}")


def main(src, out):
    here = os.path.dirname(os.path.abspath(__file__))
    rows, seen = [], set()
    for f in sorted(glob.glob(os.path.join(src, 'merged_*.json'))):
        cat = category(f)
        for r in json.load(open(f, encoding='utf-8')):
            name = (r.get('foodName') or '').strip()
            kcal = num(r.get('energyKCal'))
            if not name or kcal is None:
                continue
            item = {
                'n': name,
                'c': cat,
                'k': round(kcal, 1),
                'p': round(num(r.get('protein')) or 0.0, 1),
                'f': round(num(r.get('fat')) or 0.0, 1),
                'h': round(num(r.get('CHO')) or 0.0, 1),
            }
            key = (item['n'], item['k'])
            if key in seen:
                continue
            seen.add(key)
            rows.append(item)
    aliases = {name: a.split('、') for name, a in tsv(os.path.join(here, 'food_aliases.tsv'))}
    for item in rows:
        if item['n'] in aliases:
            item['n'] = with_aliases(item['n'], aliases.pop(item['n']))
    if aliases:
        raise SystemExit(f'aliases for unknown foods: {sorted(aliases)}')
    for n, c, k, p, f, h in tsv(os.path.join(here, 'supplement_foods.tsv')):
        item = {'n': n, 'c': c, 'k': float(k), 'p': float(p), 'f': float(f), 'h': float(h)}
        check_energy(item)
        rows.append(item)
    rows = raw_only(rows)
    with open(out, 'w', encoding='utf-8') as fh:
        json.dump(rows, fh, ensure_ascii=False, separators=(',', ':'))
    print(len(rows))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
