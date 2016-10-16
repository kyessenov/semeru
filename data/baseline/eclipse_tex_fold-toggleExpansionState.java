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
    IOverviewRuler ior = getOverviewRuler();
    ProjectionViewer pv = new ProjectionViewer(a0, a1, ior, true, 68354);
    return pv;
  }
}