abstract class Base {
    abstract fun a()
}

class C(<warning descr="Function have same name as property, please change property name (or function) to avoid confusion and or invocation clarity">val a: () -> Unit</warning>) : Base() {
    override fun a(): Unit = a()
}