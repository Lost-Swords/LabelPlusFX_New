package info.meodinger.lpfx.type

import javafx.concurrent.Task


/**
 * Author: Meodinger
 * Date: 2021/8/31
 * Location: info.meodinger.lpfx.type
 */

/**
 * LPFX Task for long-time procedure
 */
open class LPFXTask<T>(private val call: () -> T) : Task<T>() {

    fun setOnSucceeded(callback: (T) -> Unit): LPFXTask<T> {
        @Suppress("UNCHECKED_CAST")
        super.setOnSucceeded { callback.invoke(it.source.value as T) }

        return this
    }

    fun setOnFailed(callback: (Throwable) -> Unit): LPFXTask<T> {
        super.setOnFailed { callback.invoke(it.source.exception) }

        return  this
    }

    fun startInNewThread() {
        Thread(this).start()
    }

    override fun call(): T {
        return this.call.invoke()
    }
}