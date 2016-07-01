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

import org.thesemproject.opensem.tagcloud.TagClass;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import java.awt.Color;
import java.awt.LayoutManager;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

/**
 *
 * Pannello per il tag clouding
 */
public class TagCloudPanel extends JPanel {

    /**
     * Istanzia il pannello
     *
     * @param layout layout manager
     * @param isDoubleBuffered true se è double buffered
     */
    public TagCloudPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);

    }

    /**
     * Istanzia il pannello
     *
     * @param layout layout manager
     */
    public TagCloudPanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Istanzia il pannello
     *
     * @param isDoubleBuffered true se double buffered
     */
    public TagCloudPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    /**
     * Istanzia il pannello
     */
    public TagCloudPanel() {
        super();
    }

    /**
     * Imposta i risultati
     *
     * @param result risultati
     * @param count numero parole
     */
    public void setResult(TagCloudResults result, int count) {
        setBackground(new java.awt.Color(255, 255, 255));
        int size = count;
        if (size < 150) {
            size = 150;
        }
        Cloud cloud = result.getCloud(count);
        for (Tag tag : cloud.tags()) {
            final JLabel label = new JLabel(tag.getName());
            TagClass tc = result.getTagClass(tag);
            Set<String> docIds = tc.getDocumentsId();
            label.setToolTipText(tc.getWordsString());
            //Todo label cliccabili e filtro sui documenti...
            if (docIds.size() > 1) {
                //DO nothing...

            }
            label.setOpaque(false);
            label.setFont(label.getFont().deriveFont((float) tag.getWeight() * (size / 150) * 20));
            label.setForeground(new Color(0, (int) (255 * tag.getNormScore()), 255));
            add(label);
        }
    }

}
