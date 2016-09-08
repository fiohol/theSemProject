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
package org.thesemproject.opensem.segmentation.functions.rank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationResults;

/**
 * Gestisce l'elenco delle regole di valutazione del rank di un documento
 *
 * @since 1.3.4
 * @author The Sem Project
 */
public class RankEvaluations implements Serializable {

    private final List<RankEvaluator> evaluators;

    /**
     * Costruttore
     */
    public RankEvaluations() {
        evaluators = new ArrayList();
    }

    /**
     * Aggiunge una regola di rank
     *
     * @param evaluator regola di rank
     */
    public void addRule(RankEvaluator evaluator) {
        evaluators.add(evaluator);
    }

    /**
     * Ritorna la lista delle regole di rank
     *
     * @return lista regole di rank
     */
    public List<RankEvaluator> getEvaluators() {
        return evaluators;
    }

    /**
     * Valuta il risultato di una segmentazione sulle regole
     *
     * @param identifiedSegments risultato segmentazione
     * @return punteggio ottenuto
     */
    public double evaluate(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) {
        return RankEvaluator.evaluate(evaluators, identifiedSegments);
    }

    /**
     * Verifica se l'elenco delle regole è pieno o vuoto
     *
     * @return true se vuoto
     */
    public boolean isEmpty() {
        return evaluators.isEmpty();
    }

}
