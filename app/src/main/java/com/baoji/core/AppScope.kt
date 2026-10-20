package com.baoji.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/** Process-wide scope for short writes that must outlive a screen (e.g. final autosave). */
@Singleton
class AppScope @Inject constructor() : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
