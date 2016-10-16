// eclipse_brackets
class UVerifyKeyListener implements VerifyKeyListener {
  TextViewer f1;
  @Override void verifyKey(VerifyEvent a0) {
    TextViewer tv = this.f1;
    IDocument id = tv.getDocument();
    DocumentCommand dc = new DocumentCommand();
    dc.text = ??;
    String s = dc.text;
    id.replace(??, ??, s);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    TextViewer tv = new TextViewer();
    UVerifyKeyListener uvkl = new UVerifyKeyListener();
    uvkl.f1 = tv;
    tv.appendVerifyKeyListener(uvkl);
  }
}
====
// eclipse_brackets
class UVerifyKeyListener implements VerifyKeyListener {
  UAbstractDecoratedTextEditor f1;
  @Override void verifyKey(VerifyEvent a0) {
    UAbstractDecoratedTextEditor uadte = this.f1;
    ISourceViewer isv = uadte.getSourceViewer();
    IDocument id = isv.getDocument();
    id.replace(??, ??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  UVerifyKeyListener f2;
  UAbstractDecoratedTextEditor() {
    UVerifyKeyListener uvkl = new UVerifyKeyListener();
    uvkl.f1 = this;
    this.f2 = uvkl;
  }
  @Override void createPartControl(Composite a0) {
    super.createPartControl(a0);
    ISourceViewer isv = getSourceViewer();
    UVerifyKeyListener uvkl = this.f2;
    ((TextViewer) isv).prependVerifyKeyListener(uvkl);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    getPreferenceStore();
    getOverviewRuler();
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override void doSetInput(IEditorInput a0) {
    setPreferenceStore(??);
  }
}
class UAction extends Action {
  @Override void run() {
    ??.openEditor(??, ??, ??);
  }
}
====
// eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_brackets, eclipse_py
class UVerifyKeyListener implements VerifyKeyListener {
  TextViewer f1;
  @Override void verifyKey(VerifyEvent a0) {
    TextViewer tv = this.f1;
    IDocument id = tv.getDocument();
    id.replace(??, ??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    TextViewer tv = new TextViewer();
    UVerifyKeyListener uvkl = new UVerifyKeyListener();
    uvkl.f1 = tv;
    tv.appendVerifyKeyListener(uvkl);
  }
}
====
// eclipse_brackets, eclipse_tex_brackets
class UCommonActionProvider extends CommonActionProvider {
  @Override void init(ICommonActionExtensionSite a0) {
    UAction ua = new UAction();
    ArrayList al = new ArrayList();
    ua.f3 = al;
    ua.f2 = ??;
  }
}
class UTextFileDocumentProvider extends TextFileDocumentProvider {
  @Override IDocument getDocument(Object a0) {
    return ??;
  }
}
class UVerifyKeyListener implements VerifyKeyListener {
  ISourceViewer f1;
  @Override void verifyKey(VerifyEvent a0) {
    ISourceViewer isv = this.f1;
    IDocument id = isv.getDocument();
    id.replace(??, ??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override void createPartControl(Composite a0) {
    super.createPartControl(a0);
    ISourceViewer isv = getSourceViewer();
    UVerifyKeyListener uvkl = new UVerifyKeyListener();
    uvkl.f1 = isv;
    ((TextViewer) isv).prependVerifyKeyListener(uvkl);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ProjectionViewer pv = new ProjectionViewer(a0, a1, ??, ??, ??);
    return pv;
  }
}
class UAction extends Action {
  IWorkbenchPage f2;
  List f3;
  @Override void run() {
    Object o = ((PlatformObject) o).getAdapter(??);
    List l = this.f3;
    l.add(o);
    Iterator i = l.iterator();
    Object o0 = i.next();
    IWorkbenchPage iwp = this.f2;
    IDE.openEditor(iwp, o0);
  }
}
====
// eclipse_jdt_brackets
class UVerifyKeyListener implements VerifyKeyListener {
  UAbstractDecoratedTextEditor f1;
  @Override void verifyKey(VerifyEvent a0) {
    UAbstractDecoratedTextEditor uadte = this.f1;
    ISourceViewer isv = uadte.getSourceViewer();
    IDocument id = isv.getDocument();
    id.replace(??, ??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  UVerifyKeyListener f2;
  UAbstractDecoratedTextEditor() {
    UVerifyKeyListener uvkl = new UVerifyKeyListener();
    uvkl.f1 = this;
    this.f2 = uvkl;
  }
  @Override void createPartControl(Composite a0) {
    super.createPartControl(a0);
    ISourceViewer isv = getSourceViewer();
    UVerifyKeyListener uvkl = this.f2;
    ((TextViewer) isv).prependVerifyKeyListener(uvkl);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    getPreferenceStore();
    getOverviewRuler();
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override void doSetInput(IEditorInput a0) {
    setPreferenceStore(??);
  }
}