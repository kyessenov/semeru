// swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo, swing_components_TableSortDemo
class URunnable implements Runnable {
  @Override void run() {
    JFrame jf = new JFrame(??);
    UContainer uc = new UContainer();
    Object o = new Object();
    JTable jt = new JTable(o);
    jt.setAutoCreateRowSorter(??);
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