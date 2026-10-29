"""Builds app/src/main/assets/catalog/foods.json.

Source: https://github.com/Sanotsu/china-food-composition-data
(《中国食物成分表 标准版 第6版》 energy and general nutrients, folder
json_data_v3_20260825_qwen38max_kimi_k3_fixed), plus supplement_foods.tsv (prepared dishes,
snacks, drinks) and food_aliases.tsv (everyday names appended in ［］ so search finds them).

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
    with open(out, 'w', encoding='utf-8') as fh:
        json.dump(rows, fh, ensure_ascii=False, separators=(',', ':'))
    print(len(rows))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
