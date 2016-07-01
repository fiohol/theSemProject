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

import org.thesemproject.opensem.utils.FinalBoolean;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.segmentation.CaptureConfiguration;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Gestisce la mappa delle durate come {{NomeElemento, ValoreElemento},
 * Durations}
 */
public class DurationsMap {

    private final Map<Pair<String, String>, Durations> durations;
    private final Set<String> years;
    private final Set<String> months;
    private boolean isEndYearExtracted;

    private DurationsMap() {
        durations = new HashMap<>();
        years = new HashSet<>();
        months = new HashSet<>();
        isEndYearExtracted = true;
    }

    /**
     * Ritorna le chiavi della mappa.
     *
     * @return Lista delle chiavi della mappa ovvero la lista di {NomeElemento,
     * ValoreElemento}
     */
    public List<Pair<String, String>> keySet() {
        List<Pair<String, String>> keys = new ArrayList(durations.keySet());
        Collections.sort(keys, (Pair<String, String> p1, Pair<String, String> p2) -> {
            if (p1.getLeft().equals(p2.getLeft())) {
                return p1.getRight().compareTo(p2.getRight());
            }
            return p1.getLeft().compareTo(p2.getLeft());
        });
        return keys;
    }

    /**
     * Aggiorna la duration
     *
     * @param key coppia {NomeElemento, ValoreElemento}
     * @param srDuration duration
     */
    public void update(Pair key, Durations srDuration) {
        Durations values = durations.get(key);
        if (values == null) {
            values = new Durations();
        }
        values.update(srDuration);
        years.addAll(srDuration.years);
        months.addAll(srDuration.months);
        durations.put(key, values);
        isEndYearExtracted = isEndYearExtracted && srDuration.isIsEndYearExtracted();
    }

    /**
     * Ritorna la duration in anni per la chiave
     *
     * @param key {NomeElemento, ValoreElemento}
     * @return durata
     */
    public double getDurationYears(Pair<String, String> key) {
        return Math.round((durations.get(key).getDurationYears()) * 100.0) / 100.0;
    }

    /**
     * Ritorna la durata in anni (valore assoluto) per la chiave
     *
     * @param key {NomeElemento, ValoreElemento}
     * @return durata in anni
     */
    public int getIntDurationYears(Pair<String, String> key) {
        return durations.get(key).getIntDurationYears();
    }

    /**
     * Ritorna la lista degli anni per una chiave
     *
     * @param key {NomeElemento, ValoreElemento}
     * @return set degli anni
     */
    public Set<String> getYears(Pair<String, String> key) {
        return durations.get(key).getYears();
    }

    /**
     * Ritorna la lista dei mesi per una chiave
     *
     * @param key {NomeElemento, ValoreElemento}
     * @return set dei mesi
     */
    public Set<String> getMonths(Pair<String, String> key) {
        return durations.get(key).getMonths();
    }

    /**
     * Ritorna la lista degli anni per tutta la mappa
     *
     * @return lista di tutti gli anni
     */
    public List<String> getYearsList() {
        List<String> ret = new ArrayList<>();
        ret.addAll(years);
        Collections.sort(ret);
        return ret;
    }

    /**
     * Ritorna la lista dei mesi
     *
     * @return lista dei mesi
     */
    public List<String> getMonthsList() {
        List<String> ret = new ArrayList<>();
        ret.addAll(months);
        Collections.sort(ret);
        return ret;
    }

    /**
     * Costruisce la durationMap a partire da un risultato di una segmentazione
     *
     * @param identifiedSegments risultato della segmentazione
     * @return Mappa delle durate
     */
    public static DurationsMap getDurations(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) {
        List<DurationsMap> ll = getDurations(new ArrayList<>(), identifiedSegments, "", false, null);
        if (ll != null && ll.size() > 0) {
            return ll.get(0);
        }
        return new DurationsMap();
    }

    /**
     * Ritorna la durationMap per un particolare segmento
     *
     * @param identifiedSegments risultato della segmentazione
     * @param segmentNameFilter nome del segmento di cui si vogliono le durate
     * @return Lista delle mappe delle durate
     */
    public static List<DurationsMap> getDurationsBySegment(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String segmentNameFilter) {
        return getDurations(new ArrayList<>(), identifiedSegments, "", true, segmentNameFilter);
    }

    private static List<DurationsMap> getDurations(List<DurationsMap> durationsMap, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName, boolean bySegments, String segmentNameFilter) {
        if (identifiedSegments == null) {
            return durationsMap;
        }
        if (durationsMap == null) {
            durationsMap = new ArrayList<>();
        }

        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();

            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    durationsMap = getDurations(durationsMap, subSegments, segmentName, bySegments, segmentNameFilter);
                } else if ((bySegments && (segmentName.equalsIgnoreCase(segmentNameFilter))) || (!bySegments)) {
                    double duration = sr.getDurationYears();
                    if (duration > 0) {
                        DurationsMap durations;
                        if (!bySegments) {
                            if (durationsMap.size() == 0) {
                                durationsMap.add(new DurationsMap());
                            }
                            durations = durationsMap.get(0);
                        } else {
                            durations = new DurationsMap();
                        }
                        Map<CaptureConfiguration, String> captures = sr.getCaptureConfigurationResults();
                        if (!captures.isEmpty()) {
                            for (CaptureConfiguration cc : captures.keySet()) {
                                if (cc.isEndPeriod()) {
                                    continue;
                                }
                                if (cc.isStartPeriod()) {
                                    continue;
                                }
                                if (cc.isTemporary()) {
                                    continue;
                                }
                                if (cc.getType().equals("date")) {
                                    continue;
                                }
                                String value = captures.get(cc);
                                if (value.length() > 0) {
                                    Pair row = Pair.of(cc.getName(), value);
                                    durations.update(row, sr.getDurations());
                                }
                            }
                        }
                        if (s.isClassify()) {
                            List<ClassificationPath> bayes = sr.getClassificationPaths();
                            for (ClassificationPath cp : bayes) {
                                Pair row = Pair.of(CLASSIFICATIONS, cp.getLeaf());
                                durations.update(row, sr.getDurations());
                            }

                        }
                        if (bySegments) {
                            durationsMap.add(durations);
                        } else {
                            durationsMap.set(0, durations);
                        }
                    }
                }
            }
        }
        return durationsMap;
    }

    /**
     * Chiave per identificare il concetto di classificazione come primo
     * elemento della chiave (il secondo dovrà essere il valore della
     * classificazione)
     */
    public static final String CLASSIFICATIONS = "[Classifications]";

    /**
     * Ritorna true se l'annno di fine è stato estratto in tutte le esperienze
     * della mappa
     *
     * @return true se anno estratto
     */
    public boolean isIsEndYearExtracted() {
        return isEndYearExtracted;
    }

}
