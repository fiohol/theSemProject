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

/**
 *
 * @author The Sem Project
 */
public class ClassicationTreeNode extends DefaultMutableTreeNode {
    
    /**
     * Contine le info se il nodo è istruito
     */
    private boolean trained;

    /**
     * Crea il nodo
     * @param isTrained  true se istruito
     */
    public ClassicationTreeNode(boolean isTrained) {
        this.trained = isTrained;
    }

    /**
     * Crea il nodo
     * @param isTrained true se istruito
     * @param o oggetto del nodo
     */
    public ClassicationTreeNode(boolean isTrained, Object o) {
        super(o);
        this.trained = isTrained;
    }

    /**
     * Crea il nodo
     * @param isTrained true se istruito
     * @param o oggetto
     * @param bln vedi DefaultMutableTreeNode
     */
    public ClassicationTreeNode(boolean isTrained, Object o, boolean bln) {
        super(o, bln);
        this.trained = isTrained;
    }

    /**
     * 
     * @return ritorna true se istruito
     */
    public boolean isTrained() {
        return trained;
    }

    /**
     * Imposta lo stato di istruzione
     * @param isTrained true se istruito
     */
    public void setTrained(boolean isTrained) {
        this.trained = isTrained;
    }
    
    
    
}
