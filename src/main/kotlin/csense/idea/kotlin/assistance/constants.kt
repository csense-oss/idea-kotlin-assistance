package csense.idea.kotlin.assistance

import csense.kotlin.logger.*
import org.jetbrains.kotlin.name.*

object Constants {
    const val InspectionGroupName = "Csense - kotlin assistant"
    val lambdaParameterNameAnnotationFqName = FqName("kotlin.ParameterName")
    
    init {
//        L.usePrintAsLoggers()
        L.isLoggingAllowed(false)
    }
}
