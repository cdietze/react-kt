React.kt
===

React.kt is a low-level library in Kotlin that provides [signal/slot] and [functional reactive programming]-like
primitives. It can serve as the basis for a user interface toolkit, or any other library that has a
model on which clients will listen and to which they will react.

It is a port in Kotlin of the [React project in java](https://github.com/threerings/react).
It is still in early development. The idea is to make it usable in both JVM and JS. 

Building
---

The library is built using [Maven].

Invoke `mvn install` to build and install the library to your local Maven repository (i.e.
`~/.m2/repository`).

[signal/slot]: http://en.wikipedia.org/wiki/Signals_and_slots
[functional reactive programming]: http://en.wikipedia.org/wiki/Functional_reactive_programming
[Maven]: http://maven.apache.org/
