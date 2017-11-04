package react

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests aspects of the [Values] object.
 */
class ValuesTest {
    @Test
    fun testJoinedValue() {
        val number = Value(1)
        val string = Value("foo")
        val both = Values.join(number, string)
        val counter = SignalTest.Counter()
        both.connect(counter.slot)
        number.update(2)
        assertEquals(1, counter.notifies.toLong())
        assertEquals(Pair(2, "foo"), both.get())
        string.update("bar")
        assertEquals(2, counter.notifies.toLong())
        number.update(2)
        assertEquals(2, counter.notifies.toLong())
        string.update("bar")
        assertEquals(2, counter.notifies.toLong())
    }

    @Test
    fun testNot() {
        val a = Value(false)
        val not = Values.not(a)
        assertEquals(true, not.get())
        a.update(true)
        assertEquals(false, not.get())
    }

    @Test
    fun testAndValues() {
        val a = Value(false)
        val b = Value(false)
        val and = Values.and(a, b)
        assertEquals(false, and.get())
        a.update(true)
        assertEquals(false, and.get())
        b.update(true)
        assertEquals(true, and.get())
        a.update(false)
        assertEquals(false, and.get())
    }

    @Test
    fun testOrValues() {
        val a = Value(false)
        val b = Value(false)
        val or = Values.or(a, b)
        assertEquals(false, or.get())
        a.update(true)
        assertEquals(true, or.get())
        b.update(true)
        assertEquals(true, or.get())
        a.update(false)
        assertEquals(true, or.get())
    }
}
