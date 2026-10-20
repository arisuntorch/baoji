package com.baoji.feature.shell

import com.baoji.data.io.ImportKind

object Routes {
    const val ONBOARDING = "onboarding"
    const val TABS = "tabs"
    const val DIARY = "diary"
    const val DIARY_EDIT = "diary/{date}"
    const val WEIGHT = "weight"
    const val PROFILE = "profile"
    const val IMPORT = "import/{kind}"

    fun diaryEdit(epochDay: Long) = "diary/$epochDay"
    fun import(kind: ImportKind) = "import/${kind.wire}"
}

enum class Tab { TRAINING, HOME, DIET }
