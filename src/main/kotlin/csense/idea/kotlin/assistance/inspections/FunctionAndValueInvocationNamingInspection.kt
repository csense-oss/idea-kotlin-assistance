package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.base.bll.kotlin.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

/**
 * see
 * https://youtrack.jetbrains.com/issue/KT-29451
 * or
 * https://youtrack.jetbrains.com/issue/KT-27490
 * short desc:
 *
abstract class Base {
abstract fun a()
}
class C(val a: () -> Unit) : Base() {
override fun a(): Unit = a()
}

or
object foo {
operator fun invoke() {
println("an object")
}
}

fun foo() {
println("a function")
}
 
 */
class FunctionAndValueInvocationNamingInspection : AbstractKotlinInspection() {
    
    override fun getDisplayName(): String {
        return "Function and variable name overlap"
    }
    
    override fun getStaticDescription(): String? {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
            This inspection tells whenever a class have a function with a given parameter / property name.
            This can be very confusion especially if the parameter / property is a function type and can be invoked.
        """.trimIndent()
    }
    
    override fun getDescriptionFileName(): String? {
        return "more desc ? "
    }
    
    override fun getShortName(): String {
        return "FunctionAndValueInvocationNaming"
    }
    
    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }
    
    override fun isEnabledByDefault(): Boolean {
        return true
    }
    
    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return arrayOf(
                KtExpressionSuppression("Suppress Function and variable name overlap issue", groupDisplayName, shortName))
    }
    
    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.WARNING
    }
    
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean
    ): KtVisitorVoid {
        return classOrObjectVisitor {
            val functions = it.getAllFunctions()
            val properties = it.getAllClassProperties()
            val mappedProperties = mutableMapOf<Name, KtNamedDeclaration>()
            properties.forEach { declaration ->
                mappedProperties[declaration.nameAsSafeName] = declaration
            }
            functions.forEach { fnc: KtNamedFunction ->
                val propFound = mappedProperties[fnc.nameAsSafeName]
                if (propFound != null) {
                    holder.registerProblem(
                            propFound,
                            "Function have same name as property, please change property name (or function) to avoid confusion and or invocation clarity")
                }
            }
        }
    }
}

fun KtClassOrObject.getAllClassProperties(): List<KtNamedDeclaration> {
    val localFields = collectDescendantsOfType<KtProperty> {
        !it.isLocal && (it.resolveType()?.isFunctionType ?: false)
    }
    val constructorFields: List<KtNamedDeclaration> = primaryConstructor?.let {
        it.collectDescendantsOfType { param: KtParameter ->
            param.hasValOrVar() && param.isFunctionalType()
        }
    } ?: listOf()
    return localFields + constructorFields
}