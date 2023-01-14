package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import csense.idea.base.bll.*
import csense.idea.base.bll.kotlin.*
import csense.idea.base.bll.psi.*
import csense.idea.kotlin.assistance.*
import csense.kotlin.specificExtensions.string.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class MutableVariableUsageAfterOverwriting : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Using value after being overwritten (with other variable)"
    }

    override fun getStaticDescription(): String {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
            This inspection tells whenever a mutable variable have been overwritten by said value, only later to be used in conjunction with that overwritten value
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String {
        return "more desc ? "
    }

    override fun getShortName(): String {
        return "UseAfterOverwritingInConjunction"
    }

    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.WARNING
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitorVoid {
        return binaryExpressionVisitor { assignExpression: KtBinaryExpression ->
            if (assignExpression.isNotEqual) {
                return@binaryExpressionVisitor
            }
            val left = assignExpression.left ?: return@binaryExpressionVisitor
            val right = assignExpression.right ?: return@binaryExpressionVisitor
            val leftResolved = left.resolveAsKtProperty() ?: return@binaryExpressionVisitor
            val rightResolved = right.resolveAsKtProperty() ?: return@binaryExpressionVisitor
            
            //now we know that we have an assignment to a particular field / variable.
            //now look forward and see where this field/variable is used.
            //TODO what is the "real" scope ?? hmm ....
            val codeBlock = assignExpression.findParentOfType<KtBlockExpression>() ?: return@binaryExpressionVisitor
            //for now simple analysis, just look in the current scope. (otherwise assignExpression would also change the message...)
            val usages = codeBlock.collectDescendantsOfType { exp: KtNameReferenceExpression ->
                exp.textOffset > assignExpression.textOffset && exp.resolve() == leftResolved
            }
            if (usages.isEmpty()) {
                return@binaryExpressionVisitor
            }
            usages.forEach {
                val (_, usageInvocation) = it.findParentAndBeforeFromType<KtBlockExpression>()
                    ?: return@forEach
                val usesAssignment = usageInvocation.anyDescendantOfType { exp: KtNameReferenceExpression ->
                    exp.resolve() == rightResolved
                }

                val haveRightChangedInBetween = if (rightResolved.isVal) {
                    false
                } else {
                    //if mutable then we are to find any changes to it. in between assignExpression and usage
                    codeBlock.anyDescendantOfType<KtBinaryExpression> { anotherAssign ->
                        if (anotherAssign.textOffset !in assignExpression.textOffset until usageInvocation.textOffset) {
                            return@anyDescendantOfType false
                        }
                        val prop = anotherAssign.left?.resolveAsKtProperty()
                        prop == rightResolved && anotherAssign.isEqual
                    }
                }

                if (usesAssignment && !haveRightChangedInBetween) {
                    val leftName = leftResolved.name ?: ""
                    val rightName = rightResolved.name ?: ""
                    holder.registerProblemSafe(
                        usageInvocation,
                        "Using ${leftName.modifications.wrapInQuotes()} after overwriting it with ${rightName.modifications.wrapInQuotes()}, thus they are the same, this looks like a bug (use after overwrite in conjunction with overwritten value)."
                    )
                }

            }
        }
    }
}

fun KtExpression.resolveAsKtProperty(): KtProperty? {
    if (this !is KtNameReferenceExpression) {
        return null
    }
    return resolveAsKtProperty()
}

fun KtNameReferenceExpression.resolveAsKtProperty(): KtProperty? {
    return resolve() as? KtProperty
}
