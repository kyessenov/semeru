package edu.mit.csail.cap.query
package experiments

case class MatchMakerTypes(meta: Metadata) {
  /**
   * Definitions of important types.
   */
  def getType(name: String) = meta.clazz(name) match {
    case Some(t) => t
    case None =>
      warn("Missing type " + name)
      null
  }

  /**
   * Editor
   */
  val TextEditor = getType("org.eclipse.ui.editors.text.TextEditor")
  val AbstractTextEditor = getType("org.eclipse.ui.texteditor.AbstractTextEditor")
  val TexEditor = getType("net.sourceforge.texlipse.editor.TexEditor")
  val XMLEditor = getType("captest.xmleditor1.editors.XMLEditor")
  val BibEditor = getType("net.sourceforge.texlipse.bibeditor.BibEditor")
  val JavaEditor = getType("org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor")
  val PropEditor = getType("org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileEditor")

  /**
   * Scanner
   */
  val RuleBasedScanner = getType("org.eclipse.jface.text.rules.RuleBasedScanner")
  val TexScanner = getType("net.sourceforge.texlipse.editor.scanner.TexScanner")
  val JavaScanner = getType("org.eclipse.jdt.internal.ui.text.java.JavaCodeScanner")
  val XMLScanner = getType("captest.xmleditor1.editors.XMLScanner")
  val BibScanner = getType("net.sourceforge.texlipse.bibeditor.BibEntryScanner")
  val PropScanner = getType("org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertyValueScanner")

  /**
   * Completion proposals
   */
  val ICompletionProposal = getType("org.eclipse.jface.text.contentassist.ICompletionProposal")
  val TemplateProposal = getType("org.eclipse.jface.text.templates.TemplateProposal")
  val TexCompletionProposal = getType("net.sourceforge.texlipse.editor.TexCompletionProposal")

  /**
   * Actions
   */
  val Action = getType("org.eclipse.jface.action.Action")

  /**
   * Windows
   */
  val IWorkbenchWindow = getType("org.eclipse.ui.IWorkbenchWindow")
  val WorkbenchWindow = getType("org.eclipse.ui.internal.WorkbenchWindow")
}
