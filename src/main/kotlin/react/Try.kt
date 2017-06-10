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
 * Represents a computation that either provided a result, or failed with an exception. Monadic
 * methods are provided that allow one to map and compose tries in ways that propagate failure.
 * This class is not itself "reactive", but it facilitates a more straightforward interface and
 * implementation for [RFuture] and [RPromise].
 */
sealed class Try<out T> {

    /** Returns true if this is a successful try, false if it is a failed try.  */
    abstract fun isSuccess(): Boolean

    /** Returns true if this is a failed try, false if it is a successful try.  */
    fun isFailure(): Boolean = !isSuccess()

    /** Returns the value associated with a successful try, or rethrows the exception if the try
     * failed. */
    abstract fun get(): T

    /** Returns the cause of failure for a failed try. Throws [IllegalStateException] if
     * called on a successful try.  */
    abstract fun failure(): Throwable

    /** Maps successful tries through `f`, passees failure through as is.  */
    fun <R> map(f: (T) -> R): Try<R> = when (this) {
        is Success -> Success(f(this.value))
        is Failure -> this
    }

    /** Maps successful tries through `f`, passes failure through as is.  */
    fun <T, R> Try<T>.flatMap(f: (T) -> Try<R>): Try<R> = when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }

    /** Represents a successful try. Contains the successful result.  */
    data class Success<out T>(val value: T) : Try<T>() {
        override fun isSuccess(): Boolean = true
        override fun get(): T = value
        override fun failure(): Throwable = throw IllegalStateException()
    }

    /** Represents a failed try. Contains the cause of failure.  */
    data class Failure(val cause: Throwable) : Try<Nothing>() {
        override fun isSuccess(): Boolean = false
        override fun get(): Nothing = throw cause
        override fun failure(): Throwable = cause
    }

    companion object {
        /** Lifts `func`, a function on values, to a function on tries.  */
        fun <T, R> lift(func: (T) -> R): (Try<T>) -> Try<R> = { it.map(func) }
    }
}

/** Maps failed tries through `f`, passes success through as is. Note: if `f`
 * throws an exception, you will get back a failure try with the new failure.
 * This is defined as an extension function and not a member function because
 * Kotlin would complain about type parameter T being in "in" position. */
fun <T> Try<T>.recover(f: (Throwable) -> T): Try<T> = when (this) {
    is Try.Success -> this
    is Try.Failure -> Try.Success(f(this.cause))
}