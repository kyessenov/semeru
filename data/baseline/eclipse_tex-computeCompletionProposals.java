class USourceViewerConfiguration extends SourceViewerConfiguration {
  @Override IContentAssistant getContentAssistant(ISourceViewer a0) {
    ContentAssistant ca = new ContentAssistant();
    UIContentAssistProcessor uicap = new UIContentAssistProcessor();
    ca.setContentAssistProcessor(uicap, ??);
    ca.setContentAssistProcessor(uicap, ??);
    ca.setContentAssistProcessor(uicap, ??);
    return ca;
  }
}
class UIContentAssistProcessor implements IContentAssistProcessor {
  @Override ICompletionProposal[] computeCompletionProposals(ITextViewer a0, int a1) {
  }
}
class UTextEditor extends TextEditor {
  @Override void createActions() {
    super.createActions();
    UObject uo = UObject.f2;
    ResourceBundle rb = uo.f1;
    TextOperationAction toa = new TextOperationAction(rb, ??, this, ??);
    setAction(??, toa);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ProjectionViewer pv = new ProjectionViewer(a0, a1, ??, ??, ??);
    return pv;
  }
  @Override void initializeEditor() {
    USourceViewerConfiguration usvc = new USourceViewerConfiguration();
    setSourceViewerConfiguration(usvc);
  }
}
class UObject {
  ResourceBundle f1;
  static UObject f2;
  UObject() {
    UObject.f2 = this;
    ResourceBundle rb = ResourceBundle.getBundle(??);
    this.f1 = rb;
  }
}