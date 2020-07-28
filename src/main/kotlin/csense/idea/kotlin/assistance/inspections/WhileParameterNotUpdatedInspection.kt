package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import csense.idea.base.bll.*
import csense.idea.base.bll.kotlin.*
import csense.idea.kotlin.assistance.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class WhileParameterNotUpdatedInspection : AbstractKotlinInspection() {
    
    override fun getDisplayName(): String {
        return ""
    }
    
    override fun getStaticDescription(): String? {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
            The loop parameter is not updated with a "new" value (on iteration) thus the loop will continue forever.
            This is very classic when forgetting a "+" in front of a "=" (eg: "y = x", instead of "y += x")
            Or if you simply forgot to update it (with something that is not a constant).
        """.trimIndent()
    }
    
    override fun getDescriptionFileName(): String? {
        return "more desc ? "
    }
    
    override fun getShortName(): String {
        return "WhileParameterNotUpdatedInspection"
    }
    
    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }
    
    override fun isEnabledByDefault(): Boolean {
        return true
    }
    
    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }
    
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean
    ): KtVisitorVoid {
        return expressionVisitor { exp: KtExpression ->
            if (exp !is KtWhileExpression) {
                return@expressionVisitor
            }
            val condition = exp.condition
            val localVarVariableWeAreTesting = condition?.findDescendantOfType<KtNameReferenceExpression> {
                if (it.parent is KtDotQualifiedExpression) {
                    return@findDescendantOfType false
                }
                val asProp = it.resolveAsKtProperty() ?: return@findDescendantOfType false
                asProp.isLocal && asProp.isVar
            } ?: return@expressionVisitor
            val body = exp.body
            val anyValidAssignments = body?.anyDescendantOfType<KtNameReferenceExpression> {
                //only consider the loop variable.
                if (it.getReferencedName() != localVarVariableWeAreTesting.getReferencedName()) {
                    return@anyDescendantOfType false
                }
                //if we are referencing it, then test whenever we are assigning it.
                val binExp = it.parent as? KtBinaryExpression //should be of the form "x = .." which is a binary exp.
                if (binExp?.left != it //not an binary exp to our loop param or we are not any assignment type.
                        || !KtTokens.ALL_ASSIGNMENTS.contains(binExp.operationToken)) {
                    return@anyDescendantOfType false
                }
                //sub optimal, but simply put, if we are any "+=" or alike then its fine.
                if (KtTokens.AUGMENTED_ASSIGNMENTS.contains(binExp.operationToken)) {
                    return@anyDescendantOfType true
                }
                //we are only an equal here, so operation token is "=" (EQ), so verify the rhs.
                if (binExp.right?.isConstant() == true) {
                    return@anyDescendantOfType false
                }
                //lastly look if we are referencing a local val (that would never work) given the val is declared before the while.
                val isProp = binExp.right?.resolveAsKtProperty()
                //if we have a property, that is declared before the while and is val, then it will never change, thus
                //its "constant"
                if (isProp != null &&
                        isProp.isLocal && isProp.isVal && isProp.textOffset < exp.textOffset) {
                    return@anyDescendantOfType false
                }
                true
                
            }
            //we are in a while loop, with a "local var" we are testing for. so lets see if we actually change it inside of the while code.
            if (anyValidAssignments != true) {
                holder.registerProblemSafe(
                        condition,
                        "While loop parameter `${localVarVariableWeAreTesting.getReferencedName()}` is not updated (or updated to the same value) on each iteration"
                )
            }
        }
    }
}
