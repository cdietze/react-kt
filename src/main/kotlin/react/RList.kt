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
 * Provides a reactive model of a list. Note that [.remove] *will not* trigger a
 * notification if the removed element is not present in the list. Use [.removeForce] to
 * force a notification.
 */
class RList<E>
/**
 * Creates a reactive list with the supplied underlying list implementation.
 */
(
        /** Contains our underlying elements.  */
        protected val _impl: MutableList<E>) : RCollection<E>(), MutableList<E> {
    /** Publishes list events to listeners.  */
    abstract class Listener<E> {
        /** Notifies listener of an added element. This method will call the index-forgetting
         * version ([.onAdd]) by default.  */
        open fun onAdd(index: Int, elem: E) {
            onAdd(elem)
        }

        /** Notifies listener of an added element.  */
        open fun onAdd(elem: E) {
            // noop
        }

        /** Notifies listener of an updated element. This method will call the old-value-forgetting
         * version ([.onSet]) by default.  */
        open fun onSet(index: Int, newElem: E, oldElem: E?) {
            onSet(index, newElem)
        }

        /** Notifies listener of an updated element.  */
        open fun onSet(index: Int, newElem: E) {
            // noop
        }

        /** Notifies listener of a removed element. This method will call the index-forgetting
         * version ([.onRemove]) by default.  */
        open fun onRemove(index: Int, elem: E) {
            onRemove(elem)
        }

        /** Notifies listener of a removed element.  */
        open fun onRemove(elem: E) {
            // noop
        }
    }

    /**
     * Connects the supplied listener to this list, such that it will be notified on adds and
     * removes.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(listener: Listener<in E>): Connection {
        return addConnection(listener)
    }

    /**
     * Invokes `onAdd(int,E)` for all existing list elements, then connects `listener`.
     */
    fun connectNotify(listener: Listener<in E>): Connection {
        var ii = 0
        val ll = size
        while (ii < ll) {
            listener.onAdd(ii, get(ii))
            ii++
        }
        return connect(listener)
    }

    /**
     * Disconnects the supplied listener from this list if listen was called with it.
     */
    fun disconnect(listener: Listener<in E>) {
        removeConnection(listener)
    }

    /**
     * Removes the supplied element from the list, forcing a notification to the listeners
     * regardless of whether the element was in the list or not.
     * @return true if the element was in the list and was removed, false if it was not.
     */
    fun removeForce(elem: E): Boolean {
        checkMutate()
        val index = _impl.indexOf(elem)
        if (index >= 0) _impl.removeAt(index)
        emitRemove(index, elem)
        return index >= 0
    }

    // List methods that perform reactive functions in addition to calling through
    override fun add(element: E): Boolean {
        add(size, element)
        return true
    }

    override fun add(index: Int, element: E) {
        checkMutate()
        _impl.add(index, element)
        emitAdd(index, element)
    }

    override fun addAll(collection: Collection<E>): Boolean {
        return addAll(size, collection)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        var index = index
        checkMutate()
        // Call add instead of calling _impl.addAll so if a listener throws an exception on
        // emission, we don't have elements added without a corresponding emission
        for (elem in elements) {
            add(index++, elem)
        }
        return true
    }

    override fun iterator(): MutableIterator<E> {
        return listIterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        val iiter = _impl.listIterator()
        return object : MutableListIterator<E> {
            override fun add(elem: E) {
                checkMutate()
                val index = iiter.nextIndex()
                iiter.add(elem)
                emitAdd(index, elem)
            }

            override fun hasNext(): Boolean {
                return iiter.hasNext()
            }

            override fun hasPrevious(): Boolean {
                return iiter.hasPrevious()
            }

            override fun next(): E {
                _current = iiter.next()
                return _current!!
            }

            override fun nextIndex(): Int {
                return iiter.nextIndex()
            }

            override fun previous(): E {
                _current = iiter.previous()
                return _current!!
            }

            override fun previousIndex(): Int {
                return iiter.previousIndex()
            }

            override fun remove() {
                checkMutate()
                val index = iiter.previousIndex()
                iiter.remove()
                emitRemove(index, this._current!!)
            }

            override fun set(elem: E) {
                checkMutate()
                iiter.set(elem)
                emitSet(iiter.previousIndex(), elem, this._current!!)
                _current = elem
            }

            protected var _current: E? = null // the element targeted by remove or set
        }
    }

    override fun retainAll(collection: Collection<E>): Boolean {
        var modified = false
        val iter = iterator()
        while (iter.hasNext()) {
            if (!collection.contains(iter.next())) {
                iter.remove()
                modified = true
            }
        }
        return modified
    }

    override fun removeAll(collection: Collection<E>): Boolean {
        var modified = false
        for (o in collection) {
            // Cannot inline this because it would short-circuit in JS-backend, see
            // https://discuss.kotlinlang.org/t/boolean-operations-in-js-backend-do-perform-short-circuit/5157
            val r = remove(o)
            modified = modified or r
        }
        return modified
    }

    override fun remove(`object`: E): Boolean {
        checkMutate()
        val index = _impl.indexOf(`object`)
        if (index < 0) return false
        _impl.removeAt(index)
        // the cast is safe if the element was removed
        val elem = `object` as E
        emitRemove(index, elem)
        return true
    }

    override fun removeAt(index: Int): E {
        checkMutate()
        val removed = _impl.removeAt(index)
        emitRemove(index, removed)
        return removed
    }

    override operator fun set(index: Int, element: E): E {
        checkMutate()
        val removed = _impl.set(index, element)
        emitSet(index, element, removed)
        return removed
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return RList(_impl.subList(fromIndex, toIndex))
    }

    override fun equals(other: Any?): Boolean {
        return other === this || _impl == other
    }

    override fun toString(): String {
        return "RList($_impl)"
    }

    // List methods that purely pass through to the underlying list
    override fun hashCode(): Int {
        return _impl.hashCode()
    }

    override val size: Int get() {
        return _impl.size
    }

    override fun isEmpty(): Boolean {
        return _impl.isEmpty()
    }

    override fun get(index: Int): E {
        return _impl[index]
    }

    override fun indexOf(element: E): Int {
        return _impl.indexOf(element)
    }

    override fun lastIndexOf(element: E): Int {
        return _impl.lastIndexOf(element)
    }

    override operator fun contains(`object`: E): Boolean {
        return _impl.contains(`object`)
    }

    override fun containsAll(collection: Collection<E>): Boolean {
        return _impl.containsAll(collection)
    }

    override fun clear() {
        // clear in such a way as to emit events
        while (!isEmpty()) removeAt(0)
    }

    override fun placeholderListener(): Listener<E> {
        val p = NOOP as Listener<E>
        return p
    }

    // Non-list RList implementation
    protected fun emitAdd(index: Int, elem: E) {
        notify(ADD, index, elem, null)
    }

    protected fun emitSet(index: Int, newElem: E, oldElem: E) {
        notify(SET, index, newElem, oldElem)
    }

    protected fun emitRemove(index: Int, elem: E) {
        notify(REMOVE, index, elem, null)
    }

    companion object {

        /**
         * Creates a reactive list backed by an [ArrayList].
         */
        fun <E> create(): RList<E> {
            return create(ArrayList())
        }

        /**
         * Creates a reactive list with the supplied underlying list implementation.
         */
        fun <E> create(impl: MutableList<E>): RList<E> {
            return RList(impl)
        }

        protected val NOOP: Listener<Any> = object : Listener<Any>() {

        }

        protected val ADD: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, index: Any?, elem: Any?, ignored: Any?) {
                (lner as Listener<Any>).onAdd((index as Int?)!!, elem!!)
            }
        }

        protected val SET: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, index: Any?, newElem: Any?, oldElem: Any?) {
                (lner as Listener<Any>).onSet((index as Int?)!!, newElem!!, oldElem)
            }
        }

        protected val REMOVE: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(lner: Any, index: Any?, elem: Any?, ignored: Any?) {
                (lner as Listener<Any>).onRemove((index as Int?)!!, elem!!)
            }
        }
    }
}
