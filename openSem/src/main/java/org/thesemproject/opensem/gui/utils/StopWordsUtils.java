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

import org.thesemproject.opensem.gui.utils.GuiUtils;
import org.thesemproject.opensem.classification.MyAnalyzer;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.TableCellListener;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;

/**
 *
 * Gestisce gli eventi grafici del pannello stopwords
 */
public class StopWordsUtils {

    /**
     * Gestisce l'aggiunta di una nuova stopword
     *
     * @param name nome della stopword
     * @param language lingua
     * @param semGui frame
     */
    public static void addStopWord(String name, String language, SemGui semGui) {
        List<String> stopWords = semGui.getME().getStopWords(language);
        if (!stopWords.contains(name.toLowerCase())) {
            stopWords.add(name);
            semGui.setNeedUpdate(true);
            semGui.getME().storeStopWords(language, stopWords);
        } else {
            GuiUtils.showErrorDialog("La stopword " + name + " esiste gi\u00e0", "Duplicato");
        }
        populateStopWords(semGui);
    }

    /**
     * Gestice l'evento di aggiunta della stopword (pannello di richiesta)
     *
     * @param evt event
     * @param semGui frame
     */
    public static void addStopWordActionPerformed(ActionEvent evt, SemGui semGui) {
        String name = JOptionPane.showInputDialog(null, "Inserire il valore del record");
        if (name == null) {
            name = "";
        }
        if (name.length() > 0) {
            String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
            addStopWord(name, language, semGui);
        }
    }

    /**
     * Gestisce la modifica di una stopword (da tabella)
     *
     * @param tcl cell listner
     * @param semGui frame
     */
    public static void changeStopWord(TableCellListener tcl, SemGui semGui) {
        String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
        List<String> stopWords = semGui.getME().getStopWords(language);
        List<String> dStopWords = semGui.getME().getDefaultStopWords(language);
        try {
            String newStr = new String(((String) tcl.getNewValue()).getBytes(), "UTF-8");
            if (newStr.length() > 0) {
                String old = new String(((String) tcl.getOldValue()).getBytes(), "UTF-8");
                stopWords.remove(old);
                if (dStopWords.contains(old)) {
                    GuiUtils.showErrorDialog("Impossibile modificare una stopword standard", "Default Stop Word Error");
                }
                stopWords.add(newStr);
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
        semGui.setNeedUpdate(true);
        semGui.getME().storeStopWords(language, stopWords);
        populateStopWords(semGui);
    }

    /**
     * popola il pannello delle stopword
     *
     * @param semGui frame
     */
    public static void populateStopWords(SemGui semGui) {
        DefaultTableModel model = (DefaultTableModel) semGui.getStopWordsTable().getModel();
        GuiUtils.clearTable(semGui.getStopWordsTable());
        String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
        List<String> rows = semGui.getME().getStopWords(language);
        rows.stream().map((String row) -> {
            Object[] rowData = new Object[1];
            rowData[0] = row;
            return rowData;
        }).forEach((Object[] rowData) -> {
            model.addRow(rowData);
        });
        semGui.getManageStopWrodsStatus().setText("Lingua corrente: " + language + " - Totale parole: " + rows.size());
    }

    /**
     * Aggiunge il menu di aggiunta di una parola alle stopwords
     *
     * @param ta text area da dove prendere il valore
     * @param gui frame
     */
    public static void addStopWordsMenu(JTextArea ta, SemGui gui) {
        ta.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    final JPopupMenu menu = new JPopupMenu();
                    JMenuItem item;
                    item = new JMenuItem(new DefaultEditorKit.CopyAction());
                    item.setText("Copia");
                    item.setIcon(new ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_copy.png")));
                    item.setEnabled(ta.getSelectionStart() != ta.getSelectionEnd());
                    menu.add(item);


                    for (String language : MyAnalyzer.languages) {
                        JMenuItem item2;
                        String word = ta.getSelectedText().toLowerCase();
                        final String stopWord = word;
                        if (stopWord.length() > 20) {
                            item2 = new JMenuItem("Inserisci quanto selezionato nelle stop words " + language);
                        } else {
                            item2 = new JMenuItem("Inserisci '" + stopWord + "' nelle stop words " + language);
                        }
                        item2.setIcon(new ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/" + language + ".png")));
                        item2.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                String[] words = stopWord.split(" ");
                                for (String word : words) {
                                    addStopWord(word, language, gui);
                                }
                            }
                        });
                        item2.setEnabled(ta.getSelectionStart() != ta.getSelectionEnd());
                        menu.add(item2);
                    }
                    if (gui.isIsInit()) {
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     * Gestisce la cancellazione di una stopword
     *
     * @param semGui frame
     */
    public static void deleteStopWord(SemGui semGui) {
        if (GuiUtils.showConfirmDialog("Confermi l'eliminazione delle righe selezionate?\nLe stop words standard non verranno eliminate", "Conferma")) {
            ;
        }
        DefaultTableModel model = (DefaultTableModel) semGui.getStopWordsTable().getModel();
        int[] rows = semGui.getStopWordsTable().getSelectedRows();
        String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
        List<String> stopWords = semGui.getME().getStopWords(language);
        for (int i = 0; i < rows.length; i++) {
            int pos = semGui.getStopWordsTable().convertRowIndexToModel(rows[i] - i);
            String record = (String) model.getValueAt(pos, 0);
            stopWords.remove(record);
            model.removeRow(pos);
        }
        semGui.setNeedUpdate(true);
        semGui.getME().storeStopWords(language, stopWords);
        StopWordsUtils.populateStopWords(semGui);
    }

    /**
     * Gestisce l'aggiunta di una stop word
     *
     * @param semGui frame
     * @throws HeadlessException eccezione
     */
    public static void addStopWord(SemGui semGui) throws HeadlessException {
        String name = JOptionPane.showInputDialog(null, "Inserire il valore del record");
        if (name == null) {
            name = "";
        }
        if (name.length() > 0) {
            String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
            StopWordsUtils.addStopWord(name, language, semGui);
        }
    }

}
