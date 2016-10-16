package edu.mit.csail.cap.query
package experiments

import analysis._

object Queries {
  // Name to (extension?, ID)
  val ids: Map[String, String] = Map(    
    /**
     * Swing examples
     */
    
    // TextAreaDemo examples
    "JTextArea.insert" -> "=-2652597752760251824",
    "Runnable.run" -> "+-1345752073464996958",
    "Runnable.run--JTextArea.insert" -> "[+-1345752073464996958,=-2652597752760251824]",
    "actionPerformed" -> "+2146939348418227012",
    "moveCaretPosition" -> "=2735924226236465025",
    
    // nothing useful?
    "JMenuBar.processKeyBinding" -> "=-1065916030548496322",
    "JMenuBar.processKeyBinding--actionPerformed" -> "[=-1065916030548496322,+2146939348418227012]",
    "JComponent.processKeyBinding" -> "=-8108090774512258077",
    "JComponent.processKeyBinding--actionPerformed" -> "[=-8108090774512258077,+2146939348418227012]",
    
    // tooltip queries
    "JTable.getToolTipText" -> "r-5324623765875975188",
    "JToolTip.setTipText" -> "=-7746185865034245791",
    "ToolTipManager.showTipWindow" -> "=-2086807874804363547", 
     
    "UndoManager.undo" -> "=6330595624804920406", // meh
    "UndoableEdit.undo" -> "+4230122993597110976", // meh
    
    "JOptionPane.showMessageDialog" -> "=-4417462953840568171", // good, but useful?
    
    "InputVerifier.verify" -> "+-5601531675391196634",
    
    "AbstractButton.doClick" -> "=-9137763640650022529",
    "ListCellRenderer.getListCellRendererComponent" -> "+8074512621072688699",
    "ListModel.getElementAt" -> "+-7532640154121881844",
    "TableModel.getValueAt" -> "+7962743492286904736",
    

    "TransferHandler.importData" -> "+2692499929692374911", // yes
    "TransferHandler.exportToClipboard" -> "+-2234806290602669995", // no
    "Transferable.getTransferData" -> "+-2022526662690727384", // no
    
    "JTree.collapsePath" -> "=-1317612629421754034", // irrelevant
    "JTree.setSelectionPath" -> "=-5853814618651490415", // irrelevant
   
    "DefaultRowSorter.toggleSortOrder" -> "=4262993120058997405",
    
    /***
     * Eclipse example queries.
     */
    "computeCompletionProposals" -> "+7956758726426447917", 
    "toggleExpansionState" -> "=-8415365693071545440",
    "ISelectionChangedListener.selectionChanged" -> "+8810940151034673499",
    "TextViewer.setSelectedRange" -> "=4012938300624070832",
    "ISelectionChangedListener.selectionChanged - TextViewer.setSelectedRange" -> "[+8810940151034673499,=4012938300624070832]",
     // "ContentOutlinePage.selectionChanged" -> "+-4558985865022786948",
     // "ContentOutlinePage.selectionChanged invoke" -> "=-4558985865022786948",
    "IContentOutlinePage.addSelectionChangedListener" -> "{-4405292938397926201,-5440356967970778511}",  
    "IContentOutlinePage.selectionChanged" -> "{8810940151034673499,-5440356967970778511}",
    "VerifyKeyListener.verifyKey - Document.replace" -> "[+-8531063598153218297,=4272071660868227195]"
  
  )

  def get(key: String, meta: Metadata) = ids.get(key) match {
    case Some(s) =>
      CallQuery.fromString(meta, s)
    case None =>
      throw new RuntimeException(s"cannot find method $key in metadata")
  }
}
