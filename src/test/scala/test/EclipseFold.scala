package test
import edu.mit.csail.cap.query._

class EclipseFold extends SynthesisTest {
  override def default = super.default.copy(
    PrintStrings = false,
    PrintPrimitives = true)

  def metadata = "meta_eclipse"

  test("JDT fold") {
    run("eclipse_jdt_fold", "toggleExpansionState", default.copy(CoverDepth = 8))
  }

  test("Py fold") {
    run("eclipse_py_fold", "toggleExpansionState", default.copy(CoverDepth = 6))
  }

  test("ANT fold") {
    run("eclipse_ant_fold", "toggleExpansionState")
  }

  test("TeX fold") {
    run("eclipse_tex_fold", "toggleExpansionState")
  }
}
