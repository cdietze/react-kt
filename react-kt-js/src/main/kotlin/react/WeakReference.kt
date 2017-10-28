package react

/** Dummy implementation that is actually a hard reference.
 *
 * There is no support of weak references in JS yet.
 * WeakSet and WeakMap have weak *keys* not *values*, so are not useful here.
 */
actual class WeakReference<out T> actual constructor(val value: T) {
    actual fun get(): T? = value
}
