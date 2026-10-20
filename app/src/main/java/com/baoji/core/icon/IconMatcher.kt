package com.baoji.core.icon

enum class TrainingIcon {
    BENCH, SQUAT, DEADLIFT, PRESS, ROW, PULLDOWN, PULL_UP, CURL, DIP, PUSHDOWN, FLY, LATERAL_RAISE, FACE_PULL,
    LEG_PRESS, LEG_EXTENSION, LEG_CURL, CALF_RAISE, HIP_THRUST, LUNGE, CRUNCH, PLANK, PUSH_UP,
    JUMP_ROPE, CYCLE, SWIM, KETTLEBELL, STRETCH, RUN, WALK, DEFAULT,
}

enum class FoodIcon {
    EGG, MILK, YOGURT, CHEESE, PROTEIN, CHICKEN, BEEF, FISH, SHRIMP, SOY, RICE, NOODLE, BREAD, DUMPLING,
    OATS, CORN, POTATO, BANANA, APPLE, AVOCADO, VEGETABLE, NUTS, COFFEE, WATER, DEFAULT,
}

/**
 * Keyword → icon by substring match. Longer keywords win so that "腿举" beats "举"
 * and "蛋白粉" beats "蛋". On equal length the keyword listed first wins, which is why
 * dish shapes such as 饺子 come before fillings such as 肉.
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
            keywords(
                TrainingIcon.BENCH to "卧推、胸推、推胸、bench、chest press",
                TrainingIcon.SQUAT to "深蹲、蹲、squat",
                TrainingIcon.DEADLIFT to "硬拉、deadlift、rdl",
                TrainingIcon.PRESS to "推举、肩推、press、ohp",
                TrainingIcon.ROW to "划船、row",
                TrainingIcon.PULLDOWN to "下拉、pulldown、pull-down",
                TrainingIcon.PULL_UP to "引体、pull-up、pullup、chin-up",
                TrainingIcon.CURL to "弯举、curl",
                TrainingIcon.DIP to "双杠、双杠臂屈伸、dip",
                TrainingIcon.PUSHDOWN to "下压、臂屈伸、pushdown、extension",
                TrainingIcon.FLY to "飞鸟、夹胸、蝴蝶机、fly",
                TrainingIcon.LATERAL_RAISE to "平举、lateral raise、front raise",
                TrainingIcon.FACE_PULL to "面拉、face pull",
                TrainingIcon.LEG_PRESS to "腿举、倒蹬、leg press",
                TrainingIcon.LEG_EXTENSION to "腿屈伸、leg extension",
                TrainingIcon.LEG_CURL to "腿弯举、leg curl",
                TrainingIcon.CALF_RAISE to "提踵、calf",
                TrainingIcon.HIP_THRUST to "臀推、臀桥、hip thrust、glute bridge",
                TrainingIcon.LUNGE to "弓步、箭步、分腿蹲、lunge、split squat",
                TrainingIcon.CRUNCH to "卷腹、仰卧起坐、举腿、crunch、sit-up",
                TrainingIcon.PLANK to "平板支撑、支撑、plank",
                TrainingIcon.PUSH_UP to "俯卧撑、push-up、pushup",
                TrainingIcon.JUMP_ROPE to "跳绳、jump rope",
                TrainingIcon.CYCLE to "骑、单车、bike、cycling",
                TrainingIcon.SWIM to "游泳、swim",
                TrainingIcon.KETTLEBELL to "壶铃、kettlebell",
                TrainingIcon.STRETCH to "拉伸、瑜伽、stretch、yoga",
                TrainingIcon.RUN to "跑、椭圆机、run、treadmill",
                TrainingIcon.WALK to "走、步行、爬坡、walk",
            ),
            TrainingIcon.DEFAULT,
        )

        val food = IconMatcher(
            keywords(
                FoodIcon.EGG to "蛋、鸡蛋、茶叶蛋、水煮蛋、egg",
                FoodIcon.MILK to "奶、牛奶、奶昔、milk",
                FoodIcon.YOGURT to "酸奶、yogurt",
                FoodIcon.CHEESE to "奶酪、芝士、cheese",
                FoodIcon.PROTEIN to "蛋白粉、乳清、whey、protein",
                FoodIcon.DUMPLING to "饺、水饺、包子、dumpling",
                FoodIcon.CHICKEN to "鸡、鸡胸、鸡腿、chicken",
                FoodIcon.BEEF to "肉、牛肉、牛排、猪肉、beef、steak、pork",
                FoodIcon.FISH to "鱼、fish、salmon、tuna",
                FoodIcon.SHRIMP to "虾、shrimp、prawn",
                FoodIcon.SOY to "豆、豆腐、豆浆、tofu、soy",
                FoodIcon.RICE to "饭、米、粥、rice",
                FoodIcon.NOODLE to "面、粉、noodle、pasta",
                FoodIcon.BREAD to "面包、吐司、馒头、bread、toast、bagel",
                FoodIcon.OATS to "燕麦、麦片、oat",
                FoodIcon.CORN to "玉米、corn",
                FoodIcon.POTATO to "薯、土豆、红薯、山药、potato",
                FoodIcon.BANANA to "香蕉、banana",
                FoodIcon.APPLE to "苹果、水果、梨、橙、apple、fruit",
                FoodIcon.AVOCADO to "牛油果、avocado",
                FoodIcon.VEGETABLE to "菜、西兰花、沙拉、salad、veg、broccoli",
                FoodIcon.NUTS to "坚果、杏仁、核桃、花生、腰果、nut、almond、peanut",
                FoodIcon.COFFEE to "咖啡、茶、拿铁、coffee、latte、tea",
                FoodIcon.WATER to "水、电解质、water",
            ),
            FoodIcon.DEFAULT,
        )

        /** Keywords per icon, separated by 、 so English phrases like "leg press" stay whole. */
        private fun <T> keywords(vararg groups: Pair<T, String>): Map<String, T> =
            groups.flatMap { (icon, words) -> words.split("、").map { it.trim() to icon } }.toMap()
    }
}
