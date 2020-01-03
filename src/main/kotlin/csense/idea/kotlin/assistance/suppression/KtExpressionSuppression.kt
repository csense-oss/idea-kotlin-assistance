package csense.idea.kotlin.assistance.suppression

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.util.*
import csense.idea.base.bll.psi.findParentAndBeforeFromType
import org.jetbrains.kotlin.psi.*

class KtExpressionSuppression(
        val displayText: String,
        val familyNameToUse: String,
        val shortName: String
) : SuppressIntentionAction() {
    override fun getFamilyName(): String {
        return familyNameToUse
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return true
    }

    override fun getText(): String {
        return displayText
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val prop = element.findParentAndBeforeFromType<KtBlockExpression>() ?: return
        val factory = KtPsiFactory(project)
        val annotation = factory.createExpression("@Suppress(\"$shortName\")")
        try {
            prop.first
                    .addBefore(annotation, prop.second)
                    .add(factory.createWhiteSpace("\n"))
        } catch (e: IncorrectOperationException) {
            TODO("Add error handling here")
        }
    }

}