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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.gui.modelEditor.ModelTreeNode;
import org.thesemproject.opensem.gui.modelEditor.SegmentTreeNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * Gestisce tutti gli eventi grafici relativi alle catture
 */
public class CapturesUtils {

    /**
     * Gestisce il click per pulire il pannello di definizione cattura
     *
     * @param semGui frame
     */
    public static void clearCapturePatternPanel(SemGui semGui) {
        semGui.getCapturePatternDefinition().setText("");
        semGui.getCapturePatternSpinner().setValue(0);
        semGui.getCapturePatternStatus().setText("");
        semGui.getConfirmCapturePattern().setEnabled(false);
        semGui.getCapturePatternFixedValue().setText("");
    }

    /**
     * Gestisce le azioni sulla tabella dei pattern
     *
     * @param semGui frame
     */
    public static void capturePatternTableAction(SemGui semGui) {
        int position = semGui.getCapturePatternTable().getSelectedRow();
        String id = (String) semGui.getCapturePatternTable().getValueAt(position, 0);
        String dPattern = (String) semGui.getCapturePatternTable().getValueAt(position, 1);
        String set = (String) semGui.getCapturePatternTable().getValueAt(position, 2);
        String fix = (String) semGui.getCapturePatternTable().getValueAt(position, 3);
        if (fix == null) {
            fix = "";
        }
        semGui.getCapturePatternDefinition().setText(dPattern);
        semGui.getCapturePatternSpinner().setValue(new Integer(set));
        semGui.getCapturePatternStatus().setText(id);
        semGui.getConfirmCapturePattern().setEnabled(false);
        semGui.getCapturePatternFixedValue().setText(fix);
        semGui.getTestCaptureMatch().setEnabled(true);
    }

    /**
     * Gestisce la cancellazione del pattern
     *
     * @param semGui frame
     */
    public static void deleteCapturePattern(SemGui semGui) {
        int positions[] = semGui.getCapturePatternTable().getSelectedRows();
        boolean confirm = false;
        if (positions.length > 1) {
            confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare tutte le righe selezionate?", "Confermi cancellazione?");
        }
        for (int i = 0; i < positions.length; i++) {
            int position = semGui.getCapturePatternTable().convertRowIndexToModel(positions[i] - i);
            String id = (String) semGui.getCapturePatternTable().getValueAt(position, 0);
            if (id.length() > 0) {

                if (!confirm) {
                    confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare la cattura selezionata?", "Confermi cancellazione?");
                }
                if (confirm) {
                    CaptureTreeNode node = (CaptureTreeNode) semGui.getModelEditor().getCurrentNode();
                    if (node != null) {
                        node.removePattern(id);
                        populateCaptureSplit(node, semGui);
                    }
                }
            }
        }
    }

    /**
     * Al click sul nodo popola il pannello con la configurazione della cattura
     *
     * @param node nodo cliccato
     * @param semGui frame
     */
    public static void populateCaptureSplit(CaptureTreeNode node, SemGui semGui) {
        CapturesUtils.clearCapturePatternPanel(semGui);
        semGui.getCaptureName().setText(node.getNodeName());
        semGui.getCaptureType().setSelectedItem(node.getCaptureType());
        semGui.getCaptureFormat().setText(node.getCaptureFormat());
        semGui.getTempCapture().setSelected(node.isTemporary());
        semGui.getStartTimeInterval().setSelected(node.isStartPeriod());
        semGui.getEndTimeInterval().setSelected(node.isEndPeriod());
        GuiUtils.enableTimeLimits(node.getCaptureType(), semGui);
        if (node.getParent() instanceof CaptureTreeNode) {
            semGui.getCaptureTarget().setEnabled(false);
            node.setScope("local");
            semGui.getCaptureTarget().setSelectedItem(node.getScope());
        } else {
            semGui.getCaptureTarget().setEnabled(true);
            semGui.getCaptureTarget().setSelectedItem(node.getScope());
        }
        TreeNode[] anchestors = node.getPath();
        boolean isGlobal = true;
        for (TreeNode anchestor : anchestors) {
            if (anchestor instanceof SegmentTreeNode) {
                isGlobal = false;
                break;
            }
        }
        semGui.getOpenSegmentRelationshipPanel().setEnabled(isGlobal);
        if (isGlobal) {
            semGui.getCaptureName().setText(node.getNodeName() + " (Global)");
        }
        ClassificationPath cp = node.getClassificationPath();
        if (cp != null) {
            semGui.getClassifyPattern().setText(cp.toSmallClassString());
        } else {
            semGui.getClassifyPattern().setText("");
        }
        GuiUtils.clearTable(semGui.getCapturePatternTable());
        DefaultTableModel model = (DefaultTableModel) semGui.getCapturePatternTable().getModel();
        node.getPatterns().stream().forEach((String[] row) -> {
            model.addRow(row);
        });
    }

    /**
     * Salva la relazione tra cattura e path di classificaizione
     *
     * @param semGui frame
     */
    public static void saveRelationShip(SemGui semGui) {
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                String path = semGui.getCatClass().getText();
                if (path.length() != 0) {
                    ClassificationPath cp = GuiUtils.getClassificationPath(path);
                    ctn.setClassificationPath(cp);
                    semGui.getClassifyPattern().setText(cp.toSmallClassString());
                } else {
                    semGui.getClassifyPattern().setText("");
                }
            }
        }
        semGui.getCaptureClassificationRelationship().setVisible(false);
    }

    /**
     * Apre il pannello per mettere in relazione una cattura globale con i
     * segmenti
     *
     * @param semGui farme
     */
    public static void openSegmentRelationship(SemGui semGui) {
        //Deve popolare il pannello con la cattura corrente....
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                GuiUtils.clearTable(semGui.getCaptureRelationshipTable());
                Set<String> enabledSegments = ctn.getEnabledSegments();
                List<String> segNames = semGui.getModelEditor().getSegmentsNames((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot());
                DefaultTableModel model = (DefaultTableModel) semGui.getCaptureRelationshipTable().getModel();
                for (String segment : segNames) {
                    Object[] row = new Object[2];
                    row[0] = segment;
                    row[1] = new Boolean(enabledSegments.contains(segment));
                    model.addRow(row);
                }
                semGui.getGlobalCapturesSegmentsRelationship().setVisible(true);
            }
        }
    }

    /**
     * Salva la relazione tra segmenti e cattura
     *
     * @param semGui frame
     */
    public static void saveRelationship(SemGui semGui) {
        semGui.getGlobalCapturesSegmentsRelationship().setVisible(false);
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                Set<String> segNames = new HashSet();
                DefaultTableModel model = (DefaultTableModel) semGui.getCaptureRelationshipTable().getModel();
                int rowCount = model.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    String segName = (String) model.getValueAt(i, 0);
                    Boolean checked = (Boolean) model.getValueAt(i, 1);
                    if (checked) {
                        segNames.add(segName);
                    }
                }
                ctn.setEnabledSegments(segNames);
                GuiUtils.clearTable(semGui.getCaptureRelationshipTable());
            }
        }
    }

    /**
     * Salva il pattern di cattura
     *
     * @param semGui frame
     */
    public static void confirmCapturePattern(SemGui semGui) {
        String value = semGui.getCapturePatternDefinition().getText().toLowerCase();
        if (PatternsUtils.testPattern(value, false, semGui)) {
            Integer position = (Integer) semGui.getCapturePatternSpinner().getValue();
            CaptureTreeNode node = (CaptureTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                String id = semGui.getCapturePatternStatus().getText();
                if (id.length() == 0) {
                    //Inserimento
                    if (PatternsUtils.testPattern(value, false, semGui)) {
                        node.addPattern(position, value, semGui.getCapturePatternFixedValue().getText());
                    }
                } else //Modifica
                {
                    if (PatternsUtils.testPattern(value, false, semGui)) {
                        node.updatePattern(id, position, value, semGui.getCapturePatternFixedValue().getText());
                    }
                }
                CapturesUtils.populateCaptureSplit(node, semGui);
            }
            List<TreePath> path = new ArrayList<>();
            path.add(new TreePath(node.getPath()));
            GuiUtils.scrollToPath(semGui.getModelTree(), path);
        }
    }

}
