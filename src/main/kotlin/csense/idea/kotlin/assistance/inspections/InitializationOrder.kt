package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.quickfixes.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
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
            val nonDelegatesQuickLookup = ourClass.computeQuickIndexedNameLookup()
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

    fun createErrorDescription(invalidOrders: List<DangerousReference>): String {

        val allInvalid = invalidOrders.map {
            it.innerReferences
        }.flatten().map { it.getReferencedName() }.distinct()


        val haveInnerInvalid =
                allInvalid.joinToString(",\"", "\"", "\"")

        val innerMessage = if (haveInnerInvalid.isNotBlank()) {
            "\n(Indirect dangerous references = $haveInnerInvalid)\n"
        } else {
            ""
        }

        val invalidOrdersNames = invalidOrders.map {
            it.mainReference.getReferencedName()
        }.distinct().joinToString("\",\"", prefix = "\"", postfix = "\"")
        return "Initialization order is invalid for $invalidOrdersNames\n" +
                innerMessage +
                "It can / will result in null at runtime(Due to the JVM)"
    }


}

fun KtClassOrObject.getProperties() = getBody()?.properties.orEmpty()

fun KtClassOrObject.findNonDelegatingProperties(): List<KtProperty> {
    return getProperties().filterNot { prop -> prop.hasDelegate() }
}

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

class DangerousReference(
        val mainReference: KtNameReferenceExpression,
        val innerReferences: List<KtNameReferenceExpression>)

fun KtProperty.findLocalReferencesForInitializer(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>
): List<DangerousReference> {
    return initializer?.collectDescendantsOfType { nameRef: KtNameReferenceExpression ->
        //skip things that are "ok" / legit.
        nameRef.isPotentialDangerousReference(
                ourFqNameStart,
                nonDelegatesQuickLookup)
    }?.map {
        DangerousReference(it,
                resolveInnerDangerousReferences(
                        ourFqNameStart,
                        nonDelegatesQuickLookup,
                        it.resolveMainReferenceToDescriptors().firstOrNull()))
    } ?: return listOf()
}

private fun KtNameReferenceExpression.isPotentialDangerousReference(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>
): Boolean {
    val referre = this.resolveMainReferenceToDescriptors().firstOrNull() ?: return false

    val refFqName = referre.fqNameSafe
    val isInOurClass = refFqName.asString().startsWith(ourFqNameStart) &&
            nonDelegatesQuickLookup.contains(getReferencedName())
    val psi = referre.findPsi()
    return when (psi) {
        is KtProperty, is KtFunction -> {
            return resolveInnerDangerousReferences(
                    ourFqNameStart,
                    nonDelegatesQuickLookup,
                    referre).isNotEmpty()
        }
        else -> isInOurClass && !isExtensionDeclaration()
    }
}

private fun resolveInnerDangerousReferences(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>,
        mainDescriptor: DeclarationDescriptor?
): List<KtNameReferenceExpression> {
    return when (val psi = mainDescriptor?.findPsi()) {
        is KtProperty, is KtFunction -> {
            psi.findLocalReferences(ourFqNameStart, nonDelegatesQuickLookup)
        }
        else -> listOf()
    }
}

fun List<DangerousReference>.resolveInvalidOrders(
        name: String,
        order: Map<String, Int>
): List<DangerousReference> {
    val ourIndex = order[name] ?: return listOf() //should not return.... :/ ???
    return filter { ref ->
        val isMainRefOk = ref.mainReference.isBeforeOrFunction(ourIndex, order)
        //if we reference something that is declared after us, its an "issue". only if it is not a function
        !isMainRefOk || ref.innerReferences.isAllNotBeforeOrFunction(ourIndex, order)
    }
}

private fun List<KtNameReferenceExpression>.isAllNotBeforeOrFunction(
        ourIndex: Int,
        order: Map<String, Int>
) = !all { it.isBeforeOrFunction(ourIndex, order) }

private fun KtNameReferenceExpression.isBeforeOrFunction(
        ourIndex: Int,
        order: Map<String, Int>): Boolean {
    val itName = getReferencedName()
    val itOrder = (order[itName] ?: Int.MAX_VALUE)
    return itOrder < ourIndex || this.isFunction()
}

fun KtNameReferenceExpression.isFunction(): Boolean {
    return this.resolveMainReferenceToDescriptors().firstOrNull()?.findPsi() as? KtFunction != null
}


fun KtClassOrObject.computeQuickIndexedNameLookup(): Map<String, Int> {
    val resultingMap = mutableMapOf<String, Int>()
    val allProps = collectDescendantsOfType<KtProperty>()

    allProps.forEach { prop ->
        val name = prop.name ?: return@forEach
        resultingMap[name] = prop.startOffsetInParent
    }


    val allFuns = collectDescendantsOfType<KtFunction>()
    allFuns.forEach { function ->
        val name = function.name ?: return@forEach
        resultingMap[name] = function.startOffsetInParent
    }
    return resultingMap
}