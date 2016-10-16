class USourceViewerConfiguration extends SourceViewerConfiguration {
  ContentAssistant f1;
  @Override IContentAssistant getContentAssistant(ISourceViewer a0) {
    UIContentAssistProcessor uicap = new UIContentAssistProcessor();
    IContentAssistProcessor icap = new IContentAssistProcessor();
    ContentAssistant ca = this.f1;
    ca.setContentAssistProcessor(icap, ??);
    ca.setContentAssistProcessor(uicap, ??);
    return ca;
  }
}
class UIContentAssistProcessor implements IContentAssistProcessor {
  @Override ICompletionProposal[] computeCompletionProposals(ITextViewer a0, int a1) {
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  UAbstractDecoratedTextEditor() {
    USourceViewerConfiguration usvc = new USourceViewerConfiguration();
    ContentAssistant ca = new ContentAssistant();
    usvc.f1 = ca;
    setSourceViewerConfiguration(usvc);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
}
