/*
 * Copyright 2017 The React.kt Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package react

/**
 * Provides a concrete implementation [RFuture] that can be updated with a success or failure
 * result when it becomes available.

 *
 * This implementation also guarantees a useful behavior, which is that all listeners added
 * prior to the completion of the promise will be cleared when the promise is completed, and no
 * further listeners will be retained. This allows the promise to be retained after is has been
 * completed as a useful "box" for its underlying value, without concern that references to long
 * satisfied listeners will be inadvertently retained.
 */
open class RPromise<T> : RFuture<T>() {

    /** Causes this promise to be completed with `result`.  */
    fun complete(result: Try<T>) {
        if (_result != null) throw IllegalStateException("Already completed")
        _result = result
        try {
            notify(COMPLETE, result, null, null)
        } finally {
            clearConnections()
        }
    }

    /** Causes this promise to be completed successfully with `value`.  */
    open fun succeed(value: T) {
        complete(Try.success(value))
    }

    /** Causes this promise to be completed with failure caused by `cause`.  */
    open fun fail(cause: Throwable) {
        complete(Try.failure<T>(cause))
    }

    /** Returns a slot that can be used to complete this promise.  */
    fun completer(): Slot<Try<T>> = { e -> complete(e) }

    /** Returns a slot that can be used to [.succeed] this promise.  */
    fun succeeder(): Slot<T> = { e -> succeed(e) }

    /** Returns a slot that can be used to [.fail] this promise.  */
    fun failer(): Slot<Throwable> = { cause -> fail(cause) }

    override fun result(): Try<T>? {
        return _result
    }

    protected var _result: Try<T>? = null

    companion object {

        /** Creates a new, uncompleted, promise.  */
        fun <T> create(): RPromise<T> {
            return RPromise()
        }

        protected val COMPLETE: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(listener: Any, value: Any?, i0: Any?, i1: Any?) {
                (listener as SignalViewListener<Try<Any>>)(value as Try<Any>)
            }
        }
    }
}
