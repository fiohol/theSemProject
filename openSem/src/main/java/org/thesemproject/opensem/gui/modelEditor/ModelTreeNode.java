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

import javax.swing.tree.DefaultMutableTreeNode;
import org.thesemproject.opensem.utils.interning.InternPool;

/**
 *
 * Oggetto base che rappresenta tutti i nodi dell'albero del model editor
 */
public class ModelTreeNode extends DefaultMutableTreeNode {

    /**
     * Tipo nodo root
     */
    public static int TYPE_ROOT = 0;

    /**
     * Tipo nodo dizionario
     */
    public static int TYPE_DICTIONARY = 1;

    /**
     * Tipo nodo segmento (unico nodo in tutto l'albero)
     */
    public static int TYPE_SEGMENT = 2;

    /**
     * Tipo nodo definizione di segmento
     */
    public static int TYPE_SEGMENT_DEFINITION = 3;

    /**
     * Tipo nodo table (unico nodo in tutto l'albero)
     */
    public static int TYPE_TABLE = 4;

    /**
     * Tipo nodo definizione di tabella
     */
    public static int TYPE_TABLE_DEFINITION = 5;

    /**
     * Tipo nodo cattura (un nodo in ogni segmento e un nodo per le catture
     * globali)
     */
    public static int TYPE_CAPTURE = 6;

    /**
     * Tipo nodo definizione di cattura
     */
    public static int TYPE_CAPTURE_DEFINITION = 7;

    /**
     * Tipo nodo dataproviders (un nodo in tutto l'albero)
     */
    public static int TYPE_DATA_PROVIDERS = 8;

    /**
     * Tipo nodo dataprovider definition
     */
    public static int TYPE_DATA_PROVIDER_DEFINITION = 9;

    /**
     * Tipo nodo dataprovider relationship (uno o più per ogni dataprovider)
     */
    public static int TYPE_DATA_PROVIDER_RELATIONSHIP = 10;

    /**
     * Tipo nodo gruppo di cattura (uno o più nodi figli di TYPE_CAPTURE che può
     * contenere CAPTURE_DEFINITION)
     *
     * @since 1.2
     *
     */
    public static int TYPE_CAPTURE_GROUP = 11;
    
    /**
     * Tipo nodo formula definition
     *
     * @since 1.3
     *
     */
    public static int TYPE_FORMULA_DEFINITION = 12;
    
    /**
     * Tipo nodo formula 
     *
     * @since 1.3
     *
     */
    public static int TYPE_FORMULA = 13;

    /**
     * Pool interno
     */
    protected static InternPool intern = new InternPool();

    /**
     * Nome del nodo
     */
    protected String nodeName;

    /**
     * Tipo del nodo
     */
    protected final int nodeType;

    /**
     * Crea il nodo
     *
     * @param nodeName nome del nodo
     * @param nodeType tipo del nodo
     */
    public ModelTreeNode(String nodeName, int nodeType) {
        super(intern.intern(nodeName));
        this.nodeName = (String) intern.intern(nodeName);
        this.nodeType = nodeType;
    }

    /**
     * Ritorna il nome del nodo
     *
     * @return nome del nodo
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Ritorna il tipo del nodo
     *
     * @return tipo del nodo
     */
    public int getNodeType() {
        return nodeType;
    }

    /**
     * Imposta il nome del nodo
     *
     * @param nodeName nome del nodo
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

}
