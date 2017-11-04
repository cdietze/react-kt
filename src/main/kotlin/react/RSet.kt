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
 * Provides a reactive model of a set. Note that [.add] and other default mechanisms for
 * updating the set *will not* trigger a notification if the updated element is equal to an
 * element already in the set. Use [.addForce] to force a notification. Similarly, [ ][.remove] will only generate a notification if an element was actually removed, use [ ][.removeForce] to force a notification.
 */
class RSet<E>
/**
 * Creates a reactive set with the supplied underlying set implementation.
 */
(
        /** Contains our underlying elements.  */
        protected var _impl: MutableSet<E>) : RCollection<E>(), MutableSet<E> {
    /** An interface for publishing set events to listeners.  */
    abstract class Listener<E> : RListener() {
        /** Notifies listener of an added element.  */
        open fun onAdd(elem: E) {
            // noop
        }

        /** Notifies listener of a removed element.  */
        open fun onRemove(elem: E) {
            // noop
        }
    }

    /**
     * Connects the supplied listener to this set, such that it will be notified on adds and
     * removes.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(listener: Listener<in E>): Connection {
        return addConnection(listener)
    }

    /**
     * Invokes `onAdd` for all existing elements, and then connects `listener`.
     */
    fun connectNotify(listener: Listener<in E>): Connection {
        for (elem in this) listener.onAdd(elem)
        return connect(listener)
    }

    /**
     * Disconnects the supplied listener from this set if listen was called with it.
     */
    fun disconnect(listener: Listener<in E>) {
        removeConnection(listener)
    }

    /**
     * Adds the supplied element to the set, forcing a notification to the listeners regardless of
     * whether the element was already in the set or not.
     * @return true if the element was added, false if it was already in the set.
     */
    fun addForce(elem: E): Boolean {
        checkMutate()
        val added = _impl.add(elem)
        emitAdd(elem)
        return added
    }

    /**
     * Removes the supplied element from the set, forcing a notification to the listeners
     * regardless of whether the element was already in the set or not.
     * @return true if the element was in the set and was removed, false if it was not.
     */
    fun removeForce(elem: E): Boolean {
        checkMutate()
        val removed = _impl.remove(elem)
        emitRemove(elem)
        return removed
    }

    /**
     * Returns a value that models whether the specified element is contained in this map. The
     * value will report a change when the specified element is added or removed. Note that [ ][.addForce] or [.removeForce] will cause this view to trigger and incorrectly report
     * that the element was not or was previously contained in the set. Caveat user.
     */
    fun containsView(elem: E): ValueView<Boolean> {
        return object : MappedValue<Boolean>() {
            override fun get(): Boolean {
                return contains(elem)
            }

            override fun connect(): Connection {
                return this@RSet.connect(object : RSet.Listener<E>() {
                    override fun onAdd(aelem: E) {
                        if (elem == aelem) notifyChange(true, false)
                    }

                    override fun onRemove(relem: E) {
                        if (elem == relem) notifyChange(false, true)
                    }
                })
            }
        }
    }

    // from interface Set<E>
    override val size: Int
        get() {
            return _impl.size
        }

    // from interface Set<E>
    override fun isEmpty(): Boolean {
        return _impl.isEmpty()
    }

    // from interface Set<E>
    override operator fun contains(key: E): Boolean {
        return _impl.contains(key)
    }

    // from interface Set<E>
    override fun add(elem: E): Boolean {
        checkMutate()
        if (!_impl.add(elem)) return false
        emitAdd(elem)
        return true
    }

    // from interface Set<E>
    override fun remove(rawElem: E): Boolean {
        checkMutate()
        if (!_impl.remove(rawElem)) return false
        val elem = rawElem as E
        emitRemove(elem)
        return true
    }

    // from interface Set<E>
    override fun containsAll(coll: Collection<E>): Boolean {
        return _impl.containsAll(coll)
    }

    // from interface Set<E>
    override fun addAll(coll: Collection<E>): Boolean {
        var modified = false
        for (elem in coll) {
            // Cannot inline this because it would short-circuit in JS-backend, see
            // https://discuss.kotlinlang.org/t/boolean-operations-in-js-backend-do-perform-short-circuit/5157
            val r = add(elem)
            modified = modified or r
        }
        return modified
    }

    // from interface Set<E>
    override fun retainAll(coll: Collection<E>): Boolean {
        var modified = false
        val iter = iterator()
        while (iter.hasNext()) {
            if (!coll.contains(iter.next())) {
                iter.remove()
                modified = true
            }
        }
        return modified
    }

    // from interface Set<E>
    override fun removeAll(coll: Collection<E>): Boolean {
        var modified = false
        val iter = coll.iterator()
        while (iter.hasNext()) {
            // Cannot inline this because it would short-circuit in JS-backend, see
            // https://discuss.kotlinlang.org/t/boolean-operations-in-js-backend-do-perform-short-circuit/5157
            val r = remove(iter.next())
            modified = modified or r
        }
        return modified
    }

    // from interface Set<E>
    override fun clear() {
        checkMutate()
        // generate removed events for our elemens (do so on a copy of our set so that we can clear
        // our underlying set before any of the published events are processed)
        val elems = ArrayList(_impl)
        _impl.clear()
        for (elem in elems) emitRemove(elem)
    }

    // from interface Set<E>
    override fun iterator(): MutableIterator<E> {
        val iiter = _impl.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean {
                return iiter.hasNext()
            }

            override fun next(): E {
                _current = iiter.next()
                return _current!!
            }

            override fun remove() {
                checkMutate()
                iiter.remove()
                emitRemove(_current!!)
            }

            protected var _current: E? = null
        }
    }

    override fun hashCode(): Int {
        return _impl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other === this || _impl == other
    }

    override fun toString(): String {
        return "RSet" + _impl
    }

    override fun placeholderListener(): Listener<E> {
        val p = NOOP as Listener<E>
        return p
    }

    protected fun emitAdd(elem: E) {
        notifyAdd(elem)
    }

    protected fun notifyAdd(elem: E) {
        notify(ADD, elem, null, null)
    }

    protected fun emitRemove(elem: E) {
        notifyRemove(elem)
    }

    protected fun notifyRemove(elem: E) {
        notify(REMOVE, elem, null, null)
    }

    companion object {

        /**
         * Creates a reactive set backed by a @{link HashSet}.
         */
        fun <E> create(): RSet<E> {
            return create(HashSet())
        }

        /**
         * Creates a reactive set with the supplied underlying set implementation.
         */
        fun <E> create(impl: MutableSet<E>): RSet<E> {
            return RSet(impl)
        }

        protected val NOOP: Listener<Any> = object : Listener<Any>() {

        }

        protected val ADD: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, elem: Any?, _1: Any?, _2: Any?) {
                (lner as Listener<Any>).onAdd(elem!!)
            }
        }

        protected val REMOVE: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, elem: Any?, _1: Any?, _2: Any?) {
                (lner as Listener<Any>).onRemove(elem!!)
            }
        }
    }
}
