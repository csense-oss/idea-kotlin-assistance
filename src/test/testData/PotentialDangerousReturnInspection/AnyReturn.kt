object AnyReturn {
    fun List<String>.badReturn() = any {
        <error descr="Dangerous return statement in inline function 
 - is your intent to return from this lambda or (any/ the) outer function(s) ? 
 annotate the returned scope or choose an action">return true</error>
    }
}
