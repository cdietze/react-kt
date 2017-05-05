package java.lang

/** TODO (cdi) I think we should just remove this AutoCloseable interface and only use our `react.Closeable` */
interface AutoCloseable {
    fun close() : Unit
}