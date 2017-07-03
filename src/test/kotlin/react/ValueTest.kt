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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests aspects of the [Value] class.
 */
class ValueTest {
    @Test fun testSimpleListener() {
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.connect { nvalue: Int, ovalue: Int? ->
            assertEquals(42, ovalue!!.toInt().toLong())
            assertEquals(15, nvalue.toLong())
            fired[0] = true
        }
        assertEquals(42, value.update(15).toLong())
        assertEquals(15, value.get().toLong())
        assertTrue(fired[0])
    }

    @Test fun testAsSignal() {
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.connect { value ->
            assertEquals(15, value.toLong())
            fired[0] = true
        }
        value.update(15)
        assertTrue(fired[0])
    }

    @Test fun testAsOnceSignal() {
        val value = Value(42)
        val counter = SignalTest.Counter()
        value.connect(counter).once()
        value.update(15)
        value.update(42)
        assertEquals(1, counter.notifies.toLong())
    }

    @Test fun testSignalListener() {
        // this ensures that our SignalListener -> ValueListener wrapping is working until we
        // switch to the Java 1.8-only approach which will combine those two interfaces into a
        // subtype relationship using a default method
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.connect { value: Int ->
            assertEquals(15, value.toInt().toLong())
            fired[0] = true
        }
        value.update(15)
        assertTrue(fired[0])
    }

    @Test fun testMappedValue() {
        val value = Value(42)
        val mapped = value.map(Int::toString)

        val counter = SignalTest.Counter()
        val c1 = mapped.connect(counter)
        val c2 = mapped.connect(SignalTest.require("15"))

        value.update(15)
        assertEquals(1, counter.notifies.toLong())
        value.update(15)
        assertEquals(1, counter.notifies.toLong())
        value.updateForce(15)
        assertEquals(2, counter.notifies.toLong())

        // disconnect from the mapped value and ensure that it disconnects in turn
        c1.close()
        c2.close()
        assertFalse(value.hasConnections())
    }

    @Test fun testFlatMappedValue() {
        val value1 = Value(42)
        val value2 = Value(24)
        val toggle = Value(true)
        val flatMapped = toggle.flatMap { toggle ->
            if (toggle) value1 else value2
        }

        val counter1 = SignalTest.Counter()
        val counter2 = SignalTest.Counter()
        val counterM = SignalTest.Counter()
        val c1 = value1.connect(counter1)
        val c2 = value2.connect(counter2)
        val cM = flatMapped.connect(counterM)

        flatMapped.connect(SignalTest.require(10)).once()
        value1.update(10)
        assertEquals(1, counter1.notifies.toLong())
        assertEquals(1, counterM.notifies.toLong())

        value2.update(1)
        assertEquals(1, counter2.notifies.toLong())
        assertEquals(1, counterM.notifies.toLong()) // not incremented

        flatMapped.connect(SignalTest.require(15)).once()
        toggle.update(false)

        value2.update(15)
        assertEquals(2, counter2.notifies.toLong())
        assertEquals(2, counterM.notifies.toLong()) // is incremented

        // disconnect from the mapped value and ensure that it disconnects in turn
        c1.close()
        c2.close()
        cM.close()
        assertFalse(value1.hasConnections())
        assertFalse(value2.hasConnections())
    }

    @Test fun testConnectionlessFlatMappedValue() {
        val value1 = Value(42)
        val value2 = Value(24)
        val toggle = Value(true)
        val flatMapped = toggle.flatMap { toggle ->
            if (toggle) value1 else value2
        }
        assertEquals(42, flatMapped.get().toLong())
        toggle.update(false)
        assertEquals(24, flatMapped.get().toLong())
    }

    @Test fun testConnectNotify() {
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.connectNotify { value ->
            assertEquals(42, value.toInt().toLong())
            fired[0] = true
        }
        assertTrue(fired[0])
    }

    @Test fun testListenNotify() {
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.connectNotify { value ->
            assertEquals(42, value.toInt().toLong())
            fired[0] = true
        }
        assertTrue(fired[0])
    }

    @Test fun testDisconnect() {
        val value = Value(42)
        val expectedValue = intArrayOf(value.get())
        val fired = intArrayOf(0)
        val listener: ValueViewListener<Int> = object : ValueViewListener<Int> {
            override fun invoke(newValue: Int, oldValue: Int?) {
                assertEquals(expectedValue[0].toLong(), newValue.toInt().toLong())
                fired[0] += 1
                value.disconnect(this)
            }
        }
        val conn = value.connectNotify(listener)
        expectedValue[0] = 12; value.update(expectedValue[0])
        assertEquals("Disconnecting in listenNotify disconnects", 1, fired[0].toLong())
        conn.close()// Just see what happens when calling disconnect while disconnected

        value.connect(listener)
        value.connect(SignalTest.Counter())
        value.connect(listener)
        expectedValue[0] = 13; value.update(expectedValue[0])
        expectedValue[0] = 14; value.update(expectedValue[0])
        assertEquals("Disconnecting in listen disconnects", 3, fired[0].toLong())

        value.connect(listener).close()
        expectedValue[0] = 15; value.update(expectedValue[0])
        assertEquals("Disconnecting before geting an update still disconnects", 3, fired[0].toLong())
    }

    @Test fun testSlot() {
        val value = Value(42)
        val expectedValue = intArrayOf(value.get())
        val fired = intArrayOf(0)
        val listener = object : Slot<Int> {
            override fun invoke(newValue: Int) {
                assertEquals(expectedValue[0], newValue)
                fired[0] += 1
            }
        }
        val con = value.connect(listener)
        expectedValue[0] = 12; value.update(expectedValue[0])
        assertEquals(1, fired[0])

        con.close()
        expectedValue[0] = 14; value.update(expectedValue[0])
        assertEquals(1, fired[0])
    }

    @Test fun testWeakListener() {
        val value = Value(42)
        val fired = AtomicInteger(0)

        var listener: ValueViewListener<Int>? = { value: Int, oldValue: Int? -> fired.incrementAndGet() }
        System.gc()
        System.gc()
        System.gc()

        val conn = value.addConnection(listener)
        value.update(41)
        assertEquals(1, fired.get().toLong())
        assertTrue(value.hasConnections())

        // make sure that calling holdWeakly twice doesn't cause weirdness
        conn.holdWeakly()
        value.update(42)
        assertEquals(2, fired.get().toLong())
        assertTrue(value.hasConnections())

        // clear out the listener and do our best to convince the JVM to collect it
        listener = null
        System.gc()
        System.gc()
        System.gc()

        // now check that the listener has been collected and is not notified
        value.update(40)
        assertEquals(2, fired.get().toLong())
        assertFalse(value.hasConnections())
    }

    // TODO(cdi) revive test once we re-add `join`
//    @Test fun testJoinedValue() {
//        val number = Value.create(1)
//        val string = Value.create("foo")
//        val both = Values.INSTANCE.join(number, string)
//        val counter = SignalTest.Counter()
//        both.connect(counter)
//        number.update(2)
//        assertEquals(1, counter.notifies.toLong())
//        assertEquals(Values.T2(2, "foo"), both.get())
//        string.update("bar")
//        assertEquals(2, counter.notifies.toLong())
//        number.update(2)
//        assertEquals(2, counter.notifies.toLong())
//        string.update("bar")
//        assertEquals(2, counter.notifies.toLong())
//    }

    @Test fun testChanges() {
        val value = Value(42)
        val fired = booleanArrayOf(false)
        value.changes().connect { v: Int ->
            assertEquals(15, v.toLong())
            fired[0] = true
        }
        value.update(15)
        assertTrue(fired[0])
    }

    @Test fun testChangesNext() {
        val value = Value(42)
        val counter = SignalTest.Counter()
        value.changes().next().onSuccess(counter)
        value.update(15)
        value.update(42)
        assertEquals(1, counter.notifies.toLong())
    }
}
