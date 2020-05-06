package csense.idea.kotlin.assistance.inspections

import com.intellij.codeHighlighting.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import csense.idea.base.bll.findOriginalMethodArgumentNames
import csense.idea.base.bll.kotlin.findInvocationArgumentNames
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
            val usedNames = call.findInvocationArgumentNamesNew()
            val originalParameterNames = callingFunction.findOriginalMethodArgumentNames()
            val argumentNames = call.findArgumentNames()
            if (usedNames.size > originalParameterNames.size) {
                //invalid code, just skip. (invoking with more args than there is).
                return@callExpressionVisitor
            }
            val misMatches = computeMismatchingNames(usedNames, originalParameterNames, argumentNames)
            if (misMatches.isNotEmpty()) {
                reportProblem(call, misMatches, holder)
            }
            
            call.lambdaArguments.forEach { lambdaArg ->
                
                val argName = lambdaArg.getLambdaExpression() ?: return@forEach
                val usedLambdaArgumentNames = argName.valueParameters.map { parms -> parms.name }
                
                val namedArgs = callingFunction.valueParameters[0].type.arguments.map { typeArgs ->
                    typeArgs.type.findLambdaParameterName()
                }
                val lambdaMisMatch = computeMismatchingNames(usedLambdaArgumentNames, namedArgs, argumentNames)
                if (lambdaMisMatch.isNotEmpty()) {
                    reportLambdaProblem(call, lambdaMisMatch, holder)
                }
                
            }
        }
    }
    
    fun reportProblem(atElement: KtCallExpression, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        mismatches.forEach {
            val arg = atElement.valueArguments.getOrNull(it.parameterIndex)
            val argName = arg?.getArgumentName()?.text
            when {
                argName == null -> {
                    val text = "`${it.name}` should be at index ${it.shouldBeAtIndex}, but is at ${it.parameterIndex}"
                    holder.registerProblem(arg ?: atElement, text)
                }
                argName != it.name -> {
                    val text = "`${it.name}` matches another argument (not same named argument)"
                    holder.registerProblem(arg, text)
                }
            }
            
        }
    }
    
    fun reportLambdaProblem(atElement: PsiElement, mismatches: List<MismatchedName>, holder: ProblemsHolder) {
        val names = mismatches.distinctBy { it.name }.joinToString(",") {
            "\"${it.name}\" - should be at position ${it.shouldBeAtIndex}"
        }
        holder.registerProblem(atElement,
                "You have mismatched arguments names \n($names)")
    }
    
    
    fun computeMismatchingNames(
            usedNames: List<String?>,
            originalParameterNames: List<String?>,
            argumentName: List<String?>
    ): List<MismatchedName> {
        val originalNames = originalParameterNames.filterNotNull().toSet()
        val result = mutableListOf<MismatchedName>()
        usedNames.forEachIndexed { index, name ->
            if (name == null || !originalNames.contains(name)) {
                return@forEachIndexed
            }
            //only look at those who are contained.
            val org = originalParameterNames.getOrNull(index)
            if (org == null || org != name) {
                //ERROR !! mismatching name but is declared somewhere else.
                result.add(MismatchedName(name, index, originalParameterNames.indexOf(name)))
            } else {
                val argName = argumentName.getOrNull(index)
                if (argName != null && argName != name) {
                    //todo improve...
                    result.add(MismatchedName(name, index, originalParameterNames.indexOf(name)))
                }
            }
        }
        return result
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
fun KtCallExpression.findInvocationArgumentNamesNew(): List<String?> {
    return valueArguments.map { it: KtValueArgument? ->
        it?.getArgumentExpression()?.resolvePotentialArgumentName()
    }
}

fun KtExpression.resolvePotentialArgumentName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
        val lhs = receiverExpression as? KtNameReferenceExpression
        val rhs = selectorExpression as? KtNameReferenceExpression
        rhs?.resolvePotentialArgumentName() ?: lhs?.resolvePotentialArgumentName()
    }//todo callexpression,akk class with a name then something in that.
    //akk
    /*
    data class Bottom(val top: Double)
    data class Top(val bottom: Bottom)
    fun use(top:Double) {}
    fun test() {
        use(Top(Bottom(42.0)).bottom.top)
    }
    */
    else -> null
}

fun KtCallExpression.findArgumentNames(): List<String?> {
    return valueArguments.map { it: KtValueArgument? ->
        it?.getArgumentName()?.text
    }
}