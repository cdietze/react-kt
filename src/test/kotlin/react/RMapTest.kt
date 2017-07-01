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
import java.util.AbstractMap.SimpleEntry
import kotlin.collections.MutableMap.MutableEntry

/**
 * Tests aspects of the [RMap] class.
 */
class RMapTest {
    class Counter : RMap.Listener<Any, Any> {
        var notifies: Int = 0
        override fun onPut(key: Any?, value: Any?) {
            notifies++
        }

        override fun onRemove(key: Any?) {
            notifies++
        }
    }

    @Test
    fun testBasicNotify() {
        val map = RMap.create(HashMap<Int, String>())
        val counter = Counter()
        map.connect(counter)

        // add a mapping, ensure that we're notified
        map.connect(object : RMap.Listener<Int, String> {
            override fun onPut(key: Int?, value: String?, ovalue: String?) {
                assertEquals(42, key!!.toInt().toLong())
                assertEquals("LTUAE", value)
                assertNull(ovalue)
            }
        })
        map.put(42, "LTUAE")
        assertEquals(1, counter.notifies.toLong())

        // add the same mapping, and ensure that we're not notified
        map.put(42, "LTUAE")
        assertEquals(1, counter.notifies.toLong())

        // remove a mapping, ensure that we're notified
        map.connect(object : RMap.Listener<Int, String> {
            override fun onRemove(key: Int?, ovalue: String?) {
                assertEquals(42, key!!.toInt().toLong())
                assertEquals("LTUAE", ovalue)
            }
        })
        map.remove(42)
        assertEquals(2, counter.notifies.toLong())

        // remove a non-existent mapping, ensure that we're not notified
        map.remove(25)
        assertEquals(2, counter.notifies.toLong())
    }

    @Test
    fun testForceNotify() {
        val map = RMap.create(HashMap<Int, String>())
        val counter = Counter()
        map.connect(counter)

        // add a mapping, ensure that we're notified
        map.connect(object : RMap.Listener<Int, String> {
            override fun onPut(key: Int?, value: String?, ovalue: String?) {
                assertEquals(42, key!!.toInt().toLong())
                assertEquals("LTUAE", value)
            }
        })
        map.put(42, "LTUAE")
        assertEquals(1, counter.notifies.toLong())

        // update the mapping with the same value, and be sure we're notified
        map.putForce(42, "LTUAE")
        assertEquals(2, counter.notifies.toLong())

        // remove a mapping, ensure that we're notified
        map.connect(object : RMap.Listener<Int, String> {
            override fun onRemove(key: Int?, ovalue: String?) {
                assertEquals(42, key!!.toInt().toLong())
            }
        })
        map.remove(42)
        assertEquals(3, counter.notifies.toLong())

        // remove the now non-existent mapping, ensure that we're notified
        map.removeForce(42)
        assertEquals(4, counter.notifies.toLong())
    }

    @Test
    fun testKeySet() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(42, "LTUAE")
        map.put(1, "one")

        val counter = Counter()
        map.connect(counter)
        val keys = map.keys

        // test basic keySet bits
        assertTrue(keys.contains(42))
        assertTrue(keys.contains(1))

        // remove an element from the key set, ensure that we're notified
        keys.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // remove non-existent elements, and ensure that we're not notified
        keys.remove(1)
        assertEquals(1, counter.notifies.toLong())
        keys.remove(99)
        assertEquals(1, counter.notifies.toLong())

        // remove an element via the key set iterator, ensure that we're notified
        val iter = keys.iterator()
        iter.next()
        iter.remove()
        assertEquals(2, counter.notifies.toLong())

        // make sure it's not still in the key set
        keys.remove(42)
        assertEquals(2, counter.notifies.toLong())

        // finally check that the map is empty, just for kicks
        assertEquals(0, map.size.toLong())
    }

    @Test
    fun testValues() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(42, "LTUAE")
        map.put(1, "one")

        val counter = Counter()
        map.connect(counter)

        // test basic value bits
        val values = map.values
        assertTrue(values.contains("LTUAE"))
        assertTrue(values.contains("one"))

        // remove an element directly, ensure we're notified
        values.remove("one")
        assertEquals(1, counter.notifies.toLong())

        // make sure it's no longer in the collection
        values.remove("one")
        assertEquals(1, counter.notifies.toLong())

        // remove an element via the iterator, ensure we're notified
        val iter = values.iterator()
        iter.next()
        iter.remove()
        assertEquals(2, counter.notifies.toLong())

        // make sure it's not still in the collection
        values.remove("LTUAE")
        assertEquals(2, counter.notifies.toLong())

        // finally check that the map is empty, just for kicks
        assertEquals(0, map.size.toLong())
    }

    @Test
    fun testEntrySet() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(42, "LTUAE")
        map.put(1, "one")

        val puts = intArrayOf(0)
        val removes = intArrayOf(0)
        map.connect(object : RMap.Listener<Int, String> {
            override fun onPut(key: Int?, value: String?, ovalue: String?) {
                puts[0]++
            }

            override fun onRemove(key: Int?, ovalue: String?) {
                removes[0]++
            }
        })

        // test the update of a value from the entry set
        map.connect(object : RMap.Listener<Int, String> {
            override fun onPut(key: Int?, value: String?, ovalue: String?) {
                assertEquals(42, key!!.toInt().toLong())
                assertEquals("Mu", value)
                assertEquals("LTUAE", ovalue)
            }
        })
        var entry: MutableEntry<Int, String>? = null
        for (e in map.entries) {
            if (e.key === 42) entry = e
        }
        entry!!.setValue("Mu")
        assertEquals(1, puts[0])

        // test some basic entry set properties
        val entries = map.entries
        assertEquals(2, entries.size.toLong())
        assertTrue(entries.contains(SimpleEntry<Int, String>(42, "Mu")))
        assertTrue(entries.contains(SimpleEntry<Int, String>(1, "one")))

        // test removal via the entry set
        entries.remove(SimpleEntry<Int, String>(42, "Mu"))
        assertEquals(1, removes[0].toLong())
        // make sure it's no longer there
        entries.remove(SimpleEntry<Int, String>(42, "Mu"))
        assertEquals(1, removes[0].toLong())

        // test removal via the iterator
        val iter = entries.iterator()
        iter.next()
        iter.remove()
        assertEquals(2, removes[0].toLong())
        assertFalse(entries.contains(SimpleEntry<Int, String>(1, "one")))
        // make sure it's no longer there
        entries.remove(SimpleEntry<Int, String>(1, "one"))
        assertEquals(2, removes[0].toLong())

        // finally check that the map is empty, just for kicks
        assertEquals(0, map.size.toLong())
    }

    @Test
    fun testContainsKeyView() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(1, "one")

        // create some contains key views and ensure their initial values are correct
        val containsOne = map.containsKeyView(1)
        val containsTwo = map.containsKeyView(2)
        assertTrue(containsOne.get())
        assertFalse(containsTwo.get())

        // listen for notifications
        val counter = SignalTest.Counter()
        containsOne.connect(counter)
        containsTwo.connect(counter)

        // remove the mapping for one and ensure that we're notified
        containsOne.connect(SignalTest.require(false)).once()
        map.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // make sure we're not repeat notified
        map.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // add a mapping for two and ensure that we're notified
        containsTwo.connect(SignalTest.require(true)).once()
        map.put(2, "two")
        assertEquals(2, counter.notifies.toLong())

        // make sure we're not repeat notified
        map.put(2, "ii")
        assertEquals(2, counter.notifies.toLong())
    }

    @Test
    fun testGetView() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(1, "one")

        // create some views and ensure their initial values are correct
        val oneView = map.getView(1)
        val twoView = map.getView(2)
        assertEquals("one", oneView.get())
        assertNull(twoView.get())

        // listen for notifications
        val counter = SignalTest.Counter()
        oneView.connect(counter)
        twoView.connect(counter)

        // remove the mapping for one and ensure that we're notified
        oneView.connect(SignalTest.require<String?>(null)).once()
        map.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // make sure we're not repeat notified
        map.remove(1)
        assertEquals(1, counter.notifies.toLong())

        // add a mapping for two and ensure that we're notified
        twoView.connect(SignalTest.require<String?>("two")).once()
        map.put(2, "two")
        assertEquals(2, counter.notifies.toLong())

        // make sure we're not notified when the same value is put
        map.put(2, "two")
        assertEquals(2, counter.notifies.toLong())

        // make sure we are notified when the value changes
        twoView.connect(SignalTest.require<String?>("ii")).once()
        map.put(2, "ii")
        assertEquals(3, counter.notifies.toLong())
    }

    @Test
    fun testEntrySetIteratorEdgeCase() {
        val map = RMap.create(HashMap<Int, String>())
        map.put(1, "one")
        map.put(2, "two")

        val iter = map.entries.iterator()
        val e1 = iter.next()
        iter.remove()

        val e2 = iter.next()
        e2.setValue("bif")

        try {
            e1.setValue("baz")
            fail()
        } catch (ise: IllegalStateException) {
            // this is the expected behavior
        }

    }

    @Test
    fun testSizeView() {
        val map: RMap<String, Int> = RMap.create()
        map.put("one", 1)
        assertEquals(1, map.sizeView().get().toInt().toLong())
        map.remove("one")
        assertEquals(0, map.sizeView().get().toInt().toLong())

        val counter = SignalTest.Counter()
        map.sizeView().connect(counter)
        map.put("two", 2)
        assertEquals(1, counter.notifies.toLong())
        map.put("three", 3)
        assertEquals(2, counter.notifies.toLong())
        map.remove("two")
        assertEquals(3, counter.notifies.toLong())
    }
}
