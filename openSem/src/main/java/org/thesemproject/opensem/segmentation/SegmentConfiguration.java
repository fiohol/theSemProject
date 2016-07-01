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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Gestisce la configurazione di un segmento. Un segmento è una porzione di
 * documento identificato da dei pattern In una legge, ad esempio può essere un
 * articolo, in un messaggio di posta il testo, in un articolo universitario
 * l'abstract o un capitolo
 */
public class SegmentConfiguration implements Serializable {

    String name;
    boolean multiple;
    List<Pattern> patternList;
    List<SegmentConfiguration> segments;
    List<CaptureConfiguration> captureConfigurations;
    List<CaptureConfiguration> sentenceCaptureConfigurations;
    List<DataProviderRelationship> relationships;
    boolean isDefault;
    boolean classify;

    /**
     * Ritorna se un segmento è in grado di classificare
     *
     * @return true se il segmento deve classificare
     */
    public boolean isClassify() {
        return classify;
    }

    /**
     * Imposta se il segmento deve classificare
     *
     * @param classify true se si vuole che il segmento classifichi
     */
    public void setClassify(boolean classify) {
        this.classify = classify;
    }

    /**
     * Crea una configurazione di segmento
     *
     * @param name nome del segmento
     * @param multiple se a true indica che il segmento è multiplo nel
     * documento. Ad esempio una legge è fatta da 1 o più articoli, un testo da
     * uno o più capitoli
     * @param isDefault se a true indica che questo è il segmento di default. In
     * pratica all'inizio della segmentazione il sistema dirà che tutto quello
     * che incontra, prima dell'inizio di un nuovo segmento (che è identificato
     * dai pattern di segmento) appartiene al segmento di default.
     */
    public SegmentConfiguration(String name, boolean multiple, boolean isDefault) {
        configure(name, multiple, isDefault, false);
    }

    /**
     * Crea una configurazione di segmento
     *
     * @param name nome del segmento
     * @param multiple se a true indica che il segmento è multiplo
     * @param isDefault se a true indica che il segmento è di default
     * @param classify se a true indica che il segmento classifica
     */
    public SegmentConfiguration(String name, boolean multiple, boolean isDefault, boolean classify) {
        configure(name, multiple, isDefault, classify);
    }

    private void configure(String name, boolean multiple, boolean isDefault, boolean classify) {
        this.name = name;
        this.multiple = multiple;
        this.patternList = new ArrayList<>();
        this.segments = new ArrayList<>();
        this.captureConfigurations = new ArrayList<>();
        this.sentenceCaptureConfigurations = new ArrayList<>();
        this.isDefault = isDefault;
        this.classify = classify;
        this.relationships = new ArrayList<>();
    }

    /**
     * Ritorna true se il segmento è di default
     *
     * @return true se di default, false altrimenti
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Imposta il comportamento del segmento
     *
     * @param isDefault se true è di default
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Ritorna il nome del segmento
     *
     * @return nome del segmento
     */
    public String getName() {
        return name;
    }

    /**
     * Imposta il nome del segmento
     *
     * @param name nome del segmento
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Dice se il segmento è multiplo
     *
     * @return true se il segmento è multiplo
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Imposta la molteplicità del segmento
     *
     * @param multiple true se il segmento è multiplo
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * Ritorna la lista di pattern (regex) che identificano l'inizio di un
     * segmento. Un segmento finisce quando ne inizia uno nuovo. Il segmento di
     * default inizia con l'inizio del documento
     *
     * @return lista di patterns.
     */
    public List<Pattern> getPatternList() {
        return patternList;
    }

    /**
     * Imposta la lista dei pattern che identificano un documento
     *
     * @param patternList lista dei patterns
     */
    public void setPatternList(List<Pattern> patternList) {
        this.patternList = patternList;
    }

    /**
     * Aggiunge un pattern ad un segmento
     *
     * @param pattern pattern da aggiungere
     */
    public void addPattern(Pattern pattern) {
        patternList.add(pattern);
    }

    /**
     * Ritorna le configurazioni che identificano i sotto segmenti
     *
     * @return lista dei sottosegmenti
     */
    public List<SegmentConfiguration> getSegments() {
        return segments;
    }

    /**
     * Aggiunge i sotto segmenti ad un segmento
     *
     * @param segments lista dei sottosegmenti
     */
    public void setSegments(List<SegmentConfiguration> segments) {
        this.segments = segments;
    }

    /**
     * Aggiunge un sotto segmento ad un segmento. I sottosegmenti agiscono sul
     * segmento identificato. Molto spesso l'organizzazione dei documenti è
     * gerarchica. Se si pensa all'esempio dell'articolo scientifico, il
     * segmento può essere l'abstract, un'altro segmento può essere il capitolo
     * (multiplo) e il capitolo può avere (multipli) uno o più paragrafi come
     * sottosegmenti
     *
     * @param subSegment configurazione del sottosegmento
     */
    public void addSegment(SegmentConfiguration subSegment) {
        this.segments.add(subSegment);
    }

    /**
     * Aggiunge un pattern di cattura al segmento. Le catture agiranno a livello
     * del testo di segmento
     *
     * @param captureConfiguration cattura da aggiungere al segmento
     */
    public void addCapture(CaptureConfiguration captureConfiguration) {
        this.captureConfigurations.add(captureConfiguration);
    }

    /**
     * Ritorna la configurazione delle catture relative al segmento
     *
     * @return catture di segmento
     */
    public List<CaptureConfiguration> getCaptureConfigurations() {
        return captureConfigurations;
    }

    /**
     * Ritorna la configurazione delle catture su Sentenza (cioè che agiscono su
     * tutto il testo del segmento e non su ogni singola frase)
     *
     * @return catture di sentenza del segmento
     */
    public List<CaptureConfiguration> getSentenceCaptureConfigurations() {
        return sentenceCaptureConfigurations;
    }

    /**
     * Aggiunge una cattura di sentenza
     *
     * @param captureConfiguration cattura
     */
    public void addSentenceCapture(CaptureConfiguration captureConfiguration) {
        this.sentenceCaptureConfigurations.add(captureConfiguration);
    }

    /**
     * Aggiunge una relazione tra segmento e dataprovider
     *
     * @param relationships relazioni
     */
    public void addRelationships(List<DataProviderRelationship> relationships) {
        if (relationships != null) {
            this.relationships.addAll(relationships);
        }
    }

    /**
     * Ritorna le relazioni
     *
     * @return lista delle relazioni
     */
    public List<DataProviderRelationship> getRelationships() {
        return this.relationships;
    }

}
