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

import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeNode;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

/**
 * Implementa il dialog con scrollbar da mostrare mentre un file csv impostato
 * come data provider viene indicizzato
 *
 */
public class BurnToStorageDialog extends AbstractAction {

    SemGui sem;

    /**
     *
     * @param name Nome della finestra di dialogo
     * @param semGui semGui (Frame di gui principale che apre la finestra)
     */
    public BurnToStorageDialog(String name, SemGui semGui) {
        super(name);
        this.sem = semGui;
    }

    /**
     * Gestisce l'action sul dialog
     * @param evt evento
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        SwingWorker<Void, Void> mySwingWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // mimic some long-running process here...
                DataProviderTreeNode node = (DataProviderTreeNode) sem.getModelEditor().getCurrentNode();
                if (node != null) {
                    node.burnToStorage();
                }
                return null;
            }
        };

        Window win = SwingUtilities.getWindowAncestor((AbstractButton) evt.getSource());
        final JDialog dialog = new JDialog(win, "Indicizzazione dataprovider", ModalityType.APPLICATION_MODAL);

        mySwingWorker.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state")) {
                    if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
                        dialog.dispose();
                    }
                }
            }
        });
        mySwingWorker.execute();
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(new JLabel("Indicizzazione in corso. Attendere..."), BorderLayout.PAGE_START);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(win);
        dialog.setVisible(true);
    }
}
