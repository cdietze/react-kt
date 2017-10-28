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

package java.lang.ref

/**
 * An implementation of weak references in JavaScript that is not actually weak. This just serves
 * to keep KotlinJS from choking.
 * Someday JavaScript may get WeakReferences, at which point we can use 'em.
 */
class WeakReference<T>(protected val _value: T) {

    fun get(): T {
        return _value
    }
}
