class UDocumentListener implements DocumentListener {
  JTextArea f1;
  @Override void insertUpdate(DocumentEvent a0) {
    URunnable ur = new URunnable();
    ur.f2 = this;
    ur.f3 = ??;
    SwingUtilities.invokeLater(ur);
  }
  static void main(String[] a0) {
    URunnable0 ur = new URunnable0();
    SwingUtilities.invokeLater(ur);
  }
}
class URunnable implements Runnable {
  UDocumentListener f2;
  String f3;
  @Override void run() {
    UDocumentListener udl = this.f2;
    JTextArea jta = udl.f1;
    String s = this.f3;
    jta.insert(s, ??);
  }
}
class URunnable0 implements Runnable {
  @Override void run() {
    UDocumentListener udl = new UDocumentListener();
    JTextArea jta = new JTextArea();
    udl.f1 = jta;
    Document d = jta.getDocument();
    d.addDocumentListener(udl);
  }
}