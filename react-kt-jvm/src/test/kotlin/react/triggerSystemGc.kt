package react

/**
 * Try to trigger the garbage collector
 */
actual fun triggerSystemGc() {
    System.gc()
    System.gc()
    System.gc()
}
