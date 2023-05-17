package com.example.jumpropecounter.Utils
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class ConcurrentFifo<T>() {
    private val queue: BlockingQueue<T> = LinkedBlockingQueue()

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
