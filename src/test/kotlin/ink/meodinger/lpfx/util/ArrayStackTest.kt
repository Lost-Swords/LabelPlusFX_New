package ink.meodinger.lpfx.util

import ink.meodinger.lpfx.util.collection.ArrayStack
import ink.meodinger.lpfx.util.collection.addLast
import org.junit.Test

import org.junit.Assert.*
import test.lpfx.tic
import test.lpfx.toc

/**
 * Author: Meodinger
 * Date: 2022/3/11
 * Have fun with my code!
 */
class ArrayStackTest {

    @Test
    fun testIO() {
        val stack = ArrayStack<Int>()
        assertEquals(0, stack.size)
        stack.push(1)
        assertEquals(1, stack.size)
        stack.peek()
        assertEquals(1, stack.size)
        stack.pop()
        assertEquals(0, stack.size)
    }

    @Test
    fun empty() {
        val stack = ArrayStack<Int>()
        stack.push(1)
        stack.push(2)
        stack.push(3)
        assertEquals(false, stack.isEmpty())
        stack.empty()
        assertEquals(true, stack.isEmpty())
    }

    @Test
    fun isEmpty() {
        val stack = ArrayStack<Int>()
        assertEquals(true, stack.isEmpty())
        stack.push(1)
        assertEquals(false, stack.isEmpty())
        stack.pop()
        assertEquals(true, stack.isEmpty())
    }

    @Test
    fun benchmark() {
        val stack = ArrayStack<Int>()
        tic()
        for (i in 0..1_000_000) stack.push(i)
        toc()
        tic()
        for (i in 0..1_000_000) stack.pop()
        toc()

        val deque = ArrayDeque<Int>()
        tic()
        for (i in 0..1_000_000) deque.addFirst(i)
        toc()
        tic()
        for (i in 0..1_000_000) deque.removeFirst()
        toc()

        tic()
        for (i in 0..1_000_000) deque.addLast(i)
        toc()
        tic()
        for (i in 0..1_000_000) deque.removeLast()
        toc()
    }

}
