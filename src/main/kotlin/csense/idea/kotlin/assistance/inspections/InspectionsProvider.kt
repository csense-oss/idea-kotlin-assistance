package csense.idea.kotlin.assistance.inspections

import com.intellij.codeInspection.*

class InspectionsProvider : InspectionToolProvider {
    override fun getInspectionClasses(): Array<out Class<out LocalInspectionTool>> {
        return arrayOf(
                InitializationOrder::class.java,
                NamedArgsPositionMismatch::class.java,
                InheritanceInitializationOrder::class.java,
                PotentialDangerousReturn::class.java,
//                SimpleInfiniteRecursionInspection::class.java,
                FunctionAndValueInvocationNamingInspection::class.java,
                WhileParameterNotUpdatedInspection::class.java,
                UsageAfterOverwriting::class.java)
    }
    
}
