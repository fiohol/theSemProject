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
package org.thesemproject.opensem.segmentation;

import org.thesemproject.opensem.classification.ClassificationPath;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configurazione di una cattura
 */
public class CaptureConfiguration implements Serializable {

    List<CapturePattern> patterns;
    String name;
    String type;
    String format;
    boolean temporary;
    boolean notSubscribe;
    boolean startPeriod;
    boolean endPeriod;
    List<CaptureConfiguration> subCaptures;
    Set<String> enabledSegments;
    Set<String> blockedCaptures;
    private ClassificationPath classificationPath;

    /**
     * Crea una nuova cattura
     *
     * @param name nome della cattura
     * @param type tipo della cattura
     * @param format formattazione della cattura
     * @param temporary true se la cattura è temporanea (e deve essere scartata)
     * @param startPeriod true se la cattura rappresenta una data di inizio
     * periodo
     * @param endPeriod true se la cattura rappresenta una data di fine periodo
     * @param notSubsctibe true se non deve sovrascrivere;
     */
    public CaptureConfiguration(String name, String type, String format, boolean temporary, boolean startPeriod, boolean endPeriod, boolean notSubsctibe) {
        this.name = name;
        this.type = type;
        this.format = format;
        if (this.format == null) {
            this.format = "";
        }
        this.patterns = new ArrayList<>();
        this.subCaptures = new ArrayList<>();
        this.enabledSegments = new HashSet<>();
        this.classificationPath = null;
        this.temporary = temporary;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.blockedCaptures = new HashSet<>();
        this.notSubscribe = notSubsctibe;
    }

    /**
     * Ritorna l'elenco delle catture bloccate
     *
     * @since 1.4
     * @return elenco catture bloccate
     */
    public Set<String> getBlockedCaptures() {
        return blockedCaptures;
    }

    /**
     * Ritorna la formattazione di una cattura. Informazione disponibili
     * all'indirizzoq
     * https://docs.oqracle.com/javase/7/docs/api/java/util/Formatter.html. Il
     * format può essere anche un valore assoluto se si vuole normalizzre una
     * cattura
     *
     * @return formattazione di una cattura, nel formato applicabile da
     * String.format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Imposta il format della cattura
     *
     * @param format format secondo quanto spiegato in Informazione disponibili
     * all'indirizzo
     * https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html,
     * oppure Valore assoluto se si vuole normalizzare una cattura
     */
    public void setFormat(String format) {
        if (format != null) {
            this.format = format;
        }
    }

    /**
     * Aggiunge un pattern di cattura alla cattura Ogni cattura può avere più
     * pattern per la sua realizzazione, di fatto tanti quanti sono i modi in
     * cui una frase può essere scritta
     *
     * @param pattern pattern di cattura
     */
    public void addCapturePattern(CapturePattern pattern) {
        this.patterns.add(pattern);
    }

    /**
     * Aggiunge una cattura come sottocattura di una cattura esistente. Una
     * sottocattura lavora sul valore estratto dalla cattura padre. Si possono
     * far lavorare più catture in cascata. Questa modalità permette di:
     * combinare catture semplici per eseguire passi complessi, rendendo più
     * chiaro il processo, estrarre informazioni passo per passo
     *
     * @param captureConfiguration configurazione della cattura figlio
     */
    public void addSubCapture(CaptureConfiguration captureConfiguration) {
        this.subCaptures.add(captureConfiguration);
    }

    /**
     * Ritorna la lista di tutti i pattern utilizzati per la cattura
     *
     * @return lista dei pattern
     */
    public List<CapturePattern> getPatterns() {
        return patterns;
    }

    /**
     * Ritorna il nome della cattura
     *
     * @return nome della cattura
     */
    public String getName() {
        return name;
    }

    /**
     * Imposta il nome della cattura
     *
     * @param name nome della cattura
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Ritorna il tipo della cattura
     *
     * @return tipo della cattura
     */
    public String getType() {
        return type;
    }

    /**
     * Imposta il tipo della cattura
     *
     * @param type tipo della cattura
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Ritorna l'elenco delle sottocatture (di primo livello) che una cattura
     * ha. Se la sottocattura a sua volta ha dei figli, questi devono essere
     * richiesti alla sottocattura
     *
     * @return lista delle sottocatture figlie
     */
    public List<CaptureConfiguration> getSubCaptures() {
        return subCaptures;
    }

    /**
     * Aggiunge un segmento dove la cattura può agire. Una cattura, infatti, può
     * essere posta come globale su più segmenti oppure come figlia di un
     * segmento Se la cattura è globale è necessario specificare in quali
     * segmenti agisce
     *
     * @param segName nome del segmento
     */
    public void addEnabledSegment(String segName) {
        enabledSegments.add(segName);
    }

    /**
     * Controlla se il segmento può eseguire la cattura
     *
     * @param segName nome del segmento
     * @return true se la cattura attiva sul segmento
     */
    public boolean isSegmentEnabled(String segName) {
        return enabledSegments.contains(segName);
    }

    /**
     * Imposta il path di classificazione di una cattura Questa funzionalità è
     * utile se si vuole che a fronte del successo di una cattura si vada a
     * classificare su una categoria indicata nel classification path
     *
     * @param cp classification path
     */
    public void setClassificationPath(ClassificationPath cp) {
        this.classificationPath = cp;
    }

    /**
     * Ritorna il classificationPath associato ad una cattura
     *
     * @return classificationPath
     */
    public ClassificationPath getClassificationPath() {
        return classificationPath;
    }

    /**
     * Verifica se una cattura è temporanea
     *
     * @return true se la cattura è temporanea
     */
    public boolean isTemporary() {
        return temporary;
    }

    /**
     * Imposta una cattura come temporanea
     *
     * @param temporary true se la cattura è da considerarsi temporanea
     */
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    /**
     * Verifica se la cattura è una cattura di inizio periodo
     *
     * @return true se la cattura è una cattura di inizio periodo
     */
    public boolean isStartPeriod() {
        return startPeriod;
    }

    /**
     * Verifica se la cattura è una cattura di fine periodo
     *
     * @return true se la cattura è una cattura di fine periodo
     */
    public boolean isEndPeriod() {
        return endPeriod;
    }

    /**
     * Imposta la cattura come cattura di inizio periodo
     *
     * @param startPeriod true se è inizio periodo
     */
    public void setStartPeriod(boolean startPeriod) {
        this.startPeriod = startPeriod;
    }

    /**
     * Imposta la cattura come cattura di fine periodo
     *
     * @param endPeriod true se è fine periodo
     */
    public void setEndPeriod(boolean endPeriod) {
        this.endPeriod = endPeriod;
    }

    /**
     * Aggiunge una cattura da bloccare
     *
     * @since 1.4
     * @param blocked cattura da bloccare
     */
    public void addBlockedCapture(String blocked) {
        this.blockedCaptures.add(blocked);
    }

    /**
     * Verifica se la cattura non deve sovrascrivere
     *@since 1.4
     * @return true se non deve sovrascrivere
     */
    public boolean isNotSubscribe() {
        return this.notSubscribe;
    }

    /**
     * Imposta se la cattura non deve sovrascrivere
     * @param notSubscribe true se non deve sovrascrivere
     */
    public void setNotSubscribe(boolean notSubscribe) {
        this.notSubscribe = notSubscribe;
    }
    
    
}
