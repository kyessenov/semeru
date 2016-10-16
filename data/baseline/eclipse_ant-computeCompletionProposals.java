class USourceViewerConfiguration extends SourceViewerConfiguration {
  @Override IContentAssistant getContentAssistant(ISourceViewer a0) {
    ContentAssistant ca = new ContentAssistant();
    UTemplateCompletionProcessor utcp = new UTemplateCompletionProcessor();
    ca.setContentAssistProcessor(utcp, ??);
    ca.setContentAssistProcessor(utcp, ??);
    return ca;
  }
}
class UTextEditor extends TextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    UProjectionViewer upv = new UProjectionViewer();
    return upv;
  }
  @Override void initializeEditor() {
    USourceViewerConfiguration usvc = new USourceViewerConfiguration();
    setSourceViewerConfiguration(usvc);
  }
}
class UTemplateCompletionProcessor extends TemplateCompletionProcessor {
  @Override ICompletionProposal[] computeCompletionProposals(ITextViewer a0, int a1) {
  }
}
class UProjectionViewer extends ProjectionViewer {
  @Override void doOperation(int a0) {
    IContentAssistant ica = this.fContentAssistant;
    ica.showPossibleCompletions();
  }
}
