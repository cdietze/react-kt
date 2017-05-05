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

import java.lang.ref.WeakReference

/**
 * Implements [Connection] and a linked-list style listener list for [Reactor]s.
 */
class Cons(private var _owner: Reactor?, listener: RListener?) : Connection() {
    /** The next connection in our chain.  */
    var next: Cons? = null

    private var _ref: ListenerRef
    private var _oneShot: Boolean = false // defaults to false
    private var _priority: Int = 0 // defaults to zero

    /** Indicates whether this connection is one-shot or persistent.  */
    fun oneShot(): Boolean {
        return _oneShot
    }

    /** Returns the listener for this cons cell.  */
    fun listener(): RListener? {
        return _ref.get(this)
    }

    override fun close() {
        // multiple disconnects are OK, we just NOOP after the first one
        _owner?.let {
            _ref.defang(it.placeholderListener())
            it.disconnect(this)
            _owner = null
        }
    }

    override fun once(): Connection {
        _oneShot = true
        return this
    }

    override fun atPrio(priority: Int): Connection {
        if (_owner == null)
            throw IllegalStateException(
                    "Cannot change priority of disconnected connection.")
        _owner!!.disconnect(this)
        next = null
        _priority = priority
        _owner!!.addCons(this)
        return this
    }

    override fun holdWeakly(): Connection {
        if (_owner == null)
            throw IllegalStateException(
                    "Cannot change disconnected connection to weak.")
        if (!_ref.isWeak) _ref.get(this)?.let { _ref = WeakRef(it) }
        return this
    }

    override fun toString(): String {
        return "[owner=" + _owner + ", pri=" + _priority + ", lner=" + listener() +
                ", hasNext=" + (next != null) + ", oneShot=" + oneShot() + "]"
    }

    init {
        _ref = StrongRef(listener)
    }

    private abstract class ListenerRef {
        internal abstract val isWeak: Boolean
        internal abstract fun defang(noop: RListener)
        internal abstract fun get(cons: Cons): RListener?
    }

    private class StrongRef(private var _lner: RListener?) : ListenerRef() {
        public override val isWeak: Boolean
            get() = false

        public override fun defang(noop: RListener) {
            _lner = noop
        }

        public override fun get(cons: Cons): RListener? {
            return _lner
        }
    }

    private class WeakRef(lner: RListener) : ListenerRef() {
        private var _wref: WeakReference<RListener>? = null
        private var _noop: RListener? = null

        init {
            _wref = WeakReference(lner)
        }

        public override val isWeak: Boolean
            get() = true

        public override fun defang(noop: RListener) {
            _noop = noop
            _wref = null
        }

        public override fun get(cons: Cons): RListener? {
            if (_wref != null) {
                val listener = _wref!!.get()
                if (listener != null) return listener
                cons.close() // close will defang() us
            }
            return _noop
        }
    }

    companion object {

        fun insert(head: Cons?, cons: Cons): Cons {
            if (head == null) {
                return cons
            } else if (cons._priority > head._priority) {
                cons.next = head
                return cons
            } else {
                head.next = insert(head.next, cons)
                return head
            }
        }

        fun remove(head: Cons?, cons: Cons): Cons? {
            if (head == null) return head
            if (head === cons) return head.next
            head.next = remove(head.next, cons)
            return head
        }

        fun removeAll(head: Cons?, listener: RListener): Cons? {
            if (head == null) return null
            if (head.listener() === listener) return removeAll(head.next, listener)
            head.next = removeAll(head.next, listener)
            return head
        }
    }
}
