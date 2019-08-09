package csense.idea.kotlin.assistance.quickfixes

import com.intellij.codeInspection.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.util.*
import org.jetbrains.kotlin.idea.util.application.*
import org.jetbrains.kotlin.psi.*

class ByLazyDelegateQuickFix(element: KtProperty) : LocalQuickFixOnPsiElement(element) {
    override fun getFamilyName(): String {
        return "csense - kotlin assistant - wrap in by lazy quick fix"
    }

    override fun getText(): String {
        return "Wrap the expression in a \"by lazy\" to avoid initialization order issues."
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val asProp = startElement as? KtProperty ?: return
        val fact = KtPsiFactory(project)
        val content = asProp.lastChild?.text ?: return
        val exp = fact.createProperty("val ${asProp.name} by lazy {\n $content \n}")
        project.executeWriteCommand(text) {
            try {
                asProp.replace(exp)
            } catch (e: IncorrectOperationException) {
                TODO("Add error handling here")
            }
        }
    }

}