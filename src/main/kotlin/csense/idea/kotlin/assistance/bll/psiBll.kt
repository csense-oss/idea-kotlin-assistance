package csense.idea.kotlin.assistance.bll

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer


fun <E : PsiElement> E.smartPsiElementPointer(): SmartPsiElementPointer<E> =
        smartPsiElementPointer(project, containingFile)

fun <E : PsiElement> E.smartPsiElementPointer(project: Project, file: PsiFile): SmartPsiElementPointer<E> =
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this, file)
