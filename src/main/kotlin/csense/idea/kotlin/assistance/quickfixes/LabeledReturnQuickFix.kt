package csense.idea.kotlin.assistance.quickfixes

import com.intellij.codeInspection.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

/**
 * Given a return and a label allows you to add the label to the given return statement.
 */
class LabeledReturnQuickFix(
        returnStatement: KtReturnExpression,
        val order: Int,
        val labelName: String
) : LocalQuickFix {

    private val returnSt: SmartPsiElementPointer<KtReturnExpression> = returnStatement.createSmartPointer()

    override fun getFamilyName(): String = Constants.InspectionGroupName

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = returnSt.element ?: return
        val code = element.text.replace("return ", "return@$labelName ")
        val fac = KtPsiFactory(project)
        element.replace(fac.createExpression(code))
    }

    override fun getName(): String {
        return "($order) labeled return to '$labelName'"
    }

    override fun startInWriteAction(): Boolean {
        return true
    }
}
