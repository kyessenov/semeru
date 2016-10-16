// swing_components_IconDemoApp
class USwingWorker extends SwingWorker {
  UWindow f1;
  @Override Object doInBackground() {
    Object[] o = new Object[??];
    publish(o);
  }
  @Override void process(List a0) {
    Iterator i = a0.iterator();
    Object o = i.next();
    JButton jb = new JButton(o);
    UWindow uw = this.f1;
    JToolBar jtb = uw.f2;
    jtb.add(jb, ??);
  }
}
class URunnable implements Runnable {
  @Override void run() {
    UWindow uw = new UWindow();
    JToolBar jtb = new JToolBar();
    uw.f2 = jtb;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_IconDemoApp
class USwingWorker extends SwingWorker {
  UWindow f1;
  @Override Object doInBackground() {
    Object[] o = new Object[??];
    publish(o);
  }
  @Override void process(List a0) {
    Iterator i = a0.iterator();
    i.next();
    Object o = i.next();
    JButton jb = new JButton(o);
    UWindow uw = this.f1;
    JToolBar jtb = uw.f2;
    jtb.add(jb, ??);
  }
}
class URunnable implements Runnable {
  @Override void run() {
    UWindow uw = new UWindow();
    JToolBar jtb = new JToolBar();
    uw.f2 = jtb;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_IconDemoApp, swing_components_IconDemoApp
class USwingWorker extends SwingWorker {
  UWindow f1;
  @Override Object doInBackground() {
    Object[] o = new Object[??];
    publish(o);
  }
  @Override void process(List a0) {
    Iterator i = a0.iterator();
    i.next();
    // --
    Iterator i = a0.iterator();
    Object o = i.next();
    JButton jb = new JButton(o);
    UWindow uw = this.f1;
    JToolBar jtb = uw.f2;
    jtb.add(jb, ??);
  }
}
class URunnable implements Runnable {
  @Override void run() {
    UWindow uw = new UWindow();
    JToolBar jtb = new JToolBar();
    uw.f2 = jtb;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_LabelDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    JLabel jl = new JLabel(??, ??, ??);
    JLabel jl0 = new JLabel(??);
    jl.setToolTipText(??);
    jl0.setToolTipText(??);
    uc.add(jl0);
    jf.add(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_TableRenderDemo, swing_components_TableRenderDemo, swing_components_TableRenderDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    Object o = new Object();
    JTable jt = new JTable(o);
    JScrollPane jsp = new JScrollPane(jt);
    uc.add(jsp);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_TableToolTipsDemo, swing_components_TableToolTipsDemo, swing_components_TableToolTipsDemo, swing_components_TableToolTipsDemo
class UJTable extends JTable {
  @Override JTableHeader createDefaultTableHeader() {
    TableColumnModel tcm = this.columnModel;
  }
}
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    UJTable ujt = new UJTable();
    JScrollPane jsp = new JScrollPane(ujt);
    uc.add(jsp);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_TreeIconDemo2, swing_components_TreeIconDemo2
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    JTree jt = new JTree(??);
    ToolTipManager ttm = ToolTipManager.sharedInstance();
    ttm.registerComponent(jt);
    JScrollPane jsp = new JScrollPane(jt);
    JSplitPane jsp0 = new JSplitPane(??);
    jsp0.setTopComponent(jsp);
    uc.add(jsp0);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_dnd_BasicDnD
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    JPanel jp = new JPanel();
    JPanel jp0 = new JPanel();
    JColorChooser jcc = new JColorChooser();
    JPanel jp1 = new JPanel(??);
    jp1.add(jcc, ??);
    jp.add(jp1);
    JSplitPane jsp = new JSplitPane(??, jp, jp0);
    uc.add(jsp, ??);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_AccessibleScrollDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    ImageIcon ii = new ImageIcon(??, ??);
    JToggleButton jtb = new JToggleButton(??, ??);
    jtb.setToolTipText(??);
    JComponent jc = new JComponent();
    String s = ii.getDescription();
    jc.setToolTipText(s);
    JScrollPane jsp = new JScrollPane(jc);
    uc.add(jsp);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_AccessibleScrollDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    JPanel jp = new JPanel();
    JToggleButton jtb = new JToggleButton(??, ??);
    jtb.setToolTipText(??);
    jp.add(jtb);
    JScrollPane jsp = new JScrollPane(??);
    jsp.setCorner(??, jp);
    uc.add(jsp);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}
====
// swing_components_misc_ActionDemo, swing_components_misc_ActionDemo, swing_components_misc_ActionDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    Object o = new Object();
    JToolBar jtb = new JToolBar();
    uc.add(jtb, ??);
    JButton jb = new JButton(o);
    jtb.add(jb);
    jf.setContentPane(uc);
    jf.pack();
  }
}
class UContainer extends Container {
  static void main(String[] a0) {
    URunnable ur = new URunnable();
    SwingUtilities.invokeLater(ur);
  }
}