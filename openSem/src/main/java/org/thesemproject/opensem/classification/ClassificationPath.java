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
package org.thesemproject.opensem.classification;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 *
 * @author Administrator
 */
public class ClassificationPath implements Serializable {

    /**
     * Costante che identifica una classificazione fatta attraverso il
     * classificatore Bayesiano (Lucene)
     */
    public static final String BAYES = "Bayes";

    /**
     * Costante che identifica una classificazione fatta attraverso il
     * classificatore Key nearest neighbour (Lucene)
     */
    public static final String KNN = "Knn";
    String technology;
    double[] score;
    String[] path;

    /**
     * Istanzia l'oggetto ClassificationPath vuoto
     *
     * @param technology tecnologia utilizzata
     */
    public ClassificationPath(String technology) {
        this.technology = technology;
        this.score = new double[4];
        this.path = new String[4];
    }

    /**
     * Aggiunge un risultato di classificazione
     *
     * @param nodeName Nome del nodo (categoria) su cui l'oggetto è classificato
     * @param score valore associato alla classificazione
     * @param level livello di classificazione (da 3 a 0). O è il livello base 3
     * è la foglia
     */
    public void addResult(String nodeName, double score, int level) {
        if (level < 0 || level > 3) {
            return;
        }
        this.score[level] = score;
        this.path[level] = nodeName;
    }

    /**
     * Ritorna il nome del nodo ad un determinato livello
     *
     * @param level livello del nodo
     * @return nome del nodo
     */
    public String getNodeName(int level) {
        return this.path[level];
    }

    /**
     *
     * @param level
     * @return
     */
    public double getNodeScore(int level) {
        return this.score[level];
    }

    /**
     * Ritorna la tecnologia con cui è stata fatta la classificazione
     *
     * @return tecnologia utilizzata
     */
    public String getTechnology() {
        return technology;
    }

    /**
     * Ritorna gli score come array di double. Il path si legge da sinistra
     * verso destra. A sinistra i livello più alto (indice 0) a destra la foglia
     *
     * @return array degli score
     */
    public double[] getScore() {
        return score;
    }

    /**
     * Ritorna il path di classificazione. IL path si legge da sinistra verso
     * destra. A sinistra il livello più alto nell'albero (indice 0) a destra la
     * foglia. Una classificazione 
     * 
     * PIPPO &gt; PLUTO >&gt; TOPOLINO &gt; MINNIE 
     * 
     * viene memorizzata come 
     * 
     * {"PIPPO", "PLUTO", "TOPOLINO", "MINNIE"}
     *
     * @return path
     */
    public String[] getPath() {
        return path;
    }

    /**
     * Formattazione per i numeri decimali
     */
    public static DecimalFormat df = new DecimalFormat("#.###");

    /**
     * Ritorna la rappresentazione stringa del percorso di classificazione
     * Questa rappresentazione visualizza la tecnologia utilizzata, i nodi e il
     * relativo punteggio per ogni nodo
     *
     * @return rappresentazione stringa della classificazione
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(technology).append(": ");
        for (int i = 0; i < 4; i++) {
            String node = path[i];
            if (node != null) {
                if (i != 0) {
                    sb.append(" > ");
                }
                sb.append(node).append(" (").append(df.format(score[i])).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Ritorna la rappresentazione stringa della classificazione senza mostrare
     * la tecnologia utilizzata
     *
     * @return rappresentazione stringa della classificazione
     */
    public String toSmallString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String node = path[i];
            if (node != null) {
                if (i != 0) {
                    sb.append(">");
                }
                sb.append(node).append(" (").append(df.format(score[i])).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Ritorna la rappresentazione della classificazione senza mostrare
     * tecnologia utilizzata e score per ogni livello
     *
     * @return rappresentazione stringa della classificazione
     */
    public String toSmallClassString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String node = path[i];
            if (node != null) {
                if (i != 0) {
                    sb.append(">");
                }
                sb.append(node);
            }
        }
        return sb.toString();
    }

    /**
     * Ritorna la categoria più profonda di classificazione (Ultimo livello
     * popolato)
     *
     * @return foglia su cui è classificato l'item
     */
    public String getLeaf() {
        String ret = new String();
        for (int i = 0; i < 4; i++) {
            String node = path[i];
            if (node != null) {
                ret = node;
            }
        }
        return ret;
    }

}
