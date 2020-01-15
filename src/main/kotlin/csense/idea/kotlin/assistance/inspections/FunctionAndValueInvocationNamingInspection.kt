package csense.idea.kotlin.assistance.inspections

/**
 * see
 * https://youtrack.jetbrains.com/issue/KT-29451
 * or
 * https://youtrack.jetbrains.com/issue/KT-27490
 * short desc:
 *
abstract class Base {
abstract fun a()
}
class C(val a: () -> Unit) : Base() {
override fun a(): Unit = a()
}

 or
object foo {
operator fun invoke() {
println("an object")
}
}

fun foo() {
println("a function")
}

 */
class FunctionAndValueInvocationNamingInspection {

}