/* 
 * Copyright 2016 The Sem Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thesemproject.opensem.gui.utils;

import java.awt.BorderLayout;
import org.thesemproject.opensem.utils.interning.InternPool;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.classification.IndexManager;
import org.thesemproject.opensem.classification.MyAnalyzer;
import org.thesemproject.opensem.classification.NodeData;
import org.thesemproject.opensem.classification.Tokenizer;
import org.thesemproject.opensem.gui.JTableCellRender;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.ModelTreeNode;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.segmentation.SegmentationUtils;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.thesemproject.opensem.gui.modelEditor.ClassicationTreeNode;

/**
 *
 * Vari metodi gestiti dalla GUI messi a fattor compune
 */
public class GuiUtils {

    private static void resetFontSize(String object, double factor) {
        Font f = UIManager.getFont(object);
        if (f != null) {

            int fontSize = (int) Math.round(f.getSize() * getReshapeFactor(factor));
            UIManager.put(object, new Font(f.getName(), f.getStyle(), fontSize));
        }
    }

    /**
     * Ridimensiona i font in funzione della risoluzione
     */
    public static void adjustFontSize() {
        String keys[] = {"Button.font",
            "ToggleButton.font",
            "RadioButton.font",
            "CheckBox.font",
            "ColorChooser.font",
            "ComboBox.font",
            "Label.font",
            "List.font",
            "MenuBar.font",
            "MenuItem.font",
            "RadioButtonMenuItem.font",
            "CheckBoxMenuItem.font",
            "Menu.font",
            "PopupMenu.font",
            "OptionPane.font",
            "Panel.font",
            "ProgressBar.font",
            "ScrollPane.font",
            "Viewport.font",
            "TabbedPane.font",
            "Table.font",
            "TableHeader.font",
            "TextField.font",
            "PasswordField.font",
            "TextArea.font",
            "TextPane.font",
            "EditorPane.font",
            "TitledBorder.font",
            "ToolBar.font",
            "ToolTip.font",
            "Spinner.font",
            "OptionPane.messageFont",
            "OptionPane.buttonFont",
            "Tree.font"};

        for (String k : keys) {
            resetFontSize(k, 72);
        }

    }

    /**
     * Implementa la ricerca su un albero
     *
     * @param root root albero
     * @param s String cercata
     * @param onlyEquals true se si vuole il match esatto
     * @return lista dei percorsi trovati
     */
    public static List<TreePath> find(DefaultMutableTreeNode root, String s, boolean onlyEquals) {
        List<TreePath> ret = new ArrayList<>();
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (!onlyEquals) {
                if (node.toString().toLowerCase().contains(s.toLowerCase())) {
                    ret.add(new TreePath(node.getPath()));
                }
            } else if (node.toString().toLowerCase().equals(s.toLowerCase())) {
                ret.add(new TreePath(node.getPath()));
            }
        }
        return ret;
    }

    /**
     * Gestisce la pulizia di una tabella
     *
     * @param table tabella
     */
    public static void clearTable(JTable table) {
        DefaultTableModel dm = (DefaultTableModel) table.getModel();
        dm.getDataVector().removeAllElements();
        table.setAutoCreateRowSorter(false);
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        if (sorter != null) {
            sorter.setRowFilter(null);
            table.setRowSorter(sorter);
        }
        table.setAutoCreateRowSorter(true);

    }

    /**
     * Gestisce l'espansione di tutti i livelli di un albero
     *
     * @param tree albero
     */
    public static void expandAll(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), true);
    }

    /**
     * Gestisce la compressione di un albero
     *
     * @param tree albero
     */
    public static void collapseAll(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), false);
    }

    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    /**
     * Gestisce l'evidenziazione di un nodo
     *
     * @param tree albero
     * @param nodeName nome del nodo
     */
    public static void scrollToNode(JTree tree, String nodeName) {

        List<TreePath> paths = find((DefaultMutableTreeNode) tree.getModel().getRoot(), nodeName, true);
        scrollToPath(tree, paths);
    }

    /**
     * Gestice l'evidenziazione di un percorso
     *
     * @param tree albero
     * @param paths path
     */
    public static void scrollToPath(JTree tree, List<TreePath> paths) {
        if (paths.size() > 0) {
            GuiUtils.collapseAll(tree);
            TreePath[] pt = new TreePath[paths.size()];
            for (int i = 0; i < paths.size(); i++) {
                pt[i] = paths.get(i);
                tree.scrollPathToVisible(pt[i]);
            }
            tree.setSelectionPaths(pt);
        }
    }

    /**
     * Converte un path di classificazione da stringa a classification path
     *
     * @param path path stringa
     * @return path di classificazione
     */
    public static ClassificationPath getClassificationPath(String path) {
        String[] categories = path.split(">");
        ClassificationPath cp = new ClassificationPath("Bayes");
        for (int i = 0; i < categories.length; i++) {
            cp.addResult(org.thesemproject.opensem.utils.StringUtils.firstUpper(categories[i]), 1, i);
        }
        return cp;
    }

    /**
     * mostra un dialog di informazione
     *
     * @param message messaggio
     * @param title titolo del dialog
     */
    public static void showDialog(String message, String title) {
        int dialogButton = JOptionPane.INFORMATION_MESSAGE;
        JOptionPane.showMessageDialog(null, message, title, dialogButton);
    }

    /**
     * Visualizza un dialog con una text area
     *
     * @since 1.3.3
     *
     * @param title titolo del dialog
     * @param message messaggio da visualizzare
     * @param text testo da mettere nella text area
     * @return testo della textarea se confermato null altrimenti
     */
    public static String showTextAreaDialog(String title, String message, String text) {
        JTextArea ta = new JTextArea(5, 60);
        ta.setLineWrap(true);
        ta.setText(text);
        ta.setCaretPosition(0);
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());
        JLabel jl = new JLabel(message);
        jp.add(jl, BorderLayout.NORTH);
        jp.add(new JScrollPane(ta), BorderLayout.CENTER);
        switch (JOptionPane.showConfirmDialog(null, jp, title, JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null)) {
            case JOptionPane.OK_OPTION:
                return ta.getText();

        }
        return null;
    }

    /**
     * mostra un dialog di errore
     *
     * @param message messaggio
     * @param title titolo
     */
    public static void showErrorDialog(String message, String title) {
        int dialogButton = JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(null, message, title, dialogButton);

    }

    /**
     * mostra un dialog di conferma
     *
     * @param message messaggio
     * @param title titolo
     * @return true se premuto "YES"
     */
    public static boolean showConfirmDialog(String message, String title) {
        return JOptionPane.showConfirmDialog(null,
                message, title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Mostra un dialog di scelta con una lista di dropdown
     *
     * @since 1.3
     *
     * @param message messaggio
     * @param title titolo
     * @param choices lista delle scelte
     * @return selezione dell'utente o null
     */
    public static Object showChoiceDIalog(String message, String title, Object[] choices) {
        return showChoiceDIalog(message, title, choices, "");
    }

    /**
     * Mostra un dialog di scelta con una lista di dropdown
     *
     * @since 1.4.1
     *
     * @param message messaggio
     * @param title titolo
     * @param choices lista delle scelte
     * @param suggestion suggerimento
     * @return selezione dell'utente o null
     */
    public static Object showChoiceDIalog(String message, String title, Object[] choices, String suggestion) {
        if (choices == null) {
            return null;
        }
        if (choices.length == 0) {
            return null;
        }
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());
        JLabel jl = new JLabel(message);
        jp.add(jl, BorderLayout.NORTH);

        JPanel jp2 = new JPanel();
        jp2.setLayout(new BorderLayout());
        jp.add(jp2, BorderLayout.CENTER);

        JTextField jt = new JTextField();
        jp2.add(jt, BorderLayout.NORTH);
        final JList jc = new JList(choices);
        jc.setVisibleRowCount(10);
        jp2.add(new JScrollPane(jc), BorderLayout.CENTER);

        jt.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                int distance = Integer.MAX_VALUE;
                final String value = StringUtils.stripAccents(jt.getText().toLowerCase());
                if (value.length() > 0) {
                    for (int x = 0; x < choices.length; x++) {
                        String tvalue = StringUtils.stripAccents(choices[x].toString().toLowerCase());
                        if (tvalue.startsWith(value) || tvalue.endsWith(value) || tvalue.contains(value)) {
                            jc.setSelectedIndex(x);
                            jc.ensureIndexIsVisible(jc.getSelectedIndex());
                            return;
                        } else {
                            final int d2 = Tokenizer.distance(tvalue, value);
                            if (d2 < distance) {
                                distance = d2;
                                jc.setSelectedIndex(x);
                                jc.ensureIndexIsVisible(jc.getSelectedIndex());
                            }
                        }
                    }
                }

            }
        });
        jt.setText(suggestion);
        jt.requestFocus();
        jt.selectAll();
        switch (JOptionPane.showConfirmDialog(null, jp, title, JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null)) {
            case JOptionPane.OK_OPTION:
                return choices[jc.getSelectedIndex()];
        }
        return null;

    }

    /**
     * gestisce i filtri su una tabella
     *
     * @param table tabella
     * @param text testo cercato
     * @param idx indice del campo su cui cercare
     */
    public static void filterTable(JTable table, String text, int idx) {
        table.getColumnModel().getColumn(idx).setCellRenderer(new JTableCellRender(text));
        if (text != null && text.length() > 0) {
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            try {
                if (sorter != null) {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, idx));
                    table.setRowSorter(sorter);
                }
            } catch (Exception e) {
                if (sorter != null) {
                    sorter.setRowFilter(null);
                    table.setRowSorter(sorter);
                }
            }
        } else {
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            if (sorter != null) {
                sorter.setRowFilter(null);
                table.setRowSorter(sorter);
            }

        }

    }

    /**
     * gestisce filtri multicampo sulla tabella
     *
     * @param table tabella
     * @param text testo cercato
     * @param idxs elenco degli id dei campi dove cercare
     */
    public static void filterTable(JTable table, String text, int idxs[]) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        List<RowFilter<TableModel, Integer>> filters = new ArrayList<>(idxs.length);

        if (text != null && text.length() > 0) {
            for (int idx : idxs) {
                table.getColumnModel().getColumn(idx).setCellRenderer(new JTableCellRender(text));
                RowFilter<TableModel, Integer> filterC1 = RowFilter.regexFilter("(?i)" + text, idx);
                filters.add(filterC1);
            }
            RowFilter<TableModel, Integer> filter = RowFilter.orFilter(filters);
            sorter.setRowFilter(filter);
        } else {
            sorter.setRowFilter(null);
        }

        table.setRowSorter(sorter);
    }

    /**
     * gestice i backup di un file
     *
     * @param fileName nome del file che si vuole backuppare
     */
    public static void makeBackup(String fileName) {
        try {
            File original = new File(fileName);
            if (original.exists()) {
                String localPath = original.getParent() + "/backup";
                File lp = new File(localPath);
                if (!lp.exists()) {
                    lp.mkdirs();
                }
                String savingFileName = localPath + "/" + original.getName();
                String pattern = savingFileName;
                long oldestTime = System.currentTimeMillis();
                for (int i = 0; i < 20; i++) {
                    String fn = pattern + "." + i + ".bck";
                    File back = new File(fn);
                    if (!back.exists()) {
                        savingFileName = fn;
                        break;
                    } else if (back.lastModified() < oldestTime) {
                        oldestTime = back.lastModified();
                        savingFileName = back.getAbsolutePath();
                    }
                }
                FileUtils.copyFile(original, new File(savingFileName));
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * legge un xml
     *
     * @param file file
     * @return JDOM
     * @throws Exception Eccezione
     */
    public static Document readXml(String file) throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(file);
    }

    /**
     * Salva un JDOM su file
     *
     * @param doc JDOM
     * @param file percorso di salvataggio
     */
    public static void storeXml(Document doc, String file) {
        if (doc != null) {
            GuiUtils.makeBackup(file);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            try {
                FileOutputStream fos = new FileOutputStream(file);
                outputter.output(doc, fos);
                fos.close();
            } catch (IOException e) {
                LogGui.printException(e);
            }
        }
    }

    /**
     * Scrive un CSV in UTF-8
     *
     * @param fileName nome del file
     * @param lines righe
     * @return File salvato
     */
    public static File writeCSV(String fileName, List<String> lines) {
        File f = new File(fileName);
        try {
            if (f.exists()) {
                f.delete();
            }
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            for (String word : lines) {
                // String value = new String(word.getBytes(), "UTF-8");
                out.write(word + "\r\n");
            }
            out.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return f;
    }

    /**
     * Legge un file per righe
     *
     * @param fileName nome del file
     * @return lista delle righe
     */
    public static List<String> readFileLines(String fileName) {
        List<String> ret = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                ret.add(line);
            }
            br.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return ret;
    }

    /**
     * Legge un file per righe applicando un filtro che fa qualche cosa su ogni
     * riga
     *
     * @param fileName nome del file
     * @param filter filtro sulla riga
     */
    public static void readFileLines(String fileName, LineFilter filter) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                filter.applyTo(line);
            }
            br.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Gestisce una azione sul modello
     *
     * @param save true se si vuole salvare
     * @param semGui frame
     */
    public static void modelActionPerformed(boolean save, SemGui semGui) {
        GuiUtils.expandAll(semGui.getModelTree());
        int minSelect = semGui.getModelTree().getMinSelectionRow();
        generateSegmentXml(save, semGui);
        if (minSelect != -1) {
            GuiUtils.expandAll(semGui.getModelTree());
            semGui.getModelTree().setSelectionRow(minSelect);
            semGui.getModelTree().scrollRowToVisible(minSelect);
            semGui.getModelEditor().segmentsActionPerformed(semGui.getModelTree(), null, minSelect);
            ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                List<TreePath> path = new ArrayList<>();
                path.add(new TreePath(node.getPath()));
                GuiUtils.scrollToPath(semGui.getModelTree(), path);
            }
        }
    }

    /**
     * Genera la struttura xml dei nodi di classificazione
     *
     * @param save true se si vuole salvare
     * @param semGui frame
     */
    public static void generateSegmentXml(boolean save, SemGui semGui) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot();
        Document doc = semGui.getModelEditor().getXml(root);
        if (save) {
            GuiUtils.storeXml(doc, semGui.getSegmentsPath());
        }
        LogGui.info(String.valueOf(new Date()) + ": Reinizializzazione");
        if (semGui.getSE().init(doc, semGui.getPercorsoIndice().getText(), semGui.getME())) {
            semGui.getModelTree().setModel(semGui.getSE().getVisualStructure());
            DefaultTreeModel model = (DefaultTreeModel) (semGui.getModelTree().getModel());
            model.reload();
            if (!root.isLeaf()) {
                DefaultMutableTreeNode fc = (DefaultMutableTreeNode) root.getFirstChild();
                semGui.getModelTree().setSelectionPath(new TreePath(fc.getPath()));
            }
            semGui.getModelEditor().segmentsActionPerformed(semGui.getModelTree(), null, 1);
        }
    }

    /**
     * Ritorna il reshape factor in base alla risoluzione
     *
     * @param factor fattore di reshape (in punti per carattere)
     * @return rapporto tra risoluzione dello schermo e fattore di reshape (in
     * punti carattere)
     */
    public static double getReshapeFactor(double factor) {
        int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
        if (screenRes > 110) {
            screenRes = 110;
        }
        return screenRes / factor;
    }

    /**
     * Gestisce l'aggiunta di una voce ad un menu
     *
     * @param icon icona
     * @param action action alla selezione
     * @param frame frame
     * @param semGui gui
     * @return item di menu
     */
    public static JMenuItem addMenuItem(String icon, AbstractAction action, JFrame frame, SemGui semGui) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setIcon(new ImageIcon(semGui.getClass().getResource(icon)));
        return menuItem;
    }

    /**
     * Inizializza i sottomenu
     *
     * @param semGui frame gui
     */
    public static void initSubMenus(SemGui semGui) {
        final JPopupMenu popup = new JPopupMenu();
        popup.add(GuiUtils.addMenuItem("/org/thesemproject/opensem/gui/icons16/database.png", new AbstractAction("Carica Storage") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (semGui.isIsClassify()) {
                    return;
                }
                semGui.setLastFolder(semGui.getOpenFileChooser());
                semGui.getSelectOpenStorage().setVisible(true);
            }
        }, semGui, semGui));
        popup.add(GuiUtils.addMenuItem("/org/thesemproject/opensem/gui/icons16/bookmark_folder.png", new AbstractAction("Carica Cartella") {
            public void actionPerformed(ActionEvent e) {
                // TODO add your handling code here:
                if (semGui.isIsClassify()) {
                    return;
                }
                if (semGui.getRtt() == null) {
                    semGui.setLastFolder(semGui.getFolderToLoadChooser());
                    semGui.getSelectFolderToLoad().setVisible(true);
                } else {
                    semGui.getRtt().interrupt();
                    semGui.getInterrompi().setEnabled(false);
                }
            }
        }, semGui, semGui));
        popup.add(GuiUtils.addMenuItem("/org/thesemproject/opensem/gui/icons16/doc_excel_table.png", new AbstractAction("Carica Excel") {
            public void actionPerformed(ActionEvent e) {
                // TODO add your handling code here:
                if (semGui.isIsClassify()) {
                    return;
                }
                semGui.setLastFolder(semGui.getExcelCorpusChooser());
                semGui.getSelectExcelFileSer().setVisible(true);
            }
        }, semGui, semGui));
        popup.add(GuiUtils.addMenuItem("/org/thesemproject/opensem/gui/icons16/script.png", new AbstractAction("Carica File") {
            public void actionPerformed(ActionEvent e) {
                if (semGui.isIsClassify()) {
                    return;
                }
                semGui.setLastFolder(semGui.getImportFileChooser());
                semGui.getSelectFileToImport().setVisible(true);
            }
        }, semGui, semGui));
        semGui.getMenuCarica().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(semGui.getMenuCarica(), semGui.getMenuCarica().getBounds().x - 15, semGui.getMenuCarica().getBounds().y + semGui.getMenuCarica().getBounds().height);
            }
        });
    }

    /**
     * Attiva i limiti di tempo
     *
     * @param type tipo della cattura
     * @param semGui frame
     */
    public static void enableTimeLimits(String type, SemGui semGui) {
        if ("integer".equalsIgnoreCase(type) || "date".equalsIgnoreCase(type)) {
            semGui.getStartTimeInterval().setEnabled(true);
            semGui.getEndTimeInterval().setEnabled(true);
        } else {
            semGui.getStartTimeInterval().setEnabled(false);
            semGui.getEndTimeInterval().setEnabled(false);
        }
    }

    /**
     * gestisce i filtri sullo stato
     *
     * @param filter1 primo filtro
     * @param filter2 secondo filtro
     * @param semGui frame
     */
    public static void filterOnStatus(String filter1, String filter2, SemGui semGui) {
        if (semGui.isIsClassify()) {
            return;
        }
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getSegmentsTable().getRowSorter();
        sorter.setRowFilter(new RowFilter() {
            @Override
            public boolean include(RowFilter.Entry entry) {
                String val = (String) entry.getValue(6);
                if (filter2 == null) {
                    return filter1.equals(val);
                } else {
                    return filter1.equals(val) || filter2.equals(val);
                }
            }
        });
        semGui.getSegmentsTable().setRowSorter(sorter);
        semGui.getStatusSegments().setText("Totale filtrati elementi: " + semGui.getSegmentsTable().getRowCount());
    }

    /**
     * Disegna l'albero
     *
     * @param currentNode nodo corrente (logico)
     * @param currentTreeNode nodo corrente (albero)
     */
    public static void paintTree(NodeData currentNode, DefaultMutableTreeNode currentTreeNode) {
        if (currentNode != null) {
            if (currentNode.hasChildren()) {
                List<NodeData> children = currentNode.getChildrens();
                Collections.sort(children, new Comparator<NodeData>() {
                    @Override
                    public int compare(NodeData t, NodeData t1) {
                        return (t.nodeName.compareTo(t1.nodeName));
                    }
                });
                children.stream().forEach((NodeData child) -> {
                    ClassicationTreeNode node = new ClassicationTreeNode(child.isTrained(), child.nodeName);
                    currentTreeNode.add(node);
                    paintTree(currentNode.getNode(child.nodeName), node);
                });
            }
        }
    }

    /**
     * Gestisce le action su un albero
     *
     * @param tree albero
     * @param area area di testo
     * @param tokenText testo tokenizzato
     * @param evt evento
     * @param ignoreSelected true se deve ignorare i selezionati
     * @param table tabella da cui prendere i selezionati.
     * @param textIdx indice della tabella dove c'è il testo
     * @param semGui frame
     */
    public static void treeActionPerformed(JTree tree, JTextArea area, String tokenText, MouseEvent evt, final boolean ignoreSelected, SemGui semGui, JTable table, int textIdx) {
        if (semGui.isIsClassify()) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(evt)) {
            int selRow = tree.getRowForLocation(evt.getX(), evt.getY());
            TreePath selPath = tree.getPathForLocation(evt.getX(), evt.getY());
            tree.setSelectionPath(selPath);
            if (selRow != -1) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                String txt = (area == null) ? "" : area.getText();
                String language = semGui.getDP().getLanguageFromText(txt);
                String tokenized = (tokenText == null) ? "" : tokenText;
                txt = tokenized;
                if (tokenized.length() > 50) {
                    txt = tokenized.substring(0, 50) + "...";
                }
                final int rowSelected = table.getSelectedRow();
                final int[] selected = table.getSelectedRows();
                if (selected.length > 1) {
                    txt = "tutte le descrizioni selezionate";
                    tokenized = "I testi di tutte le righe selezionate";
                }
                ActionListener menuListener;
                menuListener = new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        TreePath tp = tree.getLeadSelectionPath();
                        Object[] path = tp.getPath();
                        try {
                            if (event.getActionCommand().startsWith("Istruisci")) {
                                Thread t = new Thread(() -> {
                                    semGui.setNeedUpdate(true);
                                    semGui.getSegmentaEClassifica().setEnabled(false);
                                    semGui.getSegmentaEBasta().setEnabled(false);
                                    semGui.getTagCloud().setEnabled(false);
                                    try {
                                        int factor = Integer.parseInt(semGui.getLearningFactor().getText());
                                        if (selected.length == 1 || ignoreSelected) {
                                            String text = area.getText();
                                            String language1 = semGui.getDP().getLanguageFromText(text);
                                            IndexManager.addToIndex(semGui.getPercorsoIndice().getText(), tokenText, path, language1, factor, false);
                                            if (textIdx == 2) {
                                                semGui.deleteSelected();
                                            }
                                        } else {
                                            for (int id : selected) {
                                                String text = (String) table.getValueAt(id, textIdx);
                                                String language2 = semGui.getDP().getLanguageFromText(text);
                                                IndexManager.addToIndex(semGui.getPercorsoIndice().getText(), text, path, language2, factor, true);
                                            }
                                            if (textIdx == 2) {
                                                semGui.deleteSelected();
                                            }
                                        }
                                    } catch (Exception ex) {
                                        LogGui.printException(ex);
                                    }
                                    semGui.getSegmentaEClassifica().setEnabled(true);
                                    semGui.getTagCloud().setEnabled(true);
                                    semGui.getSegmentaEBasta().setEnabled(true);
                                });
                                t.start();
                            } else if (event.getActionCommand().startsWith("Elimina")) {
                                if (GuiUtils.showConfirmDialog("Confermi la cancellazione della cartella e la rimozione dall'indice?", "Warning")) {
                                    semGui.setNeedUpdate(true);
                                    semGui.getSegmentaEClassifica().setEnabled(false);
                                    semGui.getTagCloud().setEnabled(false);
                                    semGui.getSegmentaEBasta().setEnabled(false);
                                    IndexManager.removeFromIndex(semGui.getPercorsoIndice().getText(), path);
                                    semGui.getME().removeNode(path);
                                    DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
                                    model.removeNodeFromParent(node);
                                    semGui.getSegmentaEClassifica().setEnabled(true);
                                    semGui.getTagCloud().setEnabled(true);
                                    semGui.getSegmentaEBasta().setEnabled(true);
                                }
                            } else if (event.getActionCommand().startsWith("Aggiungi")) {
                                String name = JOptionPane.showInputDialog(null, "Aggiungi nodo: ");
                                if (name != null && name.trim().length() > 0) {
                                    List<TreePath> paths = GuiUtils.find((DefaultMutableTreeNode) tree.getModel().getRoot(), name, true);
                                    if (paths.isEmpty()) {
                                        semGui.setNeedUpdate(true);
                                        node.add(new DefaultMutableTreeNode(name));
                                        DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
                                        model.reload();
                                        List<TreePath> paths2 = GuiUtils.find((DefaultMutableTreeNode) model.getRoot(), name, true);
                                        GuiUtils.scrollToPath(tree, paths2);
                                        //String language = DP.getLanguageFromText(nct);
                                        Thread t = new Thread(() -> {
                                            semGui.getSegmentaEClassifica().setEnabled(false);
                                            semGui.getTagCloud().setEnabled(false);
                                            semGui.getFilesPanelSegmenta().setEnabled(false);
                                            semGui.getSegmentaEBasta().setEnabled(false);
                                            Object[] pathNew = new Object[path.length + 1];
                                            pathNew[path.length] = name;
                                            System.arraycopy(path, 0, pathNew, 0, path.length);
                                            /* try {
                                            IndexManager.addToIndex(percorsoIndice.getText(),nct,pathNew,language,100, true);
                                            } catch (Exception ex) {
                                            LogGui.printException(ex);
                                            } */
                                            semGui.getME().addNewNode(pathNew);
                                            semGui.getSegmentaEClassifica().setEnabled(true);
                                            semGui.getTagCloud().setEnabled(true);
                                            semGui.getSegmentaEBasta().setEnabled(true);
                                            semGui.getFilesPanelSegmenta().setEnabled(true);
                                        });
                                        t.start();
                                    } else {
                                        GuiUtils.showDialog("Esiste gi\u00e0 una categoria chiamata " + name, "Warning");
                                        GuiUtils.scrollToPath(tree, paths);
                                    }
                                }
                            }
                            //jButton2.setEnabled(true);
                        } catch (Exception e) {
                            LogGui.printException(e);
                        }
                    }
                };
                if (semGui.isIsInit()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem item;
                    if (txt.length() > 0 && !node.isRoot()) {
                        String label = "Istruisci [" + node.toString() + "] con: '" + txt + "'";
                        popup.add(item = new JMenuItem(label, new ImageIcon(semGui.getClass().getResource("/org/thesemproject/opensem/gui/icons16/flag_blue.png"))));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        item.setToolTipText(tokenized);
                    }
                    if (!node.isRoot()) {
                        popup.add(item = new JMenuItem("Elimina [" + node.toString() + "]", new ImageIcon(semGui.getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                    }
                    if (node.getLevel() < ClassificationPath.MAX_DEEP) {
                        String label = "Aggiungi nodo";
                        popup.add(item = new JMenuItem(label, new ImageIcon(semGui.getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                    }
                    popup.show(tree, evt.getX(), evt.getY());
                }
            }
        }
    }

    /**
     * Ridimensiona le colonne delle tablle
     *
     * @param semGui frame
     */
    public static void prepareTables(SemGui semGui) {
        GuiUtils.prepareColumn(semGui.getFilesTable(), 0, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 1, 50, 100, 300);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 2, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 3, 50, 70, 150);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 4, 50, 70, 150);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 5, 50, 70, 150);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 6, 50, 70, 150);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 7, 50, 70, 150);
        GuiUtils.prepareColumn(semGui.getFilesTable(), 9, 0, 0, 0);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 0, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 1, 50, 120, 500);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 2, 50, 80, 500);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 3, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 5, 50, 100, 300);
        GuiUtils.prepareColumn(semGui.getSegmentsTable(), 6, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getChangedTable(), 0, 50, 50, 50);
        GuiUtils.prepareColumn(semGui.getChangedTable(), 1, 50, 100, 300);
        GuiUtils.prepareColumn(semGui.getChangedTable(), 5, 0, 0, 0);
        GuiUtils.prepareColumn(semGui.getCoverageDocumentsTable(), 0, 50, 50, 50);
    }

    /**
     * Esporta un albero su CSV
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void exportTree(ActionEvent evt, SemGui semGui) {
        semGui.getSelectExportTree().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getExportTreeFileChooser().getSelectedFile();
            semGui.updateLastSelectFolder(file.getAbsolutePath());
            List<String> lines = new ArrayList<>();
            lines.add(NodeData.getCSVDocument(semGui.getME().getRoot()));
            String filePath = file.getAbsolutePath();
            if (!filePath.endsWith(".csv")) {
                filePath = filePath + ".csv";
            }
            GuiUtils.writeCSV(filePath, lines);
        }
    }

    /**
     * Importa un albero da csv
     *
     * @param evt evento
     * @param semGui frame
     * @throws NumberFormatException eccezione sui valori numerici
     */
    public static void importTree(ActionEvent evt, SemGui semGui) throws NumberFormatException {
        semGui.getSelectImportTree().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getImportTreeFileChooser().getSelectedFile();
            semGui.updateLastSelectFolder(file.getAbsolutePath());
            List<String> lines = GuiUtils.readFileLines(file.getAbsolutePath());
            NodeData root = NodeData.getNodeData(lines, Integer.parseInt(semGui.getFattoreK().getText()), new InternPool());
            semGui.getME().storeXml(NodeData.getDocument(root));
            semGui.initializeModel();
            semGui.setNeedUpdate(false);
        }
    }

    /**
     * Fa partire il garbage collector
     */
    public static void runGarbageCollection() {
        LogGui.printMemorySummary();
        LogGui.info("Run garbage collection...");
        System.gc();
        LogGui.info("End of garbage collection...");
        LogGui.printMemorySummary();
    }

    /**
     * Calcola il tag cloud sulla tabella dei files
     *
     * @param semGui frame
     */
    public static void doTagCloud(SemGui semGui) {
        try {
            String text = semGui.getTestoDaSegmentare().getText();
            String language = semGui.getDP().getLanguageFromText(text);
            TagCloudResults result = new TagCloudResults();
            MyAnalyzer analyzer = semGui.getME().getAnalyzer(language);
            Tokenizer.getTagClasses(result, text, "", analyzer);
            semGui.openCloudFrame(result, 100);
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Fa partire la segmentazione
     *
     * @param semGui frame
     */
    public static void doSegment(SemGui semGui) {
        Thread t = new Thread(() -> {
            semGui.getFilesPanelHtml1().setContentType("text/html");
            semGui.getFilesPanelHtml1().setText("Elaborazione in corso...");
            semGui.getFilesPanelHtml1().requestFocus();
            try {
                String text = semGui.getFileText1().getText();
                String language = semGui.getDP().getLanguageFromText(text);
                Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = semGui.getSE().getSegments(text, semGui.getME(), language);
                semGui.getFilesPanelHtml1().setText(SegmentationUtils.getHtml(identifiedSegments, language));
                semGui.getFilesPanelHtml1().setCaretPosition(0);
                semGui.getFileText1().setCaretPosition(0);
            } catch (Exception e) {
                LogGui.printException(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Filtro sulle righe, utilizzato per fare qualche cosa quando si legge un
     * file
     */
    public interface LineFilter {

        /**
         *
         * @param line linea letta
         */
        public void applyTo(String line);
    }

    /**
     * ridimensiona le colonne di una tabella
     *
     * @param table tabella
     * @param col id colonna
     * @param min minima lunghezza
     * @param preferred lunghezza preferita
     * @param max lunghezza massima
     */
    public static void prepareColumn(JTable table, int col, int min, int preferred, int max) {
        table.getColumnModel().getColumn(col).setMinWidth((int) (min * getReshapeFactor(72)));
        table.getColumnModel().getColumn(col).setPreferredWidth((int) (preferred * getReshapeFactor(72)));
        table.getColumnModel().getColumn(col).setMaxWidth((int) (max * getReshapeFactor(72)));
    }

    /**
     * Muove una riga di una tabella in alto
     *
     * @since 1.0.2
     * @param table tabella
     */
    public static void moveUp(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int[] rows = table.getSelectedRows();
        model.moveRow(rows[0], rows[rows.length - 1], rows[0] - 1);
        table.setRowSelectionInterval(rows[0] - 1, rows[rows.length - 1] - 1);
    }

    /**
     * Muove una riga di una tabella in alto come prima
     *
     * @since 1.0.2
     * @param table tabella
     */
    public static void moveTop(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int[] rows = table.getSelectedRows();
        model.moveRow(rows[0], rows[rows.length - 1], 0);
        table.setRowSelectionInterval(0, rows.length - 1);
    }

    /**
     * Muove una riga di una tabella in basso
     *
     * @since 1.0.2
     * @param table tabella
     */
    public static void moveBottom(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int[] rows = table.getSelectedRows();
        model.moveRow(rows[0], rows[rows.length - 1], model.getRowCount() - rows.length);
        table.setRowSelectionInterval(model.getRowCount() - rows.length, model.getRowCount() - 1);
    }

    /**
     * Muove una riga di una tabella in basso come ultima
     *
     * @since 1.0.2
     * @param table tabella
     */
    public static void moveDown(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int[] rows = table.getSelectedRows();
        model.moveRow(rows[0], rows[rows.length - 1], rows[0] + 1);
        table.setRowSelectionInterval(rows[0] + 1, rows[rows.length - 1] + 1);
    }

}
