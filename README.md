React-kt
===

[![Build Status](https://travis-ci.org/cdietze/react-kt.svg?branch=master)](https://travis-ci.org/cdietze/react-kt)

React-kt is a low-level library in Kotlin that provides [signal/slot] and [functional reactive programming]-like
primitives. It can serve as the basis for a user interface toolkit, or any other library that has a
model on which clients will listen and to which they will react.

It is a port in Kotlin of the [React project in java](https://github.com/threerings/react).
It is still in early development. The idea is to make it usable in both JVM and JS. 

Building
---

The library is built using [Gradle].

Invoke `./gradlew build` to build the library or
`./gradlew publishToMavenLocal` to install it to your local Maven repository.

[signal/slot]: http://en.wikipedia.org/wiki/Signals_and_slots
[functional reactive programming]: http://en.wikipedia.org/wiki/Functional_reactive_programming
[Gradle]: https://gradle.org/
