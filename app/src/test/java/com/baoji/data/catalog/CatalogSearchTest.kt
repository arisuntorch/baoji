package com.baoji.data.catalog

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class CatalogSearchTest {

    private val names = listOf("红香蕉苹果", "苹果（代表值）", "红富士苹果", "香蕉［甘蕉］", "苹果脯", "鸡胸脯肉")

    private fun search(q: String) = CatalogSearch.search(names, q) { it }

    @Test
    fun exactMainNameAndRepresentativeRowComeFirst() {
        assertThat(search("苹果")).containsExactly("苹果（代表值）", "苹果脯", "红香蕉苹果", "红富士苹果").inOrder()
    }

    @Test
    fun aliasesInBracketsAreSearchable() {
        assertThat(search("甘蕉")).containsExactly("香蕉［甘蕉］")
    }

    @Test
    fun everyWordMustMatchAndBlankFindsNothing() {
        assertThat(search("红 苹果")).containsExactly("红香蕉苹果", "红富士苹果").inOrder()
        assertThat(search("   ")).isEmpty()
    }

    @Test
    fun foodMacrosScaleWithGrams() {
        val rice = FoodItem("米饭（蒸，代表值）", "谷类", 116.0, 2.6, 0.3, 25.9)
        val m = rice.macrosFor(250.0)
        assertThat(m.kcal).isWithin(1e-9).of(290.0)
        assertThat(m.protein).isWithin(1e-9).of(6.5)
        assertThat(rice.macrosFor(-10.0).kcal).isEqualTo(0.0)
    }

    @Test
    fun shippedAssetsParseAndCoverEverydayItems() {
        val json = Json { ignoreUnknownKeys = true }
        val dir = File("src/main/assets/catalog")
        val foods = json.decodeFromString(ListSerializer(FoodItem.serializer()), File(dir, "foods.json").readText())
        val exercises = json.decodeFromString(ListSerializer(ExerciseItem.serializer()), File(dir, "exercises.json").readText())
        assertThat(foods.size).isGreaterThan(1800)
        assertThat(foods.all { it.kcal >= 0 && it.protein >= 0 && it.fat >= 0 && it.carbs >= 0 }).isTrue()
        for (q in listOf("米饭", "鸡蛋", "牛奶", "鸡胸", "燕麦", "面包", "宫保鸡丁", "饺子", "奶茶", "汉堡", "沙拉")) {
            assertThat(CatalogSearch.search(foods, q) { it.name }).isNotEmpty()
        }
        // Everyday names find the table's formal ones.
        assertThat(CatalogSearch.search(foods, "橙子") { it.name }.first().name).isEqualTo("橙［橙子］")
        assertThat(CatalogSearch.search(foods, "包菜") { it.name }.map { it.name }).contains("结球甘蓝（绿）［圆白菜、包菜、卷心菜］")
        assertThat(exercises.all { it.met in 1.0..20.0 }).isTrue()
        for (q in listOf("跑步", "撸铁", "跳操", "台球")) {
            assertThat(CatalogSearch.search(exercises, q) { it.name }).isNotEmpty()
        }
        // Strength moves come first and are what the gym-goer searches for.
        assertThat(exercises.first().strength).isTrue()
        for (q in listOf("卧推", "绳索下压", "高位下拉", "深蹲", "侧平举", "器械肩推")) {
            assertThat(CatalogSearch.search(exercises, q) { it.name }.first().strength).isTrue()
        }
        assertThat(exercises.none { it.category == "生活" }).isTrue()
    }
}
