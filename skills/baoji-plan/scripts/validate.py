#!/usr/bin/env python3
"""Validate a 薄肌 (baoji) training or diet import JSON.

Usage:
    python validate.py plan.json [more.json ...]
    python validate.py - < plan.json

ERROR lines mirror the in-app validator (ImportValidator.kt): the app will reject the file.
WARN lines are importable but probably not what the user wants.
Exit code: 0 = no errors, 1 = at least one error, 2 = bad usage.
Standard library only.
"""
from __future__ import annotations

import json
import sys
from dataclasses import dataclass, field

APP_ID = "baoji"
SUPPORTED_VERSION = 1
INT_MIN, INT_MAX = -(2**31), 2**31 - 1

# Keyword tables copied from MuscleMap.kt and IconMatcher.kt (order matters for equal-length ties).
EXERCISE_KEYWORDS = [
    "卧推", "bench press", "bench", "胸推", "推胸", "俯卧撑", "push-up", "pushup",
    "飞鸟", "夹胸", "fly", "深蹲", "squat", "蹲", "箭步", "弓步", "lunge",
    "腿举", "leg press", "腿屈伸", "leg extension", "腿弯举", "leg curl", "硬拉", "deadlift",
    "罗马尼亚", "直腿硬拉", "rdl", "臀推", "臀桥", "hip thrust",
    "推举", "肩推", "press", "ohp", "平举", "lateral raise",
    "面拉", "face pull", "反向飞鸟", "reverse fly", "耸肩", "shrug", "划船", "row",
    "下拉", "pulldown", "引体", "pull-up", "pullup", "chin-up", "弯举", "curl",
    "臂屈伸", "双杠", "dip", "下压", "pushdown", "三头",
    "卷腹", "仰卧起坐", "crunch", "sit-up", "平板支撑", "plank", "举腿", "腹",
    "转体", "侧桥", "twist", "山羊挺身", "背伸", "hyperextension", "提踵", "calf", "腕", "wrist",
    "跑", "run", "走", "步行", "walk",
]
CARDIO_KEYWORDS = ["跑", "run", "走", "步行", "walk", "跳绳", "jump rope", "骑", "单车", "bike",
                   "cycling", "游泳", "swim", "椭圆机", "treadmill", "爬坡"]
FOOD_KEYWORDS = [
    "蛋", "鸡蛋", "茶叶蛋", "水煮蛋", "egg", "奶", "牛奶", "奶昔", "milk", "酸奶", "yogurt",
    "奶酪", "芝士", "cheese", "蛋白粉", "乳清", "whey", "protein", "饺", "水饺", "包子", "dumpling",
    "鸡", "鸡胸", "鸡腿", "chicken", "肉", "牛肉", "牛排", "猪肉", "beef", "steak", "pork",
    "鱼", "fish", "salmon", "tuna", "虾", "shrimp", "prawn", "豆", "豆腐", "豆浆", "tofu", "soy",
    "饭", "米", "粥", "rice", "面", "粉", "noodle", "pasta", "面包", "吐司", "馒头", "bread",
    "toast", "bagel", "燕麦", "麦片", "oat", "玉米", "corn", "薯", "土豆", "红薯", "山药", "potato",
    "香蕉", "banana", "苹果", "水果", "梨", "橙", "apple", "fruit", "牛油果", "avocado",
    "菜", "西兰花", "沙拉", "salad", "veg", "broccoli", "坚果", "杏仁", "核桃", "花生", "腰果",
    "nut", "almond", "peanut", "咖啡", "茶", "拿铁", "coffee", "latte", "tea", "水", "电解质", "water",
]
TARGET_TOLERANCE = 0.10


def best_keyword(name: str, keywords: list[str]) -> str | None:
    """Longest keyword contained in name (case-insensitive); first listed wins ties."""
    needle = name.lower()
    hits = [k for k in keywords if k.lower() in needle]
    return max(hits, key=len) if hits else None


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    def error(self, path: str, msg: str) -> None:
        self.errors.append(f"{path or '(root)'}: {msg}")

    def warn(self, path: str, msg: str) -> None:
        self.warnings.append(f"{path or '(root)'}: {msg}")


def child(path: str, key: str) -> str:
    return key if not path else f"{path}.{key}"


def is_int(v) -> bool:
    return isinstance(v, int) and not isinstance(v, bool) and INT_MIN <= v <= INT_MAX


def is_number(v) -> bool:
    return isinstance(v, (int, float)) and not isinstance(v, bool)


class Obj:
    """Typed field access on one JSON object; every problem is recorded on the report."""

    def __init__(self, data: dict, path: str, rep: Report):
        self.data, self.path, self.rep = data, path, rep

    def _present(self, key: str):
        v = self.data.get(key)
        if v is None:
            self.rep.error(self.path, f"{key} 缺失")
        return v

    def has(self, key: str) -> bool:
        return self.data.get(key) is not None

    def string(self, key: str):
        v = self._present(key)
        if v is None:
            return None
        if isinstance(v, (dict, list)):
            self.rep.error(child(self.path, key), f"{key} 类型不对")
            return None
        if not isinstance(v, str):
            self.rep.error(child(self.path, key), f"{key} 应为文字")
            return None
        return v

    def non_blank(self, key: str):
        s = self.string(key)
        if s is None:
            return None
        if not s.strip():
            self.rep.error(child(self.path, key), f"{key} 不能为空")
            return None
        return s.strip()

    def int(self, key: str, minimum: int | None = None):
        v = self._present(key)
        if v is None:
            return None
        if isinstance(v, (dict, list)):
            self.rep.error(child(self.path, key), f"{key} 类型不对")
            return None
        if not is_int(v):
            self.rep.error(child(self.path, key), f"{key} 应为整数")
            return None
        if minimum is not None and v < minimum:
            self.rep.error(child(self.path, key), f"{key} 不能小于 {minimum}")
            return None
        return v

    def number(self, key: str, minimum: float = 0.0):
        v = self._present(key)
        if v is None:
            return None
        if isinstance(v, (dict, list)):
            self.rep.error(child(self.path, key), f"{key} 类型不对")
            return None
        if not is_number(v):
            self.rep.error(child(self.path, key), f"{key} 应为数字")
            return None
        if v < minimum:
            self.rep.error(child(self.path, key), f"{key} 不能小于 {int(minimum)}")
            return None
        return float(v)

    def reps(self, key: str):
        v = self._present(key)
        if v is None:
            return None
        if not isinstance(v, (dict, list)):
            if not is_int(v) or v < 1:
                self.rep.error(child(self.path, key), f"{key} 应为正整数或 [最少, 最多]")
                return None
            return v, v
        if isinstance(v, list) and len(v) == 2 and all(is_int(x) for x in v) and 1 <= v[0] <= v[1]:
            return v[0], v[1]
        self.rep.error(child(self.path, key), f"{key} 应为正整数或 [最少, 最多]，且最少 ≤ 最多")
        return None

    def days(self, key: str):
        v = self._present(key)
        if v is None:
            return None
        if not isinstance(v, list) or any(not is_int(x) or not 1 <= x <= 7 for x in v):
            self.rep.error(child(self.path, key), f"{key} 应为 1–7 组成的数组（周一到周日）")
            return None
        return sorted(set(v))

    def array(self, key: str, required: bool):
        v = self.data.get(key)
        if v is None:
            if required:
                self.rep.error(self.path, f"{key} 缺失")
            return []
        base = child(self.path, key)
        if not isinstance(v, list):
            self.rep.error(base, f"{key} 应为数组 [ ]")
            return []
        items = []
        for i, item in enumerate(v):
            item_path = f"{base}[{i}]"
            if isinstance(item, dict):
                items.append(Obj(item, item_path, self.rep))
            else:
                self.rep.error(item_path, "这一项应为对象 { }")
        return items

    def optional_object(self, key: str):
        v = self.data.get(key)
        if v is None:
            return None
        if not isinstance(v, dict):
            self.rep.error(child(self.path, key), f"{key} 应为对象 {{ }}")
            return None
        return Obj(v, child(self.path, key), self.rep)


def check_duplicates(rep: Report, key: str, names: list[str]) -> None:
    seen = set()
    for n in names:
        if n in seen:
            rep.error(key, f"{key} 里有重名项「{n}」")
        seen.add(n)


def validate_training(root: Obj, rep: Report) -> None:
    for a in root.array("daily", required=False):
        a.non_blank("name")
        a.number("kcal")

    plans = root.array("plans", required=True)
    if root.has("plans") and not plans:
        rep.warn("plans", "plans 是空的，导入后没有任何训练日")
    names, day_owner = [], {}
    for p in plans:
        name = p.non_blank("name")
        days = p.days("days")
        exercises = p.array("exercises", required=True)
        if name is not None:
            names.append(name)
        if days == []:
            rep.warn(child(p.path, "days"), "days 为空，这个计划永远不会出现在训练页")
        for d in days or []:
            day_owner.setdefault(d, []).append(name or p.path)
        if p.has("exercises") and not exercises:
            rep.warn(child(p.path, "exercises"), "exercises 是空的")
        for e in exercises:
            validate_exercise(e, rep)
    check_duplicates(rep, "plans", names)
    weekday = "一二三四五六日"
    for d, owners in sorted(day_owner.items()):
        if len(owners) > 1:
            rep.warn("plans", f"周{weekday[d - 1]}安排了多套计划：{'、'.join(owners)}，当天会合并显示")


def validate_exercise(e: Obj, rep: Report) -> None:
    name = e.non_blank("name")
    e.int("sets", minimum=1)
    e.reps("reps")
    e.number("weight")
    e.number("kcalPerSet")
    if name is None:
        return
    if best_keyword(name, CARDIO_KEYWORDS):
        rep.warn(child(e.path, "name"), f"「{name}」像是有氧，固定的写进 daily，临时的让用户在 App 里「＋ 加练」")
    elif best_keyword(name, EXERCISE_KEYWORDS) is None:
        rep.warn(child(e.path, "name"), f"「{name}」没有匹配到动作关键词，会用默认图标、人体图不标色")
    elif best_keyword(name, EXERCISE_KEYWORDS) == "press":
        rep.warn(child(e.path, "name"), f"「{name}」只命中了 press，会按肩推计算；卧推请写 bench press 或中文")


def validate_diet(root: Obj, rep: Report) -> None:
    targets = None
    t = root.optional_object("targets")
    if t is not None:
        values = [t.number(k) for k in ("protein", "carbs", "fat")]
        if None not in values:
            targets = dict(zip(("protein", "carbs", "fat"), values))

    meals = root.array("meals", required=True)
    if root.has("meals") and not meals:
        rep.warn("meals", "meals 是空的，导入后没有任何饮食项")
    names, totals, complete = [], {"protein": 0.0, "carbs": 0.0, "fat": 0.0}, True
    for m in meals:
        name = m.non_blank("name")
        m.number("amount")
        m.non_blank("unit")
        macros = {k: m.number(k) for k in totals}
        if m.has("kcal"):
            m.number("kcal")
        if None in macros.values():
            complete = False
        else:
            for k, v in macros.items():
                totals[k] += v
        if name is not None:
            names.append(name)
            if best_keyword(name, FOOD_KEYWORDS) is None:
                rep.warn(child(m.path, "name"), f"「{name}」没有匹配到食物关键词，会用默认图标")
    check_duplicates(rep, "meals", names)

    if targets and complete and meals:
        labels = {"protein": "蛋白质", "carbs": "碳水", "fat": "脂肪"}
        for k, goal in targets.items():
            got = totals[k]
            if goal > 0 and abs(got - goal) / goal > TARGET_TOLERANCE:
                rep.warn("targets", f"{labels[k]}加总 {got:g} g，目标 {goal:g} g，相差超过 {TARGET_TOLERANCE:.0%}")


def reject_constant(name: str):
    raise ValueError(f"不允许 {name}")


def validate_text(text: str) -> Report:
    rep = Report()
    if not text.strip():
        rep.error("", "内容为空")
        return rep
    try:
        data = json.loads(text, parse_constant=reject_constant)
    except (json.JSONDecodeError, ValueError) as e:
        line = getattr(e, "lineno", 1)
        rep.error("", f"JSON 语法错误（第 {line} 行附近：{e}）")
        return rep
    if not isinstance(data, dict):
        rep.error("", "最外层必须是对象 { }")
        return rep

    root = Obj(data, "", rep)
    app = root.string("app")
    kind = root.string("type")
    version = root.int("version")
    if app is not None and app != APP_ID:
        rep.error("app", f'app 必须是 "{APP_ID}"')
    if version is not None and version != SUPPORTED_VERSION:
        rep.error("version", f"不支持的 version {version}")
    if kind == "backup":
        rep.error("type", "这是 App 导出的备份，不能在训练/饮食页导入；见 references/backup.md 转换")
    elif kind is not None and kind not in ("training", "diet"):
        rep.error("type", f'type 应为 "training" 或 "diet"，实际是 "{kind}"')
    if rep.errors:
        return rep

    if kind == "training":
        validate_training(root, rep)
    else:
        validate_diet(root, rep)
    return rep


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    failed = False
    for arg in argv:
        try:
            text = sys.stdin.read() if arg == "-" else open(arg, encoding="utf-8-sig").read()
        except OSError as e:
            print(f"{arg}: 无法读取文件：{e}")
            failed = True
            continue
        rep = validate_text(text)
        for msg in rep.errors:
            print(f"ERROR {msg}")
        for msg in rep.warnings:
            print(f"WARN  {msg}")
        status = "FAIL" if rep.errors else "OK"
        print(f"{status}  {arg}  ({len(rep.errors)} errors, {len(rep.warnings)} warnings)")
        failed = failed or bool(rep.errors)
    return 1 if failed else 0


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main(sys.argv[1:]))
