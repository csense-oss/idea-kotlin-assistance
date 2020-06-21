package csense.idea.kotlin.assistance.suppression

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import csense.idea.base.bll.psi.*
import org.jetbrains.kotlin.psi.*


class PropertyFunctionSuppressor(
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
        val prop = element.findParentOfType<KtProperty>()
        val function = element.findParentOfType<KtFunction>()
        val ktElement = prop ?: function ?: return
        val factory = KtPsiFactory(project)
        ktElement.addAnnotationEntry(factory.createAnnotationEntry("@Suppress(\"$shortName\")"))
    }
}