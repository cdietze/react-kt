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
abstract class Try<T> private constructor() {

    /** Represents a successful try. Contains the successful result.  */
    class Success<T>(val value: T) : Try<T>() {

        override fun get(): T {
            return value
        }

        override val failure: Throwable
            get() = throw IllegalStateException()
        override val isSuccess: Boolean
            get() = true

        override fun <R> map(func: (T) -> R): Try<R> {
            try {
                return success(func(value))
            } catch (t: Throwable) {
                return failure(t)
            }

        }

        override fun recover(func: (Throwable) -> T): Try<T> {
            return this
        }

        override fun <R> flatMap(func: (T) -> Try<R>): Try<R> {
            try {
                return func(value)
            } catch (t: Throwable) {
                return failure(t)
            }

        }

        override fun toString(): String {
            return "Success($value)"
        }
    }

    /** Represents a failed try. Contains the cause of failure.  */
    class Failure<T>(override val failure: Throwable) : Try<T>() {

        override fun get(): T {
            if (failure is RuntimeException) {
                throw failure
            } else if (failure is Error) {
                throw failure
            } else {
                throw object : RuntimeException("Try failed") {
                    override val cause: Throwable?
                        get() = failure
                }
            }
        }

        override val isSuccess: Boolean
            get() = false

        override fun <R> map(func: (T) -> R): Try<R> {
            return this.casted<R>()
        }

        override fun recover(func: (Throwable) -> T): Try<T> {
            try {
                return success(func(failure))
            } catch (t: Throwable) {
                return failure(t)
            }

        }

        override fun <R> flatMap(func: (T) -> Try<R>): Try<R> {
            return this.casted<R>()
        }

        override fun toString(): String {
            return "Failure($failure)"
        }

        private fun <R> casted(): Try<R> {
            return this as Try<R>
        }
    }

    /** Returns the value associated with a successful try, or rethrows the exception if the try
     * failed. If the exception is a checked exception, it will be thrown as a the `cause` of
     * a newly constructed [RuntimeException].  */
    abstract fun get(): T

    /** Returns the cause of failure for a failed try. Throws [IllegalStateException] if
     * called on a successful try.  */
    abstract val failure: Throwable

    /** Returns try if this is a successful try, false if it is a failed try.  */
    abstract val isSuccess: Boolean

    /** Returns try if this is a failed try, false if it is a successful try.  */
    val isFailure: Boolean
        get() = !isSuccess

    /** Maps successful tries through `func`, passees failure through as is.  */
    abstract fun <R> map(func: (T) -> R): Try<R>

    /** Maps failed tries through `func`, passes success through as is. Note: if `func`
     * throws an exception, you will get back a failure try with the new failure. Ideally one
     * could generalize the type `T` here but Java doesn't allow type parameters with lower
     * bounds.  */
    abstract fun recover(func: (Throwable) -> T): Try<T>

    /** Maps successful tries through `func`, passes failure through as is.  */
    abstract fun <R> flatMap(func: (T) -> Try<R>): Try<R>

    companion object {

        /** Creates a successful try.  */
        fun <T> success(value: T): Try<T> {
            return Success(value)
        }

        /** Creates a failed try.  */
        fun <T> failure(cause: Throwable): Try<T> {
            return Failure(cause)
        }

        /** Lifts `func`, a function on values, to a function on tries.  */
        fun <T, R> lift(func: (T) -> R): (Try<T>) -> Try<R> = { it.map(func) }
    }
}
