package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.quickfixes.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class InitializationOrder : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Initialization order"
    }

    override fun getStaticDescription(): String? {
        return """
            This inspection tells whenever you have an "invalid" initialization order.
            This is because the JVM does not guarantee all scenarios.
            This means that declaration order defines initialization order, and so on (for example what about inheritance ?)
            
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String? {
        return "more desc ? "
    }

    override fun getShortName(): String {
        return "InitOrder"
    }

    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return arrayOf(
                PropertyFunctionSuppressor("Suppress initialization issue", groupDisplayName, shortName))
    }

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean): KtVisitorVoid {
        return classOrObjectVisitor { ourClass: KtClassOrObject ->
            val nonDelegates = ourClass.findNonDelegatingProperties()
            val nonDelegatesQuickLookup = nonDelegates.computeQuickIndexedNameLookup()
            val ourFqName = ourClass.fqName?.asString() ?: return@classOrObjectVisitor
            nonDelegates.forEach { prop: KtProperty ->
                val propName = prop.name ?: return@forEach
                val localRefs = prop.findLocalReferencesForInitializer(
                        ourFqName,
                        nonDelegatesQuickLookup.keys)
                val invalidOrders = localRefs.resolveInvalidOrders(propName, nonDelegatesQuickLookup)
                if (invalidOrders.isNotEmpty()) {
                    holder.registerProblem(prop,
                            createErrorDescription(invalidOrders),
                            *createQuickFixes(prop, ourClass)
                    )
                }
            }
        }
    }


    fun createQuickFixes(property: KtProperty, classObj: KtClassOrObject): Array<LocalQuickFix> {
        return arrayOf(
                MoveDeclarationsQuickFix(classObj),
                ByLazyDelegateQuickFix(property)
        )
    }

    fun createErrorDescription(invalidOrders: List<KtNameReferenceExpression>): String {
        val invalidOrdersNames = invalidOrders.map {
            it.getReferencedName()
        }.toSet().joinToString("\",\"", prefix = "\"", postfix = "\"")
        return "Initialization order is invalid for $invalidOrdersNames\nIt can / will result in null at runtime(Due to the JVM)"
    }


}

fun KtClassOrObject.getProperties() = getBody()?.properties.orEmpty()

fun KtClassOrObject.findNonDelegatingProperties(): List<KtProperty> {
    return getProperties().filterNot { prop -> prop.hasDelegate() }
}

fun KtProperty.hasNoInitializer() = !hasInitializer()
fun KtNamedDeclaration.isNotAbstract(): Boolean = !isAbstract()

fun List<KtProperty>.computeQuickIndexedNameLookup(): Map<String, Int> {
    val nonDelegatesQuickLookup: MutableMap<String, Int> = mutableMapOf()
    forEachIndexed { index, item ->
        val propName = item.name
        if (propName != null) {
            nonDelegatesQuickLookup[propName] = index
        }
    }
    return nonDelegatesQuickLookup
}

fun PsiElement.findLocalReferences(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>
): List<KtNameReferenceExpression> {
    return collectDescendantsOfType<KtNameReferenceExpression> { nameRef ->
        val refFqName = nameRef.resolveMainReferenceToDescriptors().firstOrNull()?.fqNameSafe
                ?: return@collectDescendantsOfType false
        return@collectDescendantsOfType refFqName.asString().startsWith(ourFqNameStart) &&
                nonDelegatesQuickLookup.contains(nameRef.getReferencedName())
    }
}

fun KtProperty.findLocalReferencesForInitializer(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>
): List<KtNameReferenceExpression> {
    return initializer?.collectDescendantsOfType<KtNameReferenceExpression> { nameRef ->
        val refFqName = nameRef.resolveMainReferenceToDescriptors().firstOrNull()?.fqNameSafe
                ?: return@collectDescendantsOfType false
        return@collectDescendantsOfType refFqName.asString().startsWith(ourFqNameStart) &&
                nonDelegatesQuickLookup.contains(nameRef.getReferencedName())
    } ?: return listOf()
}

fun List<KtNameReferenceExpression>.resolveInvalidOrders(name: String, order: Map<String, Int>): List<KtNameReferenceExpression> {
    val ourIndex = order[name] ?: return listOf() //should not return.... :/ ???
    return filter {
        val itName = it.getReferencedName()
        val itOrder = (order[itName] ?: 0)
        //if we reference something that is declared after us, its an "issue".
        itOrder > ourIndex
    }
}

