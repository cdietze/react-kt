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
 * An interface for closeable resources. Has a single abstract method: [close].
 */
interface Closeable {

    /** Maintains a set of closeables to allow mass operations on them.  */
    class Set : Closeable {

        /** Closes all connections in this set and empties it.  */
        override fun close() {
            if (_set != null) {
                var error: MultiFailureException? = null
                for (c in _set!!)
                    try {
                        c.close()
                    } catch (e: Exception) {
                        if (error == null) error = MultiFailureException()
                        error.addFailure(e)
                    }

                _set!!.clear()
                if (error != null) throw error
            }
        }

        /** Adds the supplied connection to this set.
         * @return the supplied connection.
         */
        fun <T : Closeable> add(c: T): T {
            if (_set == null) _set = HashSet<Closeable>()
            _set!!.add(c)
            return c
        }

        /** Removes a closeable from this set while leaving its status unchanged.  */
        fun remove(c: Closeable) {
            if (_set != null) _set!!.remove(c)
        }

        protected var _set: HashSet<Closeable>? = null // lazily created
    }

    /** Provides some [Closeable]-related utilities.  */
    object Util {

        /** A closable which no-ops on [.close] and throws an exception for all other
         * methods. This is for the following code pattern:

         * <pre>`Closable _conn = Closeable.Util.NOOP;
         * void open () {
         * _conn = whatever.connect(...);
         * }
         * void close () {
         * _conn = Closeable.Util.close(_conn);
         * }
        `</pre> *

         * In that it allows `close` to avoid a null check if it's possible for
         * `close` to be called with no call to `open` or repeatedly.
         */
        val NOOP: Closeable = object : Closeable {
            override fun close() {} // noop!
        }

        /** Creates a closable that closes multiple connections at once.  */
        fun join(vararg cons: Closeable): Closeable {
            val c: Array<Closeable?> = cons as Array<Closeable?>
            return object : Closeable {
                override fun close() {
                    for (ii in c.indices) {
                        c[ii]?.close()
                        c[ii] = null
                    }
                }
            }
        }

        /** Closes `con` and returns [.NOOP]. This enables code like:
         * `con = Connection.close(con);` which simplifies disconnecting and resetting to
         * [.NOOP], a given connection reference.  */
        fun close(con: Closeable): Closeable {
            con.close()
            return NOOP
        }
    }

    /** Closes this closeable resource.  */
    fun close()
}
