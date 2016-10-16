package test
import edu.mit.csail.cap.query._

class EclipseExamples extends SynthesisTest {
  override def default = super.default.copy(
    PrintStrings = false,
    PrintPrimitives = false,
    CoverDepth = 7,
    Matches = 100)

  def metadata = "meta_eclipse"

  ignore("auto edit extraction") {
    val demos = data.get("demo_eclipse_brackets")
    info(s"demos: $demos")
    for (score <- demos.binaryDemoMatch(data.get("demo_eclipse")).sorted.take(10))
      info(score.toString)
  }
  
  test("auto edit synthesis") {
    runAll("eclipse", "VerifyKeyListener.verifyKey - Document.replace", default.copy(CoverDepth = 7))
  }
  
  test("content outline synthesis") {
    runAll("eclipse", "ISelectionChangedListener.selectionChanged - TextViewer.setSelectedRange", default.copy(CoverDepth = 6))
  }
}
