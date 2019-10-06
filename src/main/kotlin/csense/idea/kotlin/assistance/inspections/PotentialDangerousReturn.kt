package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import csense.idea.kotlin.assistance.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.psi.*

class PotentialDangerousReturn : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Potentially dangerous return from lambda"
    }

    override fun getStaticDescription(): String? {
        return """
            Since it is valid kotlin to return from a inline fun call (with a plain lambda), it can quite easily fall under the radar 
            and you end up maybe even returning while not intended. While a very useful feature, it should require some form of justification to
            break scope rules and to signal the intent, so its not "left there" silent.
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String? {
        return ".."
    }

    override fun getShortName(): String {
        return "PotentialDangerousReturn"
    }

    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.WARNING
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean): KtVisitorVoid {
        return expressionVisitor {

        }
    }
}

/*
a dangerous return can be seen here.
No error highlighting what so ever, since the return is "valid" as the predicate is just "plain" and the function is inline.
```kotlin

fun haveTestOfMethodName(fnNames: List<String>): Boolean = fnNames.any { ourFunction ->
    return true //returns the scope. not the ourFunction
}

```
this should have a justification at least,since it breaks harmony and or the
return should by kotlin (which it does not) have a @haveTestOfMethodName to signal the method itself.
This can cause subtle bugs where one is not aware its a "method" return in a lambda.
Consider if it should only work for returning types / or those should be more "dangerous".
 */