package com.baoji.data.io

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImportValidatorTest {

    private val trainingSample = """
        {
          "app": "baoji", "type": "training", "version": 1,
          "daily": [
            { "name": "步行去教室", "kcal": 120 }
          ],
          "plans": [
            {
              "name": "上肢 A",
              "days": [1, 4],
              "exercises": [
                { "name": "卧推", "sets": 4, "reps": [6, 8], "weight": 60, "kcalPerSet": 8 },
                { "name": "高位下拉", "sets": 4, "reps": 10, "weight": 50, "kcalPerSet": 7 }
              ]
            }
          ]
        }
    """.trimIndent()

    private val dietSample = """
        {
          "app": "baoji", "type": "diet", "version": 1,
          "targets": { "protein": 170, "carbs": 150, "fat": 50 },
          "meals": [
            { "name": "练前鸡蛋", "amount": 2, "unit": "个", "protein": 12, "carbs": 1, "fat": 10 },
            { "name": "奶昔", "amount": 1, "unit": "份", "protein": 79, "carbs": 48, "fat": 7, "kcal": 600 }
          ]
        }
    """.trimIndent()

    private fun <T> ok(r: ValidationResult<T>): T {
        assertThat(r).isInstanceOf(ValidationResult.Ok::class.java)
        return (r as ValidationResult.Ok).value
    }

    private fun issues(r: ValidationResult<*>): List<ImportIssue> {
        assertThat(r).isInstanceOf(ValidationResult.Invalid::class.java)
        return (r as ValidationResult.Invalid).issues
    }

    @Test
    fun planSampleTrainingParses() {
        val t = ok(ImportValidator.validateTraining(trainingSample))
        assertThat(t.daily).containsExactly(ParsedActivity("步行去教室", 120.0))
        val plan = t.plans.single()
        assertThat(plan.days).containsExactly(1, 4).inOrder()
        assertThat(plan.exercises[0]).isEqualTo(ParsedExercise("卧推", 4, 6, 8, 60.0, 8.0))
        assertThat(plan.exercises[1].repsMin).isEqualTo(10)
        assertThat(plan.exercises[1].repsMax).isEqualTo(10)
    }

    @Test
    fun dailyMayBeOmitted() {
        val text = trainingSample.replace(Regex("\"daily\": \\[[^\\]]*\\],"), "")
        assertThat(ok(ImportValidator.validateTraining(text)).daily).isEmpty()
    }

    @Test
    fun missingKcalPerSetReportsItsObjectLine() {
        val text = trainingSample.replace(", \"kcalPerSet\": 8 }", " }")
        val list = issues(ImportValidator.validateTraining(text))
        assertThat(list).hasSize(1)
        assertThat(list[0].message).contains("kcalPerSet 缺失")
        assertThat(list[0].line).isEqualTo(11)
    }

    @Test
    fun wrongTypeReportsFieldLine() {
        val text = trainingSample.replace("\"days\": [1, 4]", "\"days\": [1, 9]")
        val list = issues(ImportValidator.validateTraining(text))
        assertThat(list.single().line).isEqualTo(9)
        assertThat(list.single().message).contains("days")
    }

    @Test
    fun invalidRepsRangeIsRejected() {
        val text = trainingSample.replace("[6, 8]", "[8, 6]")
        assertThat(issues(ImportValidator.validateTraining(text)).single().message).contains("reps")
    }

    @Test
    fun syntaxErrorPointsToLine() {
        val broken = trainingSample.replace("\"sets\": 4, \"reps\": [6, 8]", "\"sets\": 4 \"reps\": [6, 8]")
        val list = issues(ImportValidator.validateTraining(broken))
        assertThat(list.single().message).contains("语法")
        assertThat(list.single().line).isEqualTo(11)
    }

    @Test
    fun wrongTypeEnvelope() {
        val list = issues(ImportValidator.validateTraining(dietSample))
        assertThat(list.first().message).contains("type")
    }

    @Test
    fun emptyText() {
        assertThat(issues(ImportValidator.validateTraining("  ")).single().message).contains("空")
    }

    @Test
    fun duplicatePlanNamesRejected() {
        val twoPlans = trainingSample.replace(
            "\"plans\": [",
            "\"plans\": [ { \"name\": \"上肢 A\", \"days\": [2], \"exercises\": [] },",
        )
        assertThat(issues(ImportValidator.validateTraining(twoPlans)).single().message).contains("重名")
    }

    @Test
    fun dietSampleParsesWithKcalDefault() {
        val d = ok(ImportValidator.validateDiet(dietSample))
        assertThat(d.targets).isEqualTo(ParsedTargets(170.0, 150.0, 50.0))
        assertThat(d.meals[0].kcal).isWithin(1e-9).of(4 * 12 + 4 * 1 + 9 * 10.0)
        assertThat(d.meals[1].kcal).isWithin(1e-9).of(600.0)
    }

    @Test
    fun dietMissingUnit() {
        val text = dietSample.replace("\"unit\": \"个\", ", "")
        val issue = issues(ImportValidator.validateDiet(text)).single()
        assertThat(issue.line).isEqualTo(5)
        assertThat(issue.message).contains("unit 缺失")
    }

    @Test
    fun negativeNumberRejected() {
        val text = dietSample.replace("\"fat\": 7,", "\"fat\": -7,")
        assertThat(issues(ImportValidator.validateDiet(text)).single().message).contains("fat")
    }

    @Test
    fun locatorMapsNestedPaths() {
        val loc = JsonLocator.index(trainingSample)
        assertThat(loc.lineOf("plans[0].exercises[1].name")).isEqualTo(12)
        assertThat(loc.lineOf("daily[0]")).isEqualTo(4)
        assertThat(loc.lineNear("plans[0].exercises[0].missing")).isEqualTo(11)
    }
}
