package io.pocketbase.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias AsyncOperation = suspend () -> Unit

class SyncQueue internal constructor(
    private val onComplete: (() -> Unit)? = null,
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val channel = Channel<AsyncOperation>(Channel.UNLIMITED)
    private val mutex = Mutex()
    private var isProcessing = false

    fun enqueue(op: AsyncOperation) {
        channel.trySend(op)
        launch {
            mutex.withLock {
                if (!isProcessing) {
                    isProcessing = true
                    dequeue()
                }
            }
        }
    }

    private fun dequeue() {
        launch {
            while (true) {
                val result = channel.tryReceive()
                val op = result.getOrNull()

                if (op == null) {
                    mutex.withLock { isProcessing = false }
                    break
                }

                try {
                    op.invoke()
                } finally {
                    val isDone = mutex.withLock {
                        if (channel.isEmpty) {
                            isProcessing = false
                            true
                        } else {
                            false
                        }
                    }

                    if (isDone) {
                        onComplete?.invoke()
                        break
                    }
                }
            }
        }
    }
}
