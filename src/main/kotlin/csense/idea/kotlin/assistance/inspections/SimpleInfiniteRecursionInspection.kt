package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.psi.*

//eg fun x()= x()
//or no throwing of exceptions and no returns except for calling the function again...

class SimpleInfiniteRecursionInspection : AbstractKotlinInspection() {
    
    override fun getDisplayName(): String {
        return ""
    }
    
    override fun getStaticDescription(): String? {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
        
        """.trimIndent()
    }
    
    override fun getDescriptionFileName(): String? {
        return "more desc ? "
    }
    
    override fun getShortName(): String {
        return "InfiniteRecursionIssue"
    }
    
    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }
    
    override fun isEnabledByDefault(): Boolean {
        return true
    }
    
    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return arrayOf(
                KtExpressionSuppression("Suppress naming mismatch issue", groupDisplayName, shortName))
    }
    
    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.WARNING
    }
    
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean
    ): KtVisitorVoid {
        return callExpressionVisitor { exp: KtCallExpression ->
//            if (exp.resolve() == exp.findParentOfType<KtFunction>()) {
//                holder.registerProblem(exp, "todo must see more.")
//            }
        }
    }
}