package csense.idea.kotlin.assistance.inspections

//idea
//catch multiple overwrites to a given variable / field in the same scope / function
/*

class MyData(var someMutableData: String)

val x = MyData("test")
//... potentially some code later
if (someExpression) {
    x.someMutableData = "test1"
} else {
    x.someMutableData = "test2"
}
val y = "myValue"
// some coder later ?
x.someMutableData = y + "-"

or if inside of any if or alike / lambda. (all future overwrites of x.someMutableData)
this should see that someMutableData is a "simple property" /if the setter is "complex" then ignore it.
given java, we might have a harder problem as any setter would have to be inspected to be "simple".
*/
