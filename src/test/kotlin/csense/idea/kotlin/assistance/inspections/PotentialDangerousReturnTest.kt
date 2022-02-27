package csense.idea.kotlin.assistance.inspections

import csense.idea.kotlin.test.*
import org.junit.*

class PotentialDangerousReturnTest: KotlinLightCodeInsightFixtureTestCaseJunit4() {
    override fun getTestDataPath(): String  = "src/test/testData/PotentialDangerousReturnInspection"

    @Before
    fun setup() {
        myFixture.allowTreeAccessForAllFiles()
        myFixture.enableInspections(PotentialDangerousReturn())
    }

    @Test
    fun first(){
        myFixture.testHighlighting("First.kt")
    }


}