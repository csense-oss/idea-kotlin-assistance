//package csense.idea.kotlin.assistance.documentationHandler
//
//import com.intellij.lang.documentation.AbstractDocumentationProvider
//import com.intellij.lang.documentation.CodeDocumentationProvider
//import com.intellij.lang.java.JavaDocumentationProvider
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.util.Pair
//import com.intellij.psi.PsiComment
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiFile
//import com.intellij.psi.PsiManager
//import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
//import java.awt.Image
//
///**
// * see
// * https://upsource.jetbrains.com/idea-ce/file/idea-ce-40e5005d02df57f58ac2d498867446c43d61101f/plugins/properties/src/com/intellij/lang/properties/PropertiesDocumentationProvider.java
// *
// */
//class DocumentationHandler : AbstractDocumentationProvider(), CodeDocumentationProvider {
//    //region use kotlin's own quick doc provider.
//    private val kotlinProvider = KotlinQuickDocumentationProvider()
//    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?): PsiElement? {
//        return kotlinProvider.getCustomDocumentationElement(editor, file, contextElement)
//    }
//
//    override fun getLocalImageForElement(element: PsiElement, imageSpec: String): Image? {
//        return kotlinProvider.getLocalImageForElement(element, imageSpec)
//    }
//
//    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
//        return kotlinProvider.getQuickNavigateInfo(element, originalElement)
//    }
//
//    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String> {
//        return kotlinProvider.getUrlFor(element, originalElement) ?: mutableListOf()
//    }
//
//    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
//        if (element == null) {
//            return null
//        }
//        return kotlinProvider.generateDoc(element, originalElement)
//    }
//
//    override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, `object`: Any?, element: PsiElement?): PsiElement? {
//        if (psiManager == null) {
//            return null
//        }
//        return kotlinProvider.getDocumentationElementForLookupItem(psiManager, `object`, element)
//    }
//
//    override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String?, context: PsiElement?): PsiElement? {
//        if (psiManager == null || link == null) {
//            return null
//        }
//        return kotlinProvider.getDocumentationElementForLink(psiManager, link, context)
//    }
//    //endregion
//
//    val javadocProvider = JavaDocumentationProvider()
//    override fun findExistingDocComment(contextElement: PsiComment?): PsiComment? {
//        return javadocProvider.findExistingDocComment(contextElement) ?: contextElement
//    }
//
//    override fun parseContext(startPoint: PsiElement): Pair<PsiElement, PsiComment>? {
//        return javadocProvider.parseContext(startPoint)
//    }
//
//    override fun generateDocumentationContentStub(contextComment: PsiComment?): String? {
//        return ""
//    }
//}