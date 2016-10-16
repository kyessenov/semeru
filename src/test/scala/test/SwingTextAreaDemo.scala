package test
import edu.mit.csail.cap.query._

/** Examples relying on data collected from real applications */
class SwingTextAreaDemo extends SynthesisTest {
  override def default = super.default.copy(
    PrintStrings = false,
    PrintPrimitives = false)

  def metadata = "meta_swing"

  test("TextAreaDemo moveCaretPosition") {
    run("swing_components_TextAreaDemo", "moveCaretPosition")
  }
  
  test("TextAreaDemo insert") {
    run("swing_components_TextAreaDemo", "Runnable.run--JTextArea.insert")
  }

  test("TextAreaDemo actionPerformed") {
    run("swing_components_TextAreaDemo", "actionPerformed")
  }
  
  test("TextAreaDemo processKeyBinding -- actionPerformed") {
    run("swing_components_TextAreaDemo", "JComponent.processKeyBinding--actionPerformed")
  }

  test("TextFieldDemo processKeyBinding -- actionPerformed") {
    run("swing_components_TextFieldDemo", "JComponent.processKeyBinding--actionPerformed")
  }
}
