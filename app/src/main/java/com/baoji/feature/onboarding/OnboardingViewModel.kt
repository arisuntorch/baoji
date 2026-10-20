package com.baoji.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.data.repo.ProfileInput
import com.baoji.data.repo.ProfileRepository
import com.baoji.domain.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { PROFILE, TRAINING, DIET }

data class OnboardingForm(
    val height: String = "",
    val weight: String = "",
    val sex: Sex? = null,
    val age: String = "",
    val errors: List<String> = emptyList(),
    val step: OnboardingStep = OnboardingStep.PROFILE,
) {
    /** Every required field filled in (format is checked on submit). */
    val complete: Boolean get() = height.isNotBlank() && weight.isNotBlank() && sex != null && age.isNotBlank()

    fun toInput(): ProfileInput? {
        val h = height.toDoubleOrNull() ?: return null
        val w = weight.toDoubleOrNull() ?: return null
        val a = age.toIntOrNull() ?: return null
        val s = sex ?: return null
        return ProfileInput(h, w, s, a)
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val profiles: ProfileRepository) : ViewModel() {

    private val _form = MutableStateFlow(OnboardingForm())
    val form: StateFlow<OnboardingForm> = _form.asStateFlow()

    fun setHeight(v: String) = _form.update { it.copy(height = v.numeric(), errors = emptyList()) }
    fun setWeight(v: String) = _form.update { it.copy(weight = v.numeric(), errors = emptyList()) }
    fun setAge(v: String) = _form.update { it.copy(age = v.filter(Char::isDigit).take(3), errors = emptyList()) }
    fun setSex(v: Sex) = _form.update { it.copy(sex = v, errors = emptyList()) }

    fun submitProfile() = viewModelScope.launch {
        val f = _form.value
        val input = f.toInput()
        if (input == null) {
            _form.update { it.copy(errors = listOf("请填写完整的数字")) }
            return@launch
        }
        val errors = input.errors()
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return@launch
        }
        profiles.create(input)
        _form.update { it.copy(step = OnboardingStep.TRAINING) }
    }

    fun next() = _form.update {
        it.copy(step = if (it.step == OnboardingStep.TRAINING) OnboardingStep.DIET else it.step)
    }

    private fun String.numeric(): String {
        val cleaned = filter { it.isDigit() || it == '.' }
        val firstDot = cleaned.indexOf('.')
        return (if (firstDot < 0) cleaned else cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")).take(6)
    }
}
