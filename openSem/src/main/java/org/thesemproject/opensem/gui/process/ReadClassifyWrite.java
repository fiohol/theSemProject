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

import org.thesemproject.opensem.utils.ParallelProcessor;
import org.thesemproject.opensem.utils.FinalBoolean;
import org.thesemproject.opensem.classification.MulticlassEngine;
import java.util.concurrent.atomic.AtomicInteger;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.parser.DocumentParser;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.thesemproject.opensem.utils.BSonUtils;

/**
 *
 * Implementa un processo a doppia coda per gestire la lettura da Excel, la
 * classificazione e la scrittura dei risultati
 */
public class ReadClassifyWrite {

    /**
     * Coda che gestice i dati letti da Excel
     */
    protected final Queue<Document> toDoList;

    /**
     * Coda che cestisce i dati da scrivere
     */
    protected final Queue<Document> toWriteList;

    /**
     * true se il sistema sta ancora leggendo, false se il sistema ha finito di
     * leggere
     */
    protected final FinalBoolean isReading;

    /**
     * Integer atomico per essere dichiarato final ma modificabile nel thread
     */
    protected final AtomicInteger classifingThread;

    /**
     * Numero di processori
     */
    protected final int processors;

    /**
     * Inizializza il processo
     *
     * @param processors numero di processori
     */
    public ReadClassifyWrite(int processors) {
        toDoList = new LinkedBlockingQueue<>(50);
        toWriteList = new LinkedBlockingQueue<>(50);
        this.processors = processors;
        isReading = new FinalBoolean(false);
        classifingThread = new AtomicInteger(0);
    }

    /**
     * Esegue il processo. Di fatto legge con un processo, scrive con un
     * processo e classifica con n processi in parallelo
     *
     * @param inputFile file di input
     * @param descriptionColumn colonna descrizione
     * @param me motore di classificazione
     * @param dp parser
     * @param th soglia del bayesiano
     */
    public void process(final String inputFile, final int descriptionColumn, final MulticlassEngine me, final DocumentParser dp, final double th) {
        //Costruisce 2+n processi: uno per leggere, n per classificare, uno per scrivere
        isReading.setValue(true); //dice che sta leggendo
        classifingThread.set(processors); //dice che sta classificando
        final ParallelProcessor executor = new ParallelProcessor(processors + 2, 6000); //100 ore
        AtomicInteger count = new AtomicInteger(0);
        for (int j = 0; j < processors; j++) {
            executor.add(() -> {
                LogGui.info("Init classification thread ");
                while (true) {
                    Document document = toDoList.poll(); //Prende la testa della coda
                    if (document == null) {
                        //Se è nullo o la coda è vuota o ha finito
                        if (!isReading.getValue()) {
                            //Nessuno sta leggendo... Lavoro finito
                            classifingThread.getAndDecrement();
                            break;
                        }
                    } else {
                        //Classifica il document
                        int pos = count.getAndIncrement();
                        if (pos % 10 == 0) {
                            LogGui.info("Process: " + pos);
                        }
                        String text = document.getString(BSonUtils.TEXT);
                        String language = dp.getLanguageFromText(text);
                        List<ClassificationPath> list = me.bayesClassify(text, th, language);
                        for (int i = 0; i < list.size(); i++) {
                            document.put("BayesPath" + (i + 1), list.get(i).getPath());
                            document.put("BayesScore" + (i + 1), list.get(i).getScore());
                        }
                        //Lo aggiunge in coda toWrite
                        toWriteList.offer(document);
                    }
                }
                LogGui.info("End classification thread...");
            } //Quello che classifica
            );
        }
        executor.add(() -> {
            //Legge il file... e agginge in coda
            LogGui.info("Start reading ");
            FileInputStream fis;
            try {
                fis = new FileInputStream(inputFile);
                Workbook workbook = new XSSFWorkbook(fis);
                int numberOfSheets = workbook.getNumberOfSheets();
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    for (Row row : sheet) {
                        Document document = new Document();
                        document.put("RowId", row.getRowNum());
                        document.put(BSonUtils.TEXT, row.getCell(descriptionColumn).getStringCellValue());
                        toDoList.offer(document); //Aggiunge in coda
                        if (row.getRowNum() % 1000 == 0) {
                            LogGui.info("Read: " + row.getRowNum());
                        }
                    }
                }
                fis.close();
            } catch (Exception e) {
                LogGui.printException(e);
            }
            //Finito di leggere
            LogGui.info("End reading " + inputFile + "... ");
            isReading.setValue(false);
        } //Quello che legge
        );
        executor.add(() -> {
            LogGui.info("Start writing " + inputFile + ".class.xlsx... ");
            try {
                final FileOutputStream fos = new FileOutputStream(inputFile + ".class.xlsx");
                Workbook workbook = new SXSSFWorkbook();
                Sheet sheet = workbook.createSheet();
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Text");
                header.createCell(1).setCellValue("1st Level1");
                header.createCell(2).setCellValue("1st Score1");
                header.createCell(3).setCellValue("1st Level2");
                header.createCell(4).setCellValue("1st Score2");
                header.createCell(5).setCellValue("1st Level3");
                header.createCell(6).setCellValue("1st Score3");
                header.createCell(7).setCellValue("1st Level4");
                header.createCell(8).setCellValue("1st Score4");
                header.createCell(9).setCellValue("2nd Level1");
                header.createCell(10).setCellValue("2nd Score1");
                header.createCell(11).setCellValue("2nd Level2");
                header.createCell(12).setCellValue("2nd Score2");
                header.createCell(13).setCellValue("2nd Level3");
                header.createCell(14).setCellValue("2nd Score3");
                header.createCell(15).setCellValue("2nd Level4");
                header.createCell(16).setCellValue("2nd Score4");
                while (true) {
                    //Ciclo infinito
                    Document document = toWriteList.poll(); //Prende la testa della coda
                    if (document == null) {
                        //Non c'è da scrivere
                        if (classifingThread.get() <= 0) {
                            //Ha finito di classificare ho finito
                            break;
                        }
                    } else {
                        //Scrive...
                        int idRow = document.getInteger("RowId");
                        Row row = sheet.createRow(idRow + 1);
                        row.createCell(0).setCellValue(document.getString(BSonUtils.TEXT));
                        String[] bayesPath1 = (String[]) document.get("BayesPath1");
                        String[] bayesPath2 = (String[]) document.get("BayesPath2");
                        double[] score1 = (double[]) document.get("BayesScore1");
                        double[] score2 = (double[]) document.get("BayesScore2");
                        if (bayesPath1 != null) {
                            for (int i = 0; i < 4; i++) {
                                String node = bayesPath1[i];
                                if (node != null) {
                                    double score = score1[i];
                                    row.createCell((2 * i) + 1).setCellValue(node);
                                    row.createCell((2 * i) + 2).setCellValue(score);
                                }
                            }
                            if (bayesPath2 != null) {
                                for (int i = 0; i < 4; i++) {
                                    String node = bayesPath2[i];
                                    if (node != null) {
                                        double score = score2[i];
                                        row.createCell((2 * i) + 9).setCellValue(node);
                                        row.createCell((2 * i) + 10).setCellValue(score);
                                    }
                                }
                            }
                        }
                        if (row.getRowNum() % 1000 == 0) {
                            LogGui.info("Write: " + row.getRowNum());
                        }
                    }
                }
                workbook.write(fos);
                fos.close();
            } catch (Exception e) {
                LogGui.printException(e);
            }
            //Fine chiude il file
            LogGui.info("End writing " + inputFile + ".class.xlsx... ");
        } //Quello che scrive
        );
        executor.waitTermination();
        LogGui.info("Terminated...");
    }

}
