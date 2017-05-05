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

/**
 * TODO(cdi) commented out for now, migrate whatever makes sense to kotlin later
 * Provides utility methods for [Slot]s.
 */
object Slots {
    /** A slot that does nothing. Useful when you don't want to fiddle with null checks.  */
    val NOOP: UnitSlot = {}

//    /**
//     * Returns a slot that logs the supplied message (via [System.err]) with the emitted
//     * value appended to it before passing the emitted value on to `slot`. Useful for
//     * debugging.
//     */
//    fun <T> trace(message: String, slot: Slot<T>): Slot<T> {
//        return object : Slot<T>() {
//            override fun onEmit(value: T) {
//                System.err.println(message + value)
//                slot.onEmit(value)
//            }
//        }
//    }
}// no constructski
