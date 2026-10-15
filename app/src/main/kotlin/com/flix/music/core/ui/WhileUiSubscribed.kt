package com.flix.music.core.ui

import kotlinx.coroutines.flow.SharingStarted

/**
 * How screen state flows are shared: kept alive for 5 s after the screen stops
 * watching, so a rotation or a quick trip back does not restart them.
 */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
