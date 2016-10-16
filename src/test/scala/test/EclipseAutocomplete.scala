package test
import edu.mit.csail.cap.query._

class EclipseAutocomplete extends SynthesisTest {
  override def default = super.default.copy(
    PrintStrings = false,
    PrintPrimitives = false)

  def metadata = "meta_eclipse"

  test("JDT auto-completion") {
    run("eclipse_jdt", "computeCompletionProposals", default.copy(CoverDepth = 8))
  }

  test("WikiText auto-completion") {
    // data.config("eclipse_wiki").get.p = p.copy(CoverDepth = 12)
    run("eclipse_wiki", "computeCompletionProposals")
  }

  test("Py auto-completion") {
    run("eclipse_py", "computeCompletionProposals")
  }

  test("TeX auto-completion") {
    run("eclipse_tex", "computeCompletionProposals")
  }

  test("ANT auto-completion") {
    run("eclipse_ant", "computeCompletionProposals")
  }

}
