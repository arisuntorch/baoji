package com.baoji.core.icon

enum class TrainingIcon {
    BENCH, SQUAT, DEADLIFT, PRESS, ROW, PULLDOWN, CURL, DIP, FLY, LEG_PRESS, RUN, WALK, DEFAULT
}

enum class FoodIcon {
    EGG, MILK, PROTEIN, CHICKEN, BEEF, FISH, RICE, NOODLE, BREAD, OATS, BANANA, VEGETABLE, DEFAULT
}

/**
 * Keyword → icon by substring match. Longer keywords win so that "腿举" beats "举"
 * and "蛋白粉" beats "蛋".
 */
class IconMatcher<T>(table: Map<String, T>, private val fallback: T) {

    private val entries: List<Pair<String, T>> =
        table.entries.map { it.key.lowercase() to it.value }.sortedByDescending { it.first.length }

    fun match(name: String): T {
        val needle = name.lowercase()
        return entries.firstOrNull { needle.contains(it.first) }?.second ?: fallback
    }

    companion object {
        val training = IconMatcher(
            mapOf(
                "卧推" to TrainingIcon.BENCH, "bench" to TrainingIcon.BENCH, "胸推" to TrainingIcon.BENCH,
                "深蹲" to TrainingIcon.SQUAT, "squat" to TrainingIcon.SQUAT, "蹲" to TrainingIcon.SQUAT,
                "硬拉" to TrainingIcon.DEADLIFT, "deadlift" to TrainingIcon.DEADLIFT, "rdl" to TrainingIcon.DEADLIFT,
                "推举" to TrainingIcon.PRESS, "肩推" to TrainingIcon.PRESS, "press" to TrainingIcon.PRESS,
                "划船" to TrainingIcon.ROW, "row" to TrainingIcon.ROW,
                "下拉" to TrainingIcon.PULLDOWN, "pulldown" to TrainingIcon.PULLDOWN,
                "引体" to TrainingIcon.PULLDOWN, "pull-up" to TrainingIcon.PULLDOWN, "pullup" to TrainingIcon.PULLDOWN,
                "弯举" to TrainingIcon.CURL, "curl" to TrainingIcon.CURL,
                "臂屈伸" to TrainingIcon.DIP, "dip" to TrainingIcon.DIP, "下压" to TrainingIcon.DIP,
                "飞鸟" to TrainingIcon.FLY, "夹胸" to TrainingIcon.FLY, "fly" to TrainingIcon.FLY,
                "腿举" to TrainingIcon.LEG_PRESS, "leg press" to TrainingIcon.LEG_PRESS, "腿屈伸" to TrainingIcon.LEG_PRESS,
                "跑" to TrainingIcon.RUN, "run" to TrainingIcon.RUN,
                "走" to TrainingIcon.WALK, "步行" to TrainingIcon.WALK, "walk" to TrainingIcon.WALK,
            ),
            TrainingIcon.DEFAULT,
        )

        val food = IconMatcher(
            mapOf(
                "蛋" to FoodIcon.EGG, "egg" to FoodIcon.EGG,
                "奶" to FoodIcon.MILK, "奶昔" to FoodIcon.MILK, "milk" to FoodIcon.MILK, "酸奶" to FoodIcon.MILK,
                "蛋白粉" to FoodIcon.PROTEIN, "乳清" to FoodIcon.PROTEIN, "whey" to FoodIcon.PROTEIN,
                "protein" to FoodIcon.PROTEIN,
                "鸡" to FoodIcon.CHICKEN, "chicken" to FoodIcon.CHICKEN,
                "牛肉" to FoodIcon.BEEF, "牛排" to FoodIcon.BEEF, "beef" to FoodIcon.BEEF, "肉" to FoodIcon.BEEF,
                "鱼" to FoodIcon.FISH, "虾" to FoodIcon.FISH, "fish" to FoodIcon.FISH, "salmon" to FoodIcon.FISH,
                "饭" to FoodIcon.RICE, "米" to FoodIcon.RICE, "rice" to FoodIcon.RICE,
                "面" to FoodIcon.NOODLE, "粉" to FoodIcon.NOODLE, "noodle" to FoodIcon.NOODLE, "pasta" to FoodIcon.NOODLE,
                "面包" to FoodIcon.BREAD, "吐司" to FoodIcon.BREAD, "bread" to FoodIcon.BREAD, "toast" to FoodIcon.BREAD,
                "燕麦" to FoodIcon.OATS, "oat" to FoodIcon.OATS, "麦片" to FoodIcon.OATS,
                "香蕉" to FoodIcon.BANANA, "banana" to FoodIcon.BANANA, "水果" to FoodIcon.BANANA,
                "菜" to FoodIcon.VEGETABLE, "西兰花" to FoodIcon.VEGETABLE, "沙拉" to FoodIcon.VEGETABLE,
                "salad" to FoodIcon.VEGETABLE, "veg" to FoodIcon.VEGETABLE,
            ),
            FoodIcon.DEFAULT,
        )
    }
}
