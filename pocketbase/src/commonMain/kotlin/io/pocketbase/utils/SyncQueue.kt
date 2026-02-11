package io.pocketbase.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

typealias AsyncOperation = suspend () -> Unit

class SyncQueue internal constructor(
    private val onComplete: (() -> Unit)? = null,
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val operations: MutableList<AsyncOperation> = mutableListOf()

    fun enqueue(op: AsyncOperation) {
        operations.add(op)

        if (operations.size == 1) {
            dequeue()
        }
    }

    private fun dequeue() {
        if (operations.isEmpty()) {
            return
        }

        launch {
            operations.first().invoke()
            operations.removeAt(0)

            if (operations.isEmpty()) {
                onComplete?.invoke()
                return@launch
            }

            dequeue()
        }
    }
}
