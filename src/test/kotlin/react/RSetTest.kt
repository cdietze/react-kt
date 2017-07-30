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

import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Tests aspects of the [RSet] class.
 */
class RSetTest {
    class Counter : RSet.Listener<Any>() {
        var notifies: Int = 0
        override fun onAdd(elem: Any) {
            notifies++
        }

        override fun onRemove(elem: Any) {
            notifies++
        }
    }

    @Test
    fun testBasicNotify() {
        val set = RSet.create(HashSet<Int>())
        val counter = Counter()
        set.connect(counter)

        // add an element, ensure that we're notified
        set.connect(requireAdd(42)).once()
        set.add(42)
        assertEquals(1, counter.notifies.toLong())

        // add the same element, and ensure that we're not notified
        set.add(42)
        assertEquals(1, counter.notifies.toLong())

        // force add the same element, ensure that we are notified
        set.addForce(42)
        assertEquals(2, counter.notifies.toLong())

        // remove an element, ensure that we're notified
        set.connect(requireRemove(42)).once()
        set.remove(42)
        assertEquals(3, counter.notifies.toLong())

        // remove it again, ensure that we're not notified
        set.remove(42)
        assertEquals(3, counter.notifies.toLong())

        // force remove it, ensure that we are notified
        set.removeForce(42)
        assertEquals(4, counter.notifies.toLong())

        // remove a non-existent element, ensure that we're not notified
        set.remove(25)
        assertEquals(4, counter.notifies.toLong())
    }

    @Test
    fun testAggregatesEtc() {
        val set = RSet.create(HashSet<Int>())
        val counter = Counter()
        set.connect(counter)

        // test adding multiple entries
        set.addAll(Arrays.asList(1, 2, 3, 4))
        assertEquals(4, counter.notifies.toLong())

        // test removing by iterator
        val iter = set.iterator()
        iter.next()
        iter.remove()
        assertEquals(5, counter.notifies.toLong())
        assertEquals(3, set.size.toLong())
        val v1 = iter.next()
        val v2 = iter.next()

        // test notification on remove all
        set.removeAll(Arrays.asList(v1, 5, 6))
        assertEquals(6, counter.notifies.toLong())
        assertEquals(2, set.size.toLong())

        // test notification on retain all
        set.retainAll(Arrays.asList(v2, 7, 8))
        assertEquals(7, counter.notifies.toLong())
        assertEquals(1, set.size.toLong())

        // finally test notification on clear
        set.clear()
        assertEquals(8, counter.notifies.toLong())
        assertEquals(0, set.size.toLong())
    }

    @Test
    fun testContainsView() {
        val set = RSet.create(HashSet<Int>())
        set.add(1)

        // create some contains key views and ensure their initial values are correct
        val containsOne = set.containsView(1)
        val containsTwo = set.containsView(2)
        assertTrue(containsOne.get())
        assertFalse(containsTwo.get())

        // listen for notifications
        val counter = SignalTest.Counter()
        containsOne.connect(counter)
        containsTwo.connect(counter)

        // remove the element for one and ensure that we're notified
        containsOne.connect(SignalTest.require(false)).once()
        set.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // make sure we're not repeat notified
        set.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // force a remove and make sure we are repeat notified
        set.removeForce(1)
        assertEquals(2, counter.notifies.toLong())

        // add an element for two and ensure that we're notified
        containsTwo.connect(SignalTest.require(true)).once()
        set.add(2)
        assertEquals(3, counter.notifies.toLong())

        // make sure we're not repeat notified
        set.add(2)
        assertEquals(3, counter.notifies.toLong())

        // force an add and make sure we are repeat notified
        set.addForce(2)
        assertEquals(4, counter.notifies.toLong())
    }

    @Test
    fun testSizeView() {
        val set: RSet<String> = RSet.create()
        set.add("one")
        assertEquals(1, set.sizeView.get().toLong())
        set.remove("one")
        assertEquals(0, set.sizeView.get().toLong())

        val counter = SignalTest.Counter()
        set.sizeView.connect(counter)
        set.add("two")
        assertEquals(1, counter.notifies.toLong())
        set.add("three")
        assertEquals(2, counter.notifies.toLong())
        set.remove("two")
        assertEquals(3, counter.notifies.toLong())
        // make sure noops don't trigger size view
        set.remove("two")
        assertEquals(3, counter.notifies.toLong())
        set.add("three")
        assertEquals(3, counter.notifies.toLong())
    }

    companion object {

        protected fun <T> requireAdd(reqElem: T): RSet.Listener<T> {
            return object : RSet.Listener<T>() {
                override fun onAdd(elem: T) {
                    assertEquals(reqElem, elem)
                }
            }
        }

        protected fun <T> requireRemove(reqElem: T): RSet.Listener<T> {
            return object : RSet.Listener<T>() {
                override fun onRemove(elem: T) {
                    assertEquals(reqElem, elem)
                }
            }
        }
    }
}
