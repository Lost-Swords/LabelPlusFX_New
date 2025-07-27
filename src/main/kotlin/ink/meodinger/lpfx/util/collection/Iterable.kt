package ink.meodinger.lpfx.util.collection

/**
 * Author: Meodinger
 * Date: 2022/1/17
 * Have fun with my code!
 */

/**
 * Contact some Iterables
 * @param iterables All Iterables to be contacted
 * @return An Iterable that contains all iterables in gived order
 */
fun <E> contact(vararg iterables: Iterable<E>): Iterable<E> {
    return iterables.reduce { acc, iterable -> acc + iterable }
}

/**
 * 获取列表中指定索引的前一个索引
 * @param index 当前索引
 * @return 前一个索引，如果当前索引为0则返回列表的最后一个索引
 */
fun <E> List<E>.prevIndex(index: Int): Int {
    return if (index <= 0) this.size - 1 else index - 1
}

/**
 * 获取列表中指定索引的后一个索引
 * @param index 当前索引
 * @return 后一个索引，如果当前索引为最后一个则返回0
 */
fun <E> List<E>.nextIndex(index: Int): Int {
    return if (index >= this.size - 1) 0 else index + 1
}
