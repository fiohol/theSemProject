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

import org.thesemproject.commons.utils.interning.InternPool;
import java.util.concurrent.atomic.AtomicInteger;
import org.thesemproject.opensem.gui.SemDocument;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.parser.DocumentParser;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import static org.apache.uima.util.FileUtils.getFiles;
import org.thesemproject.opensem.utils.FinalBoolean;
import org.thesemproject.opensem.utils.ParallelProcessor;
import static org.apache.uima.util.FileUtils.getFiles;

/**
 *
 * Gestisce il caricamento di tutti i documenti contenuti in una cartella di
 * sistema operativo (e dei suoi figli) come righe della tabella files.
 */
public class ReadFolderToTable {

    /**
     * true se il sistema sta leggendo
     */
    protected final FinalBoolean isReading;

    /**
     * numero di processori
     */
    protected final int processors;

    /**
     * executor per parallelizzare i processi
     */
    protected final ParallelProcessor executor;

    /**
     * boolean che può essere dichiarato final ma modificato nei thread
     */
    protected final FinalBoolean stop = new FinalBoolean(false);

    /**
     * Istanzia l'oggetto
     *
     * @param processors numero di processori da usare
     */
    public ReadFolderToTable(int processors) {
        this.processors = processors;
        isReading = new FinalBoolean(false);
        executor = new ParallelProcessor(processors, 6000); //100 oreF
    }

    /**
     * Interrompe il processo
     */
    public void interrupt() {
        stop.setValue(true);
    }

    /**
     * Processa la cartella
     *
     * @param inputDir percorso
     * @param filter filtro sui file
     * @param dp document parser
     * @param table tabella in cui caricare i dati
     * @param infoLabel label di status
     * @param fileList lista dei document
     * @param ocrInstallPath percorso OCR
     */
    public void process(final String inputDir, File[] filter, final DocumentParser dp, JTable table, JLabel infoLabel, final Map<Integer, SemDocument> fileList, String ocrInstallPath) {
        final Set<String> selectedFiles = new HashSet<>();
        if (filter != null) {
            for (File f : filter) {
                selectedFiles.add(f.getAbsolutePath());
            }
        }
        InternPool ip = new InternPool();
        final int startId = table.getRowCount() + 1;

        isReading.setValue(true); //dice che sta leggendo

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger countImported = new AtomicInteger(0);
        AtomicInteger countEmpty = new AtomicInteger(0);
        LogGui.info("Start reading ");
        File directory = new File(inputDir);
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        // final File[] files = directory.listFiles();
        final List<File> files = getFiles(directory, true);
        if (files == null) {
            return;
        }
        final int max = files.size();
        stop.setValue(false);
        for (int j = 0; j < processors; j++) {
            executor.add(() -> {
                //Legge il file... e agginge in coda
                while (true) {
                    if (stop.getValue()) {
                        break;
                    }
                    int id = count.getAndIncrement();
                    if (id >= max) {
                        break;
                    }
                    File file = files.get(id);

                    if (file.isDirectory()) {
                        continue;
                    }
                    if (!selectedFiles.isEmpty()) {
                        if (!selectedFiles.contains(file.getAbsolutePath())) {
                            continue;
                        }
                    }
                    String text;
                    String html = "";
                    try {
                        text = dp.getTextFromFile(file, ocrInstallPath);
                        html = dp.getHtmlFromFile(file);
                    } catch (Exception e) {
                        text = "!ERROR: " + e.getLocalizedMessage();

                    }
                    if (text == null) {
                        text = "";
                    }
                    if (html == null) {
                        html = "";
                    }
                    if (text.trim().length() == 0) {
                        countEmpty.getAndIncrement();
                    }
                    if (text.startsWith("!ERROR")) {
                        countEmpty.getAndIncrement();
                    }
                    if (id % 3 == 0) {
                        infoLabel.setText("Ho letto " + id + " files su " + max);
                        LogGui.info("Ho letto " + id + " files su " + max);
                    }

                    int idImported = countImported.getAndIncrement();
                    Object[] row = new Object[10];
                    row[0] = idImported + startId;
                    row[1] = file.getName();
                    row[2] = dp.getLanguageFromText(text);
                    row[3] = 0;
                    row[4] = 0;
                    row[5] = 0;
                    row[6] = 0;
                    row[7] = 0;
                    row[8] = text.trim();
                    row[9] = html.trim();
                    SemDocument dto = new SemDocument();
                    dto.setRow(row);
                    dto.setLanguage((String) row[2]);
                    synchronized (model) {
                        model.addRow(row);
                        fileList.put((idImported + startId), dto);
                    }

                } //Quello che legge
            });
        }
        executor.waitTermination();
        LogGui.info("Terminated...");
        infoLabel.setText("Documenti totali: " + table.getModel().getRowCount() + " - Non convertiti: " + countEmpty.get());
    }
}
