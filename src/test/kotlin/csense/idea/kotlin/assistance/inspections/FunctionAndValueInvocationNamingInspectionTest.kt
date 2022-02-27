package csense.idea.kotlin.assistance.inspections

import csense.idea.kotlin.test.*
import org.junit.*

class FunctionAndValueInvocationNamingInspectionTest : KotlinLightCodeInsightFixtureTestCaseJunit4() {
    override fun getTestDataPath(): String  = "src/test/testData/FunctionAndValueInvocationNamingInspection"

    @Before
    fun setup() {
        myFixture.allowTreeAccessForAllFiles()
        myFixture.enableInspections(FunctionAndValueInvocationNamingInspection())
    }

    @Test
    fun objectAndFunction(){
        myFixture.testHighlighting("ObjectAndFunction.kt")
    }

    @Test
    fun variableAndFunctionName(){
        myFixture.testHighlighting("VariableAndFunctionName.kt")
    }

}