package csense.idea.kotlin.assistance.inspections

import csense.idea.kotlin.test.*
import org.junit.*

class NamedArgsPositionMismatchTest: KotlinLightCodeInsightFixtureTestCaseJunit4() {
    override fun getTestDataPath(): String  = "src/test/testData/NamedArgsPositionMismatchInspection"

    @Before
    fun setup() {
        myFixture.allowTreeAccessForAllFiles()
        myFixture.enableInspections(NamedArgsPositionMismatch())
    }

    @Test
    fun foreachIntIndexed(){
        myFixture.configureByFile("ForeachIntIndex.kt")
//        myFixture.testHighlighting("ForeachIntIndex.kt")
        myFixture.checkHighlighting(true, false, false, true)
    }

}