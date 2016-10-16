class URunnable implements Runnable {
  @Override void run() {
    JTextArea jta = new JTextArea();
    ActionMap am = jta.getActionMap();
    UAction ua = new UAction();
    am.put(??, ua);
  }
}
class UAction implements Action {
  @Override void actionPerformed(ActionEvent a0) {
  }
}
class UObject {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}