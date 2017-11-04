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

/** Listener for value changes.
 * The first parameter is the new value, the second is the old one or null on the first notification.
 */
typealias ValueViewListener<T> = (T, T?) -> Unit

/**
 * A view of a [Value], to which listeners may be added, but which one cannot update. This
 * can be used in combination with [AbstractValue] to provide [Value] semantics to an
 * entity which dispatches value changes in a custom manner (like over the network). Value
 * consumers should require only a view on a value, rather than a concrete value.
 */
interface ValueView<T> {

    /**
     * Returns the current value.
     */
    fun get(): T

    /**
     * Creates a value that maps this value via a function. When this value changes, the mapped
     * listeners will be notified, regardless of whether the new and old mapped values differ. The
     * mapped value will retain a connection to this value for as long as it has connections of its
     * own.
     */
    fun <M> map(func: (T) -> M): ValueView<M>

    /**
     * Creates a value that flat maps (monadic binds) this value via a function. When this value
     * changes, the mapping function is called to obtain a new reactive value. All of the listeners
     * to the flat mapped value are "transferred" to the new reactive value. The mapped value will
     * retain a connection to the most recent reactive value for as long as it has connections of
     * its own.
     */
    fun <M> flatMap(func: (T) -> ValueView<M>): ValueView<M>

    /**
     * Returns a signal that is emitted whenever this value changes.
     */
    fun changes(): SignalView<T>

    /**
     * Returns a future which is completed with this value when the value meeds `cond`. If
     * the value meets `cond` now, the future will be completed immediately, otherwise the
     * future will be completed when the value changes to a value which meets `cond`.
     */
    fun `when`(cond: (T) -> Boolean): RFuture<T>

    /**
     * Connects the supplied listener to this value, such that it will be notified when this value
     * changes. The listener is held by a strong reference, so it's held in memory by virtue of
     * being connected.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(listener: ValueViewListener<in T>): Connection

    /**
     * Connects the supplied listener to this value, such that it will be notified when this value
     * changes. Also immediately notifies the listener of the current value. Note that the previous
     * value supplied with this notification will be null. If the notification triggers an
     * unchecked exception, the slot will automatically be disconnected and the caller need not
     * worry about cleaning up after itself.
     * @return a connection instance which can be used to cancel the connectio
     * n.
     */
    fun connectNotify(listener: ValueViewListener<in T>): Connection

    /**
     * Disconnects the supplied listener from this value if it's connected. If the listener has been
     * connected multiple times, all connections are cancelled.
     */
    fun disconnect(listener: ValueViewListener<in T>)

    /**
     * Connects the supplied listener to this value, such that it will be notified when this value
     * changes. The listener is held by a strong reference, so it's held in memory by virtue of
     * being connected.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connect(listener: SignalViewListener<in T>): Connection

    /**
     * Connects the supplied listener to this value, such that it will be notified when this value
     * changes. Also immediately notifies the listener of the current value. If the notification
     * triggers an unchecked exception, the slot will automatically be disconnected and the caller
     * need not worry about cleaning up after itself.
     * @return a connection instance which can be used to cancel the connection.
     */
    fun connectNotify(listener: SignalViewListener<in T>): Connection
}
