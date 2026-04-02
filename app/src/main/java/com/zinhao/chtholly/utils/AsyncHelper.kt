package com.zinhao.chtholly.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AsyncHelper {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun doAsyncPart(runnable: Runnable){
        scope.launch { runnable.run() }
    }
}