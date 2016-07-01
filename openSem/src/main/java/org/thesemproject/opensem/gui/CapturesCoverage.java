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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestisce la rappresentazione grafica del converage delle catture
 *
 */
public class CapturesCoverage {

    final Map<String, Object[]> captureCoverage = new HashMap<>();
    final Map<String, Set<String>> captureValuesCount = new HashMap<>();
    final Map<String, Set<String>> captureFilesId = new HashMap<>();
    final Map<String, Set<String>> captureValuesFilesId = new HashMap<>();

    /**
     * Costruisce l'oggetto attraverso il tableCorpus. Il tableCorpus
     * rappresenta i documenti caricati nel primo pannello del SemGui
     *
     * @param tableCorpus documenti caricati in SemGui
     */
    public CapturesCoverage(Map<Integer, SemDocument> tableCorpus) {
        captureCoverage.clear();
        captureValuesCount.clear();
        captureFilesId.clear();
        captureValuesFilesId.clear();
        getCapturesCoverage(tableCorpus);
    }

    /**
     * Ritorna le righe con cui popolare la tabella di coverage
     *
     * @return lista di oggetti. Ogni elemento della lista è una riga della
     * tabella, ogni oggetto dell'array è una colonna
     */
    public List<Object[]> getTableRows() {
        return new ArrayList(captureCoverage.values());
    }

    /**
     * Ritorna l'elenco dei valori delle catture
     *
     * @param segment nome del segmento
     * @param capture nome della cattura
     * @return insieme dei valori
     */
    public Set<String> getValues(String segment, String capture) {
        return captureValuesCount.get(segment + "." + capture);
    }

    /**
     * Ritorna l'id dei documenti per una cattura
     *
     * @param segment segmento
     * @param capture cattura
     * @return Insieme di id
     */
    public Set<String> getIdsForCapture(String segment, String capture) {
        return captureFilesId.get(segment + "." + capture);
    }

    /**
     * Ritorna l'elenco degli id di documento in funzione di una cattura e del
     * suo valore
     *
     * @param segment nome del segmento
     * @param capture nome della cattura
     * @param value valore della cattura
     * @return insieme degli id dei documenti
     */
    public Set<String> getIdsForCaptureValue(String segment, String capture, String value) {
        return captureValuesFilesId.get(segment + "." + capture + "." + value);
    }

    private void getCapturesCoverage(Map<Integer, SemDocument> tableCorpus) {
        Map<String, Integer> globalStats = new HashMap<>();
        if (tableCorpus == null) {
            return;
        }
        tableCorpus.values().stream().map((dto) -> dto.getStats()).forEach((stats) -> {
            stats.keySet().stream().forEach((key) -> {
                Integer value = stats.get(key);
                Integer globalValue = globalStats.get(key);
                if (globalValue == null) {
                    globalValue = 0;
                }
                globalValue += value;
                globalStats.put(key, globalValue);
            });
        });
        for (SemDocument dto : tableCorpus.values()) {
            String id = dto.getId();
            List<Object[]> captures = dto.getCapturesRows();
            dto.getCapturesRows().stream().forEach((row) -> {
                String segment = (String) row[0];
                if (Character.isDigit(segment.charAt(0))) {
                    int pos = segment.indexOf(".");
                    if (pos != -1) {
                        segment = segment.substring(pos + 1);
                    }
                }
                String capture = (String) row[1];
                String value = (String) row[2];
                String key = segment + "." + capture;
                int valuesSize;
                synchronized (captureValuesCount) {
                    Set values = captureValuesCount.get(key);
                    if (values == null) {
                        values = new HashSet();
                    }
                    values.add(value);
                    valuesSize = values.size();
                    captureValuesCount.put(key, values);
                }
                synchronized (captureFilesId) {
                    Set values = captureFilesId.get(key);
                    if (values == null) {
                        values = new HashSet();
                    }
                    values.add(id);
                    captureFilesId.put(key, values);
                }
                synchronized (captureValuesFilesId) {
                    Set values = captureValuesFilesId.get(key + "." + value);
                    if (values == null) {
                        values = new HashSet();
                    }
                    values.add(id);
                    captureValuesFilesId.put(key + "." + value, values);
                }
                Object[] captureRows = captureCoverage.get(key);
                if (captureRows == null) {
                    captureRows = new Object[6];
                    captureRows[0] = segment;
                    captureRows[1] = capture;
                    captureRows[2] = 0;
                    captureRows[3] = 0;
                    captureRows[4] = 0;
                    captureRows[5] = new Double(0);
                }
                Integer candidates = globalStats.get(segment);
                Integer extracted = (Integer) captureRows[2];
                if (extracted == null) {
                    extracted = 0;
                }
                extracted++;
                captureRows[2] = extracted;
                captureRows[3] = candidates;
                captureRows[4] = valuesSize;
                captureRows[5] = (double) ((double) extracted / (double) candidates) * 100;
                captureCoverage.put(key, captureRows);
            });
        }
    }
}
