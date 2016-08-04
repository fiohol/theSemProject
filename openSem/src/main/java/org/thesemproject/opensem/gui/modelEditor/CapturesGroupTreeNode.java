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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.segmentation.CaptureConfiguration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.jdom2.Element;

/**
 *
 * Gestisce un nodo dell'albero del modello per rappresentare un gruppo di
 * catture
 *
 * @since 1.2
 */
public class CapturesGroupTreeNode extends ModelTreeNode {

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome della cattura
     */
    public CapturesGroupTreeNode(String nodeName) {
        super(nodeName, TYPE_CAPTURE_GROUP);
    }

    /**
     * Istanzia un nodo a clonando un vecchio nodo
     *
     * @param nodeName nome della cattura
     * @param cloned nodo da clonare
     */
    public CapturesGroupTreeNode(String nodeName, CapturesGroupTreeNode cloned) {
        super(nodeName, TYPE_CAPTURE_GROUP);
    }

    
    /**
     * Ritorna la rappresentazione XML della cattura per essere salvata nel file
     * di configurazione segments.xml
     *
     * @return Elemento XML
     */
    public Element getXmlElement() {
        Element capturesGroup = new Element("cg");
        capturesGroup.setAttribute("n", nodeName);
        return capturesGroup;
    }

    
}
