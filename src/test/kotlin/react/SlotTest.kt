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

/**
 * Tests basic signals and slots behavior.
 */
class SlotTest {

    open class A
    class B : A()

    @Test
    fun andThenShouldBeCalledAfterwards() {
        var s = ""
        val a: Slot<A> = { s += "a" }
        val b: Slot<B> = { s += "b" }
        val c: Slot<B> = a.andThen(b)
        c.invoke(B())
        assertEquals(s, "ab")
    }

    @Test
    fun beforeShouldBeCalledBefore() {
        var s = ""
        val a: Slot<A> = { s += "a" }
        val b: Slot<B> = { s += "b" }
        val c: Slot<B> = a.butBeforeInvoke(b)
        c.invoke(B())
        assertEquals(s, "ba")
    }
}