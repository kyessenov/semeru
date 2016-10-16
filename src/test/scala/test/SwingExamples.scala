package test
import edu.mit.csail.cap.query._

/** Examples relying on data collected from real applications */
class SwingExamples extends SynthesisTest {
  override def default = super.default.copy(
    PrintStrings = false,
    PrintPrimitives = false,
    Matches = 100)

  def metadata = "meta_swing"
  
  test("passwordstore -- exportToClipboard") {
    run("passwordstore", "TransferHandler.exportToClipboard")
  }
  
  test("swing-importData") {
    runAll("swing_components", "TransferHandler.importData")
  }
  
  test("swing-toggleSortOrder") {
    runAll("swing_components", "DefaultRowSorter.toggleSortOrder")
  }
  
  test("swing-verify") {
    runAll("swing_components", "InputVerifier.verify")
  }
  
  test("swing-getToolTipText") {
    runAll("swing_components", "JTable.getToolTipText")
  }
  
  test("swing-setTipText") {
    runAll("swing_components", "JToolTip.setTipText")
  }
  
  test("swing-showTipWindow") {
    runAll("swing_components", "ToolTipManager.showTipWindow")
  }

}
