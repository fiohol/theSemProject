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
import org.thesemproject.opensem.gui.modelEditor.SegmentTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreePath;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.segmentation.SegmentationUtils;

/**
 *
 * Gestisce gli eventi di segmentazione
 */
public class SegmentsUtils {

    /**
     * Ripulisce il pattern panel
     * @param semGui frame
     */
    public static void clearSegmentPatternPanel(SemGui semGui) {
        semGui.getSegmentPatternDefinition().setText("");
        semGui.getSegmentPatternStatus().setText("");
        semGui.getConfirmSegmentPattern().setEnabled(false);
    }

    /**
     * Gestisce una action sulla tabella dei pattern del segment panel
     * @param semGui frame
     */
    public static void segmentPatternsTableAction(SemGui semGui) {
        int position = semGui.getSegmentPatternsTable().getSelectedRow();
        String id = (String) semGui.getSegmentPatternsTable().getValueAt(position, 0);
        String dPattern = (String) semGui.getSegmentPatternsTable().getValueAt(position, 1);
        semGui.getSegmentPatternDefinition().setText(dPattern.trim());
        semGui.getConfirmSegmentPattern().setEnabled(false);
        semGui.getSegmentPatternStatus().setText(id);
    }

    /**
     * Gestisce la cancellazione di un pattern dalla definizione del segmento
     * @param semGui frame
     */
    public static void segmentPatternDelete(SemGui semGui) {
        int positions[] = semGui.getSegmentPatternsTable().getSelectedRows();
        boolean confirm = false;
        if (positions.length > 1) {
            confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare tutte le righe selezionate?", "Confermi cancellazione?");
        }
       for (int i = 0; i < positions.length; i++) {
            int position = semGui.getSegmentPatternsTable().convertRowIndexToModel(positions[i] - i);
            String id = (String) semGui.getSegmentPatternsTable().getValueAt(position, 0);
            if (id.length() > 0) {

                if (!confirm) {
                    confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare il pattern selezionato?", "Confermi cancellazione?");
                }
                if (confirm) {
                    SegmentTreeNode node = (SegmentTreeNode) semGui.getModelEditor().getCurrentNode();
                    if (node != null) {
                        node.removePattern(id);
                        populateSegmentSplit(node, semGui);
                    }
                }
            }
        }
    }

    /**
     * Gestisce la conferma per l'inserimento di un nuovo pattern
     * @param semGui frame
     */
    public static void confirmSegmentPattern(SemGui semGui) {
        String value = semGui.getSegmentPatternDefinition().getText().toLowerCase();
        SegmentTreeNode node = (SegmentTreeNode) semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            String id = semGui.getSegmentPatternStatus().getText();
            if (id.length() == 0) {
                //Inserimento
                if (PatternsUtils.testPattern(value, false, semGui)) {
                    node.addPattern(value);
                }
            } else //Modifica
            {
                if (PatternsUtils.testPattern(value, false, semGui)) {
                    node.updatePattern(id, value);
                }
            }
            populateSegmentSplit(node, semGui);
            List<TreePath> path = new ArrayList<>();
            path.add(new TreePath(node.getPath()));
            GuiUtils.scrollToPath(semGui.getModelTree(), path);
        }
    }

    /**
     * Popola il pannello del model editor con i dati del segmento
     * @param node nodo che rappresenta il segmento
     * @param semGui frame
     */
    public static void populateSegmentSplit(SegmentTreeNode node, SemGui semGui) {
        semGui.getSegmentName().setText(node.getNodeName());
        semGui.getDefaultYN().setSelectedItem(node.isDefault() ? "Yes" : "No");
        semGui.getMultipleYN().setSelectedItem(node.isMultiple() ? "Yes" : "No");
        semGui.getClassifyYN().setSelectedItem(node.isClassify() ? "Yes" : "No");
        GuiUtils.clearTable(semGui.getSegmentPatternsTable());
        DefaultTableModel model = (DefaultTableModel) semGui.getSegmentPatternsTable().getModel();
        node.getPatterns().stream().forEach((String[] row) -> {
            model.addRow(row);
        });
    }


}
