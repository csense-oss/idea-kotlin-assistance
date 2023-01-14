package csense.idea.kotlin.assistance.inspections

import csense.idea.kotlin.test.*
import org.junit.*

class MutableVariableUsageAfterOverwritingTest : KotlinLightCodeInsightFixtureTestCaseJunit4() {
    override fun getTestDataPath(): String  = "src/test/testData/MutableVariableUsageAfterOverwriting"

    @Before
    fun setup() {
        myFixture.allowTreeAccessForAllFiles()
        myFixture.enableInspections(MutableVariableUsageAfterOverwriting())
    }

    @Test
    fun simpleOverwriteUsage(){
        myFixture.testHighlighting("SimpleOverwriteUsage.kt")
    }
}