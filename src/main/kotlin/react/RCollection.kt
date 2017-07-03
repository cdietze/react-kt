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
 * A base class for reactive collections ([RList], [RMap], [RSet]).
 */
abstract class RCollection<T> : Reactor() {

    /**
     * Returns the number of elements in this collection.
     */
    abstract val size: Int get

    private val sizeViewDelegate: Lazy<Value<Int>> = lazy {
        Value(size)
    }

    /**
     * Exposes the size of this collection as a value.
     * Initialized lazily.
     */
    val sizeView: ValueView<Int> by sizeViewDelegate

    /**
     * Returns a reactive value which is true when this collection is empty, false otherwise.
     */
    val isEmptyView: ValueView<Boolean>
        get() = sizeView.map({ it <= 0 })

    /**
     * Returns a reactive value which is false when this collection is empty, true otherwise.
     */
    val isNonEmptyView: ValueView<Boolean>
        get() = sizeView.map({ it > 0 })

    /**
     * Updates the reactive size value. The underlying collection need only call this method if it
     * changes the size of its collection *without* also calling [.notify].
     */
    protected fun updateSize() {
        if (sizeViewDelegate.isInitialized()) sizeViewDelegate.value.update(size)
    }

    override fun notify(notifier: Reactor.Notifier, a1: Any?, a2: Any?, a3: Any?) {
        try {
            super.notify(notifier, a1, a2, a3)
        } finally {
            updateSize()
        }
    }
}
