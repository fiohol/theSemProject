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

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Gestisce il salvataggio su file .ini dei parametri dell'interfaccia grafica
 * (cartella di struttura, soglia, k factor, percorso segments.xml...)
 *
 */
public class SemConfiguration {

    private String indexFolder;
    private String threshold;
    private String kFactor;
    private String segmentFile;
    private String lastFolder;
    private String language;
    private String learningFactor;
    private String ocrPath;

    private final static String FILE_NAME = "./configuration.ini";

    /**
     * Istanzia l'oggetto.
     */
    public SemConfiguration() {
        File F = new File(FILE_NAME);
        try {
            if (F.exists()) {
                RandomAccessFile RAF = new RandomAccessFile(F, "rw");
                while (RAF.getFilePointer() < RAF.length()) {
                    String line = RAF.readLine();
                    if (line.startsWith("index")) {
                        indexFolder = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("th")) {
                        threshold = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("k")) {
                        kFactor = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("segment")) {
                        segmentFile = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("last")) {
                        lastFolder = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("lang")) {
                        language = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("learning")) {
                        learningFactor = line.substring(line.indexOf("=") + 1);
                    }
                    if (line.startsWith("ocr")) {
                        ocrPath = line.substring(line.indexOf("=") + 1);
                    }
                }
                RAF.close();
            } else {
                RandomAccessFile RAF = new RandomAccessFile(F, "rw");
                indexFolder = (new File(".")).getAbsolutePath();
                threshold = "0.82";
                kFactor = "1";
                segmentFile = (new File("./segments.xml")).getAbsolutePath();
                lastFolder = ".";
                language = "it";
                learningFactor = "10";
                ocrPath = "";
                writeToFile(RAF);
                RAF.close();
            }

        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Aggiorna la configurazione
     *
     * @param indexFolder cartella struttura
     * @param threshold soglia bayes
     * @param kFactor k facto
     * @param segmentFile percorso segments.xml
     * @param lastFolder ultima cartella aperta
     * @param language ultimia lingua usata
     * @param learningFactor fattore di istruzione del bayesiano (quanti esempi
     * uguali usare)
     * @param ocr path dove è installato l'ocr
     *
     */
    public void updateConfiguration(String indexFolder, String threshold, String kFactor, String segmentFile, String lastFolder, String language, String learningFactor, String ocr) {
        this.indexFolder = indexFolder;

        this.threshold = threshold;
        this.kFactor = kFactor;
        this.segmentFile = segmentFile;
        if (lastFolder != null) {
            this.lastFolder = lastFolder;
        }
        this.language = language;
        this.learningFactor = learningFactor;
        this.ocrPath = ocr;
        File F = new File(FILE_NAME);
        if (F.exists()) {
            F.delete();
        }
        try {
            RandomAccessFile RAF = new RandomAccessFile(F, "rw");
            writeToFile(RAF);
            RAF.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }

    }

    /**
     * Ritorna l'ultima cartella
     *
     * @return ultima cartella
     */
    public String getLastFolder() {
        if (lastFolder == null) {
            lastFolder = ".";
        }
        return lastFolder;
    }

    private void writeToFile(RandomAccessFile RAF) throws Exception {
        RAF.writeBytes("index=" + indexFolder + "\r\n");
        RAF.writeBytes("th=" + threshold + "\r\n");
        RAF.writeBytes("k=" + kFactor + "\r\n");
        RAF.writeBytes("segment=" + segmentFile + "\r\n");
        RAF.writeBytes("last=" + lastFolder + "\r\n");
        RAF.writeBytes("lang=" + language + "\r\n");
        RAF.writeBytes("ocrPath=" + ocrPath + "\r\n");
        RAF.writeBytes("learning=" + learningFactor + "\r\n");
    }

    /**
     * Ritorna l'ultima cartella di struttura
     *
     * @return cartella struttura
     */
    public String getIndexFolder() {
        return indexFolder;
    }

    /**
     * Ritorna la soglia
     *
     * @return soglia bayes
     */
    public String getThreshold() {
        return threshold;
    }

    /**
     * Ritorna il k Factor
     *
     * @return fattore k per KNN
     */
    public String getkFactor() {
        return kFactor;
    }

    /**
     * Ritorna il percorso del file di segment
     *
     * @return percorso file di segment
     */
    public String getSegmentFile() {
        return segmentFile;
    }

    /**
     * Ritorna la lingua
     *
     * @return lingua
     */
    public String getLanguage() {
        if (language == null) {
            language = "it";
        }
        return language;
    }

    /**
     * Ritorna il learning factor
     *
     * @return learning factor
     */
    public String getLearningFactor() {
        if (learningFactor == null) {
            learningFactor = "10";
        }
        return learningFactor;
    }

    /**
     * Ritorna la cartella dove è installato l'OCR
     *
     * @since 1.1
     * @return percorso OCR
     */
    public String getOcrPath() {
        return ocrPath;
    }

}
