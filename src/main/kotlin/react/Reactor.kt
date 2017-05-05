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

/** The base class for all reactor listeners.  */
typealias RListener = Any

/**
 * A base class for all reactive classes. This is an implementation detail, but is public so that
 * third parties may use it to create their own reactive classes, if desired.
 */
abstract class Reactor {

    /**
     * Returns true if this reactor has at least one connection.
     */
    fun hasConnections(): Boolean {
        return _listeners != null
    }

    /**
     * Clears all connections from this reactor. This is not used in normal circumstances, but is
     * made available for libraries which build on react and need a way to forcibly disconnect all
     * connections to reactive state.

     * @throws IllegalStateException if this reactor is in the middle of dispatching an event.
     */
    @Synchronized fun clearConnections() {
        if (isDispatching)
            throw IllegalStateException(
                    "Cannot clear connections while dispatching.")
        if (_pendingRuns != null)
            throw IllegalStateException("Cannot clear connections while running.")
        _listeners = null
    }

    /** Returns the listener to be used when a weakly held listener is discovered to have been
     * collected while dispatching. This listener should NOOP when signaled.  */
    internal abstract fun placeholderListener(): RListener

    @Synchronized fun addConnection(listener: RListener?): Cons {
        if (listener == null) throw NullPointerException("Null listener")
        return addCons(Cons(this, listener))
    }

    @Synchronized fun addCons(cons: Cons): Cons {
        if (isDispatching) {
            _pendingRuns = append(_pendingRuns, object : Runs() {
                override fun run() {
                    _listeners = Cons.insert(_listeners, cons)
                    connectionAdded()
                }
            })
        } else {
            _listeners = Cons.insert(_listeners, cons)
            connectionAdded()
        }
        return cons
    }

    @Synchronized fun disconnect(cons: Cons) {
        if (isDispatching) {
            _pendingRuns = append(_pendingRuns, object : Runs() {
                override fun run() {
                    _listeners = Cons.remove(_listeners, cons)
                    connectionRemoved()
                }
            })
        } else {
            _listeners = Cons.remove(_listeners, cons)
            connectionRemoved()
        }
    }

    @Synchronized protected fun removeConnection(listener: RListener) {
        if (isDispatching) {
            _pendingRuns = append(_pendingRuns, object : Runs() {
                override fun run() {
                    _listeners = Cons.removeAll(_listeners, listener)
                    connectionRemoved()
                }
            })
        } else {
            _listeners = Cons.removeAll(_listeners, listener)
            connectionRemoved()
        }
    }

    /**
     * Called prior to mutating any underlying model; allows subclasses to reject mutation.
     */
    protected fun checkMutate() {
        // noop
    }

    /**
     * Called when a connection has been added to this reactor.
     */
    protected open fun connectionAdded() {
        // noop
    }

    /**
     * Called when a connection may have been removed from this reactor.
     */
    protected open fun connectionRemoved() {
        // noop
    }

    /**
     * Emits the supplied event to all connected slots. We omit a bunch of generic type shenanigans
     * here and force the caller to just cast things, because this is all under the hood where
     * there's zero chance of fucking up and this results in simpler, easier to read code.
     */
    protected open fun notify(notifier: Notifier, a1: Any?, a2: Any?,
                              a3: Any?) {
        var lners: Cons? = null

        synchronized(this) {
            // if we're currently dispatching, defer this notification until we're done
            if (_listeners === DISPATCHING) {
                _pendingRuns = append(_pendingRuns, object : Runs() {
                    override fun run() {
                        this@Reactor.notify(notifier, a1, a2, a3)
                    }
                })
                return@synchronized false
            } else {
                lners = _listeners
                val sentinel = DISPATCHING
                _listeners = sentinel
                return@synchronized true
            }
        } || return

        var exn: RuntimeException? = null
        try {
            // perform this dispatch, catching and accumulating any errors
            var cons: Cons? = lners
            while (cons != null) {
                try {
                    cons.listener()?.let { notifier.notify(it, a1, a2, a3) }
                } catch (ex: RuntimeException) {
                    // Java7: if (exn != null) exn.addSuppressed(ex)
                    exn = ex
                }

                if (cons.oneShot()) cons.close()
                cons = cons.next
            }

        } finally {
            // note that we're no longer dispatching
            synchronized(this) {
                _listeners = lners
            }

            // perform any operations that were deferred while we were dispatching
            while (true) {
                val run = nextRun() ?: break
                try {
                    run.run()
                } catch (ex: RuntimeException) {
                    // Java7: if (exn != null) exn.addSuppressed(ex)
                    exn = ex
                }

            }
        }

        // finally throw any exception(s) that occurred during dispatch
        if (exn != null) throw exn
    }

    @Synchronized private fun nextRun(): Runs? {
        val run = _pendingRuns
        if (run != null) _pendingRuns = run.next
        return run
    }

    // always called while lock is held on this reactor
    private val isDispatching: Boolean
        get() = _listeners === DISPATCHING

    protected var _listeners: Cons? = null
    protected var _pendingRuns: Runs? = null

    protected abstract class Runs {
        var next: Runs? = null
        abstract fun run(): Any?
    }

    abstract class Notifier {
        abstract fun notify(listener: Any, a1: Any?, a2: Any?, a3: Any?)
    }

    companion object {

        /**
         * Returns true if both values are null, reference the same instance, or are
         * [Object.equals].
         */
        fun <T> areEqual(o1: T?, o2: T): Boolean {
            return o1 === o2 || o1 != null && o1 == o2
        }

        private fun append(head: Runs?, action: Runs): Runs {
            if (head == null) return action
            head.next = append(head.next, action)
            return head
        }

        protected val DISPATCHING = Cons(null, null)
    }
}
