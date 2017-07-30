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

import org.junit.Assert.assertEquals
import org.junit.Test

class RListTest {
    class Counter : RList.Listener<Any>() {
        var notifies: Int = 0
        override fun onAdd(index: Int, elem: Any) {
            notifies++
        }

        override fun onSet(index: Int, newElem: Any, oldElem: Any?) {
            notifies++
        }

        override fun onRemove(index: Int, elem: Any) {
            notifies++
        }
    }

    @Test
    fun addAndRemove() {
        val list: RList<String> = RList.create()
        val counter = Counter()
        list.connect(counter)
        list.connect(requireAdd("1")).once()
        list.add("1")
        assertEquals(1, counter.notifies.toLong())

        // add the same element, and ensure that we're notified again
        list.add("2")
        assertEquals(2, counter.notifies.toLong())

        // remove elements, ensure that we're notified
        list.remove("1")
        list.connect(requireRemove("2")).once()
        list.removeAt(0)
        assertEquals(4, counter.notifies.toLong())

        // remove it again, ensure that we're not notified
        list.remove("2")
        assertEquals(4, counter.notifies.toLong())

        // force remove it, ensure that we are notified
        list.removeForce("2")
        assertEquals(5, counter.notifies.toLong())

        // remove a non-existent element, ensure that we're not notified
        list.remove("3")
        assertEquals(5, counter.notifies.toLong())
    }

    @Test
    fun testSet() {
        val list: RList<String> = RList.create()
        list.add("1")
        list.add("2")

        val counter = Counter()
        list.connect(counter)

        list.connect(object : RList.Listener<String>() {
            override fun onSet(index: Int, newElem: String, oldElem: String?) {
                assertEquals(1, index.toLong())
                assertEquals("2", oldElem)
                assertEquals("3", newElem)
            }
        })
        list.set(1, "3")
        assertEquals(1, counter.notifies.toLong())
    }

    @Test
    fun listIterate() {
        val list: RList<String> = RList.create()
        list.add("1")
        list.add("2")
        val counter = Counter()
        list.connect(counter)

        val literator = list.listIterator()
        literator.next()
        // removing the last next call makes one remove notification
        list.connect(requireRemove("1")).once()
        literator.remove()
        assertEquals(1, counter.notifies.toLong())

        // setting the last next call makes one set notification
        literator.next()
        list.connect(object : RList.Listener<String>() {
            override fun onSet(index: Int, newElem: String, oldElem: String?) {
                assertEquals(0, index.toLong())
                assertEquals("2", oldElem)
                assertEquals("3", newElem)
            }
        }).once()
        literator.set("3")
        assertEquals(2, counter.notifies.toLong())

        // adding on the iterator makes one notification
        list.connect(requireAdd("4")).once()
        literator.add("4")
        assertEquals(3, counter.notifies.toLong())

        // 3 and 4 in the list now
        assertEquals(2, list.size.toLong())
    }

    @Test
    fun testSizeView() {
        val list: RList<String> = RList.create()
        list.add("one")
        assertEquals(1, list.sizeView.get().toLong())
        list.remove("one")
        assertEquals(0, list.sizeView.get().toLong())

        val counter = SignalTest.Counter()
        list.sizeView.connect(counter)
        list.add("two")
        assertEquals(1, counter.notifies.toLong())
        list.add("three")
        assertEquals(2, counter.notifies.toLong())
        list.remove("two")
        assertEquals(3, counter.notifies.toLong())
    }

    companion object {

        fun <T> requireAdd(reqElem: T): RList.Listener<T> {
            return object : RList.Listener<T>() {
                override fun onAdd(elem: T) {
                    assertEquals(reqElem, elem)
                }
            }
        }

        fun <T> requireRemove(reqElem: T): RList.Listener<T> {
            return object : RList.Listener<T>() {
                override fun onRemove(elem: T) {
                    assertEquals(reqElem, elem)
                }
            }
        }
    }
}
