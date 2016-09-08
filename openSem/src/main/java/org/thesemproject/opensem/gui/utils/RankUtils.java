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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.thesemproject.opensem.classification.NodeData;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemDocument;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.segmentation.functions.DurationsMap;
import org.thesemproject.opensem.segmentation.functions.rank.RankEvaluations;
import org.thesemproject.opensem.segmentation.functions.rank.RankEvaluator;
import static org.thesemproject.opensem.segmentation.functions.rank.RankEvaluator.EQUALS;
import static org.thesemproject.opensem.segmentation.functions.rank.RankEvaluator.MATCH_REGEX;

/**
 * Classe che gestisce gli eventi della tabella di definizione del rank
 *
 * @since 1.3.4
 * @author The Sem Project
 */
public class RankUtils {

    /**
     * Gestisce l'evento quando si seleziona il campo su cui si vuole mettere la
     * condizione
     *
     * @param sem gui
     */
    public static void manageRankName(SemGui sem) {
        int idx = sem.getFieldRankName().getSelectedIndex();
        if (idx != -1) {
            if (DurationsMap.CLASSIFICATIONS.equals(sem.getFieldRankName().getSelectedItem())) {
                //Classificazione
                //Se è classificazione devo popolare la popup delle classi
                NodeData root = sem.getME().getRoot();
                List<String> children = root.visitSubTree(null);
                Collections.sort(children);
                String[] classValues = new String[children.size()];
                for (int i = 0; i < children.size(); i++) {
                    classValues[i] = children.get(i);
                }
                sem.getFieldRankValue().setModel(new DefaultComboBoxModel<>(classValues));
                //Inoltre le codizioni sono solo uguale a
                String[] conditions = {RankEvaluator.EQUALS};
                sem.getFieldRankCondition().setModel(new DefaultComboBoxModel<>(conditions));
                sem.getFieldRankCondition().setSelectedItem(RankEvaluator.EQUALS);
                //Essendoci uguale allora si abilita la durata
                sem.getRankDurationCondition().setEnabled(true);
                sem.getRankDurationValue().setEnabled(true);
                sem.getRankStartYear().setEnabled(true);
                sem.getRankEndYear().setEnabled(true);
            } else {
                CaptureTreeNode cx = gcxz.get(idx - 1);
                String type = cx.getCaptureType();
                String[] conditions = null;
                switch (type) {
                    case "date":
                    case "integer":
                    case "real":
                    case "number":
                        String[] d1 = {RankEvaluator.GREAT, RankEvaluator.EQUALS, RankEvaluator.LESS, RankEvaluator.NOT_EQUALS, RankEvaluator.GREAT_OR_EQUAL, RankEvaluator.LESS_OR_EQUAL};
                        conditions = d1;
                        break;
                    case "boolean":
                    case "text":
                        String[] d2 = {RankEvaluator.EQUALS, RankEvaluator.NOT_EQUALS, RankEvaluator.MATCH_REGEX};
                        conditions = d2;
                        break;
                    default:
                        break;
                }
                sem.getFieldRankCondition().setModel(new DefaultComboBoxModel<>(conditions));
                sem.getFieldRankCondition().setSelectedItem(null);
                sem.getFieldRankValue().setModel(new DefaultComboBoxModel<>(new String[]{}));
                sem.getFieldRankValue().setEditable(true);
            }
        }
    }

    /**
     * Gestisce l'evento quando si clicca sulla tabella delle condizioni
     *
     * @param sem gui
     */
    public static void manageRankTable(SemGui sem) {
        clearRank(sem);
        int currentRow = sem.getRankTable().getSelectedRow();
        sem.getRankStatus().setText(String.valueOf(sem.getRankTable().getModel().getValueAt(currentRow, 0)));
        sem.getFieldRankName().setSelectedItem(sem.getRankTable().getModel().getValueAt(currentRow, 1));
        sem.getFieldRankCondition().setSelectedItem(sem.getRankTable().getModel().getValueAt(currentRow, 2));
        sem.getFieldRankValue().setSelectedItem(sem.getRankTable().getModel().getValueAt(currentRow, 3));
        sem.getRankDurationCondition().setSelectedItem(sem.getRankTable().getModel().getValueAt(currentRow, 4));
        sem.getRankDurationValue().setText(String.valueOf(sem.getRankTable().getModel().getValueAt(currentRow, 5)));
        sem.getRankStartYear().setText(String.valueOf(sem.getRankTable().getModel().getValueAt(currentRow, 6)));
        sem.getRankEndYear().setText(String.valueOf(sem.getRankTable().getModel().getValueAt(currentRow, 7)));
        sem.getRankScore().setText(String.valueOf(sem.getRankTable().getModel().getValueAt(currentRow, 8)));
    }

    /**
     * Gestisce l'evento di aggiunta o modifica di una condizione
     *
     * @param sem gui
     */
    public static void addModifyRank(SemGui sem) {
        String id = sem.getRankStatus().getText();
        DefaultTableModel model = (DefaultTableModel) sem.getRankTable().getModel();
        double rank = 0;
        try {
            rank = Double.parseDouble(String.valueOf(sem.getRankScore().getText()));
        } catch (Exception e) {
            GuiUtils.showErrorDialog("Valore di rank non valido", "Errore");
            return;
        }
        if (sem.getRankDurationValue().getText().length() > 0) {
            try {
                Double.parseDouble(sem.getRankDurationValue().getText());
            } catch (Exception e) {
                GuiUtils.showErrorDialog("La durata deve essere un numero valido", "Errore");
                return;
            }
        }
        if (sem.getRankStartYear().getText().length() > 0) {
            try {
                Integer.parseInt(sem.getRankStartYear().getText());
            } catch (Exception e) {
                GuiUtils.showErrorDialog("Anno inizio non valido", "Errore");
                return;
            }
        }
        if (sem.getRankEndYear().getText().length() > 0) {
            try {
                Integer.parseInt(sem.getRankEndYear().getText());
            } catch (Exception e) {
                GuiUtils.showErrorDialog("Anno fine non valido", "Errore");
                return;
            }
        }

        if (rank > 0 && String.valueOf(sem.getFieldRankValue().getSelectedItem()).length() > 0) {
            if ("...".equalsIgnoreCase(id)) {
                Object[] row = new Object[9];
                row[0] = String.valueOf(System.currentTimeMillis());
                row[1] = sem.getFieldRankName().getSelectedItem();
                row[2] = sem.getFieldRankCondition().getSelectedItem();
                row[3] = sem.getFieldRankValue().getSelectedItem();
                row[4] = sem.getRankDurationCondition().getSelectedItem();
                row[5] = sem.getRankDurationValue().getText();
                row[6] = sem.getRankStartYear().getText();
                row[7] = sem.getRankEndYear().getText();
                row[8] = sem.getRankScore().getText();
                model.addRow(row);
            } else {
                int currentRow = sem.getRankTable().getSelectedRow();
                sem.getRankTable().getModel().setValueAt(sem.getFieldRankName().getSelectedItem(), currentRow, 1);
                sem.getRankTable().getModel().setValueAt(sem.getFieldRankCondition().getSelectedItem(), currentRow, 2);
                sem.getRankTable().getModel().setValueAt(sem.getFieldRankValue().getSelectedItem(), currentRow, 3);
                sem.getRankTable().getModel().setValueAt(sem.getRankDurationCondition().getSelectedItem(), currentRow, 4);
                sem.getRankTable().getModel().setValueAt(sem.getRankDurationValue().getText(), currentRow, 5);
                sem.getRankTable().getModel().setValueAt(sem.getRankStartYear().getText(), currentRow, 6);
                sem.getRankTable().getModel().setValueAt(sem.getRankEndYear().getText(), currentRow, 7);
                sem.getRankTable().getModel().setValueAt(sem.getRankScore().getText(), currentRow, 8);
            }
            saveRank(sem);
            loadRank(sem);
            clearRank(sem);
            clearFieldRank(sem);
        }
    }

    /**
     * Pulisce tutti i field di inserimento rank
     *
     * @param sem gui
     */
    public static void clearFieldRank(SemGui sem) {
        sem.getRankScore().setEnabled(false);
        sem.getOkRank().setEnabled(false);
        sem.getFieldRankName().setEnabled(false);
        sem.getFieldRankValue().setEnabled(false);
        sem.getFieldRankCondition().setEnabled(false);
        sem.getRankDurationCondition().setEnabled(false);
        sem.getRankEndYear().setEnabled(false);
        sem.getRankStartYear().setEnabled(false);
        sem.getRankDurationValue().setEnabled(false);
    }

    /**
     * Gestisce l'evento di cancellazione di una regola
     *
     * @param sem gui
     */
    public static void deleteRankRule(SemGui sem) {
        int[] positions = sem.getRankTable().getSelectedRows();
        boolean confirm = false;
        if (positions.length > 1) {
            confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare tutte le righe selezionate?", "Confermi cancellazione?");
        }
        DefaultTableModel model = (DefaultTableModel) sem.getRankTable().getModel();
        for (int i = 0; i < positions.length; i++) {
            int position = sem.getRankTable().convertRowIndexToModel(positions[i] - i);
            String id = (String) sem.getRankTable().getValueAt(position, 0);
            if (id.length() > 0) {
                if (!confirm) {
                    confirm = GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare la regola selezionata?", "Confermi cancellazione?");
                }
                if (confirm) {
                    model.removeRow(position);
                }
            }
        }
        saveRank(sem);
        clearRank(sem);
        clearFieldRank(sem);
    }

    /**
     * Setta nello stato iniziale i campi per la definizione della regola di
     * rank
     *
     * @param sem gui
     */
    public static void clearRank(SemGui sem) {
        sem.getRankStatus().setText("...");
        List<CaptureTreeNode> cxz = sem.getModelEditor().getSegmentsCapturesNodes((DefaultMutableTreeNode) sem.getModelTree().getModel().getRoot(), false);
        int j = 0;
        gcxz.clear();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < cxz.size(); i++) {
            CaptureTreeNode ctn = cxz.get(i);
            if (!ctn.isTemporary() && ctn.getClassificationPath() == null && !used.contains(ctn.getNodeName())) {
                gcxz.add(cxz.get(i));
                used.add(ctn.getNodeName());
            }
        }
        Collections.sort(gcxz, new Comparator() {
            @Override
            public int compare(Object t, Object t1) {
                CaptureTreeNode c = (CaptureTreeNode) t;
                CaptureTreeNode c1 = (CaptureTreeNode) t1;
                return c.getNodeName().compareTo(c1.getNodeName());
            }
        });
        String[] values = new String[gcxz.size() + 1];
        values[0] = DurationsMap.CLASSIFICATIONS;
        for (int i = 0; i < gcxz.size(); i++) {
            values[i + 1] = (gcxz.get(i).getNodeName());
        }

        sem.getFieldRankName().setModel(new javax.swing.DefaultComboBoxModel<>(values));
        sem.getFieldRankName().setSelectedIndex(-1);
        sem.getFieldRankCondition().setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{}));
        sem.getFieldRankValue().setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{}));
        sem.getFieldRankValue().setEditable(false);
        sem.getRankDurationCondition().setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{RankEvaluator.GREAT, RankEvaluator.EQUALS, RankEvaluator.LESS, RankEvaluator.NOT_EQUALS, RankEvaluator.GREAT_OR_EQUAL, RankEvaluator.LESS_OR_EQUAL}));
        sem.getRankDurationCondition().setSelectedIndex(-1);
        sem.getRankDurationValue().setText("");
        sem.getRankStartYear().setText("");
        sem.getRankEndYear().setText("");
        sem.getRankScore().setText("");
        sem.getRankScore().setEnabled(true);
        sem.getOkRank().setEnabled(true);
        sem.getFieldRankName().setEnabled(true);
        sem.getFieldRankValue().setEnabled(true);
        sem.getFieldRankCondition().setEnabled(true);
        sem.getRankDurationCondition().setEnabled(false);
        sem.getRankEndYear().setEnabled(false);
        sem.getRankStartYear().setEnabled(false);
        sem.getRankDurationValue().setEnabled(false);

    }

    /**
     * Gestisce l'evento di modifica della condizione di rank
     *
     * @param sem gui
     */
    public static void manageRankCondition(SemGui sem) {
        if (sem.getFieldRankCondition().getSelectedItem() == null) {
            return;
        }
        if (sem.getFieldRankCondition().getSelectedItem().equals(EQUALS) || sem.getFieldRankCondition().getSelectedItem().equals(MATCH_REGEX)) {
            sem.getRankDurationCondition().setEnabled(true);
            sem.getRankDurationValue().setEnabled(true);
            sem.getRankStartYear().setEnabled(true);
            sem.getRankEndYear().setEnabled(true);
        } else {
            sem.getRankDurationCondition().setEnabled(false);
            sem.getRankDurationValue().setEnabled(false);
            sem.getRankDurationValue().setText("");
            sem.getRankStartYear().setEnabled(false);
            sem.getRankStartYear().setText("");
            sem.getRankEndYear().setEnabled(false);
            sem.getRankEndYear().setText("");
        }

    }
    private static List<CaptureTreeNode> gcxz = new ArrayList<>();

    /**
     * Gestisce il salvataggio delle condizioni di rank
     *
     * @param sem gui
     */
    public static void saveRank(SemGui sem) {
        FileOutputStream fout;
        try {
            String path = sem.getPercorsoIndice().getText() + "/evaluations.rank";
            fout = new FileOutputStream(new File(path));
        } catch (Exception e) {
            LogGui.printException(e);
            return;
        }
        try {
            RankEvaluations evs = sem.getEvaluations();
            evs.getEvaluators().clear();
            DefaultTableModel model = (DefaultTableModel) sem.getRankTable().getModel();
            int rowCount = model.getRowCount();
            for (int currentRow = 0; currentRow < rowCount; currentRow++) {
                String fieldName = (String) model.getValueAt(currentRow, 1);
                String fieldCondition = (String) model.getValueAt(currentRow, 2);
                String fieldRankValue = (String) model.getValueAt(currentRow, 3);
                String rankDuration = (String) model.getValueAt(currentRow, 4);
                String durationValue = (String) model.getValueAt(currentRow, 5);
                String startYear = (String) model.getValueAt(currentRow, 6);
                String endYear = (String) model.getValueAt(currentRow, 7);
                String rank = (String) model.getValueAt(currentRow, 8);
                try {
                    RankEvaluator ev = new RankEvaluator(fieldName, fieldCondition, fieldRankValue, Double.parseDouble(rank));
                    if (durationValue != null && durationValue.length() > 0) {
                        ev.setDurationCondition(rankDuration);
                        ev.setDuration(Double.parseDouble(durationValue));
                    }
                    if (startYear != null && startYear.length() > 0) {
                        ev.setStartYear(Integer.parseInt(startYear));
                    }
                    if (endYear != null && endYear.length() > 0) {
                        ev.setEndYear(Integer.parseInt(endYear));
                    }
                    evs.addRule(ev);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(evs);
        } catch (Exception e) {
            LogGui.printException(e);
            return;
        }
        try {
            fout.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }

    }

    /**
     * Gestisce il caricamento delle condizioni di rank
     *
     * @param sem gui
     */
    public static void loadRank(SemGui sem) {
        GuiUtils.clearTable(sem.getRankTable());
        RankEvaluations evs = sem.getEvaluations();
        evs.getEvaluators().clear();
        FileInputStream finp;
        try {
            String path = sem.getPercorsoIndice().getText() + "/evaluations.rank";
            finp = new FileInputStream(new File(path));
        } catch (Exception e) {
            LogGui.printException(e);
            return;
        }
        DefaultTableModel model = (DefaultTableModel) sem.getRankTable().getModel();
        try {
            ObjectInputStream oos = new ObjectInputStream(finp);
            RankEvaluations evsNew = (RankEvaluations) oos.readObject();
            if (evsNew != null) {
                for (RankEvaluator ev : evsNew.getEvaluators()) {
                    evs.addRule(ev);
                    Object[] row = new Object[9];
                    row[0] = String.valueOf(System.currentTimeMillis());
                    row[1] = String.valueOf(ev.getField());
                    row[2] = String.valueOf(ev.getFieldConditionOperator());
                    row[3] = String.valueOf(ev.getFieldConditionValue());
                    row[4] = ev.getDurationCondition() != null ? String.valueOf(ev.getDurationCondition()) : null;
                    row[5] = ev.getDuration() != 0 ? String.valueOf(ev.getDuration()) : "";
                    row[6] = ev.getStartYear() != 0 ? String.valueOf(ev.getStartYear()) : "";
                    row[7] = ev.getEndYear() != 0 ? String.valueOf(ev.getEndYear()) : "";
                    row[8] = String.valueOf(ev.getScore());
                    model.addRow(row);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            LogGui.printException(e);
            return;
        }
        try {
            finp.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }

    }

}
