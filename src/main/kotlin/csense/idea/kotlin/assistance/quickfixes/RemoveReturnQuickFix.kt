package csense.idea.kotlin.assistance.quickfixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import csense.idea.kotlin.assistance.Constants
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class RemoveReturnQuickFix(
        returnStatement: KtReturnExpression
) : LocalQuickFix {
    private val returnSt: SmartPsiElementPointer<KtReturnExpression> = returnStatement.createSmartPointer()

    override fun getFamilyName(): String = Constants.InspectionGroupName

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = returnSt.element ?: return
        val fact = KtPsiFactory(project)
        element.replace(fact.createExpression(element.text.replace("return ", "")))
    }

    override fun getName(): String {
        return "remove return"
    }

    override fun startInWriteAction(): Boolean {
        return true
    }
}