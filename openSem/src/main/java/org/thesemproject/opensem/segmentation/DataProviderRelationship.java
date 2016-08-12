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
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.thesemproject.opensem.gui.LogGui;

/**
 *
 * Rappresenta un oggetto che mette in relazione il dataprovider e un segmento
 * dove il dataprovider agisce.
 *
 * Per un dataprovider possono esistere da 1 ad N relazioni anche con lo stesso
 * segmento in funzione dei campi
 *
 */
public class DataProviderRelationship implements Serializable {

    private final DataProviderConfiguration dpConfiguration;
    private final String segmentName;
    private final boolean override;
    private final Map<String, String> keys;
    private final Map<String, String> toImport;

    /**
     * Istanzia la relazione
     *
     * @param segmentName nome del segmento
     * @param dpConfiguration configurazione del dataprovider
     * @param override true se le catture ottenute dall'arricchimento devono
     * sovrascrivere valori già esistenti nei field di segmento. Si supponga,
     * per esempio che il field "PIPPO" sia già estratto da una cattura ed
     * assuma valore "Pluto". Se con l'arricchimento il valore di PIPPO diventa
     * "Topolino" se il flag è a true anche nel risultato finale il valore di
     * PIPPO diverrà "Topolino" altrimenti rimarrà "Pluto".
     */
    public DataProviderRelationship(String segmentName, DataProviderConfiguration dpConfiguration, boolean override) {
        this.segmentName = segmentName;
        this.dpConfiguration = dpConfiguration;
        this.override = override;
        this.keys = new HashMap<>();
        this.toImport = new HashMap<>();
    }

    /**
     * Imposta il mapping tra il field del dataprovider e le catture del
     * segmento
     *
     * @param field nome del field del dataprovider
     * @param capture noem della cattura del segmento che si vuole mettere in
     * relazione con il field
     * @param key se a true indica che si tratta di una chiave di relazione. Ad
     * esempio: la colonna CAP del data provider è in relazione con la cattura
     * A_Cap del segmento. Questo significa che stiamo costruendo una relazione
     * @param toImport se a true indica che il valore che il dataprovider trova
     * nel field verificando la relazione tra tutti i field deve essere
     * importato come cattura nel segmento, eventualmente nella cattura passata
     * come parametro precedente
     */
    public void setMapping(String field, String capture, boolean key, boolean toImport) {
        Map<String, String> values = new HashMap<>();
        values.put("capture", capture);
        values.put("key", String.valueOf(key));
        values.put("import", String.valueOf(toImport));
        if (key) {
            if (capture.length() == 0) {
                return;
            }
            keys.put(field, capture);
        }
        if (toImport) {
            if (capture.length() > 0) {
                this.toImport.put(field, capture);
            } else {
                this.toImport.put(field, field);
            }
        }

    }

    /**
     * Arricchisce un segmentationResult incrociando il dataprovider
     * @param sr segmentation result
     * @param captures mappa delle catture 
     */
    public void enrich(SegmentationResults sr, Map<String, CaptureConfiguration> captures) {
        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        for (String field : keys.keySet()) {
            String capture = keys.get(field);
            String captureValue = sr.getCaptureResults().get(capture);
            if (captureValue == null) {
                return;
            }
            TermQuery tq = new TermQuery(new Term(field+"_lower", captureValue.toLowerCase()));
            bq.add(tq, BooleanClause.Occur.MUST);
        }
        try {
            Document search = dpConfiguration.search(bq.build());
            if (search != null) {
                for (String field : toImport.keySet()) {
                    String capture = toImport.get(field); //Dove voglio far andare dentro le cose
                    String value = search.get(field);
                    if (value != null) {
                        if (override || (sr.getCaptureResults().get(capture) == null)) {
                            sr.addCaptureResult(captures.get(capture), value, override);
                        }
                    }
                }

            }
        } catch (Exception e) {
            LogGui.printException(e);
        }

    }
}
