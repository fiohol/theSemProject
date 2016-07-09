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
package org.thesemproject.opensem.gui.process;

import org.thesemproject.opensem.segmentation.SegmentationUtils;
import org.thesemproject.opensem.utils.ParallelProcessor;
import org.thesemproject.opensem.utils.FinalBoolean;
import org.thesemproject.opensem.classification.MulticlassEngine;
import java.util.concurrent.atomic.AtomicInteger;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.parser.DocumentParser;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentEngine;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.bson.Document;
import org.thesemproject.opensem.utils.BSonUtils;
import static org.apache.uima.util.FileUtils.getFiles;

/**
 *
 * Impelementa un processo a doppia coda per leggere i documenti, applicare il
 * segmentatore in parallelo e scrivere il risultato
 */
public class ReadSegmentWrite {

    /**
     * Coda dei documenti letti
     */
    protected final Queue<Document> toDoList;

    /**
     * Coda dei documenti processati
     */
    protected final Queue<Document> toWriteList;

    /**
     * true se sta ancora popolando la toDoList
     */
    protected final FinalBoolean isReading;

    /**
     * integer final modificabile
     */
    protected final AtomicInteger segmentThread;

    /**
     * numero processori
     */
    protected final int processors;

    /**
     * Istanzia l'oggetto
     *
     * @param processors numero di processori
     */
    public ReadSegmentWrite(int processors) {
        toDoList = new LinkedBlockingQueue<>(50);
        toWriteList = new LinkedBlockingQueue<>(50);
        this.processors = processors;
        isReading = new FinalBoolean(false);
        segmentThread = new AtomicInteger(0);
    }

    /**
     * processa la cartella
     *
     * @param inputDir cartella
     * @param dp document parser
     * @param se segmentEngine
     * @param me multiclass engine
     * @param th soglia bayes
     * @param writeHtml true se nel processo deve generare anche i files HTML
     * @param ocrInstallPath percorso OCR
     */
    public void process(final String inputDir, final DocumentParser dp, final SegmentEngine se, final MulticlassEngine me, final double th, boolean writeHtml, String ocrInstallPath) {
        //Costruisce 2+n processi: uno per leggere, n per classificare, uno per scrivere
        isReading.setValue(true); //dice che sta leggendo
        segmentThread.set(processors); //dice che sta classificando
        final ParallelProcessor executor = new ParallelProcessor(processors + 2, 6000); //100 ore
        AtomicInteger count = new AtomicInteger(0);
        for (int j = 0; j < processors; j++) {
            executor.add(() -> {
                LogGui.info("Init tagging thread ");
                while (true) {
                    Document document = toDoList.poll(); //Prende la testa della coda
                    if (document == null) {
                        //Se è nullo o la coda è vuota o ha finito
                        if (!isReading.getValue()) {
                            //Nessuno sta leggendo... Lavoro finito
                            segmentThread.getAndDecrement();
                            break;
                        }
                    } else {
                        //Classifica il document
                        int pos = count.getAndIncrement();
                        LogGui.info("Tag: " + document.getString(BSonUtils.SOURCE));
                        String text = document.getString(BSonUtils.TEXT);
                        String language = document.getString("Language");
                        try {
                            //Lo aggiunge in coda toWrite
                            Map<SegmentConfiguration, List<SegmentationResults>> result = se.getSegments(text, me, th, language);
                            document.put("SegmentResult", result);
                            toWriteList.offer(SegmentationUtils.getDocument(document, result));
                        } catch (Exception e) {
                            LogGui.printException(e);
                        }
                    }
                }
                LogGui.info("End classification thread...");
            } //Quello che classifica
            );
        }
        executor.add(() -> {
            //Legge il file... e agginge in coda
            LogGui.info("Start reading ");
            File directory = new File(inputDir);
            List<File> files = getFiles(directory, true);
            files.stream().filter((file) -> !(file.isDirectory())).filter((file) -> !(file.getAbsolutePath().contains(inputDir + "/results/"))).filter((file) -> (!file.getAbsolutePath().contains("tagResult.xlsx"))).map((file) -> {
                LogGui.info("Read: " + file);
                return file;
            }).map((file) -> {
                String text = dp.getTextFromFile(file, ocrInstallPath);
                Document document = new Document();
                document.put(BSonUtils.TEXT, text);
                document.put(BSonUtils.SOURCE, file.getName());
                document.put("Language", dp.getLanguageFromText(text));
                return document;
            }).forEach((document) -> {
                toDoList.offer(document); //Aggiunge in coda
            });
            //Finito di leggere
            LogGui.info("End reading " + inputDir + "... ");
            isReading.setValue(false);
        } //Quello che legge
        );
        executor.add(() -> {
            String destDir = inputDir + "/results/";
            File fdest = new File(destDir);
            if (!fdest.exists()) {
                fdest.mkdirs();
            }
            LogGui.info("Start writing results into " + fdest.getAbsolutePath() + "/tagResult.xlsx... ");
            try {
                final FileOutputStream fos = new FileOutputStream(fdest.getAbsolutePath() + "/tagResult.xlsx");
                SegmentationExcelWriter sew = new SegmentationExcelWriter(se);
                AtomicInteger rr = new AtomicInteger(0);
                while (true) {
                    //Ciclo infinito
                    Document document = toWriteList.poll(); //Prende la testa della coda
                    if (document == null) {
                        //Non c'è da scrivere
                        if (segmentThread.get() <= 0) {
                            //Ha finito di taggare ho finito
                            break;
                        }
                    } else //Scrive...
                    {
                        Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = (Map<SegmentConfiguration, List<SegmentationResults>>) document.get("SegmentResult");
                        if (writeHtml) {
                            try {
                                FileOutputStream fHtml = new FileOutputStream(fdest.getAbsolutePath() + "/" + document.getString(BSonUtils.SOURCE) + ".html");
                                fHtml.write(SegmentationUtils.getHtml(identifiedSegments, document.getString("Language")).getBytes());
                                fHtml.close();
                            } catch (Exception e) {
                                LogGui.printException(e);
                            }
                        }
                        int resultsRow = rr.incrementAndGet();
                        String fileName = document.getString(BSonUtils.SOURCE);
                        String language = document.getString("Language");
                        sew.addDocument(resultsRow, fileName, document.getString(BSonUtils.TEXT), language, identifiedSegments);
                    }
                }
                sew.write(fos);
                fos.close();
            } catch (Exception e) {
                LogGui.printException(e);
            }
            //Fine chiude il file
            LogGui.info("End writing " + fdest.getAbsolutePath() + "/tagResult.xlsx... ");
        } //Quello che scrive
        );
        executor.waitTermination();
        LogGui.info("Terminated...");
    }
}
