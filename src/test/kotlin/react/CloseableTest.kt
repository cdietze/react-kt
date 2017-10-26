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