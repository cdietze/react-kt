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

import org.junit.Test
import kotlin.test.assertEquals

class CloseableTest {
    class Counter : Closeable {
        var calls: Int = 0
        override fun close() {
            calls += 1
        }
    }

    @Test
    fun joinShouldCallCloseOnlyOnce() {
        val a = Counter()
        val joined = Closeable.Util.join(a)
        assertEquals(0, a.calls)
        joined.close()
        assertEquals(1, a.calls)
        joined.close()
        assertEquals(1, a.calls)
    }
}