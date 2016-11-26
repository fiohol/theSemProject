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
package org.thesemproject.opensem.gui.modelEditor;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author The Sem Project
 */
public class MyCellRenderer extends DefaultTreeCellRenderer {

    /**
     * Fa il rendering di un nodo dell'albero
     * @param tree albero
     * @param value nodo
     * @param sel true se selezionato
     * @param exp true secondo sopra
     * @param leaf true se foglia
     * @param row nuemero riga
     * @param hasFocus true se ha fuocus
     * @return 
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);

        if (value instanceof CaptureTreeNode) {
            CaptureTreeNode ctn = (CaptureTreeNode) value;
            if (ctn.isIsOrphan()) { //Rosso se punta ad un nodo non esistente
                setForeground(Color.RED);
            } else if (ctn.isPointToNotBayes()) { //Rosa se punta ad un nodo non istruito
                setForeground(Color.PINK);
            }
        } else if (value instanceof ClassicationTreeNode) {
            ClassicationTreeNode ctn = (ClassicationTreeNode) value;
            if (!ctn.isTrained()) { //Blue scuro se non istruito
                setForeground(Color.BLUE);
            }
        }

        return this;
    }
}
