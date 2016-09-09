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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gestisce la configurazione di una formula
 *
 * @since 1.3
 *
 * @author The Sem Project
 */
public class FormulaConfiguration implements Serializable {

    private final Set<String> captures;
    private String formatPattern;
    private boolean actBeforeEnrichment;
    private String name;

    /**
     * Costruttore della classe
     *
     * @param formulaName nome della formula
     * @param formatPattern pattern di formattazione
     * @param actBeforeEnrichment true se deve essere eseguita prima
     * dell'enrichment
     */
    public FormulaConfiguration(String formulaName, String formatPattern, boolean actBeforeEnrichment) {
        name = formulaName;
        this.formatPattern = formatPattern;
        this.actBeforeEnrichment = actBeforeEnrichment;
        captures = new LinkedHashSet<>();
    }

    /**
     * Aggiunge una cattura come parametro
     *
     * @param value nome della cattura
     */
    public void addCapture(String value) {
        captures.add(value);
    }

    /**
     * Ritorna il pattern di formattazione
     *
     * @return pattern di formattazione
     */
    public String getFormatPattern() {
        return formatPattern;
    }

    /**
     * Imposta il pattern di formattazione
     *
     * @param formatPattern pattern di formattazione
     */
    public void setFormatPattern(String formatPattern) {
        this.formatPattern = formatPattern;
    }

    /**
     * Controlla se deve agire prima delle normalizzazioni
     *
     * @return true se deve agire prima della normalizzazione
     */
    public boolean isActBeforeEnrichment() {
        return actBeforeEnrichment;
    }

    /**
     * Imposta quando deve agire la formula
     *
     * @param actBeforeEnrichment true se prima dell'enrichment
     */
    public void setActBeforeEnrichment(boolean actBeforeEnrichment) {
        this.actBeforeEnrichment = actBeforeEnrichment;
    }

    /**
     * Nome della formula
     *
     * @return ritorna il nome della forumla.
     */
    public String getName() {
        return name;
    }

    /**
     * Imposta il nome della formula
     *
     * @param name nome della formula
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Applica la formula ad un risultato di segmentazione
     *
     * @param sr SegmentationResult
     * @param capturesIndex indice delle catture
     */
    public void applyFormula(SegmentationResults sr, Map<String, CaptureConfiguration> capturesIndex) {
        Object[] parameters = new Object[captures.size()];
        int i = 0;
        for (String capture : captures) {
            String captureValue = sr.getCaptureResults().get(capture);
            parameters[i++] = captureValue;
        }
        sr.addCaptureResult(capturesIndex.get(getName()), String.format(formatPattern, parameters), true);
    }

    /**
     * Costruisce una configurazione di cattura a partire da una formula (perché
     * la formula è pur sempre una cattura)
     *
     * @return Configurazione di cattura
     */
    public CaptureConfiguration getCaptureConfigurations() {
        return new CaptureConfiguration(name, "text", "", false, false, false, false);
    }

}
