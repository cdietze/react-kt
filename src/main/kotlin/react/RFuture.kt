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
 * Represents an asynchronous result. Unlike standard Java futures, you cannot block on this
 * result. You can [.map] or [.flatMap] it, and listen for success or failure via the
 * [.success] and [.failure] signals.

 *
 *  The benefit over just using `Callback` is that results can be composed. You can
 * subscribe to an object, flatmap the result into a service call on that object which returns the
 * address of another object, flat map that into a request to subscribe to that object, and finally
 * pass the resulting object to some other code via a slot. Failure can be handled once for all of
 * these operations and you avoid nesting yourself three callbacks deep.
 */
abstract class RFuture<T> : Reactor() {

    /** Causes `slot` to be notified if/when this future is completed with success. If it has
     * already succeeded, the slot will be notified immediately.
     * @return this future for chaining.
     */
    fun onSuccess(slot: SignalViewListener<in T>): RFuture<T> {
        return onComplete({ result -> if (result.isSuccess()) slot(result.get()) })
    }

    /** Causes `slot` to be notified if/when this future is completed with failure. If it has
     * already failed, the slot will be notified immediately.
     * @return this future for chaining.
     */
    fun onFailure(slot: SignalViewListener<in Throwable>): RFuture<T> {
        return onComplete({ result -> if (result.isFailure()) slot(result.failure()) })
    }

    /** Causes `slot` to be notified when this future is completed. If it has already
     * completed, the slot will be notified immediately.
     * @return this future for chaining.
     */
    fun onComplete(slot: SignalViewListener<in Try<T>>): RFuture<T> {
        val result = result()
        if (result != null)
            slot(result)
        else
            addConnection(slot)
        return this
    }

    /** Returns a value that indicates whether this future has completed.  */
    val isComplete: ValueView<Boolean>
        get() {
            fun createCompleteView(): Value<Boolean> {
                val isCompleteView = Value.create(false)
                onComplete({ isCompleteView.update(true) })
                _isCompleteView = isCompleteView
                return isCompleteView
            }
            return _isCompleteView ?: createCompleteView()
        }

    /** Returns whether this future is complete right now. This is an unfortunate name, but I
     * foolishly defined [.isComplete] to return a reactive view of completeness.  */
    val isCompleteNow: Boolean
        get() = result() != null

    /** Convenience method to [ValueView.connectNotify] `slot` to [.isComplete].
     * This is useful for binding the disabled state of UI elements to this future's completeness
     * (i.e. disabled while the future is incomplete, then reenabled when it is completed).
     * @return this future for chaining.
     */
    fun bindComplete(slot: SignalViewListener<Boolean>): RFuture<T> {
        isComplete.connectNotify(slot)
        return this
    }

    /** Transforms this future by mapping its result upon arrival.  */
    fun <R> transform(func: (Try<T>) -> Try<R>): RFuture<R> {
        val xf = RPromise.create<R>()
        onComplete({ result ->
            try {
                xf.complete(func(result))
            } catch (t: Throwable) {
                xf.fail(t)
            }
        })
        return xf
    }

    /** Maps the value of a successful result using `func` upon arrival.  */
    fun <R> map(func: (T) -> R): RFuture<R> {
        val lifted: (Try<T>) -> Try<R> = Try.lift(func)
        return transform(lifted)
    }

    /** Maps the value of a failed result using `func` upon arrival. Ideally one could
     * generalize the type `T` here but Java doesn't allow type parameters with lower
     * bounds.  */
    fun recover(func: (Throwable) -> T): RFuture<T> {
        val sigh: (Try<T>) -> Try<T> = { result -> result.recover(func) }
        //val lifted = sigh as Function<Try<in T>, Try<T>>
        return transform(sigh)
    }

    /** Maps a successful result to a new result using `func` when it arrives. Failure on the
     * original result or the mapped result are both dispatched to the mapped result. This is
     * useful for chaining asynchronous actions. It's also known as monadic bind.  */
    fun <R> flatMap(func: (T) -> RFuture<R>): RFuture<R> {
        val mapped = RPromise.create<R>()
        onComplete({ result: Try<T> ->
            if (result.isFailure())
                mapped.fail(result.failure())
            else
                try {
                    func(result.get()).onComplete(mapped.completer())
                } catch (t: Throwable) {
                    mapped.fail(t)
                }
        })
        return mapped
    }

    /** Returns the result of this future, or null if it is not yet complete.

     *
     * *NOTE:* don't use this method! You should wire up reactions to the completion of
     * this future via [.onSuccess] or [.onFailure]. React is not a blocking async
     * library where on might block a calling thread on the result of a future and then obtain the
     * result synchronously. This is *only* appropriate when you're trying to abstract over
     * synchronous and asynchronous variants of a computation, and you want to use the future
     * machinery in both cases, but in the synchronous case you know that your future will be
     * complete by the time you want to obtain its result.
     */
    abstract fun result(): Try<T>?

    internal override fun placeholderListener(): RListener {
        /*@SuppressWarnings("unchecked")*/
        return Slots.NOOP
    }

    // TODO(cdi) can we use a lazy property here?
    private var _isCompleteView: ValueView<Boolean>? = null

    companion object {

        /** Returns a future with a pre-existing success value.  */
        fun <T> success(value: T): RFuture<T> {
            return result(Try.Success(value))
        }

        /** Returns a future result for a `Unit` method.  */
        fun success(): RFuture<Unit> {
            return success()
        }

        /** Returns a future with a pre-existing failure value.  */
        fun <T> failure(cause: Throwable): RFuture<T> {
            return result(Try.Failure(cause))
        }

        /** Returns a future with an already-computed result.  */
        fun <T> result(result: Try<T>): RFuture<T> {
            return object : RFuture<T>() {
                override fun result(): Try<T>? {
                    return result
                }
            }
        }

        /** Returns a future containing a list of all success results from `futures` if all of
         * the futures complete successfully, or a [MultiFailureException] aggregating all
         * failures, if any of the futures fails.

         *
         * If `futures` is an ordered collection, the resulting list will match the order of
         * the futures. If not, result list is in `futures`' iteration order.  */
        fun <T> sequence(futures: Collection<RFuture<T>>): RFuture<List<T>> {
            // if we're passed an empty list of futures, succeed immediately with an empty list
            if (futures.isEmpty()) return RFuture.success<List<T>>(emptyList<T>())

            val pseq = RPromise.create<List<T>>()
            val count = futures.size

            class Sequencer {
                @Synchronized fun onResult(idx: Int, result: Try<T>) {
                    if (result.isSuccess()) {
                        _results[idx] = result.get()
                    } else {
                        if (_error == null) _error = MultiFailureException()
                        _error!!.addFailure(result.failure())
                    }
                    if (--_remain == 0) {
                        _error?.let { pseq.fail(it) }
                        if (_error == null) {
                            val results = _results as Array<T>
                            pseq.succeed(results.asList())
                        }
                    }
                }

                protected val _results = arrayOfNulls<Any>(count)
                protected var _remain = count
                protected var _error: MultiFailureException? = null
            }

            val seq = Sequencer()
            val iter = futures.iterator()
            var ii = 0
            while (iter.hasNext()) {
                val idx = ii
                iter.next().onComplete({ result -> seq.onResult(idx, result) })
                ii++
            }
            return pseq
        }

        /** Returns a future containing the results of `a` and `b` if both futures complete
         * successfully, or a [MultiFailureException] aggregating all failures, if either of the
         * futures fails.  */
        fun <A, B> sequence(a: RFuture<A>, b: RFuture<B>): RFuture<Pair<A, B>> {
            val oa = a as RFuture<Any>
            val ob = b as RFuture<Any>
            return sequence(listOf(oa, ob)).map({ results ->
                val a = results[0] as A
                val b = results[1] as B
                Pair(a, b)
            })
        }

        /** Returns a future containing the results of `a`, `b`, and `c` if all
         * futures complete successfully, or a [MultiFailureException] aggregating all failures,
         * if any of the futures fails.  */
        fun <A, B, C> sequence(a: RFuture<A>, b: RFuture<B>, c: RFuture<C>): RFuture<Triple<A, B, C>> {
            val oa = a as RFuture<Any>
            val ob = b as RFuture<Any>
            val oc = c as RFuture<Any>
            return sequence(listOf(oa, ob, oc)).map({ results ->
                val a = results[0] as A
                val b = results[1] as B
                val c = results[2] as C
                Triple(a, b, c)
            })
        }

        /** Returns a future containing a list of all success results from `futures`. Any failure
         * results are simply omitted from the list. The success results are also in no particular
         * order. If all of `futures` fail, the resulting list will be empty.  */
        fun <T> collect(futures: Collection<RFuture<T>>): RFuture<Collection<T>> {
            // if we're passed an empty list of futures, succeed immediately with an empty list
            if (futures.isEmpty()) return RFuture.success<Collection<T>>(emptyList<T>())

            val pseq = RPromise.create<Collection<T>>()
            val count = futures.size
            val _results: MutableList<T> = ArrayList()
            var _remain = count
            val collector: SignalViewListener<Try<T>> = { result ->
                if (result.isSuccess()) _results.add(result.get())
                if (--_remain == 0) pseq.succeed(_results)
            }
            for (future in futures) future.onComplete(collector)
            return pseq
        }
    }
}
