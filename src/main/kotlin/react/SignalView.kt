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
 * A view of a [Signal], on which slots may listen, but to which one cannot emit events. This
 * is generally used to provide signal-like views of changing entities. See [AbstractValue]
 * for an example.
 */
interface SignalView<T> {

    /**
     * Creates a signal that maps this signal via a function. When this signal emits a value, the
     * mapped signal will emit that value as transformed by the supplied function. The mapped
     * signal will retain a connection to this signal for as long as it has connections of its own.
     */
    fun <M> map(func: (T) -> M): SignalView<M>

    /**
     * Creates a signal that emits a value only when the supplied filter function returns true. The
     * filtered signal will retain a connection to this signal for as long as it has connections of
     * its own.
     */
    fun filter(pred: (T) -> Boolean): SignalView<T>

    /**
     * Returns a future that is completed with the next value from this signal.
     */
    fun next(): RFuture<T>

    /**
     * Connects this signal to the supplied slot, such that when an event is emitted from this
     * signal, the slot will be notified.

     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(slot: SignalViewListener<in T>): Connection

    /**
     * Disconnects the supplied slot from this signal if connect was called with it. If the slot has
     * been connected multiple times, all connections are cancelled.
     */
    fun disconnect(slot: SignalViewListener<in T>)
}

/** Used to observe events from a signal.
 * Called when a signal to which this slot is connected has emitted an event.
 * @param T the event type emitted by the signal.*/
typealias SignalViewListener<T> = (T) -> Unit
