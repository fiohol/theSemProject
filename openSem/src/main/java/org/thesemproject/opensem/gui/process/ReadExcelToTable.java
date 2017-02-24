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

import java.util.concurrent.atomic.AtomicInteger;
import org.thesemproject.opensem.gui.SemDocument;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.parser.DocumentParser;
import java.io.FileInputStream;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.thesemproject.opensem.utils.interning.InternPool;

/**
 *
 * Legge un excel e lo carica nella tabella dei files
 */
public class ReadExcelToTable {

    /**
     * Istanzia l'oggetto
     */
    public ReadExcelToTable() {

    }

    /**
     * Processa il file
     *
     * @param inputFile file di input
     * @param dp document parser
     * @param table tabella dove inserire le informazioni lette
     * @param infoLabel label dove viene scritto lo status dell'operazione
     * @param tableList mappa posizione e documento che viene popolata con i
     * dati letti
     */
    public void process(final String inputFile, final DocumentParser dp, JTable table, JLabel infoLabel, final Map<Integer, SemDocument> tableList) {
        InternPool ip = new InternPool();
        final int startId = table.getRowCount() + 1;

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger countEmpty = new AtomicInteger(0);

        LogGui.info("Start reading ");
        FileInputStream fis;
        try {
            fis = new FileInputStream(inputFile);
            Workbook workbook = new XSSFWorkbook(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                int cr = 0;
                for (Row excelRow : sheet) {
                    String text = "";
                    double kpi = 0;

                    try {
                        text = excelRow.getCell(0).getStringCellValue();
                        cr++;
                    } catch (Exception e) {
                        text = "!ERROR: " + e.getLocalizedMessage();
                    }

                    int intKpi = 0;
                    try {
                        kpi = excelRow.getCell(1).getNumericCellValue();
                        intKpi = (int) kpi;
                    } catch (Exception e) {
                        LogGui.printException(e);
                    }
                    if (text == null) {
                        text = "";
                    }
                    if (text.trim().length() == 0) {
                        countEmpty.getAndIncrement();
                    }
                    if (text.startsWith("!ERROR")) {
                        countEmpty.getAndIncrement();
                    }
                    if (cr % 3 == 0) {
                        infoLabel.setText("Ho letto " + cr + " righe ");
                        LogGui.info("Ho letto " + cr + " righe ");
                    }

                    Object[] row = new Object[11];
                    row[0] = new Integer(cr + startId - 1);
                    row[1] = inputFile + "." + cr;
                    row[2] = dp.getLanguageFromText(text);
                    row[3] = new Integer(0);
                    row[4] = new Integer(0);
                    row[5] = new Integer(0);
                    row[6] = new Integer(0);
                    row[7] = new Integer(0);
                    row[8] = text.trim();
                    row[9] = "";
                    if (intKpi != 0) {
                        row[10] = intKpi;
                    }
                    synchronized (table) {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        model.addRow(row);
                        SemDocument dto = new SemDocument();
                        dto.setRow(row);
                        dto.setLanguage((String) row[2]);
                        tableList.put((cr + startId - 1), dto);
                    }
                    if (excelRow.getRowNum() % 1000 == 0) {
                        LogGui.info("Read: " + excelRow.getRowNum());
                    }
                }
            }
            fis.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }

        LogGui.info("Terminated...");
        infoLabel.setText("Documenti totali: " + table.getModel().getRowCount() + " - Non convertiti: " + countEmpty.get());
    }
}
