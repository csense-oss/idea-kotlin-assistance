object ForeachIntIndex {
    fun wrongNames() {
        val x = listOf(42)
        x.<error descr="You have mismatched arguments names 
(\"index\" - should be at position 0)">forEachIndexed</error> { item, index ->

        }
    }

    fun correctNames() {
        val x = listOf(42)
        x.forEachIndexed { index, item ->

        }
    }
}