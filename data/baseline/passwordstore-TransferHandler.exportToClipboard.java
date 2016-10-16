class U1 extends UObject2 {
  @Override void actionPerformed(ActionEvent a0) {
    Clipboard c = UObject0.f3;
    JComponent jc = this.f10;
    TransferHandler th = jc.getTransferHandler();
    th.exportToClipboard(jc, c, ??);
  }
}
class U2 extends UObject2 {
}
class URunnable implements Runnable {
  UObject f1;
  @Override void run() {
    UObject uo = this.f1;
    String s = UIManager.getSystemLookAndFeelClassName();
    UIManager.setLookAndFeel(s);
    ResourceBundle rb = ResourceBundle.getBundle(??);
    rb.getString(??);
    UObject1 uo0 = new UObject1();
    Object o = new Object();
    PropertyChangeSupport pcs = new PropertyChangeSupport(o);
    List l = Collections.emptyList();
    List l0 = Collections.unmodifiableList(l);
    UPropertyChangeListener upcl = new UPropertyChangeListener();
    upcl.f2 = uo0;
    pcs.addPropertyChangeListener(upcl);
    JList jl = new JList();
    uo0.f9 = jl;
    ActionMap am = jl.getActionMap();
    Action a = UObject0.f7;
    am.put(a, a);
    Action a0 = UObject0.f8;
    am.put(a0, a0);
    Action a1 = UObject0.f4;
    am.put(a1, a1);
    PropertyChangeListener pcl = new PropertyChangeListener();
    pcs.addPropertyChangeListener(pcl);
    PropertyChangeListener pcl0 = new PropertyChangeListener();
    pcs.addPropertyChangeListener(pcl0);
    pcs.firePropertyChange(??, l0, null);
  }
}
class UTransferHandler extends TransferHandler {
  @Override void exportToClipboard(JComponent a0, Clipboard a1, int a2) {
  }
}
class UPropertyChangeListener implements PropertyChangeListener {
  UObject1 f2;
  @Override void propertyChange(PropertyChangeEvent a0) {
    UObject1 uo = this.f2;
    JList jl = uo.f9;
    Object o = UObject0.f6;
    jl.putClientProperty(o, ??);
    Object o0 = UObject0.f5;
    jl.putClientProperty(o0, ??);
  }
}
class UPropertyChangeListener0 implements PropertyChangeListener {
  @Override void propertyChange(PropertyChangeEvent a0) {
    Object o = a0.getNewValue();
    ??.f10 = o;
  }
}
class UObject {
  static void main(String[] a0) {
    UObject uo = new UObject();
    URunnable ur = new URunnable();
    ur.f1 = uo;
    SwingUtilities.invokeLater(ur);
  }
}
class UObject0 {
  static Clipboard f3;
  static Action f4;
  static Object f5;
  static Object f6;
  static Action f7;
  static Action f8;
}
class UObject1 {
  JList f9;
}
class UObject2 {
  JComponent f10;
}
class UObject3 {
  static ComponentUI createUI(JComponent a0) {
    UObject0.f5 = ??;
    UObject0.f6 = ??;
    UObject0.f3 = ??;
    U1 u = new U1();
    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    UPropertyChangeListener0 upcl = new UPropertyChangeListener0();
    kfm.addPropertyChangeListener(upcl);
    UObject0.f4 = u;
    U1 u0 = new U1();
    UObject0.f7 = u0;
    U2 u1 = new U2();
    UObject0.f8 = u1;
    KeyStroke.getKeyStroke(??);
  }
}