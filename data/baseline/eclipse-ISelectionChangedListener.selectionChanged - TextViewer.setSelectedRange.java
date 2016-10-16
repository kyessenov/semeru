// eclipse_ant_outline
class UISelectionChangedListener implements ISelectionChangedListener {
  @Override void selectionChanged(SelectionChangedEvent a0) {
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    Object[] o = ??.getListeners();
    Object o0 = o[??];
    UTextEditor ute = ((UObject) o0).f1;
    ISelection is = sce.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ISourceViewer isv = ute.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UTextEditor extends TextEditor {
  UTextEditor() {
    UObject uo = new UObject();
    uo.f1 = this;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    ??.add(??);
    return ??;
  }
}
class UObject {
  UTextEditor f1;
}
class UContentOutlinePage extends ContentOutlinePage {
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_ant_outline
class UISelectionChangedListener implements ISelectionChangedListener {
  UContentOutlinePage f1;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UContentOutlinePage ucop = this.f1;
    ISelection is = a0.getSelection();
    SelectionChangedEvent sce = new SelectionChangedEvent(ucop, is);
    ListenerList ll = ucop.f5;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    ((UISelectionChangedListener0) o0).selectionChanged(sce);
  }
}
class UISelectionChangedListener0 implements ISelectionChangedListener {
  UTextEditor f2;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UTextEditor ute = this.f2;
    ISelection is = a0.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ISourceViewer isv = ute.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UTextEditor extends TextEditor {
  ISelectionChangedListener f3;
  UTextEditor() {
    UISelectionChangedListener0 uiscl = new UISelectionChangedListener0();
    uiscl.f2 = this;
    this.f3 = uiscl;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UObject uo = UObject.f4;
    UContentOutlinePage ucop = new UContentOutlinePage();
    ListenerList ll = new ListenerList();
    ucop.f5 = ll;
    ISelectionChangedListener iscl = this.f3;
    ll.add(iscl);
    return ucop;
  }
  @Override void initializeEditor() {
    UObject uo = new UObject();
    UObject.f4 = uo;
  }
}
class UObject {
  static UObject f4;
}
class UContentOutlinePage extends ContentOutlinePage {
  ListenerList f5;
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f1 = this;
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_ant_outline
class UOpenAndLinkWithEditorHelper extends OpenAndLinkWithEditorHelper {
  @Override void open(ISelection a0, boolean a1) {
    IDE.openEditor(??, ??, ??);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  @Override void selectionChanged(SelectionChangedEvent a0) {
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    ListenerList ll = ??.f2;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UTextEditor ute = ((UObject) o0).f1;
    ISelection is = sce.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ISourceViewer isv = ute.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UTextEditor extends TextEditor {
  UTextEditor() {
    UObject uo = new UObject();
    uo.f1 = this;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    ??.f2 = ??;
    ListenerList ll = ??.f2;
    ll.add(??);
    return ??;
  }
}
class UObject {
  UTextEditor f1;
}
class UContentOutlinePage extends ContentOutlinePage {
  ListenerList f2;
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_ant_outline
class UOpenAndLinkWithEditorHelper extends OpenAndLinkWithEditorHelper {
  @Override void open(ISelection a0, boolean a1) {
    IDE.openEditor(??, ??, ??);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UContentOutlinePage f1;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UContentOutlinePage ucop = this.f1;
    ISelection is = a0.getSelection();
    SelectionChangedEvent sce = new SelectionChangedEvent(ucop, is);
    ListenerList ll = ucop.f5;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    ((UISelectionChangedListener0) o0).selectionChanged(sce);
  }
}
class UISelectionChangedListener0 implements ISelectionChangedListener {
  UTextEditor f2;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UTextEditor ute = this.f2;
    ISelection is = a0.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ISourceViewer isv = ute.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UTextEditor extends TextEditor {
  ISelectionChangedListener f3;
  UTextEditor() {
    UISelectionChangedListener0 uiscl = new UISelectionChangedListener0();
    uiscl.f2 = this;
    this.f3 = uiscl;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UObject uo = UObject.f4;
    UContentOutlinePage ucop = new UContentOutlinePage();
    ListenerList ll = new ListenerList();
    ucop.f5 = ll;
    ISelectionChangedListener iscl = this.f3;
    ll.add(iscl);
    return ucop;
  }
  @Override void initializeEditor() {
    UObject uo = new UObject();
    UObject.f4 = uo;
  }
}
class UObject {
  static UObject f4;
}
class UContentOutlinePage extends ContentOutlinePage {
  ListenerList f5;
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f1 = this;
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_ant_outline, eclipse_ant_outline
class UISelectionChangedListener implements ISelectionChangedListener {
  @Override void selectionChanged(SelectionChangedEvent a0) {
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    Object[] o = ??.getListeners();
    Object o0 = o[??];
    UTextEditor ute = ((UObject) o0).f1;
    ISelection is = sce.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ute.setHighlightRange(??, ??, ??);
  }
}
class UTextEditor extends TextEditor {
  UTextEditor() {
    UObject uo = new UObject();
    uo.f1 = this;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    ??.add(??);
    return ??;
  }
}
class UObject {
  UTextEditor f1;
}
class UContentOutlinePage extends ContentOutlinePage {
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_ant_outline, eclipse_ant_outline
class UOpenAndLinkWithEditorHelper extends OpenAndLinkWithEditorHelper {
  @Override void open(ISelection a0, boolean a1) {
    IDE.openEditor(??, ??, ??);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UContentOutlinePage f1;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UContentOutlinePage ucop = this.f1;
    ISelection is = a0.getSelection();
    SelectionChangedEvent sce = new SelectionChangedEvent(ucop, is);
    ListenerList ll = ucop.f5;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    ((UISelectionChangedListener0) o0).selectionChanged(sce);
  }
}
class UISelectionChangedListener0 implements ISelectionChangedListener {
  UTextEditor f2;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UTextEditor ute = this.f2;
    ISelection is = a0.getSelection();
    ((StructuredSelection) is).getFirstElement();
    ute.setHighlightRange(??, ??, ??);
  }
}
class UTextEditor extends TextEditor {
  ISelectionChangedListener f3;
  UTextEditor() {
    UISelectionChangedListener0 uiscl = new UISelectionChangedListener0();
    uiscl.f2 = this;
    this.f3 = uiscl;
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UObject uo = UObject.f4;
    UContentOutlinePage ucop = new UContentOutlinePage();
    ListenerList ll = new ListenerList();
    ucop.f5 = ll;
    ISelectionChangedListener iscl = this.f3;
    ll.add(iscl);
    return ucop;
  }
  @Override void initializeEditor() {
    UObject uo = new UObject();
    UObject.f4 = uo;
  }
}
class UObject {
  static UObject f4;
}
class UContentOutlinePage extends ContentOutlinePage {
  ListenerList f5;
  @Override void createControl(Composite a0) {
    super.createControl(a0);
    TreeViewer tv = getTreeViewer();
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f1 = this;
    tv.addPostSelectionChangedListener(uiscl);
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  @Override void mouseUp(MouseEvent a0) {
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    ISelection is = sce.getSelection();
    SelectionChangedEvent sce0 = new SelectionChangedEvent(??, is);
    ListenerList ll = ??.f2;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f5 = sce0;
    uisr.f6 = o0;
    SafeRunner.run(uisr);
  }
}
class UPage extends Page {
  UAbstractDecoratedTextEditor f1;
  ListenerList f2;
  ISelectionChangedListener f3;
  @Override void createControl(Composite a0) {
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f4 = this;
    this.f3 = uiscl;
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UPage f4;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UPage up = this.f4;
    UAbstractDecoratedTextEditor uadte = up.f1;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UISafeRunnable implements ISafeRunnable {
  SelectionChangedEvent f5;
  ISelectionChangedListener f6;
  @Override void run() {
    UPage up = ??.f4;
    ISelectionChangedListener iscl = up.f3;
    ListenerList ll = up.f2;
    ll.add(iscl);
    // --
    ISelectionChangedListener iscl = this.f6;
    SelectionChangedEvent sce = this.f5;
    iscl.selectionChanged(sce);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UPage up = new UPage();
    ListenerList ll = new ListenerList();
    up.f2 = ll;
    up.f1 = this;
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  @Override void mouseUp(MouseEvent a0) {
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    ISelection is = sce.getSelection();
    SelectionChangedEvent sce0 = new SelectionChangedEvent(??, is);
    ListenerList ll = ??.f2;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f5 = sce0;
    uisr.f6 = o0;
    SafeRunner.run(uisr);
  }
}
class UPage extends Page {
  UAbstractDecoratedTextEditor f1;
  ListenerList f2;
  ISelectionChangedListener f3;
  @Override void createControl(Composite a0) {
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f4 = this;
    this.f3 = uiscl;
  }
}
class URunnable implements Runnable {
  @Override void run() {
    ISelectionChangedListener iscl = ??.f3;
    ListenerList ll = ??.f2;
    ll.add(iscl);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UPage f4;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UPage up = this.f4;
    UAbstractDecoratedTextEditor uadte = up.f1;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UISafeRunnable implements ISafeRunnable {
  SelectionChangedEvent f5;
  ISelectionChangedListener f6;
  @Override void run() {
    ISelectionChangedListener iscl = this.f6;
    SelectionChangedEvent sce = this.f5;
    iscl.selectionChanged(sce);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UPage up = new UPage();
    ListenerList ll = new ListenerList();
    up.f2 = ll;
    up.f1 = this;
    return up;
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  UPage f1;
  @Override void mouseUp(MouseEvent a0) {
    UPage up = this.f1;
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    up.selectionChanged(sce);
  }
}
class UPage extends Page implements ISelectionChangedListener {
  UAbstractDecoratedTextEditor f2;
  ListenerList f3;
  @Override void createControl(Composite a0) {
    UObject uo = new UObject();
    uo.f5 = this;
    UMouseListener uml = new UMouseListener();
    uml.f1 = this;
  }
  @Override void selectionChanged(SelectionChangedEvent a0) {
    a0.getSelection();
    ListenerList ll = this.f3;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f4 = o0;
    SafeRunner.run(uisr);
  }
}
class UISafeRunnable implements ISafeRunnable {
  ISelectionChangedListener f4;
  @Override void run() {
    ListenerList ll = ??.f3;
    ll.add(??);
    // --
    ISelectionChangedListener iscl = this.f4;
    UPage up = ((UObject) iscl).f5;
    UAbstractDecoratedTextEditor uadte = up.f2;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UObject {
  UPage f5;
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    ??.f3 = ??;
    UPage up = new UPage();
    up.f2 = this;
    return up;
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  UPage f1;
  @Override void mouseUp(MouseEvent a0) {
    UPage up = this.f1;
    SelectionChangedEvent sce = new SelectionChangedEvent(??, ??);
    up.selectionChanged(sce);
  }
}
class UPage extends Page implements ISelectionChangedListener {
  UAbstractDecoratedTextEditor f2;
  ListenerList f3;
  @Override void createControl(Composite a0) {
    UObject uo = new UObject();
    uo.f5 = this;
    UMouseListener uml = new UMouseListener();
    uml.f1 = this;
  }
  @Override void selectionChanged(SelectionChangedEvent a0) {
    a0.getSelection();
    ListenerList ll = this.f3;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f4 = o0;
    SafeRunner.run(uisr);
  }
}
class UOpenAndLinkWithEditorHelper extends OpenAndLinkWithEditorHelper {
  @Override void open(ISelection a0, boolean a1) {
    IDE.openEditor(??, ??, ??);
  }
}
class UISafeRunnable implements ISafeRunnable {
  ISelectionChangedListener f4;
  @Override void run() {
    ListenerList ll = ??.f3;
    ll.add(??);
    // --
    ISelectionChangedListener iscl = this.f4;
    UPage up = ((UObject) iscl).f5;
    UAbstractDecoratedTextEditor uadte = up.f2;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UObject {
  UPage f5;
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    ??.f3 = ??;
    UPage up = new UPage();
    up.f2 = this;
    return up;
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  UPage f1;
  @Override void mouseUp(MouseEvent a0) {
    UPage up = this.f1;
    TreeViewer tv = up.f4;
    ISelection is = tv.getSelection();
    SelectionChangedEvent sce = new SelectionChangedEvent(tv, is);
    up.selectionChanged(sce);
  }
}
class UPage extends Page implements ISelectionChangedListener {
  UAbstractDecoratedTextEditor f2;
  ListenerList f3;
  TreeViewer f4;
  ISelectionChangedListener f5;
  @Override void createControl(Composite a0) {
    FilteredTree ft = new FilteredTree(a0, ??, ??, ??);
    TreeViewer tv = ft.getViewer();
    this.f4 = tv;
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f6 = this;
    this.f5 = uiscl;
    UMouseListener uml = new UMouseListener();
    uml.f1 = this;
  }
  @Override void selectionChanged(SelectionChangedEvent a0) {
    a0.getSelection();
    ListenerList ll = this.f3;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f7 = o0;
    SafeRunner.run(uisr);
  }
}
class URunnable implements Runnable {
  @Override void run() {
    ISelectionChangedListener iscl = ??.f5;
    ListenerList ll = ??.f3;
    ll.add(iscl);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UPage f6;
}
class UISafeRunnable implements ISafeRunnable {
  ISelectionChangedListener f7;
  @Override void run() {
    ISelectionChangedListener iscl = this.f7;
    UPage up = ((UISelectionChangedListener) iscl).f6;
    UAbstractDecoratedTextEditor uadte = up.f2;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UPage up = new UPage();
    ListenerList ll = new ListenerList();
    up.f3 = ll;
    up.f2 = this;
    return up;
  }
}
====
// eclipse_py_outline
class UMouseListener implements MouseListener {
  UPage f1;
  @Override void mouseUp(MouseEvent a0) {
    UPage up = this.f1;
    TreeViewer tv = up.f4;
    ISelection is = tv.getSelection();
    SelectionChangedEvent sce = new SelectionChangedEvent(tv, is);
    up.selectionChanged(sce);
  }
}
class UPage extends Page implements ISelectionChangedListener {
  UAbstractDecoratedTextEditor f2;
  ListenerList f3;
  TreeViewer f4;
  ISelectionChangedListener f5;
  @Override void createControl(Composite a0) {
    FilteredTree ft = new FilteredTree(a0, ??, ??, ??);
    TreeViewer tv = ft.getViewer();
    this.f4 = tv;
    UISelectionChangedListener uiscl = new UISelectionChangedListener();
    uiscl.f6 = this;
    this.f5 = uiscl;
    UMouseListener uml = new UMouseListener();
    uml.f1 = this;
  }
  @Override void selectionChanged(SelectionChangedEvent a0) {
    a0.getSelection();
    ListenerList ll = this.f3;
    Object[] o = ll.getListeners();
    Object o0 = o[??];
    UISafeRunnable uisr = new UISafeRunnable();
    uisr.f7 = o0;
    SafeRunner.run(uisr);
  }
}
class URunnable implements Runnable {
  @Override void run() {
    ISelectionChangedListener iscl = ??.f5;
    ListenerList ll = ??.f3;
    ll.add(iscl);
  }
}
class UOpenAndLinkWithEditorHelper extends OpenAndLinkWithEditorHelper {
  @Override void open(ISelection a0, boolean a1) {
    IDE.openEditor(??, ??, ??);
  }
}
class UISelectionChangedListener implements ISelectionChangedListener {
  UPage f6;
}
class UISafeRunnable implements ISafeRunnable {
  ISelectionChangedListener f7;
  @Override void run() {
    ISelectionChangedListener iscl = this.f7;
    UPage up = ((UISelectionChangedListener) iscl).f6;
    UAbstractDecoratedTextEditor uadte = up.f2;
    ISourceViewer isv = uadte.getSourceViewer();
    isv.setSelectedRange(??, ??);
  }
}
class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
  @Override Object getAdapter(Class a0) {
    UPage up = new UPage();
    ListenerList ll = new ListenerList();
    up.f3 = ll;
    up.f2 = this;
    return up;
  }
}
====
// eclipse_tex_outline
class UTextEditor extends TextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ProjectionViewer pv = new ProjectionViewer(a0, a1, ??, ??, ??);
    return pv;
  }
  @Override Object getAdapter(Class a0) {
    UContentOutlinePage ucop = new UContentOutlinePage();
    ucop.f1 = this;
    return ucop;
  }
}
class UContentOutlinePage extends ContentOutlinePage {
  UTextEditor f1;
  @Override void selectionChanged(SelectionChangedEvent a0) {
    UTextEditor ute = this.f1;
    ute.setHighlightRange(??, ??, ??);
  }
}