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
import kotlin.test.*

/**
 * Tests basic signals and slots behavior.
 */
open class SignalTest {

    // TODO: merge with [TestBase.Counter]
    class Counter {
        var notifies: Int = 0
        var slot: UnitSlot = fun(_: Any?) { notifies++ }
    }

    @Test
    fun testSignalToSlot() {
        val signal = Signal<Int>()
        val accSlot = AccSlot<Int>()
        signal.connect(accSlot.slot)
        signal.emit(1)
        signal.emit(2)
        signal.emit(3)
        assertEquals(listOf(1, 2, 3), accSlot.events)
    }

    @Test
    fun testOneShotSlot() {
        val signal = Signal<Int>()
        val accSlot = AccSlot<Int>()
        signal.connect(accSlot.slot).once()
        signal.emit(1) // accSlot should be removed after this emit
        signal.emit(2)
        signal.emit(3)
        assertEquals(listOf(1), accSlot.events)
    }

    @Test
    fun testSlotPriority() {
        val counter = intArrayOf(0)

        class TestSlot {
            var order: Int = 0
            val slot: UnitSlot = fun(_: Any?) { order = ++counter[0] }
        }

        val slot1 = TestSlot()
        val slot2 = TestSlot()
        val slot3 = TestSlot()
        val slot4 = TestSlot()

        val signal = UnitSignal()
        signal.connect(slot3.slot).atPrio(2)
        signal.connect(slot1.slot).atPrio(4)
        signal.connect(slot2.slot).atPrio(3)
        signal.connect(slot4.slot).atPrio(1)
        signal.emit()
        assertEquals(1, slot1.order.toLong())
        assertEquals(2, slot2.order.toLong())
        assertEquals(3, slot3.order.toLong())
        assertEquals(4, slot4.order.toLong())
    }

    @Test
    fun testAddDuringDispatch() {
        val signal = Signal<Int>()
        val toAdd = AccSlot<Int>()
        signal.connect { signal.connect(toAdd.slot) }.once()

        // this will connect our new signal but not dispatch to it
        signal.emit(5)
        assertEquals(0, toAdd.events.size.toLong())

        // now dispatch an event that should go to the added signal
        signal.emit(42)
        assertEquals(listOf(42), toAdd.events)
    }

    @Test
    fun testRemoveDuringDispatch() {
        val signal = Signal<Int>()
        val toRemove = AccSlot<Int>()
        val rconn = signal.connect(toRemove.slot)

        // dispatch one event and make sure it's received
        signal.emit(5)
        assertEquals(listOf(5), toRemove.events)

        // now add our removing signal, and dispatch again
        signal.connect({
            rconn.close()
        }).atPrio(1) // ensure that we're before toRemove
        signal.emit(42)
        // since toRemove will have been removed during this dispatch, it will not receive the
        // signal in question, because the higher priority signal triggered first and removed it
        assertEquals(listOf(5), toRemove.events)
        // finally dispatch one more event and make sure toRemove didn't get it
        signal.emit(9)
        assertEquals(listOf(5), toRemove.events)
    }

    @Test
    fun testAddAndRemoveDuringDispatch() {
        val signal = Signal<Int>()
        val toAdd = AccSlot<Int>()
        val toRemove = AccSlot<Int>()
        val rconn = signal.connect(toRemove.slot)

        // dispatch one event and make sure it's received by toRemove
        signal.emit(5)
        assertEquals(listOf(5), toRemove.events)

        // now add our adder/remover signal, and dispatch again
        signal.connect({
            rconn.close()
            signal.connect(toAdd.slot)
        })
        signal.emit(42)

        // make sure toRemove got this event (in this case the adder/remover signal fires *after*
        // toRemove gets the event) and toAdd didn't
        assertEquals(listOf(5, 42), toRemove.events)
        assertEquals(0, toAdd.events.size.toLong())

        // finally emit one more and ensure that toAdd got it and toRemove didn't
        signal.emit(9)
        assertEquals(listOf(9), toAdd.events)
        assertEquals(listOf(5, 42), toRemove.events)
    }

    @Test
    fun testDispatchDuringDispatch() {
        val signal = Signal<Int>()
        val counter = AccSlot<Int>()
        signal.connect(counter.slot)

        // connect a slot that will emit during dispatch
        signal.connect({ value: Int? ->
            if (value === 5)
                signal.emit(value * 2)
            else
                fail("once() lner notified more than once")// ensure that we're not notified twice even though we emit during dispatch
        }).once()

        // dispatch one event and make sure that both events are received
        signal.emit(5)
        assertEquals(listOf(5, 10), counter.events)
    }

    @Test
    fun testUnitSlot() {
        val signal = Signal<Int>()
        val fired = booleanArrayOf(false)
        signal.connect({
            fired[0] = true
        })
        signal.emit(42)
        assertTrue(fired[0])
    }

    @Test
    fun testSingleFailure() {
        val signal = UnitSignal()
        signal.connect({
            throw RuntimeException("Bang!")
        })
        assertFailsWith(RuntimeException::class) {
            signal.emit()
        }
    }

    @Test
    fun testMultiFailure() {
        val signal = UnitSignal()
        signal.connect({
            throw RuntimeException("Bing!")
        })
        signal.connect({
            throw RuntimeException("Bang!")
        })
        assertFailsWith(RuntimeException::class) {
            signal.emit()
        }
    }

    @Test
    fun testMappedSignal() {
        val signal = Signal<Int>()
        val mapped = signal.map(Int::toString)

        val counter = Counter()
        val c1 = mapped.connect(counter.slot)
        val c2 = mapped.connect(require("15"))

        signal.emit(15)
        assertEquals(1, counter.notifies.toLong())
        signal.emit(15)
        assertEquals(2, counter.notifies.toLong())

        // disconnect from the mapped signal and ensure that it clears its connection
        c1.close()
        c2.close()
        assertFalse(signal.hasConnections())
    }

    @Test
    fun testFilter() {
        val triggered = IntArray(1)
        val onString: Slot<String?> = { value ->
            assertFalse(value == null)
            triggered[0]++
        }
        val sig = Signal<String?>()
        sig.filter({ it != null }).connect(onString)
        sig.emit(null)
        sig.emit("foozle")
        assertEquals(1, triggered[0].toLong())
    }

    @Test
    fun testFiltered() {
        val triggered = IntArray(1)
        val onString: Slot<String?> = { value ->
            assertFalse(value == null)
            triggered[0]++
        }
        val sig = Signal<String?>()
        sig.connect(onString.filtered({ it != null }))
        sig.emit(null)
        sig.emit("foozle")
        assertEquals(1, triggered[0].toLong())
    }

    @Test
    fun testNext() {
        class Accum<T> {
            var values: MutableList<T> = ArrayList()

            val slot: Slot<T> = fun(value: T) { values.add(value) }

            fun assertContains(values: List<T>) {
                assertEquals(values, this.values)
            }
        }

        val signal = Signal<Int>()
        val accum = Accum<Int>()
        val accum3 = Accum<Int>()

        signal.next().onSuccess(accum.slot)
        signal.filter({ it == 3 }).next().onSuccess(accum3.slot)

        val NONE = emptyList<Int>()
        val ONE = listOf(1)
        val THREE = listOf(3)

        signal.emit(1) // adder should only receive this value
        accum.assertContains(ONE)
        accum3.assertContains(NONE)

        signal.emit(2)
        accum.assertContains(ONE)
        accum3.assertContains(NONE)

        signal.emit(3)
        accum.assertContains(ONE)
        accum3.assertContains(THREE)

        // signal should no longer have connections at this point
        assertFalse(signal.hasConnections())

        signal.emit(3) // adder3 should not receive multiple threes
        accum3.assertContains(listOf(3))
    }

    class AccSlot<T> {
        var events: MutableList<T> = ArrayList()
        val slot: Slot<T> = fun(event: T) { events.add(event) }
    }

    companion object {

        fun <T> require(reqValue: T): Slot<T> {
            return {
                assertEquals(reqValue, it)
            }
        }
    }
}
