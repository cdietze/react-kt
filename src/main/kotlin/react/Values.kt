/*
 * Copyright 2017 The React-kt Authors
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
 * Provides utility methods for [Value]s.
 */
object Values {

    /**
     * Returns a reactive value which is triggered when either of `a, b` emits an event. The
     * mapped value will retain connections to `a+b` only while it itself has connections.
     */
    fun <A, B> join(a: ValueView<A>, b: ValueView<B>): ValueView<Pair<A, B>> {
        return object : MappedValue<Pair<A, B>>() {
            override fun get(): Pair<A, B> {
                return _current
            }

            override fun connect(): Connection {
                return Connection.join(a.connect(_trigger), b.connect(_trigger))
            }

            protected val _trigger: UnitSlot = { _: Any? ->
                val ovalue = _current
                _current = Pair(a.get(), b.get())
                notifyChange(_current, ovalue)
            }
            protected var _current = Pair(a.get(), b.get())
        }
    }

    /**
     * Returns a reactive value which is triggered when either of `a, b, c` emits an event.
     * The mapped value will retain connections to `a+b+c` only while it itself has
     * connections.
     */
    fun <A, B, C> join(a: ValueView<A>, b: ValueView<B>,
                       c: ValueView<C>): ValueView<Triple<A, B, C>> {
        return object : MappedValue<Triple<A, B, C>>() {
            override fun get(): Triple<A, B, C> {
                return _current
            }

            override fun connect(): Connection {
                return Connection.join(
                        a.connect(_trigger), b.connect(_trigger), c.connect(_trigger))
            }

            protected val _trigger: UnitSlot = { _: Any? ->
                val ovalue = _current
                _current = Triple(a.get(), b.get(), c.get())
                notifyChange(_current, ovalue)
            }
            protected var _current = Triple(a.get(), b.get(), c.get())
        }
    }

    /**
     * Creates a boolean value that is toggled every time the supplied signal fires.
     *
     * @param signal the signal that will trigger the toggling.
     * @param initial the initial value of the to be toggled value.
     */
    fun toggler(signal: SignalView<*>, initial: Boolean): ValueView<Boolean> {
        return object : MappedValue<Boolean>() {
            override fun get(): Boolean {
                return _current
            }

            override fun connect(): Connection {
                return signal.connect({
                    val old = _current
                    _current = !old
                    notifyChange(_current, old)
                })
            }

            protected var _current = initial
        }
    }

    /**
     * Returns a value which is the logical NOT of the supplied value.
     */
    fun not(value: ValueView<Boolean>): ValueView<Boolean> = value.map { !it }

    /**
     * Returns a value which is the logical AND of the supplied values.
     */
    fun and(one: ValueView<Boolean>, two: ValueView<Boolean>): ValueView<Boolean> {
        return and(listOf(one, two))
    }

    /**
     * Returns a value which is the logical AND of the supplied values.
     */
    fun and(vararg values: ValueView<Boolean>): ValueView<Boolean> {
        return and(values.asList())
    }

    /**
     * Returns a value which is the logical AND of the supplied values.
     */
    fun and(values: Collection<ValueView<Boolean>>): ValueView<Boolean> {
        return aggValue(values, { v -> v.all { it.get() } })
    }

    /**
     * Returns a value which is the logical OR of the supplied values.
     */
    fun or(one: ValueView<Boolean>, two: ValueView<Boolean>): ValueView<Boolean> {
        return or(listOf(one, two))
    }

    /**
     * Returns a value which is the logical OR of the supplied values.
     */
    fun or(vararg values: ValueView<Boolean>): ValueView<Boolean> {
        return or(values.asList())
    }

    /**
     * Returns a value which is the logical OR of the supplied values.
     */
    fun or(values: Collection<ValueView<Boolean>>): ValueView<Boolean> {
        return aggValue(values, { v -> v.any { it.get() } })
    }

    /**
     * Returns a view of the supplied signal as a value. It will contain the value `initial`
     * until the signal fires, at which time the value will be updated with the emitted value.
     */
    fun <T> asValue(signal: SignalView<T>, initial: T): ValueView<T> {
        return object : MappedValue<T>() {
            override fun get(): T {
                return _value
            }

            override fun updateLocal(value: T): T {
                val ovalue = _value
                _value = value
                return ovalue
            }

            override fun connect(): Connection {
                return signal.connect({ value: T ->
                    updateAndNotifyIf(value)
                })
            }

            protected var _value = initial
        }
    }

    private fun aggValue(
            values: Collection<ValueView<Boolean>>,
            aggOp: (Iterable<ValueView<Boolean>>) -> Boolean): ValueView<Boolean> {

        return object : MappedValue<Boolean>() {
            override fun get(): Boolean {
                return aggOp.invoke(values)
            }

            override fun connect(): Connection {
                val conns = arrayOfNulls<Connection>(values.size)
                val iter = values.iterator()
                var _current: Boolean = aggOp.invoke(values)
                for (ii in conns.indices) conns[ii] = iter.next().connect({ _: Any? ->
                    val ovalue = _current
                    _current = aggOp.invoke(values)
                    notifyChange(_current, ovalue)
                })
                return Connection.join(*conns as Array<Connection>)
            }
        }
    }
}
