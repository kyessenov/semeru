class USourceViewerConfiguration extends SourceViewerConfiguration {
  @Override IContentAssistant getContentAssistant(ISourceViewer a0) {
    UIContentAssistProcessor uicap = new UIContentAssistProcessor();
    ContentAssistant ca = new ContentAssistant();
    ca.setContentAssistProcessor(uicap, ??);
    String[] s = UObject.f1;
    String s0 = s[??];
    ca.setContentAssistProcessor(uicap, s0);
    return ca;
  }
}
class UIContentAssistProcessor implements IContentAssistProcessor {
  @Override ICompletionProposal[] computeCompletionProposals(ITextViewer a0, int a1) {
  }
}

class UTextFileDocumentProvider extends TextFileDocumentProvider {
  @Override void connect(Object a0) {
    String[] s = new String[??];
    s[??] = ??;
    UObject.f1 = s;
  }
}
class UTextEditor extends TextEditor {
  UTextEditor() {
    UTextFileDocumentProvider utfdp = new UTextFileDocumentProvider();
    setDocumentProvider(utfdp);
    USourceViewerConfiguration usvc = new USourceViewerConfiguration();
    setSourceViewerConfiguration(usvc);
  }
  @Override void createActions() {
    super.createActions();
    Object o = new Object();
    ContentAssistAction caa = new ContentAssistAction(o, ??, this);
    setAction(??, caa);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
}
class UObject {
  static String[] f1;
}
