package react

expect class WeakReference<out T>(value: T) {
    fun get(): T?
}
