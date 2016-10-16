// swing_components_misc_InputVerificationDemo
class UInputVerifier extends InputVerifier {
  @Override boolean shouldYieldFocus(JComponent a0) {
    verify(a0);
  }
  @Override boolean verify(JComponent a0) {
  }
}
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    UInputVerifier uiv = new UInputVerifier();
    JLabel jl = new JLabel(??);
    JTextField jtf = new JTextField(??, ??);
    jtf.setInputVerifier(uiv);
    JTextField jtf0 = new JTextField(??, ??);
    jl.setLabelFor(jtf);
    JPanel jp = new JPanel(??);
    jp.add(jtf0);
    uc.add(jp, ??);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    UIManager.put(??, null);
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo
class UInputVerifier extends InputVerifier {
  @Override boolean shouldYieldFocus(JComponent a0) {
    verify(a0);
  }
  @Override boolean verify(JComponent a0) {
  }
}
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    UInputVerifier uiv = new UInputVerifier();
    JLabel jl = new JLabel(??);
    JTextField jtf = new JTextField(??, ??);
    JTextField jtf0 = new JTextField(??, ??);
    jtf0.setInputVerifier(uiv);
    jl.setLabelFor(jtf0);
    JPanel jp = new JPanel(??);
    jp.add(jtf);
    uc.add(jp, ??);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    UIManager.put(??, null);
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDemo, swing_components_misc_InputVerificationDialogDemo
class UInputVerifier extends InputVerifier {
  @Override boolean shouldYieldFocus(JComponent a0) {
    verify(a0);
  }
  @Override boolean verify(JComponent a0) {
  }
}
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    UInputVerifier uiv = new UInputVerifier();
    JLabel jl = new JLabel(??);
    JTextField jtf = new JTextField(??, ??);
    jtf.setInputVerifier(uiv);
    jl.setLabelFor(jtf);
    JPanel jp = new JPanel(??);
    jp.add(jtf);
    uc.add(jp, ??);
    jf.setContentPane(uc);
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    UIManager.put(??, null);
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_InputVerificationDialogDemo
class UInputVerifier extends InputVerifier {
  @Override boolean shouldYieldFocus(JComponent a0) {
    a0.setInputVerifier(this);
    // --
    verify(a0);
  }
  @Override boolean verify(JComponent a0) {
  }
}
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    UInputVerifier uiv = new UInputVerifier();
    JLabel jl = new JLabel(??);
    JTextField jtf = new JTextField(??, ??);
    jtf.setInputVerifier(uiv);
    jl.setLabelFor(jtf);
    JPanel jp = new JPanel(??);
    jp.add(jtf);
    uc.add(jp, ??);
    jf.setContentPane(uc);
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    UIManager.put(??, null);
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_InputVerificationDialogDemo, swing_components_misc_InputVerificationDialogDemo, swing_components_misc_InputVerificationDialogDemo
class UActionListener extends InputVerifier implements ActionListener {
  @Override void actionPerformed(ActionEvent a0) {
    Object o = a0.getSource();
    verify(o);
  }
  @Override boolean verify(JComponent a0) {
  }
}
class URunnable implements Runnable {
  @Override void run() {
    UActionListener ual = new UActionListener();
    JTextField jtf = new JTextField(??, ??);
    jtf.addActionListener(ual);
  }
}
class UObject {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}