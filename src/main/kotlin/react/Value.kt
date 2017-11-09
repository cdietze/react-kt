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

/**
 * A container for a single value, which may be observed for changes.
 */
open class Value<T>
/**
 * Creates an instance with the supplied starting value.
 */
(protected var _value: T)// we can't have any listeners at this point, so no need to notify
    : AbstractValue<T>() {

    /**
     * Updates this instance with the supplied value. Registered listeners are notified only if the
     * value differs from the current value, as determined via [Any.equals].
     * @return the previous value contained by this instance.
     */
    fun update(value: T): T {
        return updateAndNotifyIf(value)
    }

    /**
     * Updates this instance with the supplied value. Registered listeners are notified regardless
     * of whether the new value is equal to the old value.
     * @return the previous value contained by this instance.
     */
    fun updateForce(value: T): T {
        return updateAndNotify(value)
    }

    /**
     * Returns a slot which can be used to wire this value to the emissions of a [Signal] or
     * another value.
     */
    fun slot(): Slot<T> = { e -> update(e) }

    override fun get(): T {
        return _value
    }

    override fun updateLocal(value: T): T {
        val oldValue = _value
        _value = value
        return oldValue
    }
}
