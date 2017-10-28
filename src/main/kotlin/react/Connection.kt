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
 * Provides a mechanism to cancel a slot or listener registration, or to perform post-registration
 * adjustment like making the registration single-shot.
 */
abstract class Connection : Closeable {

    /**
     * Disconnects this registration. Subsequent events will not be dispatched to the associated
     * slot or listener.
     */
    abstract override fun close()

    /**
     * Converts this connection into a one-shot connection. After the first time the slot or
     * listener is notified, it will automatically be disconnected.

     *
     * *NOTE:* if you are dispatching signals in a multithreaded environment, it is
     * possible for your connected listener to be notified before this call has a chance to mark it
     * as one-shot. Thus you could receive multiple notifications. If you require this to be
     * avoided, you must synchronize on the signal/value/etc. on which you are adding a
     * listener:

     * <pre>`Signal<Foo> signal = ...;
     * Connection conn;
     * synchronized (signal) {
     * conn = signal.connect(slot).once();
     * }
    `</pre> *

     * @return this connection instance for convenient chaining.
     */
    abstract fun once(): Connection

    /**
     * Changes the priority of this connection to the specified value. Connections are notified from
     * highest priority to lowest priority. The default priority is zero.

     *
     * This should generally be done simultaneously with creating a connection. For example:

     * <pre>`Signal<Foo> signal = ...;
     * Connection conn = signal.connect(new Slot<Foo>() { ... }).atPrio(5);
    `</pre> *

     *
     * *NOTE:* if you are dispatching signals in a multithreaded environment, it is
     * possible for your connected listener to be notified at priority zero before this call has a
     * chance to update its priority. If you require this to be avoided, you must synchronize on
     * the signal/value/etc. on which you are adding a listener:

     * <pre>`Signal<Foo> signal = ...;
     * Connection conn;
     * synchronized (signal) {
     * conn = signal.connect(slot).atPrio(5);
     * }
    `</pre> *

     * @return this connection instance for convenient chaining.
     */
    abstract fun atPrio(priority: Int): Connection

    /**
     * Changes this connection to one held by a weak reference. It only remains connected as long
     * as its target listener is referenced elsewhere.

     *
     * *NOTE:* weak references are not supported in JavaScript. When using this library
     * in GWT, the reference remains strong.

     * @return this connection instance for convenient chaining.
     */
    abstract fun holdWeakly(): Connection

    companion object {
        /**
         * Returns a single connection which aggregates all of the supplied connections. When the
         * aggregated connection is closed, the underlying connections are closed. When its priority is
         * changed the underlying connections' priorities are changed. Etc.
         */
        fun join(vararg conns: Connection): Connection {
            return object : Connection() {
                override fun close() {
                    for (c in conns) c.close()
                }

                override fun once(): Connection {
                    for (c in conns) c.once()
                    return this
                }

                override fun atPrio(priority: Int): Connection {
                    for (c in conns) c.atPrio(priority)
                    return this
                }

                override fun holdWeakly(): Connection {
                    for (c in conns) c.holdWeakly()
                    return this
                }
            }
        }
    }
}
