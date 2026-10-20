package com.baoji.data.io

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** Every JSON block in docs/IMPORT_FOR_AI.md must import cleanly, so the guide can't drift from the validator. */
class ImportGuideExamplesTest {

    private val blocks: List<String> = run {
        // Gradle runs unit tests from the module directory.
        val guide = File("../docs/IMPORT_FOR_AI.md").readText()
        Regex("```json\\r?\\n(.*?)```", RegexOption.DOT_MATCHES_ALL).findAll(guide).map { it.groupValues[1] }.toList()
    }

    @Test
    fun guideHasTrainingAndDietExamples() {
        assertThat(blocks.count { "\"training\"" in it }).isAtLeast(2)
        assertThat(blocks.count { "\"diet\"" in it }).isAtLeast(2)
    }

    @Test
    fun everyExampleValidates() {
        blocks.forEach { json ->
            val result = if ("\"training\"" in json) ImportValidator.validateTraining(json) else ImportValidator.validateDiet(json)
            assertThat(result).isInstanceOf(ValidationResult.Ok::class.java)
        }
    }
}
