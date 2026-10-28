# 薄肌

一个只做「记录」的训练 + 饮食 Android App，为练薄肌设计。

计划不在 App 里生成：先和你自己的 AI 讨论好训练和饮食计划，再导入。数据也不在 App 里分析：导出 JSON，交给你的 AI 去看。App 只负责让每天的执行尽量顺手、安静。

<p>
  <img src="docs/screenshots/home.png" width="200" alt="主页：摄入与消耗">
  <img src="docs/screenshots/training.png" width="200" alt="训练页：人体肌肉图与动作列表">
  <img src="docs/screenshots/calendar.png" width="200" alt="训练日历">
  <img src="docs/screenshots/diet.png" width="200" alt="饮食页：营养素条与饮食任务">
</p>

## 不打扰

- 不发通知，不用闹钟，没有倒计时
- 没有连续打卡、完成率，不显示缺口
- 没完成的项目保持中性，不强调「未完成」

## 功能

**主页**
- 「摄入」「消耗」两个大数字。消耗 = 基础代谢（Mifflin-St Jeor）× 1.2 按时间均匀累加 + 日常活动 + 已完成训练组 + 加练和步数
- 日记、体重（平滑曲线，可缩放拖动）
- 左上角导出

**训练**
- 点圆形按钮完成一组；长按动作（震动一下）清空它今天的组，点错了可以重来；点动作可调重量、次数和组数
- 向右滑动动作把它从计划里删除（历史记录保留）
- 「每日步数」每天都在：点一下填今天的步数，按快走速度换算成消耗；长按清空
- 人体肌肉图：今天练到的部位标红，随完成的组数逐渐加深
- 上次所有组都做到次数上限时，重量旁出现一个很小的 `↑`
- 点动作调整重量 / 次数，看最近几次的最大重量
- 点日期打开日历：查看以后哪天练什么、以前哪天做了什么
- `＋ 加练` 从内置运动表里搜索（跑步、游泳、球类等 80 多项），填分钟数，按 MET 值计入消耗；找不到也可以只记文字（不计入消耗）

**饮食**
- 蛋白 / 碳水 / 脂肪三条进度，打勾即计入；长按清除今天的勾，向右滑动从食谱删除
- `＋ 加餐` 从内置食物库（约 1700 种）搜索，填克数，自动算热量和三大营养素并计入摄入；找不到也可以只记文字（不计入摄入）

**个人页**
- 身高、体重、性别、年龄（每年自动加一）
- 营养目标、震动强度、浅色 / 深色 / 跟随系统
- 从备份恢复、清空数据
- 检查更新：按下才去 GitHub 查有没有新版本，有就下载并打开安装（从不自动检查；这也是 App 唯一联网的地方）

## 导入计划

训练页和饮食页左上角「导入」，粘贴 JSON 或从文件选择，可以合并或替换（替换时旧计划归档，历史记录保留）。

想让 AI 帮你写导入文件，把 [docs/IMPORT_FOR_AI.md](docs/IMPORT_FOR_AI.md) 整个发给它即可，里面有完整格式、字段说明和示例。最小示例见 [docs/samples](docs/samples)。

## 安装

在 [Releases](https://github.com/arisuntorch/baoji/releases) 下载 `baoji-*.apk`，在手机上打开安装（需要允许「安装未知来源应用」）。要求 Android 8.0 及以上。

## 内置数据

- 食物：[Sanotsu/china-food-composition-data](https://github.com/Sanotsu/china-food-composition-data) 整理的《中国食物成分表 标准版（第 6 版）》能量和一般营养成分，按每 100 克可食部计算；另补了几种面包、方便面、饮料。版权归原作者，用 `tools/catalog/build_foods.py` 重新生成。
- 运动：MET 值取自 Compendium of Physical Activities。加练的消耗 = (MET − 1) × 体重 × 小时，减去的 1 MET 已经算在基础代谢里；步数按每分钟 110 步、3.5 MET 换算。

## 构建

需要 JDK 17 和 Android SDK 35。

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

正式签名：在仓库根目录建一个 `keystore.properties`（已被 git 忽略）：

```properties
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

然后运行 `./gradlew :app:assembleRelease`。没有这个文件时 release 包不签名。

技术栈：Kotlin、Jetpack Compose、Hilt、Room、DataStore、kotlinx.serialization。设计细节见 [docs/PLAN.md](docs/PLAN.md)。
