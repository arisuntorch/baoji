package com.baoji.data.io

import com.baoji.domain.CalorieEngine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Two-pass validation: kotlinx for syntax (offset → line), then field-by-field checks
 * that report "第 N 行 …" using [JsonLocator].
 */
object ImportValidator {

    const val APP_ID = "baoji"
    const val SUPPORTED_VERSION = 1

    /** Syntax + envelope check. Returns the root object or issues. */
    fun parseEnvelope(text: String, expected: ImportKind): ValidationResult<Pair<JsonObject, JsonLocator>> {
        if (text.isBlank()) return invalid(1, "内容为空")
        val root = try {
            Json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            return invalid(syntaxLine(text, e.message), "JSON 语法错误")
        } catch (e: IllegalArgumentException) {
            return invalid(syntaxLine(text, e.message), "JSON 语法错误")
        }
        val locator = JsonLocator.index(text)
        if (root !is JsonObject) return invalid(1, "最外层必须是对象 { }")
        val r = Reader(root, "", locator)
        val app = r.string("app")
        val type = r.string("type")
        val version = r.int("version")
        if (app != null && app != APP_ID) r.issue("app", "app 必须是 \"$APP_ID\"")
        if (type != null && type != expected.wire) r.issue("type", "type 应为 \"${expected.wire}\"，实际是 \"$type\"")
        if (version != null && version != SUPPORTED_VERSION) r.issue("version", "不支持的 version $version")
        return if (r.issues.isEmpty()) ValidationResult.Ok(root to locator) else ValidationResult.Invalid(r.issues)
    }

    fun validateTraining(text: String): ValidationResult<ParsedTraining> {
        val env = parseEnvelope(text, ImportKind.TRAINING)
        if (env !is ValidationResult.Ok) return env as ValidationResult.Invalid
        val (root, locator) = env.value
        val r = Reader(root, "", locator)

        val daily = r.optionalArray("daily").mapNotNull { (path, el) ->
            val o = r.objectAt(path, el) ?: return@mapNotNull null
            val name = o.nonBlankString("name")
            val kcal = o.number("kcal", min = 0.0)
            if (name != null && kcal != null) ParsedActivity(name, kcal) else null
        }

        val plans = r.requiredArray("plans").mapNotNull { (path, el) ->
            val o = r.objectAt(path, el) ?: return@mapNotNull null
            val name = o.nonBlankString("name")
            val days = o.days("days")
            val exercises = o.requiredArray("exercises").mapNotNull { (ep, ee) -> exercise(r, ep, ee) }
            if (name != null && days != null) ParsedPlan(name, days, exercises) else null
        }
        duplicateNames(r, "plans", plans.map { it.name })
        return r.result(ParsedTraining(daily, plans))
    }

    fun validateDiet(text: String): ValidationResult<ParsedDiet> {
        val env = parseEnvelope(text, ImportKind.DIET)
        if (env !is ValidationResult.Ok) return env as ValidationResult.Invalid
        val (root, locator) = env.value
        val r = Reader(root, "", locator)

        val targets = r.optionalObject("targets")?.let { t ->
            val p = t.number("protein", min = 0.0)
            val c = t.number("carbs", min = 0.0)
            val f = t.number("fat", min = 0.0)
            if (p != null && c != null && f != null) ParsedTargets(p, c, f) else null
        }
        val meals = r.requiredArray("meals").mapNotNull { (path, el) ->
            val o = r.objectAt(path, el) ?: return@mapNotNull null
            val name = o.nonBlankString("name")
            val amount = o.number("amount", min = 0.0)
            val unit = o.nonBlankString("unit")
            val protein = o.number("protein", min = 0.0)
            val carbs = o.number("carbs", min = 0.0)
            val fat = o.number("fat", min = 0.0)
            val kcal = if (o.has("kcal")) o.number("kcal", min = 0.0) else null
            if (name == null || amount == null || unit == null || protein == null || carbs == null || fat == null) {
                null
            } else {
                ParsedMeal(name, amount, unit, protein, carbs, fat, kcal ?: CalorieEngine.kcalFromMacros(protein, carbs, fat))
            }
        }
        duplicateNames(r, "meals", meals.map { it.name })
        return r.result(ParsedDiet(targets, meals))
    }

    private fun exercise(r: Reader, path: String, el: JsonElement): ParsedExercise? {
        val o = r.objectAt(path, el) ?: return null
        val name = o.nonBlankString("name")
        val sets = o.int("sets", min = 1)
        val reps = o.reps("reps")
        val weight = o.number("weight", min = 0.0)
        val kcalPerSet = o.number("kcalPerSet", min = 0.0)
        if (name == null || sets == null || reps == null || weight == null || kcalPerSet == null) return null
        return ParsedExercise(name, sets, reps.first, reps.second, weight, kcalPerSet)
    }

    private fun duplicateNames(r: Reader, arrayKey: String, names: List<String>) {
        names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            r.issue(arrayKey, "$arrayKey 里有重名项「$it」")
        }
    }

    private val offsetRegex = Regex("""offset (\d+)""")

    private fun syntaxLine(text: String, message: String?): Int {
        val offset = message?.let { offsetRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        return if (offset == null) 1 else JsonLocator.lineAtOffset(text, offset)
    }

    private fun <T> invalid(line: Int, message: String): ValidationResult<T> =
        ValidationResult.Invalid(listOf(ImportIssue(line, message)))

    /** Typed field access on one object that records an issue for every problem. */
    private class Reader(
        private val obj: JsonObject,
        private val path: String,
        private val locator: JsonLocator,
        val issues: MutableList<ImportIssue> = mutableListOf(),
    ) {
        fun <T> result(value: T): ValidationResult<T> =
            if (issues.isEmpty()) ValidationResult.Ok(value) else ValidationResult.Invalid(issues.sortedBy { it.line })

        fun has(key: String) = obj[key] != null && obj[key] !is JsonNull

        fun issue(key: String, message: String) {
            issues += ImportIssue(locator.lineNear(JsonLocator.child(path, key)), message)
        }

        private fun missing(key: String) {
            issues += ImportIssue(locator.lineNear(path), "$key 缺失")
        }

        private fun primitive(key: String): JsonPrimitive? {
            val el = obj[key]
            if (el == null || el is JsonNull) { missing(key); return null }
            if (el !is JsonPrimitive) { issue(key, "$key 类型不对"); return null }
            return el
        }

        fun string(key: String): String? {
            val p = primitive(key) ?: return null
            if (!p.isString) { issue(key, "$key 应为文字"); return null }
            return p.content
        }

        fun nonBlankString(key: String): String? {
            val s = string(key) ?: return null
            if (s.isBlank()) { issue(key, "$key 不能为空"); return null }
            return s.trim()
        }

        fun int(key: String, min: Int = Int.MIN_VALUE): Int? {
            val p = primitive(key) ?: return null
            val v = if (p.isString) null else p.intOrNull
            if (v == null) { issue(key, "$key 应为整数"); return null }
            if (v < min) { issue(key, "$key 不能小于 $min"); return null }
            return v
        }

        fun number(key: String, min: Double = Double.NEGATIVE_INFINITY): Double? {
            val p = primitive(key) ?: return null
            val v = if (p.isString) null else p.doubleOrNull
            if (v == null || v.isNaN() || v.isInfinite()) { issue(key, "$key 应为数字"); return null }
            if (v < min) { issue(key, "$key 不能小于 ${min.toInt()}"); return null }
            return v
        }

        /** `reps` is either `8` or `[6, 8]`. */
        fun reps(key: String): Pair<Int, Int>? {
            val el = obj[key]
            if (el == null || el is JsonNull) { missing(key); return null }
            if (el is JsonPrimitive) {
                val v = if (el.isString) null else el.intOrNull
                if (v == null || v < 1) { issue(key, "$key 应为正整数或 [最少, 最多]"); return null }
                return v to v
            }
            if (el is JsonArray && el.size == 2) {
                val a = (el[0] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                val b = (el[1] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                if (a != null && b != null && a >= 1 && a <= b) return a to b
            }
            issue(key, "$key 应为正整数或 [最少, 最多]，且最少 ≤ 最多")
            return null
        }

        fun days(key: String): List<Int>? {
            val el = obj[key]
            if (el == null || el is JsonNull) { missing(key); return null }
            val arr = el as? JsonArray
            val values = arr?.map { (it as? JsonPrimitive)?.takeUnless { p -> p.isString }?.intOrNull }
            if (values == null || values.any { it == null || it !in 1..7 }) {
                issue(key, "$key 应为 1–7 组成的数组（周一到周日）")
                return null
            }
            return values.filterNotNull().distinct().sorted()
        }

        fun requiredArray(key: String): List<Pair<String, JsonElement>> {
            val el = obj[key]
            if (el == null || el is JsonNull) { missing(key); return emptyList() }
            return arrayItems(key, el)
        }

        fun optionalArray(key: String): List<Pair<String, JsonElement>> {
            val el = obj[key]
            if (el == null || el is JsonNull) return emptyList()
            return arrayItems(key, el)
        }

        private fun arrayItems(key: String, el: JsonElement): List<Pair<String, JsonElement>> {
            if (el !is JsonArray) { issue(key, "$key 应为数组 [ ]"); return emptyList() }
            val base = JsonLocator.child(path, key)
            return el.mapIndexed { i, item -> JsonLocator.item(base, i) to item }
        }

        fun optionalObject(key: String): Reader? {
            val el = obj[key]
            if (el == null || el is JsonNull) return null
            if (el !is JsonObject) { issue(key, "$key 应为对象 { }"); return null }
            return Reader(el, JsonLocator.child(path, key), locator, issues)
        }

        fun objectAt(itemPath: String, el: JsonElement): Reader? {
            if (el !is JsonObject) {
                issues += ImportIssue(locator.lineNear(itemPath), "这一项应为对象 { }")
                return null
            }
            return Reader(el, itemPath, locator, issues)
        }
    }
}
