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
package org.thesemproject.opensem.examples;

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.classification.MulticlassEngine;
import org.thesemproject.opensem.classification.MyAnalyzer;
import org.thesemproject.opensem.classification.Tokenizer;
import org.thesemproject.opensem.parser.DocumentParser;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentEngine;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.segmentation.SegmentationUtils;
import org.thesemproject.opensem.segmentation.functions.DurationsMap;
import org.thesemproject.opensem.tagcloud.TagClass;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

/**
 * Questo è un esempio su come utilzzare SEM
 *
 * SEM è composto da tre componenti indipendenti
 *
 * Parser: Per trasformare documenti in testo attraverso Tika
 *
 * Motore di classificazione: bayesiano basato su lucene
 *
 * Motore di segmentazione: pattern matching
 *
 * Tutti i componenti possono essere usati singolarmente e possono essere
 * istanziati una sola volta nella vita del programma.
 *
 * Ogni componente può essere inizializzato ogni volta che si cambia un file.
 */
public class Example {

    /**
     * Programma demo
     *
     * @param args argomenti
     * @throws Exception Eccezione
     */
    public static void main(String args[]) throws Exception {
        //-------------------- PARSER ------------------------------------------
        //Istanzio il document parser (posso farlo una sola volta nel ciclo di 
        //vita del software
        DocumentParser dp = new DocumentParser();
        //Se voglio il testo a partire da un file mi basta semplicemente fare
        String text = dp.getTextFromFile(new File("C:\\esempi\\esempio.docx"));
        //Se voglio sapere la lingua in cui è scritto il testo mi basta fare
        String language = dp.getLanguageFromText(text);
        //Se volessi avere la vista formatta (Html) di un testo mi basta fare
        String formattedText = dp.getHtmlFromFile(new File("C:\\esempi\\esempio.docx"));

        //-------------------- CLASSIFICATORE ----------------------------------
        //Per isanziare il classificatore (posso farlo una sola volta nel ciclo
        //di vita del software
        MulticlassEngine me = new MulticlassEngine();
        //Per istruire il classificatore devo passargli un indice di lucene,
        //Dizionario delle stop words, lingua dell'analizzatore sintattico e 
        //fattore K del classificatre KNN
        String indexFolder = "C:\\esempi\\indiceDiLucene";
        String stopWordFile = "C:\\esempi\\dizionario.txt";
        String parserLanguage = "it"; //it, en, de, fr, ru, ro...
        int knnFactor = 1;
        //L'inizializzazione può essere fatta una volta per tutte nel ciclo di vita del 
        //programma
        //me.init(indexFolder, stopWordFile, parserLanguage, knnFactor);
        me.init(indexFolder, knnFactor);
        //Se voglio classificare il documento mi basta fare:
        double threshold = 0.5; //Soglia minima di percentuale di classificazione
        //Dato che posso avere più di una classificazione il risultato è una lista
        //List<ClassificationPath> cps = me.bayesClassify(text, threshold);
        List<ClassificationPath> cps = me.bayesClassify(text, threshold, dp.getLanguageFromText(text));
        for (ClassificationPath cp : cps) {
            System.out.println(cp.toSmallClassString()); //Versione compatta
            System.out.println(cp.toSmallString()); //Versione compatta ma con %
            System.out.println(cp.toString()); //Versione completa
            String[] path = cp.getPath(); //Così ho il path di classificazione (4 livelli)
            double[] score = cp.getScore(); //Lista dei punteggi (4 livelli)
            String technology = cp.getTechnology(); //Tecnologia utilizzata
        }

        //-------------------- SEGMENTATORE ------------------------------------
        //Il segmentatore può funzionare autonomamente o abbinato al classificatore
        //Il classificatore viene attivato se nel file di segmentazione c'è un
        //segmento marcato come classificabile
        //Anche questo può essere instanziato una volta per tutte
        SegmentEngine se = new SegmentEngine();
        String model = "C:\\esempi\\segment.xml";
        //Inizializzo il segmentatore. Posso reinizializzarlo quante volte voglio
        se.init(model);
        //Se vogio segmentare un testo (senza classificare) mi basta fare:
        Map<SegmentConfiguration, List<SegmentationResults>> results = se.getSegments(text, dp.getLanguageFromText(text));
        //Se vogio segmentare e classificare i segmenti marcati come classificabili:
        Map<SegmentConfiguration, List<SegmentationResults>> results2 = se.getSegments(text, me, threshold, dp.getLanguageFromText(text));
        //Passare i risultati non è immediato. Ma ci sono delle utilities per farlo
        //Per avere il tagging in HTML
        String html = SegmentationUtils.getHtml(results, language);
        //Per avere in JSON gerarchico
        Document document = new Document();
        document = SegmentationUtils.getDocument(document, results2);
        //In ogni caso basta visualizzare SegmentsUtils per vedere come trattare i result

        //------------------------ TIMELINE -----------------------------------------------
        //Come fare ad avere una timeline. Basta passare il risultato della segmentazione.
        String htmlTimeline = SegmentationUtils.getHtmlDurations(results);
        //L'html viene costruito in modo molto semplice. Se non si vuole usare l'html generato si può usare la Duration
        DurationsMap durations = DurationsMap.getDurations(results);
        //Una volta che si ha la duration si possono fare le varie operazioni.
        //Prima di tutto si possono estrarre le chiavi (ovvero le prime due colonne della tabella). La prima colonna è la tipologia 
        //La seconda colonna è il valore
        List<Pair<String, String>> keys = durations.keySet();
        //Per avere l'elenco degli anni basta chiederli alla duration. Un metodo analogo fornisce l'elenco dei mesi
        List<String> years = durations.getYearsList();
        //A questo punto ho la possibilità di definire tutte le colonne quindi mi basta scorrere per riga e vedere per ogni colonna
        //cosa posso scrivere
        for (Pair<String, String> key : keys) {
            //Qui so se l'esperienza in questione era abiliatata per l'anno
            Set<String> enabledYears = durations.getYears(key);
            for (String year : years) { //Scorro gli anni e faccio vedere se l'esperienza è ok...
                if (enabledYears.contains(year));
                //Do something
            }
        }

        //------------------------ IMMAGINI E CONTENUTI ------------------------------------
        //Immagini e contenuti extra sono binari presenti nei file originari
        //La loro estrazione è un processo parallelo perché, specie per i PDF il processo potrebbe essere lento
        //Per avere un'immagine la cosa più semplice è chiederla al parser chiedendo di avere il contenuto di tipo immagine più grande 
        //Difficilmente in un CV l'immagine che pesa di più non è la foto del candidato
        BufferedImage image = dp.getLargestImageFromFile(new File("C:\\esempi\\esempio.docx"));
        //Buffer image è un oggeto swing che può essere usato per fare streaming delle immagini.
        //Da una BufferedImage si può salvare facilmente una immagine
        File outputfile = new File("image.jpg");
        ImageIO.write(image, "jpg", outputfile);
        //Se si vogliono tutti i contenuti sottoforma di byte
        Map<String, byte[]> map = dp.getInlinesContentFromFile(new File("C:\\esempi\\esempio.docx"));
        // Il primo parametro della mappa è un fileName fittizio con tanto di estesione. In base all'estesione si può capire il tipo di contenuto

        //------------------------ TAG CLOUD -----------------------------------------------
        //IL tag cloud è stato pensato per fare tagcloud su un testo o su più testi
        //La struttura è tread safe quindi può essere usata anche su più processi in parallelo
        //Per fare tagcloud per prima cosa si deve istanziare l'oggetto che conterrà la relazione parole, frequenze
        TagCloudResults result = new TagCloudResults();
        //Se voglio fare un lavoro su un solo documento mi basta prendere il testo del documento e la sua lingua
        //Poi prosso prendermi l'analizzatore sintattico
        MyAnalyzer analyzer = me.getAnalyzer(language);
        //Quindi tokenizzare
        Tokenizer.getTagClasses(result, text, "", analyzer);
        //In result troverò i risultati.
        //Se voglio tokenizzare più di un documento mi basta inizializzare il "result" una volta e poi ciclicamente chiamare
        //Il tokenizer
        for (int i = 0; i < 100; i++) {
            Tokenizer.getTagClasses(result, "testo dell'i.esimo documento", "Identificativo_del_documento", analyzer);
        }
        //Una volta ottenuto il result posso chiedere il Cloud (in formato OpenCloud).
        //Il 100 passato come parametro identifica il numero massimo di tag che si vogliono
        //Se ci sono tanti documenti un numero ragionevole è tra i 150 e i 200. Su un documento singolo 50 dovrebbe essere ok
        Cloud cloud = result.getCloud(100);
        for (Tag tag : cloud.tags()) {
            String parolaDaStampare = tag.getName();
            //Poi c'è lo score che può essere int, double, normalizzato o no
            int normScoreInt = tag.getNormScoreInt();
            double normScoreDouble = tag.getNormScore();
            int scoreInt = tag.getScoreInt();
            //E se serve anche il peso
            double weigth = tag.getWeight(); //Anche questo in varie modalità
            //Su sem i pesi vengono usati per la grandezza del carattere ma possono essere usati anche per la colorazione 
            //In funzione dell'algoritmo di presentazione che si decide di scegliere.

            //L'oggetto Tag Class contiene una serie di info aggiuntive oltre la frequenza
            TagClass tc = result.getTagClass(tag);
            //Ad esempio l'elenco di tutti i documentId che afferiscono a quel tag specifico
            Set<String> docIds = tc.getDocumentsId();
            //Oppure l'elenco delle parole che sono state normalizzate nel tag (ad esempio Consulenza e Consulenze sono sotto Consulenza)
            String toolTip = tc.getWordsString();
            //Todo label cliccabili e filtro sui documenti...
            if (docIds.size() > 1) {
                //DO Something (tipo metto il link)...
            }
            //Faccio qualche cosa del tag tipo stamparlo a video....
        }
    }
}
