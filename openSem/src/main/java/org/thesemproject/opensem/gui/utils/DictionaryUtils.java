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

import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DictionaryTreeNode;
import org.thesemproject.opensem.gui.modelEditor.ModelTreeNode;
import org.thesemproject.opensem.gui.modelEditor.SegmentTreeNode;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * Utility per gestire il dizionario
 */
public class DictionaryUtils {

    /**
     * Gestisce l'inserimento della definizione
     *
     * @param ta text area
     * @param gui frame
     */
    public static void addCreateDefinitionMenu(JTextArea ta, SemGui gui) {
        ta.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    DefaultTreeModel model = (DefaultTreeModel) gui.getModelTree().getModel();
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
                    DictionaryTreeNode dict = (DictionaryTreeNode) root.getFirstChild();
                    List<String[]> content = dict.getTableContent();
                    Set<String> definitions = new HashSet<>();
                    String[] defs = new String[content.size()];
                    for (int i = 0; i < content.size(); i++) {
                        defs[i] = content.get(i)[0];
                        definitions.add(defs[i]);
                    }
                    Arrays.sort(defs);
                    final JPopupMenu menu = new JPopupMenu();
                    JMenuItem item;
                    item = new JMenuItem(new DefaultEditorKit.CopyAction());
                    item.setText("Copia");
                    item.setIcon(new ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_copy.png")));
                    item.setEnabled(ta.getSelectionStart() != ta.getSelectionEnd());
                    menu.add(item);
                    JMenuItem item2;
                    String word = ta.getSelectedText().toLowerCase();
                    final String selectedWord = word.trim();
                    if (selectedWord.contains("#")) {
                        return;
                    }
                    item2 = new JMenuItem("Inserisci '" + selectedWord + "' come nuova definizione");
                    item2.setIcon(new ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/book_open.png")));
                    item2.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            String name = JOptionPane.showInputDialog(null, "Inserire il nome della definizione");
                            if (name == null) {
                                return;
                            }
                            if (name.length() > 0) {
                                name = name.toLowerCase();
                                String definition = null;
                                if (!definitions.contains(name)) {
                                    if (PatternsUtils.testPattern(selectedWord, false, gui)) {
                                        definition = name;
                                        dict.addDefinition(name, selectedWord);
                                    } else {
                                        GuiUtils.showErrorDialog("La definizione selezionata non \u00e8 una espressione regolare valida", "Errore");
                                        return;
                                    }
                                } else {
                                    GuiUtils.showErrorDialog("Esiste gi\u00e0 una definizione con quel nome", "Errore");
                                    return;
                                }
                                if (definition != null) {
                                    manageDictionaryDefinition(definition, selectedWord, gui);
                                }
                            } else {
                                GuiUtils.showErrorDialog("Il nome della definizione non pu\u00f2 essere vuoto", "Errore");
                            }
                        }
                    });
                    item2.setEnabled(ta.getSelectionStart() != ta.getSelectionEnd());
                    menu.add(item2);
                    JMenuItem item3;
                    item3 = new JMenuItem("Sostituisci '" + selectedWord + "' con una definizione");
                    item3.setIcon(new ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/book_next.png")));
                    item3.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            String definizione = (String) JOptionPane.showInputDialog(null, "Quale definizione aggiornare?", "Seleziona definizione", JOptionPane.QUESTION_MESSAGE, null, defs, defs[0]);
                            manageDictionaryDefinition(definizione, selectedWord, gui);
                        }
                    });
                    item3.setEnabled(ta.getSelectionStart() != ta.getSelectionEnd());
                    menu.add(item3);
                    if (gui.isIsInit()) {
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private static void manageDictionaryDefinition(String definizione, String selectedWord, SemGui gui) throws HeadlessException, NumberFormatException {
        if (definizione != null) {
            ModelTreeNode node = (ModelTreeNode) gui.getModelEditor().getCurrentNode();
            if (node != null) {
                if (node instanceof CaptureTreeNode) {
                    CaptureTreeNode stn = (CaptureTreeNode) node;
                    List<String[]> patterns = stn.getPatterns();
                    for (String[] pattern : patterns) {
                        String patternDefinition = pattern[1];
                        if (patternDefinition.startsWith(selectedWord + " ")) {
                            patternDefinition = "#" + definizione + " " + patternDefinition.substring(selectedWord.length());
                            stn.updatePattern(pattern[0], Integer.parseInt(pattern[2]), patternDefinition, pattern[3]);
                        }
                        if (patternDefinition.endsWith(" " + selectedWord)) {
                            patternDefinition = patternDefinition.substring(0, patternDefinition.length() - selectedWord.length()) + "#" + definizione + " ";
                            stn.updatePattern(pattern[0], Integer.parseInt(pattern[2]), patternDefinition, pattern[3]);
                        }
                        if (patternDefinition.contains(selectedWord)) {
                            patternDefinition = patternDefinition.replace(selectedWord, "#" + definizione + " ");
                            stn.updatePattern(pattern[0], Integer.parseInt(pattern[2]), patternDefinition, pattern[3]);
                        }
                        if (patternDefinition.equals(selectedWord)) {
                            patternDefinition = "#" + definizione + " ";
                            stn.updatePattern(pattern[0], Integer.parseInt(pattern[2]), patternDefinition, pattern[3]);
                        }
                    }
                    CapturesUtils.populateCaptureSplit(stn, gui);
                    List<TreePath> path = new ArrayList<>();
                    path.add(new TreePath(node.getPath()));
                    GuiUtils.scrollToPath(gui.getModelTree(), path);
                } else if (node instanceof SegmentTreeNode) {
                    SegmentTreeNode stn = (SegmentTreeNode) node;
                    List<String[]> patterns = stn.getPatterns();
                    for (String[] pattern : patterns) {
                        String patternDefinition = pattern[1];
                        if (patternDefinition.startsWith(selectedWord + " ")) {
                            patternDefinition = "#" + definizione + " " + patternDefinition.substring(selectedWord.length());
                            stn.updatePattern(pattern[0], patternDefinition);
                        }
                        if (patternDefinition.endsWith(" " + selectedWord)) {
                            patternDefinition = patternDefinition.substring(0, patternDefinition.length() - selectedWord.length()) + "#" + definizione + " ";
                            stn.updatePattern(pattern[0], patternDefinition);
                        }
                        if (patternDefinition.contains(selectedWord)) {
                            patternDefinition = patternDefinition.replace(selectedWord, "#" + definizione + " ");
                            stn.updatePattern(pattern[0], patternDefinition);
                        }
                        if (patternDefinition.equals(selectedWord)) {
                            patternDefinition = "#" + definizione + " ";
                            stn.updatePattern(pattern[0], patternDefinition);
                        }
                    }
                    SegmentsUtils.populateSegmentSplit(stn, gui);
                    List<TreePath> path = new ArrayList<>();
                    path.add(new TreePath(node.getPath()));
                    GuiUtils.scrollToPath(gui.getModelTree(), path);
                }
            }
        }
    }

    /**
     * Gestisce la cancellazione della definizione
     *
     * @param semGui frame
     */
    public static void deleteDefinition(SemGui semGui) {
        int positions[] = semGui.getDictionaryTable().getSelectedRows();
        boolean confirm = false;
        if (positions.length > 1) {
            confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare tutte le righe selezionate?", "Confermi cancellazione?");
        }
        for (int i = 0; i < positions.length; i++) {
            int position = semGui.getDictionaryTable().convertRowIndexToModel(positions[i] - i);
            String dName = (String) semGui.getDictionaryTable().getValueAt(position, 0);
            if (dName.length() > 0) {
                if (!confirm) {
                    confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare la definizione " + dName + "?", "Confermi cancellazione?");
                }
                if (confirm) {
                    DictionaryTreeNode node = (DictionaryTreeNode) semGui.getModelEditor().getCurrentNode();
                    if (node != null) {
                        node.removeDefinition(dName);
                        populateDictionarySplit(node, semGui);
                    }
                }
            }
        }
    }

    /**
     * Gestisce le action sulla tabella
     *
     * @param semGui frame
     */
    public static void dictionaryTableAction(SemGui semGui) {
        int position = semGui.getDictionaryTable().getSelectedRow();
        String dName = (String) semGui.getDictionaryTable().getValueAt(position, 0);
        String dPattern = (String) semGui.getDictionaryTable().getValueAt(position, 1);
        semGui.getDefinitionName().setEditable(false);
        semGui.getDefinitionName().setText(dName.trim());
        semGui.getDefinitionPattern().setText(dPattern.trim());
        semGui.getConfirmDefinitionPattern().setEnabled(false);
    }

    /**
     * Gestisce la cancellazione del pattern
     *
     * @param semGui frame
     */
    public static void confirmDefinitionPattern(SemGui semGui) {
        // TODO add your handling code here:
        DictionaryTreeNode node = (DictionaryTreeNode) semGui.getModelEditor().getCurrentNode();
        if (semGui.getDefinitionName().getText().length() == 0) {
            GuiUtils.showErrorDialog("Il nome della definizione non pu\u00f2 essere vuoto", "Problema");
            return;
        }
        if (node != null) {
            String value = semGui.getDefinitionPattern().getText().toLowerCase();
            if (PatternsUtils.testPattern(value, false, semGui)) {
                if (semGui.getDefinitionName().isEditable()) {
                    //Nuovo
                    if (!node.containsDefinition(semGui.getDefinitionName().getText())) {
                        node.addDefinition(semGui.getDefinitionName().getText(), semGui.getDefinitionPattern().getText());
                    } else {
                        GuiUtils.showErrorDialog("La definizione esiste gi\u00e0", "Problema");
                        return;
                    }
                } else {
                    //Modifica
                    node.updateDefinition(semGui.getDefinitionName().getText(), semGui.getDefinitionPattern().getText());
                }
                populateDictionarySplit(node, semGui);
            }
        }
    }

    /**
     * Ripulisce il pannello della definizione
     *
     * @param semGui frame
     */
    public static void clearDefinitionPanel(SemGui semGui) {
        semGui.getDefinitionName().setEditable(true);
        semGui.getDefinitionName().setText("");
        semGui.getDefinitionPattern().setText("");
        semGui.getDefinitionStatus().setText("");
    }

    /**
     * Popola il pannello del dizionario in funzione del nodo
     *
     * @param dictionaryTreeNode nodo dizionario
     * @param semGui frame
     */
    public static void populateDictionarySplit(DictionaryTreeNode dictionaryTreeNode, SemGui semGui) {
        clearDefinitionPanel(semGui);
        GuiUtils.clearTable(semGui.getDictionaryTable());
        DefaultTableModel model = (DefaultTableModel) semGui.getDictionaryTable().getModel();
        dictionaryTreeNode.getTableContent().stream().forEach((String[] row) -> {
            model.addRow(row);
        });
    }

    /**
     * Gestisce la rinomina di una definizione
     *
     * @param semGui frame
     */
    public static void renameDefinition(SemGui semGui) {
        int position = semGui.getDictionaryTable().getSelectedRow();
        String dName = (String) semGui.getDictionaryTable().getValueAt(position, 0);
        if (dName.length() > 0) {
            DictionaryTreeNode node = (DictionaryTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                String name = JOptionPane.showInputDialog(null, "Nuovo nome definizione: ", dName);
                if (name != null) {
                    if (name.length() == 0) {
                        GuiUtils.showErrorDialog("Il nome della definizione non pu\u00f2 essere vuoto", "Problema");
                    } else if (node.containsDefinition(name) && !name.equals(dName)) {
                        GuiUtils.showErrorDialog("Esiste gi\u00e0 una definizione con questo nome", "Problema");
                    } else if (!name.equals(dName)) {
                        String definition = (String) semGui.getDictionaryTable().getValueAt(position, 1);
                        node.removeDefinition(dName);
                        node.addDefinition(name, definition);
                        DictionaryUtils.populateDictionarySplit(node, semGui);
                    }
                }
            }
        }
    }

}
