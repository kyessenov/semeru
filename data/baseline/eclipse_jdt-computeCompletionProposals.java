class USourceViewerConfiguration extends SourceViewerConfiguration {
  @Override IContentAssistant getContentAssistant(ISourceViewer a0) {
    ContentAssistant ca = new ContentAssistant();
    UIContentAssistProcessor uicap = new UIContentAssistProcessor();
    ca.setContentAssistProcessor(uicap, ??);
    return ca;
  }
}
class UIContentAssistProcessor implements IContentAssistProcessor {
  @Override ICompletionProposal[] computeCompletionProposals(ITextViewer a0, int a1) {
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override void doSetInput(IEditorInput a0) {
    USourceViewerConfiguration usvc = new USourceViewerConfiguration();
    setSourceViewerConfiguration(usvc);
  }
}
