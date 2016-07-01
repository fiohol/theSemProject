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
package org.thesemproject.opensem.segmentation.functions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Elemento per la conservazione dei dati di durata di un segmento
 */
public class Durations {

    private double duration;
    private int durationYears;
    Set<String> years;
    Set<String> months;
    private boolean isEndYearExtracted;

    /**
     * Crea un elemento Duration per un segmentationResult
     */
    public Durations() {
        duration = 0;
        durationYears = 0;
        years = new HashSet<>();
        months = new HashSet<>();
        isEndYearExtracted = true;
    }

    /**
     * Imposta la duration in anni
     *
     * @param duration duration anni
     */
    public void setDurationYears(double duration) {
        this.duration = duration;
    }

    /**
     * Imposta la duration in anni in valore assoluto
     *
     * @param intDurationYears durata in anni (numero di anni)
     */
    public void setIntDurationYears(int intDurationYears) {
        durationYears = intDurationYears;
    }

    /**
     * Imposta gli anni di durata
     *
     * @param years anni di durata
     */
    public void setYears(String[] years) {
        this.years.addAll(Arrays.asList(years));
    }

    /**
     * Imposta i mesi di durata
     *
     * @param months mesi di durata
     */
    public void setMonths(String[] months) {
        this.months.addAll(Arrays.asList(months));
    }

    /**
     * Ritorna la durata in anni
     *
     * @return durata in anni (valore decimale)
     */
    public double getDurationYears() {
        return duration;
    }

    /**
     * Ritorna la durata in assoluto in anni (anno fine - anno inizio + 1)
     *
     * @return durata in anni
     */
    public int getIntDurationYears() {
        return durationYears;
    }

    /**
     * Ritorna l'insieme degli anni della durata
     *
     * @return insieme degli anni
     */
    public Set<String> getYears() {
        return years;
    }

    /**
     * Ritorna l'insieme dei mesi
     *
     * @return mesi
     */
    public Set<String> getMonths() {
        return months;
    }

    /**
     * Aggiorna una durata aggiungendo una durata. Si supponga che si stia
     * misurando una particolare coppia NomeAttributo, ValoreAttributo. Se
     * questa coppia viene rilevata in più di un segmento le durate si devono
     * sommare o comunque concatenare. Questo metodo permette di addizionare una
     * durata di un segmento alla durata di un altro
     *
     * @param srDuration duration
     */
    public void update(Durations srDuration) {
        if (srDuration == null) {
            return;
        }
        duration += srDuration.duration;
        durationYears += srDuration.durationYears;
        years.addAll(srDuration.years);
        months.addAll(srDuration.months);
        isEndYearExtracted = isEndYearExtracted && srDuration.isEndYearExtracted;
    }

    /**
     * Imposta true se l'anno di fine è stato estratto o è stata usata la data
     * odierna per calcolare la durata.
     *
     * @param isEndYearExtracted true se l'anno di fine è stato estratto
     */
    public void setIsEndYearExtracted(boolean isEndYearExtracted) {
        this.isEndYearExtracted = isEndYearExtracted;
    }

    /**
     * Ritorna true se l'anno di fini è stato estratto
     *
     * @return true se anno di fine è stato estratto
     */
    public boolean isIsEndYearExtracted() {
        return isEndYearExtracted;
    }

}
