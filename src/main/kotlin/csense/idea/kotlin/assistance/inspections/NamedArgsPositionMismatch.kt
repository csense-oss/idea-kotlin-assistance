package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.base.bll.*
import csense.idea.base.bll.kotlin.*
import csense.idea.base.bll.psiWrapper.function.*
import csense.idea.base.bll.psiWrapper.function.operations.*
import csense.idea.kotlin.assistance.*
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.*
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.*
import org.jetbrains.kotlin.types.*

class NamedArgsPositionMismatch : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Mismatched naming for parameter names"
    }

    override fun getStaticDescription(): String {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
            This inspection tells whenever a used name (such as a variable)
                is passed to / or from a function where that name is also used but at a different location.
            This generally is an error, such as swapping arguments around or parameter names for that matter.
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String {
        return "more desc ? "
    }

    override fun getShortName(): String {
        return "NamedArgsPositionMismatch"
    }

    override fun getGroupDisplayName(): String {
        return Constants.InspectionGroupName
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitorVoid {
        return callExpressionVisitor {
            val call: KtCallExpression = it
            val hasNoArguments: Boolean = call.valueArguments.isEmpty()
            if (hasNoArguments) {
                return@callExpressionVisitor
            }


            val callingFunction: KtPsiFunction = call.resolveMainReferenceAsFunction() ?: return@callExpressionVisitor
            val usedNames: List<ArgumentName?> = call.findInvocationArgumentNamesNew()

            val originalParameterNames: List<String> = callingFunction.valueArgumentNames()
            val argumentNames: List<String?> = call.findArgumentNames()

            if (usedNames.size > originalParameterNames.size) {
                //invalid code, just skip. (invoking with more args than there is).
                return@callExpressionVisitor
            }
            val misMatches: List<MismatchedName> = computeMismatchingNames(
                usedNames = usedNames,
                originalParameterNames = originalParameterNames,
                argumentName = argumentNames
            )

            if (misMatches.isNotEmpty()) {
                reportProblem(call, misMatches, holder)
            }

            call.lambdaArguments.forEach { lambdaArg: KtLambdaArgument ->

                val argName: KtLambdaExpression = lambdaArg.getLambdaExpression() ?: return@forEach
                val usedLambdaArgumentNames: List<ArgumentName> =
                    argName.valueParameters.map { parameters: KtParameter ->
                        ArgumentName(resultingName = parameters.name ?: "", nameParts = listOf())
                    }

                val namedArgs: List<String?> = (callingFunction as KtPsiFunction.Kt).function.valueParameters[0].typeParameters.map { typeArgs ->
                    typeArgs.type()?.findLambdaParameterName()
                }
                val lambdaMisMatch: List<MismatchedName> = computeMismatchingNames(
                    usedNames = usedLambdaArgumentNames,
                    originalParameterNames = namedArgs,
                    argumentName = argumentNames
                )
                if (lambdaMisMatch.isNotEmpty()) {
                    reportLambdaProblem(
                        atElement = call,
                        mismatches = lambdaMisMatch,
                        holder = holder
                    )
                }

            }
        }
    }

    fun reportProblem(atElement: KtCallExpression, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        mismatches.forEach { it: MismatchedName ->
            val arg: KtValueArgument? = atElement.valueArguments.getOrNull(it.parameterIndex)
            val argName: String? = arg?.getArgumentName()?.text
            when {
                argName == null -> {
                    val text = "`${it.name}` should be at index ${it.shouldBeAtIndex}, but is at ${it.parameterIndex}"
                    holder.registerProblemSafe(arg ?: atElement, text)
                }

                argName != it.name -> {
                    val text = "`${it.name}` matches another argument (not same named argument)"
                    holder.registerProblemSafe(arg, text)
                }
            }

        }
    }

    fun reportLambdaProblem(atElement: PsiElement, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        val names: String = mismatches.distinctBy { it.name }.joinToString(",") { it: MismatchedName ->
            "\"${it.name}\" - should be at position ${it.shouldBeAtIndex}"
        }
        holder.registerProblemSafe(
            psiElement = atElement,
            descriptionTemplate = "You have mismatched arguments names \n($names)"
        )
    }


    fun computeMismatchingNames(
        usedNames: List<ArgumentName?>,
        originalParameterNames: List<String?>,
        argumentName: List<String?>
    ): List<MismatchedName> {
        //TODO default args.
        val originalNames: Set<String> = originalParameterNames.filterNotNull().toSet()
        val result: MutableList<MismatchedName> = mutableListOf()
        usedNames.forEachIndexed { index: Int, name: ArgumentName? ->
            if (name == null || !originalNames.contains(name.resultingName)) {
                return@forEachIndexed
            }
            //only look at those who are contained.
            val org: String? = originalParameterNames.getOrNull(index)
            if (org == null || !org.areEqualOrCamelCaseEqual(name)) {
                //ERROR !! mismatching name but is declared somewhere else.
                result.add(
                    MismatchedName(
                        name = name.resultingName,
                        parameterIndex = index,
                        shouldBeAtIndex = originalParameterNames.indexOf(name.resultingName)
                    )
                )
            } else {
                val argName: String? = argumentName.getOrNull(index)
                if (argName != null && argName != name.resultingName) {
                    //todo improve...
                    result.add(
                        MismatchedName(
                            name = name.resultingName,
                            parameterIndex = index,
                            shouldBeAtIndex = originalParameterNames.indexOf(name.resultingName)
                        )
                    )
                }
            }
        }
        return result
    }

    fun String.areEqualOrCamelCaseEqual(argumentName: ArgumentName): Boolean {
        val isResultingNameEquals = this == argumentName.resultingName
        if (isResultingNameEquals) {
            return true
        }
        val asString: String = argumentName.nameParts.joinToString("")
        return asString.endsWith(this, ignoreCase = true)
    }

}

data class MismatchedName(val name: String, val parameterIndex: Int, val shouldBeAtIndex: Int)

fun KotlinType.findLambdaParameterName(): String? {
    return annotations.findAnnotation(
        Constants.lambdaParameterNameAnnotationFqName
    )?.argumentValue("name")?.value
            as? String
}


/**
 * Find all argument names in order they are declared.
 * @receiver KtCallExpression
 * @return List<String?>
 */
fun KtCallExpression.findInvocationArgumentNamesNew(): List<ArgumentName?> {
    return valueArguments.map { it: KtValueArgument? ->
        it?.getArgumentExpression()?.resolvePotentialArgumentName(listOf())
    }
}

/**
 * Used to try and find a potential name for a given argument.
 * @receiver KtExpression
 * @return String?
 */
tailrec fun KtExpression.resolvePotentialArgumentName(nameParts: List<String>): ArgumentName? = when (this) {
    is KtNameReferenceExpression -> ArgumentName(getReferencedName(), nameParts + listOf(getReferencedName()))
    is KtDotQualifiedExpression -> {
        (selectorExpression ?: receiverExpression).resolvePotentialArgumentName(
            nameParts + listOfNotNull(receiverExpression.text)
        )
    }

    is KtCallExpression -> {
        calleeExpression?.resolvePotentialArgumentName(nameParts)
    }

    else -> null
}

data class ArgumentName(val resultingName: String, val nameParts: List<String>)