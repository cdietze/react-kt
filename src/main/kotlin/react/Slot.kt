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
 * Reacts to signal emissions.
 */
typealias Slot<T> = SignalViewListener<T>
typealias UnitSlot = Slot<Any>

/**
 * Returns a slot that maps values via `f` and then passes them to this slot.
 * This is essentially function composition in that `slot.compose(f)` means
 * `slot(f(value)))` where this slot is treated as a side effecting void function.
 */
fun <T, S> Slot<T>.compose(f: (S) -> T): Slot<S> =
        { value -> this@compose(f(value)) }

/**
 * Returns a slot that is only notified when the signal to which this slot is connected emits a
 * value which causes `pred` to return true.
 */
fun <T, S : T> Slot<T>.filtered(pred: (S) -> Boolean): Slot<S> =
        { value -> if (pred(value)) this@filtered(value) }

/**
 * Returns a new slot that invokes this slot and then evokes `after`.
 */
fun <T, S : T> Slot<T>.andThen(after: Slot<S>): Slot<S> =
        { event ->
            this@andThen(event)
            after(event)
        }

/**
 * Allows a slot to be used as a {@link ValueView.Listener} by passing just the new value
 * through.
 */
fun <T> Slot<T>.asValueViewListener(): ValueViewListener<T> =
        { newValue, oldValue ->
            this@asValueViewListener(newValue)
        }
