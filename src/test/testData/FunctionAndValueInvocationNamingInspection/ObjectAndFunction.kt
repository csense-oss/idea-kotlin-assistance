
package test.test

<warning descr="Function have same name as property, please change property name (or function) to avoid confusion and or invocation clarity">object foo</warning> {
    operator fun invoke() {
        println("an object")
    }
}

<warning descr="Function have same name as property, please change property name (or function) to avoid confusion and or invocation clarity">fun foo()</warning> {
    println("a function")
}

fun main() {
    foo()
}