class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override void createPartControl(Composite a0) {
    super.createPartControl(a0);
    ISourceViewer isv = getSourceViewer();
    IAnnotationAccess iaa = getAnnotationAccess();
    ISharedTextColors istc = getSharedColors();
    ProjectionSupport ps = new ProjectionSupport(isv, iaa, istc);
    ps.install();
    ((ProjectionViewer) isv).doOperation(19);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    ISharedTextColors istc = getSharedColors();
    IOverviewRuler ior = createOverviewRuler(istc);
    this.fOverviewRuler = ior;
    getOverviewRuler();
    ISourceViewer isv = new ISourceViewer();
    return isv;
  }
}