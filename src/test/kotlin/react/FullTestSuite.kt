package react

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        CloseableTest::class,
        RFutureTest::class,
        RListTest::class,
        RMapTest::class,
        RSetTest::class,
        SignalTest::class,
        SlotTest::class,
        ValuesTest::class,
        ValueTest::class
)
class FullTestSuite
