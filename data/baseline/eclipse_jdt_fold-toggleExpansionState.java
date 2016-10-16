class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    UProjectionViewer upv = new UProjectionViewer();
    IAnnotationAccess iaa = getAnnotationAccess();
    ISharedTextColors istc = getSharedColors();
    ProjectionSupport ps = new ProjectionSupport(upv, iaa, istc);
    ps.install();
  }
}
class UProjectionViewer extends ProjectionViewer {
  @Override void setVisibleDocument(IDocument a0) {
    enableProjection();
  }
}