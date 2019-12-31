package csense.idea.kotlin.assistance.quickfixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import csense.idea.kotlin.assistance.Constants
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression

/**
 * Given a return and a label allows you to add the label to the given return statement.
 */
class LabeledReturnQuickFix(
        returnStatement: KtReturnExpression,
        val labelName: String
) : LocalQuickFix {

    private val returnSt: SmartPsiElementPointer<KtReturnExpression> =
            SmartPointerManager.getInstance(returnStatement.project).createSmartPsiElementPointer(returnStatement, returnStatement.containingFile)

    override fun getFamilyName(): String = Constants.InspectionGroupName

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = returnSt.element ?: return
        val code = element.text.replace("return ", "return@$labelName ")
        val fac = KtPsiFactory(project)
        element.replace(fac.createExpression(code))
    }

    override fun getName(): String {
        return "labeled return to '$labelName'"
    }

    override fun startInWriteAction(): Boolean {
        return true
    }
}
