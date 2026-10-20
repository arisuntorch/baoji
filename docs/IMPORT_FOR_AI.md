# 给 AI 的说明：如何为「薄肌」App 写导入 JSON

> 使用方法：把这整份文件发给你的 AI，再告诉它你的训练或饮食安排，让它按这里的规则输出 JSON。
> 然后在 App 的「训练」或「饮食」页左上角点「导入」，粘贴或选择文件即可。

---

## 你（AI）的任务

用户会描述自己的训练计划或饮食计划。请输出**一个**可以直接导入「薄肌」App 的 JSON。

输出要求：
1. 只输出一个 JSON 代码块，不要在 JSON 里写注释（JSON 不支持 `//`）。
2. 训练计划和饮食计划是**两个独立的文件**，用户同时要两个时，分两个代码块输出。
3. 数字一律写成数字，不要加引号或单位（写 `60`，不要写 `"60kg"`）。
4. 同一个文件里，`plans` 的 `name` 不能重复，`meals` 的 `name` 也不能重复。
5. 信息不全时（比如用户没说重量），先按常见水平给一个合理的估计值，并在 JSON 之外用一两句话告诉用户哪些是估计的。

---

## 一、训练计划

### 结构

```json
{
  "app": "baoji",
  "type": "training",
  "version": 1,
  "daily": [
    { "name": "日常活动名称", "kcal": 120 }
  ],
  "plans": [
    {
      "name": "计划名称",
      "days": [1, 4],
      "exercises": [
        { "name": "动作名", "sets": 4, "reps": [6, 8], "weight": 60, "kcalPerSet": 8 }
      ]
    }
  ]
}
```

### 字段规则

| 字段 | 必填 | 规则 |
|---|---|---|
| `app` | 是 | 固定写 `"baoji"` |
| `type` | 是 | 固定写 `"training"` |
| `version` | 是 | 固定写 `1` |
| `daily` | 否 | 每天固定的日常活动消耗（通勤、走路上课等），会计入每天的消耗。没有就省略或写 `[]` |
| `daily[].name` | 是 | 非空文字 |
| `daily[].kcal` | 是 | 每天消耗的千卡，≥ 0 |
| `plans` | 是 | 训练日模板数组，至少一项 |
| `plans[].name` | 是 | 非空文字，不能重复，例如 `"上肢 A"` |
| `plans[].days` | 是 | 这套计划在一周里的哪几天，用 1–7 表示**周一到周日**，例如 `[1, 4]` = 周一和周四。没有安排计划的日子就是休息日。建议同一天只放一套计划 |
| `plans[].exercises` | 是 | 动作数组，按实际训练顺序排列 |
| `exercises[].name` | 是 | 动作名，见下方「动作命名」 |
| `exercises[].sets` | 是 | 组数，正整数（≥ 1） |
| `exercises[].reps` | 是 | 每组次数：区间 `[最少, 最多]`（如 `[6, 8]`，要求最少 ≤ 最多），或单个正整数（如 `12`） |
| `exercises[].weight` | 是 | 重量，单位**公斤**，可以有小数（如 `62.5`）。徒手动作写 `0` |
| `exercises[].kcalPerSet` | 是 | 每完成一组算多少千卡，≥ 0，不清楚可以按下方估算 |

### kcalPerSet 估算参考

按每组的大致消耗估计即可，不需要很精确：

| 动作类型 | 每组大约 |
|---|---|
| 深蹲、硬拉、腿举等大重量下肢 | 8–12 |
| 卧推、划船、下拉、推举等复合上肢 | 6–9 |
| 弯举、侧平举、下压、提踵等孤立动作 | 3–5 |
| 卷腹、平板支撑等核心 | 2–4 |

### 动作命名（很重要）

App 会按动作名里的**关键词**自动匹配图标，并在训练页的人体图上标出练到的肌肉。名字里要包含下表中的某个关键词，前后可以加修饰词（如「上斜哑铃卧推」「坐姿腿弯举」）。匹配时较长的关键词优先，所以「腿弯举」不会被当成二头的「弯举」。

| 关键词（中文 / 英文） | 主要练到 | 辅助 |
|---|---|---|
| 卧推、胸推、推胸、俯卧撑 / bench, push-up | 胸 | 肩、三头 |
| 飞鸟、夹胸 / fly | 胸 | 肩 |
| 深蹲、蹲 / squat | 股四头、臀 | 内收肌、腘绳肌、下背 |
| 箭步、弓步 / lunge | 股四头、臀 | 腘绳肌、内收肌 |
| 腿举 / leg press | 股四头、臀 | 内收肌、腘绳肌 |
| 腿屈伸 / leg extension | 股四头 | — |
| 腿弯举 / leg curl | 腘绳肌 | 小腿 |
| 硬拉 / deadlift | 腘绳肌、臀、下背 | 斜方肌、股四头、前臂、背阔肌 |
| 罗马尼亚、直腿硬拉 / rdl | 腘绳肌、臀 | 下背 |
| 臀推、臀桥 / hip thrust | 臀 | 腘绳肌 |
| 推举、肩推 / press, ohp | 肩 | 三头、斜方肌 |
| 平举 / lateral raise | 肩 | — |
| 面拉、反向飞鸟 / face pull, reverse fly | 肩 | 斜方肌 |
| 耸肩 / shrug | 斜方肌 | — |
| 划船 / row | 背阔肌 | 斜方肌、二头、肩 |
| 下拉、引体 / pulldown, pull-up, chin-up | 背阔肌 | 二头、前臂 |
| 弯举 / curl | 二头 | 前臂 |
| 臂屈伸、双杠 / dip | 三头 | 胸、肩 |
| 下压、三头 / pushdown | 三头 | — |
| 卷腹、仰卧起坐、平板支撑、举腿、腹 / crunch, sit-up, plank | 腹 | 腹斜肌 |
| 转体、侧桥 / twist | 腹斜肌 | 腹 |
| 山羊挺身、背伸 / hyperextension | 下背 | 臀、腘绳肌 |
| 提踵 / calf | 小腿 | — |
| 腕 / wrist | 前臂 | — |
| 跑、走、步行 / run, walk | — | 股四头、小腿、腘绳肌 |

名字里不含任何关键词的动作也能导入，只是用默认图标，人体图上也不会标色。

**不要**把有氧运动或步数写成训练动作。跑步、走路这类临时活动，用户会在 App 里用「＋ 加练」自己记录；每天固定的活动写进 `daily`。

### 完整示例

```json
{
  "app": "baoji",
  "type": "training",
  "version": 1,
  "daily": [
    { "name": "步行通勤", "kcal": 120 }
  ],
  "plans": [
    {
      "name": "上肢",
      "days": [1],
      "exercises": [
        { "name": "卧推", "sets": 4, "reps": [6, 8], "weight": 60, "kcalPerSet": 8 },
        { "name": "高位下拉", "sets": 4, "reps": [8, 10], "weight": 50, "kcalPerSet": 7 },
        { "name": "哑铃侧平举", "sets": 3, "reps": [12, 15], "weight": 8, "kcalPerSet": 4 },
        { "name": "绳索下压", "sets": 3, "reps": [10, 12], "weight": 25, "kcalPerSet": 4 }
      ]
    },
    {
      "name": "下肢",
      "days": [3],
      "exercises": [
        { "name": "深蹲", "sets": 4, "reps": [5, 6], "weight": 80, "kcalPerSet": 10 },
        { "name": "罗马尼亚硬拉", "sets": 3, "reps": [8, 10], "weight": 70, "kcalPerSet": 9 },
        { "name": "腿弯举", "sets": 3, "reps": [10, 12], "weight": 35, "kcalPerSet": 5 },
        { "name": "站姿提踵", "sets": 3, "reps": 15, "weight": 40, "kcalPerSet": 3 }
      ]
    },
    {
      "name": "全身",
      "days": [5],
      "exercises": [
        { "name": "上斜哑铃卧推", "sets": 3, "reps": [8, 10], "weight": 24, "kcalPerSet": 7 },
        { "name": "坐姿划船", "sets": 3, "reps": [8, 10], "weight": 55, "kcalPerSet": 7 },
        { "name": "腿举", "sets": 3, "reps": [10, 12], "weight": 120, "kcalPerSet": 8 },
        { "name": "卷腹", "sets": 3, "reps": 15, "weight": 0, "kcalPerSet": 3 }
      ]
    }
  ]
}
```

---

## 二、饮食计划

### 结构

```json
{
  "app": "baoji",
  "type": "diet",
  "version": 1,
  "targets": { "protein": 150, "carbs": 220, "fat": 60 },
  "meals": [
    { "name": "食物名", "amount": 2, "unit": "个", "protein": 12, "carbs": 1, "fat": 10 }
  ]
}
```

### 字段规则

| 字段 | 必填 | 规则 |
|---|---|---|
| `app` | 是 | 固定写 `"baoji"` |
| `type` | 是 | 固定写 `"diet"` |
| `version` | 是 | 固定写 `1` |
| `targets` | 否 | 每日营养目标（克）：`protein` 蛋白质、`carbs` 碳水、`fat` 脂肪，都 ≥ 0。写了就三个都要写 |
| `meals` | 是 | 每天固定要吃的项目，至少一项。用户每天在 App 里逐项打勾 |
| `meals[].name` | 是 | 非空文字，不能重复。同一种食物在不同餐次出现时要区分，如 `"早餐鸡蛋"`、`"练前鸡蛋"` |
| `meals[].amount` | 是 | 数量，≥ 0，可以有小数 |
| `meals[].unit` | 是 | 单位文字，如 `"个"`、`"g"`、`"份"`、`"ml"`、`"根"` |
| `meals[].protein` | 是 | 这一项**总共**含多少克蛋白质，≥ 0 |
| `meals[].carbs` | 是 | 这一项总共含多少克碳水，≥ 0 |
| `meals[].fat` | 是 | 这一项总共含多少克脂肪，≥ 0 |
| `meals[].kcal` | 否 | 这一项的总热量。不写时 App 按 `4×蛋白 + 4×碳水 + 9×脂肪` 计算 |

**注意**：营养素填的是「这一项按这个数量吃下去的总量」，不是每 100 克的含量。比如 200 g 鸡胸肉，就填 200 g 的总蛋白约 46，而不是 23。

建议让所有 `meals` 的营养素加起来接近 `targets`，这样用户全部打勾后正好达标。

### 食物命名（影响图标）

名字里包含以下关键词就会显示对应图标，否则用默认图标：
蛋 / egg，奶、奶昔、酸奶 / milk，蛋白粉、乳清 / whey、protein，鸡 / chicken，牛肉、牛排、肉 / beef，鱼、虾 / fish、salmon，饭、米 / rice，面、粉 / noodle、pasta，面包、吐司 / bread、toast，燕麦、麦片 / oat，香蕉、水果 / banana，菜、西兰花、沙拉 / salad、veg。

### 完整示例

```json
{
  "app": "baoji",
  "type": "diet",
  "version": 1,
  "targets": { "protein": 150, "carbs": 220, "fat": 60 },
  "meals": [
    { "name": "燕麦", "amount": 60, "unit": "g", "protein": 8, "carbs": 40, "fat": 4 },
    { "name": "鸡蛋", "amount": 3, "unit": "个", "protein": 18, "carbs": 2, "fat": 15 },
    { "name": "鸡胸肉", "amount": 200, "unit": "g", "protein": 46, "carbs": 0, "fat": 3 },
    { "name": "米饭", "amount": 250, "unit": "g", "protein": 6, "carbs": 70, "fat": 1 },
    { "name": "蛋白粉奶昔", "amount": 1, "unit": "份", "protein": 25, "carbs": 5, "fat": 2 },
    { "name": "香蕉", "amount": 1, "unit": "根", "protein": 1, "carbs": 27, "fat": 0, "kcal": 105 }
  ]
}
```

---

## 三、输出前自检清单

- [ ] 顶层有 `"app": "baoji"`、正确的 `"type"`、`"version": 1`
- [ ] 训练：每个动作都有 `name`、`sets`、`reps`、`weight`、`kcalPerSet` 五个字段
- [ ] 训练：`days` 只用 1–7，`reps` 区间的最少 ≤ 最多
- [ ] 饮食：每项都有 `name`、`amount`、`unit`、`protein`、`carbs`、`fat`
- [ ] 计划名、食物名没有重复
- [ ] 没有注释、没有多余的逗号、数字没加引号
- [ ] 动作名包含上面表格里的关键词

## 四、导入时会发生什么（可以告诉用户）

- 导入前 App 会校验，出错时会指出「第几行哪个字段」有问题。把报错原文发回给 AI 修改即可。
- 导入时可以选**合并**（同名的更新，新名字的新增）或**替换**（旧计划整体换成新计划）。
- 已经记录过的历史数据不会因为重新导入而改变。
