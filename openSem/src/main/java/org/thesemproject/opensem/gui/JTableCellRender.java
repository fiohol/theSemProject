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
package org.thesemproject.opensem.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;

/**
 * Gestisce il rendering di una cella per permetterne l'evidenziazione dei
 * termini che matchano una ricerca. Classe copiata da un esempio su
 * Stackoverflow
 */
public class JTableCellRender extends DefaultTableCellRenderer {

    private final JTextField f;
    private final String textForSearch;

    /**
     * Inizializza il render
     *
     * @param textForSearch testo ricercato
     */
    public JTableCellRender(String textForSearch) {
        this.f = new JTextField() {
            @Override
            public void setBorder(Border border) {
                // No!
            }
        };
        if (textForSearch == null) {
            textForSearch = "";
        }
        this.textForSearch = textForSearch.toLowerCase();
    }

    /**
     * Riscrive il componente evidenziando il testo ricercato
     *
     * @param table tabella
     * @param value valore
     * @param isSelected true se è selezionato
     * @param hasFocus true se la cella ha il focus
     * @param row numero di riga
     * @param column numero di colonna
     * @return
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        f.getHighlighter().removeAllHighlights();
        if (value != null) {
            f.setText(value.toString());
            String string = value.toString().toLowerCase();

            if (string.contains(textForSearch)) {
                int indexOf = string.indexOf(textForSearch);
                try {
                    f.getHighlighter().addHighlight(indexOf, indexOf + textForSearch.length(), new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
        } else {
            f.setText("");
            f.getHighlighter().removeAllHighlights();
        }
        f.setBackground(cellComponent.getBackground());

        return f;
    }
}
