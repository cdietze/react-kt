package react

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeakListenerTest {
    @Test
    fun testWeakListener() {
        val value = Value(42)
        var fired = 0

        var listener: ValueViewListener<Int>? = { value: Int, oldValue: Int? ->
            fired += 1
        }
        System.gc()
        System.gc()
        System.gc()

        val conn = value.addConnection(listener!!)
        value.update(41)
        assertEquals(1, fired)
        assertTrue(value.hasConnections())

        // make sure that calling holdWeakly twice doesn't cause weirdness
        conn.holdWeakly()
        value.update(42)
        assertEquals(2, fired)
        assertTrue(value.hasConnections())

        // clear out the listener and do our best to convince the JVM to collect it
        listener = null
        System.gc()
        System.gc()
        System.gc()

        // now check that the listener has been collected and is not notified
        value.update(40)
        assertEquals(2, fired)
        assertFalse(value.hasConnections())
    }
}
