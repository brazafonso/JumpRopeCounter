package com.example.jumpropecounter.Utils
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class ConcurrentFifo<T>(private val capacity: Int) {
    private val queue: BlockingQueue<T> = ArrayBlockingQueue(capacity)

    fun enqueue(item: T) {
        queue.put(item)
    }

    fun dequeue(): T {
        return queue.take()
    }

    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    fun size(): Int {
        return queue.size
    }
}
