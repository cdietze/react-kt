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

typealias Slot<T> = SignalViewListener<T>
typealias UnitSlot = Slot<Any>

/**
 * TODO(cdi) commented out for now, migrate whatever makes sense to kotlin later
 * Reacts to signal emissions.
 */
//abstract class Slot<T> : ValueView.Listener<T>, SignalViewListener<T> {
//    /**
//     * Returns a slot that maps values via `f` and then passes them to this slot.
//     * This is essentially function composition in that `slot.compose(f)` means
//     * `slot(f(value)))` where this slot is treated as a side effecting void function.
//     */
//    fun <S> compose(f: Function<S, T>): Slot<S> {
//        val outer = this
//        return object : Slot<S>() {
//            override fun onEmit(value: S) {
//                outer.onEmit(f.apply(value))
//            }
//        }
//    }
//
//    /**
//     * Returns a slot that is only notified when the signal to which this slot is connected emits a
//     * value which causes `pred` to return true.
//     */
//    fun <S : T> filtered(pred: Function<in S, Boolean>): Slot<S> {
//        val outer = this
//        return object : Slot<S>() {
//            override fun onEmit(value: S) {
//                if (pred.apply(value)) outer.onEmit(value)
//            }
//        }
//    }
//
//    /**
//     * Returns a new slot that invokes this slot and then evokes `after`.
//     */
//    fun <S : T> andThen(after: Slot<in S>): Slot<S> {
//        val before = this
//        return object : Slot<S>() {
//            override fun onEmit(event: S) {
//                before.onEmit(event)
//                after.onEmit(event)
//            }
//        }
//    }
//
//    /**
//     * Allows a slot to be used as a [ValueView.Listener] by passing just the new value
//     * through to [.onEmit].
//     */
//    override fun onChange(value: T, oldValue: T) {
//        onEmit(value)
//    }
//}
