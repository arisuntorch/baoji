"""Builds app/src/main/assets/catalog/foods.json.

Source: https://github.com/Sanotsu/china-food-composition-data
(《中国食物成分表 标准版 第6版》 energy and general nutrients, folder
json_data_v3_20260825_qwen38max_kimi_k3_fixed), plus supplement_foods.json.

Usage: python -I build_foods.py <folder with merged_*.json> <output foods.json>
Values are per 100 g edible portion. "Tr" (trace) counts as 0; rows without energy are dropped.
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
    for item in json.load(open(os.path.join(here, 'supplement_foods.json'), encoding='utf-8')):
        rows.append(item)
    with open(out, 'w', encoding='utf-8') as fh:
        json.dump(rows, fh, ensure_ascii=False, separators=(',', ':'))
    print(len(rows))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
