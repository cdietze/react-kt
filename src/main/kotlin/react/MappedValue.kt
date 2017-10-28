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
 * Plumbing to implement mapped values in such a way that they automatically manage a connection to
 * their underlying value. When the mapped value adds its first connection, it establishes a
 * connection to the underlying value, and when it removes its last connection it clears its
 * connection from the underlying value.
 */
internal abstract class MappedValue<T> : AbstractValue<T>() {
    /**
     * Establishes a connection to our source value. Called when go from zero to one listeners.
     * When we go from one to zero listeners, the connection will automatically be cleared.

     * @return the newly established connection.
     */
    protected abstract fun connect(): Connection

    protected open fun disconnect() {
        if (_conn != null) {
            _conn!!.close()
            _conn = null
        }
    }

    protected fun reconnect() {
        disconnect()
        _conn = connect()
    }

    override fun connectionAdded() {
        super.connectionAdded()
        if (_conn == null) _conn = connect()
    }

    override fun connectionRemoved() {
        super.connectionRemoved()
        if (!hasConnections()) disconnect()
    }

    protected var _conn: Connection? = null
}
