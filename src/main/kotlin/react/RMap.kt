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

import kotlin.collections.MutableMap.MutableEntry

/**
 * Provides a reactive model of a map. Note that [.put] and other default mechanisms for
 * updating the map *will not* trigger a notification if the updated value is equal to the
 * value already in the map. Use [.putForce] to force a notification. Similarly, [ ][.remove] will only generate a notification if a mapping for the specified key existed, use
 * [.removeForce] to force a notification.
 */
class RMap<K, V>
/**
 * Creates a reactive map with the supplied underlying map implementation.
 */
(
        /** Contains our underlying mappings.  */
        protected var _impl: MutableMap<K, V>) : RCollection<MutableEntry<K, V>>(), MutableMap<K, V> {
    /** An interface for publishing map events to listeners.  */
    interface Listener<K, V> {
        /**
         * Notifies listener of an added or updated mapping. This method will call the
         * old-value-forgetting version ([.onPut]) by default.
         */
        fun onPut(key: K, value: V, oldValue: V?) {
            onPut(key, value)
        }

        /** Notifies listener of an added or updated mapping.  */
        fun onPut(key: K, value: V) {
            // noop
        }

        /**
         * Notifies listener of a removed mapping. This method will call the old-value-forgetting
         * version ([.onRemove]) by default.
         * [oldValue] may be `null` when force removing a inexistant key.
         */
        fun onRemove(key: K, oldValue: V?) {
            onRemove(key)
        }

        /** Notifies listener of a removed mapping.  */
        fun onRemove(key: K) {
            // noop
        }
    }

    /**
     * Connects the supplied listener to this map, such that it will be notified on puts and
     * removes.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(listener: Listener<in K, in V>): Connection {
        return addConnection(listener)
    }

    /**
     * Invokes `onPut` for all existing entries and then connects `listener`. Note that
     * the previous value supplied to the `onPut` calls will be null.
     */
    fun connectNotify(listener: Listener<in K, in V>): Connection {
        for ((key, value) in entries) {
            listener.onPut(key, value, null)
        }
        return connect(listener)
    }

    /**
     * Disconnects the supplied listener from this map if listen was called with it.
     */
    fun disconnect(listener: Listener<in K, in V>) {
        removeConnection(listener)
    }

    /**
     * Returns the mapping for `key` or `defaultValue` if there is no mapping for
     * `key`. *NOTE:* this method assumes the map does not contain a mapping to `null`. A mapping to `null` will be treated as if the mapping does not exist.
     */
    fun getOrElse(key: K, defaultValue: V): V {
        val value = _impl[key]
        return value ?: defaultValue
    }

    /**
     * Updates the mapping with the supplied key and value, and notifies registered listeners
     * regardless of whether the new value is equal to the old value.
     * @return the previous value mapped to the supplied key, or null.
     */
    fun putForce(key: K, value: V): V? {
        checkMutate()
        val ovalue = _impl.put(key, value)
        emitPut(key, value, ovalue)
        return ovalue
    }

    /**
     * Removes the mapping associated with the supplied key, and notifies registered listeners
     * regardless of whether a previous mapping existed or not.
     * @return the previous value mapped to the supplied key, or null.
     */
    fun removeForce(key: K): V? {
        checkMutate()
        val ovalue = _impl.remove(key)
        emitRemove(key, ovalue)
        return ovalue
    }

    /**
     * Returns a value view that models whether the specified key is contained in this map. The
     * view will report a change when a mapping for the specified key is added or removed. Note:
     * this view only works on maps that *do not* contain mappings to `null`. The view
     * will retain a connection to this map for as long as it has connections of its own.
     */
    fun containsKeyView(key: K): ValueView<Boolean> {
        return object : MappedValue<Boolean>() {
            override fun get(): Boolean {
                return containsKey(key)
            }

            override fun connect(): Connection {
                return this@RMap.connect(object : RMap.Listener<K, V> {
                    override fun onPut(pkey: K, value: V, ovalue: V?) {
                        if (key == pkey && ovalue == null) notifyChange(true, false)
                    }

                    override fun onRemove(rkey: K, ovalue: V?) {
                        if (key == rkey) notifyChange(false, true)
                    }
                })
            }
        }
    }

    /**
     * Returns a value view that models the mapping of the specified key in this map. The view will
     * report a change when the mapping for the specified key is changed or removed. The view will
     * retain a connection to this map for as long as it has connections of its own.
     */
    fun getView(key: K): ValueView<V?> {
        return object : MappedValue<V?>() {
            override fun get(): V? {
                return this@RMap[key]
            }

            override fun connect(): Connection {
                return this@RMap.connect(object : RMap.Listener<K, V> {
                    override fun onPut(pkey: K, value: V, ovalue: V?) {
                        if (key == pkey) notifyChange(value, ovalue)
                    }

                    override fun onRemove(pkey: K, ovalue: V?) {
                        if (key == pkey) notifyChange(null, ovalue)
                    }
                })
            }
        }
    }

    // from interface Map<K,V>
    override val size: Int get() {
        return _impl.size
    }

    // from interface Map<K,V>
    override fun isEmpty(): Boolean {
        return _impl.isEmpty()
    }

    // from interface Map<K,V>
    override fun containsKey(key: K): Boolean {
        return _impl.containsKey(key)
    }

    // from interface Map<K,V>
    override fun containsValue(value: V): Boolean {
        return _impl.containsValue(value)
    }

    override fun hashCode(): Int {
        return _impl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other === this || _impl == other
    }

    override fun toString(): String {
        return "RMap" + _impl
    }

    // from interface Map<K,V>
    override operator fun get(key: K): V? {
        return _impl.get(key)
    }

    // from interface Map<K,V>
    override fun put(key: K, value: V): V? {
        checkMutate()
        val ovalue = _impl.put(key, value)
        if (!areEqual(value, ovalue)) {
            emitPut(key, value, ovalue)
        }
        return ovalue
    }

    // from interface Map<K,V>
    override fun remove(rawKey: K): V? {
        checkMutate()

        // avoid generating an event if no mapping exists for the supplied key
        if (!_impl.containsKey(rawKey)) {
            return null
        }

        val key = rawKey as K
        val ovalue = _impl.remove(key)
        emitRemove(key, ovalue)

        return ovalue
    }

    // from interface Map<K,V>
    override fun putAll(map: Map<out K, V>) {
        for ((key, value) in map) {
            put(key, value)
        }
    }

    // from interface Map<K,V>
    override fun clear() {
        checkMutate()
        // generate removed events for our keys (do so on a copy of our set so that we can clear
        // our underlying map before any of the published events are processed)
        val entries = HashSet<MutableEntry<K, V>>(_impl.entries)
        _impl.clear()
        for (entry in entries) emitRemove(entry.key, entry.value)
    }

    // from interface Map<K,V>
    override val keys: MutableSet<K> get() {
        val iset = _impl.keys
        return object : AbstractMutableSet<K>() {
            override fun add(element: K): Boolean = throw UnsupportedOperationException()

            override fun iterator(): MutableIterator<K> {
                val iiter = iset.iterator()
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean {
                        return iiter.hasNext()
                    }

                    override fun next(): K {
                        _current = iiter.next()
                        return _current!!
                    }

                    override fun remove() {
                        checkMutate()
                        if (_current == null) throw IllegalStateException()
                        val ovalue = this@RMap[_current!!]
                        iiter.remove()
                        emitRemove(_current!!, ovalue)
                        _current = null
                    }

                    protected var _current: K? = null
                }
            }

            override val size: Int get () = this@RMap.size

            override fun remove(element: K): Boolean {
                checkMutate()
                val ovalue = this@RMap[element]
                val modified = iset.remove(element)
                if (modified) {
                    emitRemove(element, ovalue)
                }
                return modified
            }

            override fun clear() {
                this@RMap.clear()
            }
        }
    }

    // from interface Map<K,V>
    override val values: MutableCollection<V> get() {
        val iset = _impl.entries
        return object : AbstractMutableCollection<V>() {
            override fun add(element: V): Boolean {
                throw UnsupportedOperationException()
            }

            override fun iterator(): MutableIterator<V> {
                val iiter = iset.iterator()
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean {
                        return iiter.hasNext()
                    }

                    override fun next(): V {
                        _current = iiter.next()
                        return _current!!.value
                    }

                    override fun remove() {
                        checkMutate()
                        iiter.remove()
                        emitRemove(_current!!.key, _current!!.value)
                        _current = null
                    }

                    protected var _current: MutableEntry<K, V>? = null
                }
            }

            override val size: Int get() {
                return this@RMap.size
            }

            override fun contains(o: V): Boolean {
                return this@RMap.containsValue(o)
            }

            override fun clear() {
                this@RMap.clear()
            }
        }
    }

    // from interface Map<K,V>
    override val entries: MutableSet<MutableEntry<K, V>> get() {
        val iset = _impl.entries
        return object : AbstractMutableSet<MutableEntry<K, V>>() {
            override fun add(element: MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()
            override val size: Int get() = this@RMap.size

            override fun iterator(): MutableIterator<MutableEntry<K, V>> {
                val iiter = iset.iterator()
                return object : MutableIterator<MutableEntry<K, V>> {
                    override fun hasNext(): Boolean {
                        return iiter.hasNext()
                    }

                    override fun next(): MutableEntry<K, V> {
                        _current = iiter.next()
                        return object : MutableEntry<K, V> {
                            override val key: K get() {
                                return _ientry!!.key
                            }

                            override var value: V
                                get() {
                                    return _ientry!!.value
                                }
                                set(value) = setValue(value) as Unit

                            override fun setValue(newValue: V): V {
                                checkMutate()
                                if (!iset.contains(this))
                                    throw IllegalStateException(
                                            "Cannot update removed map entry.")
                                val ovalue = _ientry!!.setValue(newValue)
                                if (!areEqual(newValue, ovalue)) {
                                    emitPut(_ientry!!.key, newValue, ovalue)
                                }
                                return ovalue
                            }

                            // it's safe to pass these through because Map.Entry's
                            // implementations operate solely on getKey/getValue
                            override fun equals(o: Any?): Boolean {
                                return _ientry == o
                            }

                            override fun hashCode(): Int {
                                return _ientry!!.hashCode()
                            }

                            protected var _ientry = _current
                        }
                    }

                    override fun remove() {
                        checkMutate()
                        iiter.remove()
                        emitRemove(_current!!.key, _current!!.value)
                        _current = null
                    }

                    protected var _current: MutableEntry<K, V>? = null
                }
            }

            override fun contains(element: MutableEntry<K, V>): Boolean = iset.contains(element)

            override fun remove(element: MutableEntry<K, V>): Boolean {
                checkMutate()
                val modified = iset.remove(element)
                if (modified) {
                    emitRemove(element.key, element.value)
                }
                return modified
            }

            override fun clear() {
                this@RMap.clear()
            }
        }
    }

    override fun placeholderListener(): Listener<K, V> {
        return NOOP as Listener<K, V>
    }

    protected fun emitPut(key: K, value: V, oldValue: V?) {
        notifyPut(key, value, oldValue)
    }

    protected fun notifyPut(key: K, value: V, oldValue: V?) {
        notify(PUT, key, value, oldValue)
    }

    protected fun emitRemove(key: K, oldValue: V?) {
        notifyRemove(key, oldValue)
    }

    protected fun notifyRemove(key: K, oldValue: V?) {
        notify(REMOVE, key, oldValue, null)
    }

    companion object {

        /**
         * Creates a reactive map that uses a [HashMap] as its underlying implementation.
         */
        fun <K, V> create(): RMap<K, V> {
            return create(HashMap())
        }

        /**
         * Creates a reactive map with the supplied underlying map implementation.
         */
        fun <K, V> create(impl: MutableMap<K, V>): RMap<K, V> {
            return RMap(impl)
        }

        protected val NOOP: Listener<Any, Any> = object : Listener<Any, Any> {

        }

        protected val PUT: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, key: Any?, value: Any?, oldValue: Any?) {
                (lner as Listener<Any, Any>).onPut(key!!, value!!, oldValue)
            }
        }

        protected val REMOVE: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, key: Any?, oldValue: Any?, ignored: Any?) {
                (lner as Listener<Any, Any>).onRemove(key!!, oldValue)
            }
        }
    }
}
