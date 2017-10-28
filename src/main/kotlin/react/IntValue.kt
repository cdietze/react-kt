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
 * A [Value] specialized for ints, which has some useful methods. Note: this specialization
 * does not mean "avoids auto-boxing", it just means "adds useful methods".
 */
class IntValue
/**
 * Creates an instance with the specified starting value.
 */
(value: Int) : Value<Int>(value) {

    /**
     * Increments this value by `amount`.
     * @return the incremented value. Note that this differs from [.update], which returns
     * * the previous value.
     */
    fun increment(amount: Int): Int {
        return updateInt(get() + amount)
    }

    /**
     * Increments this value by `amount`, clamping at `max` if the current value plus
     * `amount` exceeds `max`.
     * @return the incremented and clamped value. Note that this differs from [.update],
     * * which returns the previous value.
     */
    fun incrementClamp(amount: Int, max: Int): Int {
        return updateInt(minOf(get() + amount, max))
    }

    /**
     * Increments this value by `amount` (or decrements if negative), clamping to the range
     * [`min`, `max`].
     * @return the incremented and clamped value. Note that this differs from [.update],
     * * which returns the previous value.
     */
    fun incrementClamp(amount: Int, min: Int, max: Int): Int {
        return updateInt(maxOf(min, minOf(get() + amount, max)))
    }

    /**
     * Decrements this value by `amount`, clamping at `min` if the current value minus
     * `amount` is less than `min`.
     * @return the decremented and clamped value. Note that this differs from [.update],
     * * which returns the previous value.
     */
    fun decrementClamp(amount: Int, min: Int): Int {
        return updateInt(maxOf(get() - amount, min))
    }

    protected fun updateInt(value: Int): Int {
        update(value)
        return value
    }
}
