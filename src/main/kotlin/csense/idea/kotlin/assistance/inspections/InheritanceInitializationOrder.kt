package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import kotlin.system.*

class InheritanceInitializationOrder : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Wrong use of initialization across inheritance"
    }

    override fun getStaticDescription(): String? {
        return """
            Since initialization in inheritance is non trivial, and the use of open / abstract methods / functions coupled with
            field / init initialization, opens up the ability to screw up the JVM's initialization design. 
            The result is primitives could end up with their neutral value, and object references will be null.
            These unforeseen consequences, does not necessarily manifest immediately, some can even appear to work, until a random event. 
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String? {
        return "more desc ? "
    }

    override fun getShortName(): String {
        return "InheritanceInitializationOrder"
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
                PropertyFunctionSuppressor("Suppress inheritance initialization issue", groupDisplayName, shortName))
    }

    //we have 2 parts of this inspection
    //part 1 is the base class "issue", where we are accessing / using abstract / open from fields or the init function.
    //this can / may cause null due to the instantiation order for jvm.
    //which is the second part of this inspection, to see if this is done at the usage site.

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean): KtVisitorVoid {
        return classVisitor { ourClass: KtClass ->
            val time = measureTimeMillis {
                val ourFqName = ourClass.fqName?.asString() ?: return@classVisitor
                val isAbstractOrOpen = ourClass.isAbstractOrOpen()
                val isInheriting = ourClass.superTypeListEntries.isNotEmpty()

                if (!isInheriting && !isAbstractOrOpen) {
                    //we are not using inheritance bail out
                    return@classVisitor
                }
                //case child class
                if (isInheriting) {
                    println("child class handling")
                    handleChildClass(ourClass, ourFqName, holder)
                }
                //case base class (potentially child as well)

                if (isAbstractOrOpen) {
                    println("base class handling")
                    handleBaseClass(ourClass, ourFqName, holder)
                }
            }
            println("inheritance took $time ms")
        }
    }

    fun handleBaseClass(ourClass: KtClass, ourFqName: String, holder: ProblemsHolder) {
        computeBaseClassDangerousStarts(ourClass, ourFqName)
                .forEach { (property, references) ->
                    val refNames = references.map { it.getReferencedName() }.distinct()
                    holder.registerProblem(property,
                            "You are using a constructor provided argument for an overridden property\n" +
                                    "For the following: \"${refNames.joinToString("\",\"")}\" - " +
                                    "This has the potential to cause a NullPointerException \n" +
                                    "if the base class uses this in any initialization  (field or init)",
                            ProblemHighlightType.WEAK_WARNING)
                }
    }

    fun handleChildClass(ourClass: KtClass, ourFqName: String, holder: ProblemsHolder) {
        val parentProblems = ourClass.superClass ?: return
        val superProblems = computeBaseClassDangerousStarts(
                parentProblems,
                parentProblems.fqName?.asString() ?: "")


        val constructorArguments = ourClass.getPrimaryConstructorParameterList() ?: return
        val inputParameterNames = constructorArguments.parameters.mapNotNull {
            it.name
        }
        val nonDelegates = ourClass.findNonDelegatingProperties()
        val propertiesOverridden = nonDelegates.filter {
            it.isOverriding()
        }
        val functions = ourClass.getAllFunctions()
        val functionsOverriding = functions.filter {
            it.isOverriding()
        }

        val toLookFor = (
                inputParameterNames
                ).toSet()


        val superProblemsNames = superProblems.values.map {
            it.map { name ->
                name.getReferencedName()
            }
        }.flatten().toSet()
        //TODO cache this
        val cachedProperties = propertiesOverridingCache[ourClass]
        propertiesOverridden.forEach {
            println("looking at prop")
            val cachedProperty = cachedProperties?.cached?.get(it)
            if (cachedProperty != null && cachedProperty.second == it.modificationStamp) {
                println("uses cache for property")
                if (cachedProperty.first.isNotEmpty()) {
                    holder.registerProblem(it,
                            "You are using a constructor provided argument for an overridden property\n" +
                                    "References: \"${cachedProperty.first.joinToString("\",\"")}\";\n" +
                                    "This has the potential to cause a NullPointerException \n" +
                                    "if the base class uses this in any initialization  (field or init)")

                }
                return@forEach
            }
            println("NOT uses cache for property $cachedProperty")
            val ent = propertiesOverridingCache.getOrPut(ourClass, {
                PropertiesOverridingCacheData(mutableMapOf())
            })
            val usesConstructorParameterInOverridden = it.findLocalReferences(ourFqName, toLookFor)
            if (usesConstructorParameterInOverridden.isNotEmpty() && superProblemsNames.contains(it.name ?: "")) {
                val refNames = usesConstructorParameterInOverridden.map { exp -> exp.getReferencedName() }.distinct()
                ent.cached[it] = Pair(refNames, it.modificationStamp)
                holder.registerProblem(it,
                        "You are using a constructor provided argument for an overridden property\n" +
                                "References: \"${refNames.joinToString("\",\"")}\";\n" +
                                "This has the potential to cause a NullPointerException \n" +
                                "if the base class uses this in any initialization  (field or init)")
            } else {
                ent.cached[it] = Pair(listOf(), it.modificationStamp)
            }

        }

        val cachedFunctions = functionsOverridingCache[ourClass]
        functionsOverriding.forEach { function ->
            println("looking at fnc")
            val haveCachedFnc = cachedFunctions?.cached?.get(function)
            if (haveCachedFnc != null && haveCachedFnc.second == function.modificationStamp) {
                println("uses cache for functions overriden")
                if (haveCachedFnc.first.isEmpty()) {
                    return@forEach//no problems :)
                }
                val refNames = haveCachedFnc.first
                holder.registerProblem(function,
                        "You are using a constructor provided argument for an overridden function.\n" +
                                "References: \"${refNames.joinToString("\",\"")}\";\n" +
                                "This will cause a NullPointerException, since it is used in the base class initialization")
            } else {
                val ent = functionsOverridingCache.getOrPut(ourClass, {
                    FunctionsOverridingCacheData(mutableMapOf())
                })
                println("NOT uses cache for functions overridden; is there = $haveCachedFnc ")
                val usesConstructorParameterInOverridden = function.findLocalReferences(ourFqName, toLookFor)
                if (usesConstructorParameterInOverridden.isNotEmpty() && superProblemsNames.contains(function.name
                                ?: "")) {
                    val refNames = usesConstructorParameterInOverridden.map { exp -> exp.getReferencedName() }.distinct()
                    ent.cached[function] = Pair(refNames, function.modificationStamp)
                    holder.registerProblem(function,
                            "You are using a constructor provided argument for an overridden function.\n" +
                                    "References: \"${refNames.joinToString("\",\"")}\";\n" +
                                    "This will cause a NullPointerException, since it is used in the base class initialization")
                } else {
                    ent.cached[function] = Pair(listOf(), function.modificationStamp)
                }
            }
        }

    }
    //TODO limit this cache

    class PropertiesOverridingCacheData(
            val cached: MutableMap<KtProperty, Pair<List<String>, Long>>)

    private val propertiesOverridingCache = mutableMapOf<KtClass, PropertiesOverridingCacheData>()
    //TODO limit this cache

    class FunctionsOverridingCacheData(
            val cached: MutableMap<KtNamedFunction, Pair<List<String>, Long>>)

    private val functionsOverridingCache = mutableMapOf<KtClass, FunctionsOverridingCacheData>()

    fun computeBaseClassDangerousStarts(
            theClass: KtClass,
            ourFqName: String
    ): Map<KtProperty, List<KtNameReferenceExpression>> {
        val inCache = superClassDangerousStateCache[theClass]
        if (inCache?.second == theClass.modificationStamp) {
            println("uses cache.")
            return inCache.first
        }
        val nonDelegates = theClass.findNonDelegatingProperties().filter {
            it.isAbstractOrOpen() || it.hasInitializer()
        }
        val dangerousProperties = nonDelegates.filter {
            it.isAbstractOrOpen()
        }
        val functions = theClass.getAllFunctions()
        val dangerousFunctions = functions.filter {
            it.isAbstractOrOpen()
        }

        val namesToLookFor = (dangerousFunctions.map { it.name } + dangerousProperties.map { it.name }).filterNotNull()
        val namesToLookForSet = namesToLookFor.toSet()

        val resultingMap: MutableMap<KtProperty, List<KtNameReferenceExpression>> = mutableMapOf()
        nonDelegates.forEach {
            val references = it.findLocalReferences(ourFqName, namesToLookForSet)
            if (references.isNotEmpty()) {
                resultingMap[it] = references
            }
        }
        superClassDangerousStateCache[theClass] = Pair(resultingMap, theClass.modificationStamp)
        return resultingMap
    }

    //TODO limit this cache
    private val superClassDangerousStateCache:
            MutableMap<KtClass, Pair<Map<KtProperty, List<KtNameReferenceExpression>>, Long>> =
            mutableMapOf()
}

val KtClass.superClass: KtClass?
    get() {
        val superTypes = superTypeListEntries
        if (superTypes.isEmpty()) {
            return null
        }
        superTypes.forEach {
            val realClass = it.typeAsUserType?.referenceExpression?.resolveMainReferenceToDescriptors()
                    ?.firstOrNull()?.containingDeclaration?.findPsi() as? KtClass
            if (realClass != null) {
                return realClass
            }
        }
        return null
    }

fun KtProperty.isOverriding(): Boolean {
    return modifierList?.isOverriding() ?: false
}

fun KtNamedFunction.isOverriding(): Boolean {
    return modifierList?.isOverriding() ?: false
}

fun KtModifierList.isOverriding(): Boolean {
    return hasModifier(KtTokens.OVERRIDE_KEYWORD)
}

fun KtClass.isAbstractOrOpen(): Boolean {
    return isAbstract() || isOverridable()
}

fun KtNamedFunction.isAbstractOrOpen(): Boolean {
    return isAbstract() || isOverridable()
}

fun KtProperty.isAbstractOrOpen(): Boolean {
    return isAbstract() || isOverridable()
}

fun KtClass.getAllFunctions(): List<KtNamedFunction> = collectDescendantsOfType()

inline fun <reified T : Any> PsiElement.findParentOfType(): T? {
    return findParentAndBeforeFromType<T>()?.first
}


inline fun <reified T : Any> PsiElement.findParentAndBeforeFromType(): Pair<T, PsiElement>? {
    var currentElement: PsiElement? = this
    var previousType = this
    while (currentElement != null) {
        if (currentElement is T) {
            return Pair(currentElement, previousType)
        }
        previousType = currentElement
        currentElement = currentElement.parent
    }
    return null
}