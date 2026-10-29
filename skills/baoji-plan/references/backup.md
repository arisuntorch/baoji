# 读懂 App 导出的备份文件

用户在 App 里「导出」得到的文件形如 `baoji-2026-10-22.json`，最外层 `"type": "backup"`。它包含当前的模板和每天的记录，可以用来分析训练进度、饮食执行情况，或者作为修改计划的起点。

**备份文件不能在「训练」「饮食」页导入**，那里只接受 `training` / `diet`。要基于备份改计划，需要按本文最后一节转换。

## 结构

```json
{
  "app": "baoji",
  "type": "backup",
  "version": 1,
  "exportedAt": "2026-10-22T21:30:00+08:00",
  "range": { "type": "all" },
  "profile": { "heightCm": 175, "sex": "MALE", "age": 25, "enteredAge": 24, "enteredYear": 2025 },
  "targets": { "protein": 150, "carbs": 220, "fat": 60 },
  "daily": [ { "id": "…", "name": "步行通勤", "kcal": 120, "archived": false } ],
  "plans": [
    {
      "id": "…", "name": "上肢", "days": [1, 4], "order": 0, "archived": false,
      "exercises": [
        { "id": "…", "name": "卧推", "sets": 4, "reps": [6, 8], "weight": 62.5, "currentReps": 7, "kcalPerSet": 8, "order": 0, "archived": false }
      ]
    }
  ],
  "meals": [ { "id": "…", "name": "鸡胸肉", "amount": 200, "unit": "g", "protein": 46, "carbs": 0, "fat": 3, "kcal": 211, "order": 0, "archived": false } ],
  "days": [
    {
      "date": "2026-10-20",
      "weight": 72.4,
      "diary": "状态不错",
      "training": { "id": "…", "planId": "…", "start": "19:02", "end": "20:10" },
      "sets": [ { "exerciseId": "…", "name": "卧推", "time": "19:10", "weight": 62.5, "reps": 8, "kcal": 8 } ],
      "meals": [ { "mealTaskId": "…", "name": "鸡胸肉", "time": "12:30", "amount": 200, "unit": "g", "protein": 46, "carbs": 0, "fat": 3, "kcal": 211 } ],
      "free": [ { "type": "training", "text": "慢跑 30 分钟", "time": "07:00" } ],
      "activities": [ { "kind": "steps", "name": "每日步数", "time": "21:30", "amount": 8000, "unit": "步", "kcal": 212.1 } ],
      "kcalIn": 2150.0,
      "kcalOut": 2480.5
    }
  ],
  "exports": []
}
```

（示例省略了 `updatedAt`、`ts` 等时间戳字段。）

## 字段含义

| 字段 | 含义 |
|---|---|
| `range.type` | `all` 全部、`since_last` 上次导出以来、`dates` 指定日期（带 `from` / `to`） |
| `profile` | 身高、性别（`MALE` / `FEMALE`）、当前年龄。体重在 `days[].weight` |
| `targets` | 当前每日营养目标 |
| `plans[].exercises[].weight` | **当前**工作重量（用户在 App 里加重后会变，可能和最初导入的不同） |
| `plans[].exercises[].currentReps` | 用户当前每组的目标次数，介于 `reps` 区间内 |
| `archived: true` | 已被替换或删除的旧模板，只在 `range.type = all` 时出现；历史记录仍会引用它们 |
| `days[].training` | 当天开始/结束训练的时间 |
| `days[].sets` | 当天每一个完成的组：动作名、重量、次数、该组计入的 kcal |
| `days[].meals` | 当天打勾的饮食项（数值是打勾时的快照）。`mealTaskId` 以 `extra:` 开头的是从内置食物库加的餐，`amount` 是克数 |
| `days[].activities` | 计划外的运动：`kind` 为 `steps` 是当天步数（`amount` 是步数），`timed` 是从内置运动表加的练习（`amount` 是分钟），`sets` 是加练的力量动作（`amount` 是组数，`reps` 每组次数，`weightKg` 重量，没有则是自重）。`kcal` 已计入 `kcalOut` |
| `days[].free` | 自由记录：`type` 为 `training` 是「加练」（如跑步），`meal` 是「加餐」，内容是文字 |
| `days[].kcalIn` | 当天摄入（打勾项 kcal 之和） |
| `days[].kcalOut` | 当天总消耗：基础代谢 × 1.2 + 日常活动 + 完成组的 kcal + `activities` 的 kcal；缺体重或资料时为空 |

## 常见分析

- **进步曲线**：按动作名把 `days[].sets` 串起来，看同一动作的重量和次数随日期的变化。
- **是否该加重**：某动作最近一次训练里，组数 ≥ 计划组数，且每组次数都 ≥ `reps` 区间上限 → App 会显示「加重」，建议加 2.5 kg（下肢可 5 kg）。
- **饮食执行率**：每天 `meals` 打勾项数 / 模板 `meals` 中未归档的项数；营养素加总对比 `targets`。
- **热量缺口**：`kcalOut - kcalIn`，配合 `weight` 的趋势判断减脂/增肌节奏。
- 注意 `free` 里的加练和加餐只有文字，没有计入 `kcalIn` / `kcalOut`；`activities` 和 `extra:` 开头的 `meals` 已经计入。

## 转回导入格式

只取**未归档**（`archived` 为 `false`）的项，去掉 `id`、`order`、`archived`、`updatedAt`、`currentReps`：

- 训练文件：`{"app":"baoji","type":"training","version":1,"daily":[{name,kcal}…],"plans":[{name,days,exercises:[{name,sets,reps,weight,kcalPerSet}…]}…]}`。`weight` 用备份里的当前重量，这样用户不会因为重新导入而丢掉进度。
- 饮食文件：`{"app":"baoji","type":"diet","version":1,"targets":{…},"meals":[{name,amount,unit,protein,carbs,fat,kcal}…]}`。

然后在这个基础上修改，再告诉用户用「合并」（只改动同名项）还是「替换」（整体换掉）导入。
