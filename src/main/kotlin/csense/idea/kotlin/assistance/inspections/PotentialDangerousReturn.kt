package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import csense.idea.base.bll.kotlin.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.quickfixes.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

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
        return HighlightDisplayLevel.ERROR
    }
    
    override fun isEnabledByDefault(): Boolean {
        return true
    }
    
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean
    ): KtVisitorVoid {
        return namedFunctionVisitor { ourFnc ->
            //as a start, we only consider functions that are expression functions or have a return function as the first thing.
            //so skip if not.
            val firstChildAsReturn = ourFnc.children.firstOrNull() as? KtReturnExpression
            if (ourFnc.bodyExpression == null && firstChildAsReturn == null) {
                return@namedFunctionVisitor
            }
            val firstExp = if (ourFnc.bodyExpression != null) {
                ourFnc.bodyExpression
            } else {
                firstChildAsReturn
            } ?: return@namedFunctionVisitor
            
            val fncCalls = firstExp.collectDescendantsOfType<KtCallExpression> { exp ->
                //only look for function calls that involves inline (and not only on parameters that are no inline).
                exp.resolveMainReferenceAsKtFunction()?.isInlineWithInlineParameters() ?: false
            }
            fncCalls.forEach { firstCall ->
                val innerReturns = firstCall.collectDescendantsOfType<KtReturnExpression>()
                //only consider simple cases where there are only 1 return (otherwise ourFnc "might" be intended
                val first = innerReturns.singleOrNull() ?: return@namedFunctionVisitor
                //if ourFnc is not a labeled expression then we have 2 returns nested as the "last" things akk, potentially dangerous.
                if (first.labeledExpression == null) {
                    val labelName = firstCall.calleeExpression?.text ?: "-"
                    holder.registerProblem(
                            first,
                            "Dangerous return statement in inline function \n" +
                                    " - is your intent to return from this lambda or (any/ the) outer function(s) ? \n" +
                                    " annotate the returned scope or choose an action",
                            RemoveReturnQuickFix(first),
                            LabeledReturnQuickFix(first, 2, labelName),
                            LabeledReturnQuickFix(first, 1, ourFnc.name
                                    ?: ""))//the last option is to suppress this inspection.
                }
            }
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