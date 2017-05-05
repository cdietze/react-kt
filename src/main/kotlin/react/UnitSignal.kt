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
 * A signal that emits an event with no associated data. It can be used with `UnitSlot`
 * like so:
 * <pre>`UnitSignal signal = new UnitSignal();
 * signal.connect(new UnitSlot() {
 * public void onEmit () {
 * // ...
 * }
 * });
`</pre> *
 */
class UnitSignal : AbstractSignal<Unit>() {
    /**
     * Causes this signal to emit an event to its connected slots.
     */
    fun emit() {
        notifyEmit(Unit)
    }

    /**
     * Returns a slot which can be used to wire this signal to the emissions of a [Signal] or
     * another value.
     */
    fun slot(): UnitSlot = { _ -> emit() }
}
