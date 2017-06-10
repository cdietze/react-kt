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
 * Handles the machinery of connecting slots to a signal and emitting events to them, without
 * exposing a public interface for emitting events. This can be used by entities which wish to
 * expose a signal-like interface for listening, without allowing external callers to emit signals.
 */
open class AbstractSignal<T> : Reactor(), SignalView<T> {
    override fun <M> map(func: (T) -> M): SignalView<M> {
        val outer = this
        return object : MappedSignal<M>() {
            override fun connect(): Connection {
                return outer.connect { value -> notifyEmit(func(value)) }
            }
        }
    }

    override fun filter(pred: (T) -> Boolean): SignalView<T> {
        val outer = this
        return object : MappedSignal<T>() {
            override fun connect(): Connection {
                return outer.connect { value ->
                    if (pred(value)) {
                        notifyEmit(value)
                    }
                }
            }
        }
    }

    override fun next(): RFuture<T> {
        val result = RPromise.create<T>()
        connect(result.succeeder()).once()
        return result
    }

    override fun connect(slot: SignalViewListener<in T>): Connection {
        return addConnection(slot)
    }

    override fun disconnect(slot: SignalViewListener<in T>) {
        removeConnection(slot)
    }

    override fun placeholderListener(): SignalViewListener<T> {
        val p = Slots.NOOP as SignalViewListener<T>
        return p
    }

    /**
     * Emits the supplied event to all connected slots.
     */
    protected fun notifyEmit(event: T) {
        notify(EMIT, event, null, null)
    }

    companion object {

        protected val EMIT: Reactor.Notifier = object : Reactor.Notifier() {
            override fun notify(listener: Any, event: Any?, a2: Any?, a3: Any?) {
                (listener as SignalViewListener<Any?>)(event)
            }
        }
    }
}
