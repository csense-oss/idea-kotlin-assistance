@file:Suppress("UNUSED_VALUE", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "UNUSED_PARAMETER")

object SimpleOverwriteUsage {

    fun iDoBadStuff() {
        var x = 42
        var y = 11
        
        x = y
        
        <warning descr="Using \"x\" after overwriting it with \"y\", thus they are the same, this looks like a bug (use after overwrite in conjunction with overwritten value).">y = x</warning>
        useX(y)
        useX(x)
    }

    fun useX(x: Int) {

    }
}