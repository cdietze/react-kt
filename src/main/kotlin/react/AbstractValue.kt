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
 * Handles the machinery of connecting listeners to a value and notifying them, without exposing a
 * public interface for updating the value. This can be used by libraries which wish to provide
 * observable values, but must manage the maintenance and distribution of value updates themselves
 * (so that they may send them over the network, for example).
 */
abstract class AbstractValue<T> : Reactor(), ValueView<T> {
    override fun <M> map(func: (T) -> M): ValueView<M> {
        val outer = this
        return object : MappedValue<M>() {
            override fun get(): M {
                return func(outer.get())
            }

            override fun toString(): String {
                return outer.toString() + ".map(" + func + ")"
            }

            override fun connect(): Connection {
                return outer.connect { value: T, oldValue: T? -> notifyChange(func(value), oldValue?.let { func(it) }) }
            }
        }
    }

    override fun <M> flatMap(func: (T) -> ValueView<M>): ValueView<M> {
        val outer = this
        val mapped = map(func)
        return object : MappedValue<M>() {
            private var conn: Connection? = null

            override fun get(): M {
                return mapped.get().get()
            }

            override fun toString(): String {
                return outer.toString() + ".flatMap(" + func + ")"
            }

            override fun connect(): Connection {
                conn = mapped.connect { value: ValueView<M>, oldValue: ValueView<M>? -> reconnect() }
                return mapped.get().connect { value: M, oldValue: M? -> notifyChange(value, oldValue) }
            }

            override fun disconnect() {
                super.disconnect()
                if (conn != null) conn!!.close()
            }
        }
    }

    override fun changes(): SignalView<T> {
        val outer = this
        return object : MappedSignal<T>() {
            override fun connect(): Connection {
                return outer.connect { value: T, oldValue: T? -> notifyEmit(value) }
            }
        }
    }

    override fun `when`(cond: (T) -> Boolean): RFuture<T> {
        val current = get()
        if (cond(current))
            return RFuture.success(current)
        else
            return changes().filter(cond).next()
    }

    override fun connect(listener: ValueViewListener<T>): Connection {
        return addConnection(listener)
    }

    override fun connectNotify(listener: ValueViewListener<T>): Connection {
        // connect before calling emit; if the listener changes the value in the body of onEmit, it
        // will expect to be notified of that change; however if onEmit throws a runtime exception,
        // we need to take care of disconnecting the listener because the returned connection
        // instance will never reach the caller
        val conn = connect(listener)
        try {
            listener(get(), null)
            return conn
        } catch (re: RuntimeException) {
            conn.close()
            throw re
        } catch (e: Error) {
            conn.close()
            throw e
        }
    }

    override fun connect(listener: SignalViewListener<T>): Connection {
        return addConnection(wrap(listener))
    }

    override fun connectNotify(listener: SignalViewListener<T>): Connection {
        return connectNotify(wrap(listener))
    }
//
//    override fun connect(slot: Slot<in T>): Connection {
//        return connect(slot as ValueViewListener<in T>)
//    }
//
//    override fun connectNotify(slot: Slot<in T>): Connection {
//        return connectNotify(slot as ValueViewListener<in T>)
//    }

    override fun disconnect(listener: ValueViewListener<in T>) {
        removeConnection(listener)
    }

//    override fun disconnect(listener: SignalViewListener<in T>) {
//        removeConnection(listener)
//    }

    override fun hashCode(): Int {
        val value = get()
        return value?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other::class != this::class) return false
        val value = get()
        val ovalue = (other as AbstractValue<T>).get()
        return Reactor.areEqual(value, ovalue)
    }

    override fun toString(): String {
        val cname = this::class.simpleName ?: "null"
        return cname.substring(cname.lastIndexOf(".") + 1) + "(" + get() + ")"
    }

    override fun placeholderListener(): ValueViewListener<T> {
        val p = NOOP as ValueViewListener<T>
        return p
    }

    /**
     * Updates the value contained in this instance and notifies registered listeners iff said
     * value is not equal to the value already contained in this instance (per [.areEqual]).
     */
    protected fun updateAndNotifyIf(value: T): T {
        return updateAndNotify(value, false)
    }

    /**
     * Updates the value contained in this instance and notifies registered listeners.
     * @param force if true, the listeners will always be notified, if false the will be notified
     * * only if the new value is not equal to the old value (per [.areEqual]).
     * *
     * @return the previously contained value.
     */
    protected fun updateAndNotify(value: T, force: Boolean = true): T {
        checkMutate()
        val ovalue = updateLocal(value)
        if (force || !Reactor.areEqual(value, ovalue)) {
            emitChange(value, ovalue)
        }
        return ovalue
    }

    /**
     * Emits a change notification. Default implementation immediately notifies listeners.
     */
    protected fun emitChange(value: T, oldValue: T) {
        notifyChange(value, oldValue)
    }

    /**
     * Notifies our listeners of a value change.
     */
    protected fun notifyChange(value: T, oldValue: T?) {
        notify(CHANGE, value, oldValue, null)
    }

    /**
     * Updates our locally stored value. Default implementation throws unsupported operation.
     * @return the previously stored value.
     */
    protected open fun updateLocal(value: T): T {
        throw UnsupportedOperationException()
    }

    companion object {

        val NOOP: ValueViewListener<Any> = { _, _ -> }

        private fun <T> wrap(listener: SignalViewListener<T>): ValueViewListener<T> {
            return { value: T, oldValue: T? -> listener.invoke(value) }
        }

        protected val CHANGE: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(listener: Any, value: Any?, oldValue: Any?, a3: Any?) {
                (listener as ValueViewListener<Any>).invoke(value!!, oldValue!!)
            }
        }
    }
}
/**
 * Updates the value contained in this instance and notifies registered listeners.
 * @return the previously contained value.
 */
