package io.pocketbase.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private var isRunning = false

    fun enqueue(op: AsyncOperation) {
        channel.trySend(op)

        launch {
            mutex.withLock {
                if (!isRunning) {
                    isRunning = true
                    processQueue()
                }
            }
        }
    }

    private fun processQueue() {
        launch {
            while (true) {
                val result = channel.tryReceive()
                val op = result.getOrNull()

                if (op != null) {
                    try {
                        op.invoke()
                    } catch (e: Exception) {
                        // If an operation fails, we continue processing the queue
                    }
                } else {
                    var shouldStop = false
                    mutex.withLock {
                        if (channel.isEmpty) {
                            isRunning = false
                            shouldStop = true
                        }
                    }

                    if (shouldStop) {
                        onComplete?.invoke()
                        break
                    }
                }
            }
        }
    }
}
