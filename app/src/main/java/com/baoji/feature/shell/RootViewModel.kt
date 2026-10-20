package com.baoji.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.data.db.ProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class RootState { LOADING, ONBOARDING, READY }

@HiltViewModel
class RootViewModel @Inject constructor(profileDao: ProfileDao) : ViewModel() {
    val state: StateFlow<RootState> = profileDao.observe()
        .map { if (it == null) RootState.ONBOARDING else RootState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.LOADING)

    val avatarPath: StateFlow<String?> = profileDao.observe()
        .map { it?.avatarPath }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
