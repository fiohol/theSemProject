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

import static org.thesemproject.opensem.classification.IndexManager.BODY;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * Tokenizzatore di testi utilizzando l'analizzatore sintattico multilingua
 */
public class Tokenizer {

    /**
     * Tokenizza un testo. Utile per mostare l'effetto della tokenizzazione su
     * un testo o per preparare un testo prima dell'istruzione del motore
     *
     * @param text testo da tokenizzare
     * @param analyzer analizzatore sintattico
     * @return testo tokenizzato (come testo)
     * @throws Exception Eccezione
     */
    public static String tokenize(String text, Analyzer analyzer) throws Exception {
        return tokenize(text, analyzer, 25);
    }

    /**
     * Tokenizza un testo. Utile per mostare l'effetto della tokenizzazione su
     * un testo o per preparare un testo prima dell'istruzione del motore
     *
     * @since 1.2
     *
     * @param text testo da tokenizzare
     * @param analyzer analizzatore sintattico
     * @param tokens numero massimo di token da tenere
     * @return testo tokenizzato (come testo)
     * @throws Exception Eccezione
     */
    public static String tokenize(String text, Analyzer analyzer, int tokens) throws Exception {
        TokenizerFilter tf = new TokenizerFilter() {
            private final StringBuilder body = new StringBuilder();

            @Override
            public void applyTo(String term) {
                if (term != null) {
                    if (term.trim().length() > 0) {
                        body.append(term).append(" ");
                    }
                }
            }

            public String toString() {
                return body.toString().trim();
            }
        };
        tokenize(text, analyzer, tokens, tf);
        return tf.toString();
    }

    /**
     * Ritorna il risultato di un tagcloud su di un testo. Dato che ogni termine
     * che appare nel cloud è un rappresentate di una serie di termini il
     * sistema fa un doppio passaggio. Quello che si vuole fare è riunire sotto
     * uno stesso termine rappresentante tutti i token che hanno la stessa
     * tokenizzazione. In pratica l'analizzatore sintattico di fronte ai due
     * termini come "tecnico" e "tecnica" ritorna lo stesso token "tecnic"
     * Quello che si vuole è che nel could non appaia la parola "tecnic" ma
     * sempre e comunque il primo dei termini che è apparso nel testo Nella
     * nuova comparira quindi la parola "tecnico" che va a rappresentare sia
     * "tecnico" che "tecnica"
     *
     * @param ret Risultato del tag clouding
     * @param text testo su cui fare il tagcloud
     * @param id id univoco del documento il cui testo è passato come parametro
     * @param analyzer analizzatore sintattico
     * @throws Exception Eccezione
     */
    public static void getTagClasses(final TagCloudResults ret, String text, String id, Analyzer analyzer) throws Exception {
        TokenizerFilter tf = (String term) -> {
            try {
                String newTerm = tokenize(term.trim(), analyzer, -1);
                if (newTerm.length() > 0) {
                    
                    ret.add(newTerm, term, id);
                }
            } catch (Exception exception) {
            }

        };
        tokenize(text, new SimpleAnalyzer(), -1, tf);
    }

    private static Pattern preplace = Pattern.compile("\\P{L}"); //[^a-zA-Z]

    /**
     * Tokenizza un testo utilizzando il filtro passato come parametro
     *
     * @param text testo da tokenizzare
     * @param analyzer analizzatore sintattico
     * @param tokens numero massimo di token da tenere
     * @param filter filtro di tokenizzazione. Questa interfaccia permette di
     * inserire una logica durante la tokenizzazione. Alla ricezione di un token
     * il sistema applica una logica. Ad esempio unisce tutti i metodi in una
     * stringa ma mano che lo riceve
     * @throws Exception Eccezione
     */
    public static void tokenize(String text, Analyzer analyzer, int tokens, TokenizerFilter filter) throws Exception {
        if (text == null) {
            return;
        }
        if (analyzer == null) {
            return;
        }
        text = text.toLowerCase();
        Matcher m = preplace.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, " ");
        }
        m.appendTail(sb);
        TokenStream tokenStream = analyzer.tokenStream(BODY, new StringReader(sb.toString()));
        OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        int tokenNumber = 0;
        while (tokenStream.incrementToken()) {
            int startOffset = offsetAttribute.startOffset();
            int endOffset = offsetAttribute.endOffset();
            String term = charTermAttribute.toString();
            filter.applyTo(term);
            tokenNumber++;
            if (tokens != -1) {
                if (tokenNumber > tokens) {
                    break;
                }
            }
        }
        tokenStream.close();
    }

    /**
     * Calcola la distanza di Levenshtein tra due stringhe. Nella teoria
     * dell'informazione e nella teoria dei linguaggi, la distanza di
     * Levenshtein, o distanza di edit, è una misura per la differenza fra due
     * stringhe. Introdotta dallo scienziato russo Vladimir Levenshtein nel
     * 1965, serve a determinare quanto due stringhe siano simili. Viene
     * applicata per esempio per semplici algoritmi di controllo ortografico e
     * per fare ricerca di similarità tra immagini, suoni, testi, etc.
     *
     * La distanza di Levenshtein tra due stringhe A e B è il numero minimo di
     * modifiche elementari che consentono di trasformare la A nella B. Per
     * modifica elementare si intende
     *
     * la cancellazione di un carattere, la sostituzione di un carattere con un
     * altro, o l'inserimento di un carattere. Per esempio, per trasformare
     * "bar" in "biro" occorrono due modifiche:
     *
     * "bar" -&gt; "bir" (sostituzione di 'a' con 'i') "bir" -&gt; "biro"
     * (inserimento di 'o') Non è possibile trasformare la prima parola nella
     * seconda con meno di due modifiche, quindi la distanza di Levenshtein fra
     * "bar" e "biro" è 2.
     *
     * @param a prima stringa
     * @param b seconda stringa
     * @return distanza
     */
    public static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Filtro astratto di tokenizzazione. implementa una logica ogni qual volta
     * riceve un token.
     */
    public interface TokenizerFilter {

        /**
         * Applica la tokenizzazione al termine
         *
         * @param term temine con cui fare qualche cosa
         */
        public void applyTo(String term);

    }

}
