package react

actual class WeakReference<out T> actual constructor(value: T) {
    private val ref = java.lang.ref.WeakReference(value)
    actual fun get(): T? = ref.get()
}
