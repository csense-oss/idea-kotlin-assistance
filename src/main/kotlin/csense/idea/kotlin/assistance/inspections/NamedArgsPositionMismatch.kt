package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.kotlin.assistance.*
import csense.idea.kotlin.assistance.suppression.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.*
import org.jetbrains.kotlin.types.*

class NamedArgsPositionMismatch : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Mismatched naming for parameter names"
    }

    override fun getStaticDescription(): String? {
        //the ctrl  + f1 box +  desc of the inspection.
        return """
            This inspection tells whenever a used name (such as a variable)
                is passed to / or from a function where that name is also used but at a different location.
            This generally is an error, such as swapping arguments around or parameter names for that matter.
        """.trimIndent()
    }

    override fun getDescriptionFileName(): String? {
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

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return arrayOf(
                KtExpressionSuppression("Suppress naming mismatch issue", groupDisplayName, shortName))
    }

    override fun getDefaultLevel(): HighlightDisplayLevel {
        return HighlightDisplayLevel.ERROR
    }

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean): KtVisitorVoid {
        return callExpressionVisitor {
            val call: KtCallExpression = it
            if (call.valueArguments.isEmpty()) {
                //no arguments to check
                return@callExpressionVisitor
            }

            val callingFunction = call.resolveToCall()?.resultingDescriptor ?: return@callExpressionVisitor
            val usedNames = call.findInvocationArgumentNames()
            val originalParameterNames = callingFunction.findOriginalMethodArgumentNames()
            if (usedNames.size > originalParameterNames.size) {
                //invalid code, just skip. (invoking with more args than there is).
                return@callExpressionVisitor
            }
            val misMatches = computeMismatchingNames(usedNames, originalParameterNames)
            if (misMatches.isNotEmpty()) {
                reportProblem(call, misMatches, holder)
            }

            call.lambdaArguments.forEach { lambdaArg ->

                val argName = lambdaArg.getLambdaExpression() ?: return@forEach
                val usedLambdaArgumentNames = argName.valueParameters.map { parms -> parms.name }

                val namedArgs = callingFunction.valueParameters[0].type.arguments.map { typeArgs ->
                    typeArgs.type.findLambdaParameterName()
                }
                val lambdaMisMatch = computeMismatchingNames(usedLambdaArgumentNames, namedArgs)
                if (lambdaMisMatch.isNotEmpty()) {
                    reportLambdaProblem(call, lambdaMisMatch, holder)
                }

            }
        }
    }

    fun reportProblem(atElement: PsiElement, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        val names = mismatches.distinctBy { it.name }.joinToString(",") {
            it.name
        }
        holder.registerProblem(atElement,
                "You have mismatched arguments names \n($names)")
    }

    fun reportLambdaProblem(atElement: PsiElement, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        val names = mismatches.distinctBy { it.name }.joinToString(",") {
            "\"${it.name}\" - should be at position ${it.shouldBeAtIndex}"
        }
        holder.registerProblem(atElement,
                "You have mismatched arguments names \n($names)")
    }


    fun computeMismatchingNames(usedNames: List<String?>, originalParameterNames: List<String?>): List<MismatchedName> {
        val originalNames = originalParameterNames.filterNotNull().toSet()
        val result = mutableListOf<MismatchedName>()
        usedNames.forEachIndexed { index, name ->
            if (name == null || !originalNames.contains(name)) {
                return@forEachIndexed
            }
            //only look at those who are contained.
            val org = originalParameterNames[index]
            if (org == null || org != name) {
                //ERROR !! mismatching name but is declared somewhere else.
                result.add(MismatchedName(name, index, originalParameterNames.indexOf(name)))
            }
        }
        return result
    }


}

data class MismatchedName(val name: String, val parameterIndex: Int, val shouldBeAtIndex: Int)


fun KtCallExpression.findInvocationArgumentNames(): List<String?> {
    return valueArguments.map {
        val isNamed = it.getArgumentExpression() as? KtNameReferenceExpression
        isNamed?.getReferencedName()
    }
}

/**
 *
 * @return List<String> the order of the arguments as well as the name
 */
fun CallableDescriptor.findOriginalMethodArgumentNames(): List<String> {
    return valueParameters.map { param ->
        param.name.asString()
    }
}


fun KotlinType.findLambdaParameterName(): String? {
    return annotations.findAnnotation(
            Constants.lambdaParameterNameAnnotationFqName
    )?.argumentValue("name")?.value
            as? String
}