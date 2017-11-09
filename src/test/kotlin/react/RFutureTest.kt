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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RFutureTest {

    inner class FutureCounter {
        val successes = NotificationCounter()
        val failures = NotificationCounter()
        val completes = NotificationCounter()

        fun bind(future: RFuture<*>) {
            reset()
            future.onSuccess(successes.slot)
            future.onFailure(failures.slot)
            future.onComplete(completes.slot)
        }

        fun check(state: String, scount: Int, fcount: Int, ccount: Int) {
            assertEquals(scount, successes.notifies, "Successes " + state)
            assertEquals(fcount, failures.notifies, "Failures " + state)
            assertEquals(ccount, completes.notifies, "Completes " + state)
        }

        fun reset() {
            successes.reset()
            failures.reset()
            completes.reset()
        }
    }

    @Test
    fun testImmediate() {
        val counter = FutureCounter()

        val success = RFuture.success("Yay!")
        counter.bind(success)
        counter.check("immediate succeed", 1, 0, 1)

        val failure = RFuture.failure<String>(Exception("Boo!"))
        counter.bind(failure)
        counter.check("immediate failure", 0, 1, 1)
    }

    @Test
    fun testDeferred() {
        val counter = FutureCounter()

        val success = RPromise.create<String>()
        counter.bind(success)
        counter.check("before succeed", 0, 0, 0)
        success.succeed("Yay!")
        counter.check("after succeed", 1, 0, 1)

        val failure = RPromise.create<String>()
        counter.bind(failure)
        counter.check("before fail", 0, 0, 0)
        failure.fail(Exception("Boo!"))
        counter.check("after fail", 0, 1, 1)

        assertFalse(success.hasConnections())
        assertFalse(failure.hasConnections())
    }

    @Test
    fun testMappedImmediate() {
        val counter = FutureCounter()

        val success: RFuture<String?> = RFuture.success("Yay!")
        counter.bind(success.map { it != null })
        counter.check("immediate succeed", 1, 0, 1)

        val failure = RFuture.failure<String?>(Exception("Boo!"))
        counter.bind(failure.map { it != null })
        counter.check("immediate failure", 0, 1, 1)
    }

    @Test
    fun testMappedDeferred() {
        val counter = FutureCounter()

        val success = RPromise.create<String?>()
        counter.bind(success.map { it != null })
        counter.check("before succeed", 0, 0, 0)
        success.succeed("Yay!")
        counter.check("after succeed", 1, 0, 1)

        val failure = RPromise.create<String?>()
        counter.bind(failure.map { it != null })
        counter.check("before fail", 0, 0, 0)
        failure.fail(Exception("Boo!"))
        counter.check("after fail", 0, 1, 1)

        assertFalse(success.hasConnections())
        assertFalse(failure.hasConnections())
    }

    @Test
    fun testFlatMappedImmediate() {
        val scounter = FutureCounter()
        val fcounter = FutureCounter()
        val ccounter = FutureCounter()
        val successMap: (String) -> RFuture<Boolean> = { _ ->
            RFuture.success(true)
        }
        val failMap: (String) -> RFuture<Boolean> = { _ ->
            RFuture.failure<Boolean>(Exception("Barzle!"))
        }
        val crashMap: (String) -> RFuture<Boolean> = { _ ->
            throw RuntimeException("Barzle!")
        }

        val success = RFuture.success("Yay!")
        scounter.bind(success.flatMap(successMap))
        fcounter.bind(success.flatMap(failMap))
        ccounter.bind(success.flatMap(crashMap))
        scounter.check("immediate success/success", 1, 0, 1)
        fcounter.check("immediate success/failure", 0, 1, 1)
        ccounter.check("immediate success/crash", 0, 1, 1)

        val failure = RFuture.failure<String>(Exception("Boo!"))
        scounter.bind(failure.flatMap(successMap))
        fcounter.bind(failure.flatMap(failMap))
        ccounter.bind(failure.flatMap(crashMap))
        scounter.check("immediate failure/success", 0, 1, 1)
        fcounter.check("immediate failure/failure", 0, 1, 1)
        ccounter.check("immediate failure/crash", 0, 1, 1)
    }

    @Test
    fun testFlatMappedDeferred() {
        val scounter = FutureCounter()
        val fcounter = FutureCounter()
        val successMap: (String) -> RFuture<Boolean> = { _ ->
            RFuture.success(true)
        }
        val failMap: (String) -> RFuture<Boolean> = { _ ->
            RFuture.failure<Boolean>(Exception("Barzle!"))
        }
        val success = RPromise.create<String>()
        scounter.bind(success.flatMap(successMap))
        scounter.check("before succeed/succeed", 0, 0, 0)
        fcounter.bind(success.flatMap(failMap))
        fcounter.check("before succeed/fail", 0, 0, 0)
        success.succeed("Yay!")
        scounter.check("after succeed/succeed", 1, 0, 1)
        fcounter.check("after succeed/fail", 0, 1, 1)

        val failure = RPromise.create<String>()
        scounter.bind(failure.flatMap(successMap))
        fcounter.bind(failure.flatMap(failMap))
        scounter.check("before fail/success", 0, 0, 0)
        fcounter.check("before fail/failure", 0, 0, 0)
        failure.fail(Exception("Boo!"))
        scounter.check("after fail/success", 0, 1, 1)
        fcounter.check("after fail/failure", 0, 1, 1)

        assertFalse(success.hasConnections())
        assertFalse(failure.hasConnections())
    }

    @Test
    fun testFlatMappedDoubleDeferred() {
        val scounter = FutureCounter()
        val fcounter = FutureCounter()

        run {
            val success = RPromise.create<String>()
            val innerSuccessSuccess = RPromise.create<Boolean>()
            scounter.bind(success.flatMap { value -> innerSuccessSuccess })
            scounter.check("before succeed/succeed", 0, 0, 0)
            val innerSuccessFailure = RPromise.create<Boolean>()
            fcounter.bind(success.flatMap { value -> innerSuccessFailure })
            fcounter.check("before succeed/fail", 0, 0, 0)

            success.succeed("Yay!")
            scounter.check("after first succeed/succeed", 0, 0, 0)
            fcounter.check("after first succeed/fail", 0, 0, 0)
            innerSuccessSuccess.succeed(true)
            scounter.check("after second succeed/succeed", 1, 0, 1)
            innerSuccessFailure.fail(Exception("Boo hoo!"))
            fcounter.check("after second succeed/fail", 0, 1, 1)

            assertFalse(success.hasConnections())
            assertFalse(innerSuccessSuccess.hasConnections())
            assertFalse(innerSuccessFailure.hasConnections())
        }

        run {
            val failure = RPromise.create<String>()
            val innerFailureSuccess = RPromise.create<Boolean>()
            scounter.bind(failure.flatMap { value -> innerFailureSuccess })
            scounter.check("before fail/succeed", 0, 0, 0)
            val innerFailureFailure = RPromise.create<Boolean>()
            fcounter.bind(failure.flatMap { value -> innerFailureFailure })
            fcounter.check("before fail/fail", 0, 0, 0)

            failure.fail(Exception("Boo!"))
            scounter.check("after first fail/succeed", 0, 1, 1)
            fcounter.check("after first fail/fail", 0, 1, 1)
            innerFailureSuccess.succeed(true)
            scounter.check("after second fail/succeed", 0, 1, 1)
            innerFailureFailure.fail(Exception("Is this thing on?"))
            fcounter.check("after second fail/fail", 0, 1, 1)

            assertFalse(failure.hasConnections())
            assertFalse(innerFailureSuccess.hasConnections())
            assertFalse(innerFailureFailure.hasConnections())
        }
    }

    @Test
    fun testSequenceImmediate() {
        val counter = FutureCounter()

        val success1 = RFuture.success("Yay 1!")
        val success2 = RFuture.success("Yay 2!")

        val failure1 = RFuture.failure<String>(Exception("Boo 1!"))
        val failure2 = RFuture.failure<String>(Exception("Boo 2!"))

        val sucseq = RFuture.sequence(list(success1, success2))
        counter.bind(sucseq)
        sucseq.onSuccess { results: List<String> ->
            assertEquals(list("Yay 1!", "Yay 2!"), results)
        }
        counter.check("immediate seq success/success", 1, 0, 1)

        counter.bind(RFuture.sequence(list(success1, failure1)))
        counter.check("immediate seq success/failure", 0, 1, 1)

        counter.bind(RFuture.sequence(list(failure1, success2)))
        counter.check("immediate seq failure/success", 0, 1, 1)

        counter.bind(RFuture.sequence(list(failure1, failure2)))
        counter.check("immediate seq failure/failure", 0, 1, 1)
    }

    @Test
    fun testSequenceDeferred() {
        val counter = FutureCounter()

        val success1 = RPromise.create<String>()
        val success2 = RPromise.create<String>()
        val failure1 = RPromise.create<String>()
        val failure2 = RPromise.create<String>()

        val suc2seq = RFuture.sequence(list(success1, success2))
        counter.bind(suc2seq)
        suc2seq.onSuccess { results: List<String> ->
            assertEquals(list("Yay 1!", "Yay 2!"), results)
        }
        counter.check("before seq succeed/succeed", 0, 0, 0)
        success1.succeed("Yay 1!")
        success2.succeed("Yay 2!")
        counter.check("after seq succeed/succeed", 1, 0, 1)

        val sucfailseq = RFuture.sequence(list(success1, failure1))
        sucfailseq.onFailure { cause: Throwable ->
            assertTrue(cause is MultiFailureException)
            assertEquals("1 failures: SomeException: Boo 1!", cause.message)
        }
        counter.bind(sucfailseq)
        counter.check("before seq succeed/fail", 0, 0, 0)
        failure1.fail(SomeException("Boo 1!"))
        counter.check("after seq succeed/fail", 0, 1, 1)

        val failsucseq = RFuture.sequence(list(failure1, success2))
        failsucseq.onFailure { cause: Throwable ->
            assertTrue(cause is MultiFailureException)
            assertEquals("1 failures: SomeException: Boo 1!", cause.message)
        }
        counter.bind(failsucseq)
        counter.check("after seq fail/succeed", 0, 1, 1)

        val fail2seq = RFuture.sequence(list(failure1, failure2))
        fail2seq.onFailure { cause: Throwable ->
            assertTrue(cause is MultiFailureException)
            assertEquals("2 failures: SomeException: Boo 1!, SomeException: Boo 2!",
                    cause.message)
        }
        counter.bind(fail2seq)
        counter.check("before seq fail/fail", 0, 0, 0)
        failure2.fail(SomeException("Boo 2!"))
        counter.check("after seq fail/fail", 0, 1, 1)
    }

    @Test
    fun testSequenceEmpty() {
        val counter = FutureCounter()
        val seq = RFuture.sequence(emptyList<RFuture<String>>())
        counter.bind(seq)
        counter.check("sequence empty list succeeds", 1, 0, 1)
    }

    @Test
    fun testSequenceTuple() {
        val counter = FutureCounter()
        val string = RFuture.success("string")
        val integer = RFuture.success(42)

        val sucsuc = RFuture.sequence(string, integer)
        sucsuc.onSuccess { tup: Pair<String, Int> ->
            assertEquals("string", tup.first)
            assertEquals(42, tup.second)
        }
        counter.bind(sucsuc)
        counter.check("tuple2 seq success/success", 1, 0, 1)

        val fail = RFuture.failure<Int>(Exception("Alas, poor Yorrick."))
        val sucfail = RFuture.sequence(string, fail)
        counter.bind(sucfail)
        counter.check("tuple2 seq success/fail", 0, 1, 1)

        val failsuc = RFuture.sequence(fail, string)
        counter.bind(failsuc)
        counter.check("tuple2 seq fail/success", 0, 1, 1)
    }

    @Test
    fun testCollectEmpty() {
        val counter = FutureCounter()
        val seq = RFuture.collect(emptyList<RFuture<String>>())
        counter.bind(seq)
        counter.check("collect empty list succeeds", 1, 0, 1)
    }

    // fucking Java generics and arrays... blah
    protected fun <T> list(one: T, two: T): List<T> {
        val list = ArrayList<T>()
        list.add(one)
        list.add(two)
        return list
    }
}

/** Dummy exception that has consistent toString() behavior across platforms.
 * [kotlin.Exception] would print `java.lang.Exception` on jvm backend and `Exception` in js backend.
 */
class SomeException(message: String) : Exception(message) {
    override fun toString(): String {
        return "SomeException: $message"
    }
}
