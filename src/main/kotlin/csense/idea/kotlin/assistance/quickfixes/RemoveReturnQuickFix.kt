package csense.idea.kotlin.assistance.quickfixes

import com.intellij.codeInspection.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

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