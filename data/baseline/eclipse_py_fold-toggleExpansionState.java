class UAbstractDecoratedTextEditor extends AbstractDecoratedTextEditor {
  @Override void createPartControl(Composite a0) {
    super.createPartControl(a0);
    ISourceViewer isv = getSourceViewer();
    ((ProjectionViewer) isv).doOperation(19);
  }
  @Override ISourceViewer createSourceViewer(Composite a0, IVerticalRuler a1, int a2) {
    return ??;
  }
}