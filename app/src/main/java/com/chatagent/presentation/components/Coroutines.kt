package com.chatagent.presentation.components

import kotlinx.coroutines.android.awaitFrame as androidAwaitFrame

suspend fun composeAwaitFrame() {
    androidAwaitFrame()
}
