// swing_components_IconDemoApp
class USwingWorker extends SwingWorker {
  UWindow f1;
  @Override Object doInBackground() {
    AbstractAction[] aa = new AbstractAction[??];
    publish(aa);
    UWindow uw = this.f1;
    String[] s = uw.f3;
    String s0 = s[??];
    ImageIcon ii = new ImageIcon(??);
    String s1 = s[??];
    AbstractAction aa0 = new AbstractAction();
    aa0.putValue(??, s1);
    aa0.putValue(??, ii);
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
    String[] s = new String[??];
    s[??] = ??;
    uw.f3 = s;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  String[] f3;
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
    UWindow uw = this.f1;
    String[] s = uw.f3;
    String s0 = s[??];
    ImageIcon ii = new ImageIcon(??);
    String s1 = s[??];
    AbstractAction aa = new AbstractAction();
    aa.putValue(??, s1);
    aa.putValue(??, ii);
    AbstractAction[] aa0 = new AbstractAction[??];
    publish(aa0);
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
    String[] s = new String[??];
    s[??] = ??;
    uw.f3 = s;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  String[] f3;
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
    AbstractAction[] aa = new AbstractAction[??];
    publish(aa);
    UWindow uw = this.f1;
    String[] s = uw.f3;
    String s0 = s[??];
    ImageIcon ii = new ImageIcon(??);
    String s1 = s[??];
    AbstractAction aa0 = new AbstractAction();
    aa0.putValue(??, s1);
    aa0.putValue(??, ii);
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
    String[] s = new String[??];
    s[??] = ??;
    uw.f3 = s;
    USwingWorker usw = new USwingWorker();
    usw.f1 = uw;
    uw.add(jtb, ??);
    uw.setVisible(??);
  }
}
class UWindow extends Window {
  JToolBar f2;
  String[] f3;
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
    TableColumnModel tcm = jt.getColumnModel();
    TableColumn tc = tcm.getColumn(??);
    DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
    dtcr.setToolTipText(??);
    tc.setCellRenderer(dtcr);
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
  @Override String getToolTipText(MouseEvent a0) {
    return ??;
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
// swing_components_TreeIconDemo2
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(??);
    DefaultMutableTreeNode dmtn0 = new DefaultMutableTreeNode(??);
    dmtn.add(dmtn0);
    DefaultMutableTreeNode dmtn1 = new DefaultMutableTreeNode(??);
    dmtn0.add(dmtn1);
    JTree jt = new JTree(dmtn);
    ToolTipManager ttm = ToolTipManager.sharedInstance();
    ttm.registerComponent(jt);
    UDefaultTreeCellRenderer udtcr = new UDefaultTreeCellRenderer();
    jt.setCellRenderer(udtcr);
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
class UDefaultTreeCellRenderer extends DefaultTreeCellRenderer {
  @Override Component getTreeCellRendererComponent(JTree a0, Object a1, boolean a2, boolean a3, boolean a4, int a5, boolean a6) {
    Component c = ((DefaultTreeCellRenderer) c).getTreeCellRendererComponent(a0, a1, ??, ??, ??, ??, ??);
    ((JComponent) c).setToolTipText(??);
    // --
    Component c = ((DefaultTreeCellRenderer) c).getTreeCellRendererComponent(a0, a1, ??, ??, ??, ??, ??);
    ((JComponent) c).setToolTipText(??);
    // --
    Component c = ((DefaultTreeCellRenderer) c).getTreeCellRendererComponent(a0, a1, ??, ??, ??, ??, ??);
    ((JComponent) c).setToolTipText(??);
    return c;
  }
}
====
// swing_components_TreeIconDemo2
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(??);
    DefaultMutableTreeNode dmtn0 = new DefaultMutableTreeNode(??);
    dmtn.add(dmtn0);
    DefaultMutableTreeNode dmtn1 = new DefaultMutableTreeNode(??);
    dmtn0.add(dmtn1);
    JTree jt = new JTree(dmtn);
    ToolTipManager ttm = ToolTipManager.sharedInstance();
    ttm.registerComponent(jt);
    UDefaultTreeCellRenderer udtcr = new UDefaultTreeCellRenderer();
    jt.setCellRenderer(udtcr);
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
class UDefaultTreeCellRenderer extends DefaultTreeCellRenderer {
  @Override Component getTreeCellRendererComponent(JTree a0, Object a1, boolean a2, boolean a3, boolean a4, int a5, boolean a6) {
    Component c = ((DefaultTreeCellRenderer) c).getTreeCellRendererComponent(a0, a1, ??, ??, ??, ??, ??);
    ((JComponent) c).setToolTipText(??);
    // --
    Component c = ((DefaultTreeCellRenderer) c).getTreeCellRendererComponent(a0, a1, ??, ??, ??, ??, ??);
    ((JComponent) c).setToolTipText(??);
    return c;
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
    AbstractAction aa = new AbstractAction();
    aa.putValue(??, ??);
    aa.putValue(??, ??);
    JToolBar jtb = new JToolBar();
    uc.add(jtb, ??);
    JButton jb = new JButton(aa);
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