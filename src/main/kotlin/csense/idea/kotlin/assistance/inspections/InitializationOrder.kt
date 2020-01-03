package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.base.bll.findNonDelegatingProperties
import csense.idea.base.bll.isFunction
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.quickfixes.*
import csense.idea.kotlin.assistance.suppression.*
import csense.kotlin.ds.cache.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import kotlin.collections.set

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
            val cached = propertyCache[ourClass]
            val nonDelegates = ourClass.findNonDelegatingProperties()
            val nonDelegatesQuickLookup = ourClass.computeQuickIndexedNameLookup()
            val ourFqName = ourClass.fqName?.asString() ?: return@classOrObjectVisitor
            nonDelegates.forEach { prop: KtProperty ->
                val innerCached = cached?.properties?.get(prop)
                val invalidOrders: List<DangerousReference> = if (
                        innerCached != null &&
                        innerCached.second == prop.modificationStamp
                        && cached.timestampOfClassOrObject == ourClass.modificationStamp) {
                    innerCached.first
                } else {
                    val propName = prop.name ?: return@forEach
                    val localRefs = prop.findLocalReferencesForInitializer(
                            ourFqName,
                            nonDelegatesQuickLookup.keys)
                    localRefs.resolveInvalidOrders(propName, nonDelegatesQuickLookup).apply {
                        if (propertyCache[ourClass]?.timestampOfClassOrObject != ourClass.modificationStamp) {
                            propertyCache.remove(ourClass)
                        }
                        propertyCache.getOrPut(ourClass) {
                            PropertyCacheData(mutableMapOf(), ourClass.modificationStamp)
                        }.properties[prop] = Pair(this, prop.modificationStamp)
                    }
                }
                if (invalidOrders.isNotEmpty()) {
                    holder.registerProblem(prop,
                            createErrorDescription(invalidOrders),
                            *createQuickFixes(prop, ourClass)
                    )
                }
            }
        }
    }

    class PropertyCacheData(
            val properties: MutableMap<KtProperty, Pair<List<DangerousReference>, Long>>,
            val timestampOfClassOrObject: Long
    )

    private val propertyCache: SimpleLRUCache<KtClassOrObject, PropertyCacheData> = SimpleLRUCache(1000)

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
                allInvalid.joinToString(",\"")

        val innerMessage = if (haveInnerInvalid.isNotBlank()) {
            "\n(Indirect dangerous references = \"$haveInnerInvalid\")\n"
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
    return collectDescendantsOfType { nameRef: KtNameReferenceExpression ->
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
                nonDelegatesQuickLookup,
                this.name)
    }?.map {
        DangerousReference(it,
                resolveInnerDangerousReferences(
                        ourFqNameStart,
                        nonDelegatesQuickLookup,
                        it.resolveMainReferenceToDescriptors().firstOrNull()?.findPsi()))
    } ?: return listOf()
}

private fun KtNameReferenceExpression.isPotentialDangerousReference(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>,
        fromName: String?
): Boolean {
    val referre = this.resolveMainReferenceToDescriptors().firstOrNull() ?: return false

    val refFqName = referre.fqNameSafe
    val refName = referre.name.asString()
    if (refName == fromName && refFqName.asString().startsWith(ourFqNameStart)) {
        return false //self reference: either the compiler will fail (field initialized to itself) or its a field with the same name as a parameter..
    }
    val isInOurClass = refFqName.asString().startsWith(ourFqNameStart) &&
            nonDelegatesQuickLookup.contains(getReferencedName())
    if (!isInOurClass) {
        return false
    }
    return when (val psi = referre.findPsi()) {
        is KtProperty -> {
            //no getter / setter => real "property"
            // (if no getter is specified, it will be synthesized if a setter is there).
            return if (psi.getter != null) {
                //synthetic property, just like a function.
                resolveInnerDangerousReferences(
                        ourFqNameStart,
                        nonDelegatesQuickLookup,
                        psi.getter).isNotEmpty()
            } else if (psi.setter != null) {
                //setter but no getter ? then only look at the initializer.
                resolveInnerDangerousReferences(
                        ourFqNameStart,
                        nonDelegatesQuickLookup,
                        psi.initializer).isNotEmpty()
            } else {
                //there are no getter /setter so its a raw property
                true
            }
        }
        is KtFunction -> {
            return resolveInnerDangerousReferences(
                    ourFqNameStart,
                    nonDelegatesQuickLookup,
                    psi).isNotEmpty()
        }
        //we are a type argument../ ref. (not a problem)
        //only functions and "properties" are dangerous together.
//        is KtClass, is KtClassOrObject -> false
        else -> false
    }
}

private fun resolveInnerDangerousReferences(
        ourFqNameStart: String,
        nonDelegatesQuickLookup: Set<String>,
        mainPsi: PsiElement?
): List<KtNameReferenceExpression> {
    return when (mainPsi) {
        is KtProperty, is KtFunction -> {
            mainPsi.findLocalReferences(ourFqNameStart, nonDelegatesQuickLookup)
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