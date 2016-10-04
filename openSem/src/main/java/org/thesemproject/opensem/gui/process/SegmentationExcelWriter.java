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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.segmentation.CaptureConfiguration;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentEngine;
import org.thesemproject.opensem.segmentation.SegmentationResults;

/**
 *
 * Scrive il risultato della segmentazione su un file excel.
 */
public class SegmentationExcelWriter {

    private final SXSSFWorkbook wb;
    private final Map<String, Map<String, Integer>> sheetCellIndex;
    private final SXSSFSheet sheetResults;
    private final SXSSFRow headerResults;

    /**
     * Istanzia oggetto
     *
     * @param se segment engine
     */
    public SegmentationExcelWriter(SegmentEngine se) {
        sheetCellIndex = new LinkedHashMap<>();
        wb = new SXSSFWorkbook();
        sheetResults = wb.createSheet("Results");
        headerResults = sheetResults.createRow(0);
        SXSSFCell fCell = headerResults.createCell(0);
        fCell.setCellValue("File Name");
        fCell = headerResults.createCell(1);
        fCell.setCellValue("Language");
        fCell = headerResults.createCell(2);
        fCell.setCellValue("Original Text");
        fCell = headerResults.createCell(3);
        fCell.setCellValue("KPI");
        int idxCell = 4;
        for (SegmentConfiguration sc : se.getPatternMatrix()) {
            idxCell = createExcelHeader(wb, headerResults, sheetCellIndex, idxCell, "", sc, sheetResults);
        }
        writeClassificationHeader(wb);
    }

    /**
     * Aggiunge un documento all'excel che viene scritto in streaming
     *
     * @param resultsRow id riga
     * @param fileName nome del file
     * @param language lingua
     * @param text testo
     * @param kpi valore del kpi
     * @param identifiedSegments risultato tagging
     * @throws IOException Eccezione di input/output
     */
    public void addDocument(int resultsRow, String fileName, String language, String text, String kpi, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) throws IOException {
        SXSSFRow row = sheetResults.createRow(resultsRow);
        row.createCell(0).setCellValue(fileName);
        row.createCell(1).setCellValue(language);
        if (text != null) {
            if (text.length() > 32767) {
                text = text.substring(0, 32766);
            }
            row.createCell(2).setCellValue(text);
            row.createCell(3).setCellValue(kpi);
        }
        writeSegments(fileName, row, wb, sheetCellIndex, identifiedSegments, "");
    }

    private static void writeClassificationHeader(SXSSFWorkbook workbook) {
        SXSSFSheet sheet = workbook.createSheet("Bayes");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("File");
        header.createCell(1).setCellValue("Text");
        header.createCell(2).setCellValue("1st Level1");
        header.createCell(3).setCellValue("1st Score1");
        header.createCell(4).setCellValue("1st Level2");
        header.createCell(5).setCellValue("1st Score2");
        header.createCell(6).setCellValue("1st Level3");
        header.createCell(7).setCellValue("1st Score3");
        header.createCell(8).setCellValue("1st Level4");
        header.createCell(9).setCellValue("1st Score4");
        header.createCell(10).setCellValue("1st Level5");
        header.createCell(11).setCellValue("1st Score5");
        header.createCell(12).setCellValue("1st Level6");
        header.createCell(13).setCellValue("1st Score7");
        header.createCell(14).setCellValue("2nd Level1");
        header.createCell(15).setCellValue("2nd Score1");
        header.createCell(16).setCellValue("2nd Level2");
        header.createCell(17).setCellValue("2nd Score2");
        header.createCell(18).setCellValue("2nd Level3");
        header.createCell(19).setCellValue("2nd Score3");
        header.createCell(20).setCellValue("2nd Level4");
        header.createCell(21).setCellValue("2nd Score4");
        header.createCell(22).setCellValue("2nd Level5");
        header.createCell(23).setCellValue("2nd Score5");
        header.createCell(24).setCellValue("2nd Level6");
        header.createCell(25).setCellValue("2nd Score6");
    }

    private static void writeSegments(String fileName, SXSSFRow row, SXSSFWorkbook wb, Map<String, Map<String, Integer>> sheetCellIndex, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName) throws IOException {
        if (identifiedSegments == null) {
            return;
        }
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            SXSSFSheet currentSheet;
            boolean addRow = false;
            currentSheet = wb.getSheet(segmentName);
            if (currentSheet == null) {
                currentSheet = wb.getSheet("Results");
            } else {
                addRow = true;
            }
            Map<String, Integer> cellIndex;
            cellIndex = sheetCellIndex.get(currentSheet.getSheetName());
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                //Controllo se ho durata...

                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    if (cellIndex != null) {
                        Integer idxSeg = cellIndex.get("SEGMENT." + segmentName);
                        if (idxSeg != null) {
                            String value;
                            SXSSFCell cell = row.getCell(idxSeg);
                            StringBuilder sb = new StringBuilder();
                            if (cell != null) {
                                value = cell.getStringCellValue();
                                if (value != null) {
                                    sb.append(value).append("\n");
                                }
                            }
                            List<String> lines = sr.getSentencies();
                            lines.stream().forEach((line) -> {
                                sb.append(line).append("\n");
                            });
                            String txt = sb.toString();
                            if (txt.endsWith("\n")) {
                                txt = txt.substring(0, txt.length() - 2);
                            }

                            if (cell != null) {
                                if (txt.length() > 32767) {
                                    txt = txt.substring(0, 32766);
                                }
                                cell.setCellValue(txt);
                            } else {
                                if (txt.length() > 32767) {
                                    txt = txt.substring(0, 32766);
                                }
                                row.createCell(idxSeg).setCellValue(txt);
                            }
                        }
                    }
                    writeSegments(fileName, row, wb, sheetCellIndex, subSegments, segmentName);
                } else {
                    String txt = "";
                    if (cellIndex != null) {
                        Integer idxSeg = cellIndex.get("SEGMENT." + segmentName);
                        if (idxSeg != null) {
                            if (addRow) {
                                row = currentSheet.createRow(currentSheet.getLastRowNum() + 1);
                                row.createCell(0).setCellValue(fileName);
                            }
                            StringBuilder sb = new StringBuilder();
                            List<String> lines = sr.getSentencies();
                            lines.stream().forEach((line) -> {
                                sb.append(line).append("\n");
                            });
                            txt = sb.toString();
                            if (txt.endsWith("\n")) {
                                txt = txt.substring(0, txt.length() - 2);
                            }
                            if (txt.length() > 32767) {
                                txt = txt.substring(0, 32766);
                            }
                            row.createCell(idxSeg).setCellValue(txt);
                        }
                    }

                    List<ClassificationPath> cps = sr.getClassificationPaths();
                    if (cps.size() > 0) {
                        Sheet sheet = wb.getSheet("Bayes");
                        Row rx = sheet.createRow(sheet.getLastRowNum() + 1);
                        rx.createCell(0).setCellValue(fileName);
                        if (txt.length() > 32767) {
                            txt = txt.substring(0, 32766);
                        }
                        rx.createCell(1).setCellValue(txt);
                        String[] bayesPath1 = (String[]) cps.get(0).getPath();
                        double[] score1 = (double[]) cps.get(0).getScore();
                        String[] bayesPath2 = null;
                        double[] score2 = null;
                        if (cps.size() > 1) {
                            bayesPath2 = (String[]) cps.get(1).getPath();
                            score2 = (double[]) cps.get(1).getScore();
                        }
                        if (bayesPath1 != null) {
                            int ClassficationPath;
                            for (int i = 0; i < ClassificationPath.MAX_DEEP; i++) {
                                String node = bayesPath1[i];
                                if (node != null) {
                                    double score = score1[i];
                                    rx.createCell((2 * i) + 2).setCellValue(node);
                                    rx.createCell((2 * i) + 3).setCellValue(score);
                                }
                            }
                            if (bayesPath2 != null) {
                                for (int i = 0; i < ClassificationPath.MAX_DEEP; i++) {
                                    String node = bayesPath2[i];
                                    if (node != null) {
                                        double score = score2[i];
                                        rx.createCell((2 * i) + 14).setCellValue(node);
                                        rx.createCell((2 * i) + 15).setCellValue(score);
                                    }
                                }
                            }
                        }
                    }
                    double duration = sr.getDurationYears();
                    if (duration > 0) {
                        String key = segmentName + ".SegmentDuration";
                        if (cellIndex != null) {
                            Integer idx = cellIndex.get(key);
                            if (idx != null) {
                                SXSSFCell dest = row.getCell(idx);
                                if (dest == null) {
                                    row.createCell(idx).setCellValue(duration);
                                } else {
                                    dest.setCellValue(duration);
                                }
                            }
                        }
                    }
                    Map<CaptureConfiguration, String> captures = sr.getCaptureConfigurationResults();
                    if (!captures.isEmpty()) {
                        for (CaptureConfiguration captureConfiguration : captures.keySet()) {
                            String key = segmentName + "." + captureConfiguration.getName();
                            if (cellIndex != null) {
                                Integer idx = cellIndex.get(key);
                                if (idx != null) {
                                    SXSSFCell dest = row.getCell(idx);
                                    if (dest == null) {
                                        row.createCell(idx).setCellValue(captures.get(captureConfiguration));
                                    } else {
                                        String val = dest.getStringCellValue();
                                        String newOne = captures.get(captureConfiguration);
                                        if (!val.equals(newOne)) {
                                            if (!val.contains(newOne + ", ")) {
                                                val = val + ", " + newOne;
                                            }
                                            dest.setCellValue(val);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int createExcelHeader(SXSSFWorkbook wb, SXSSFRow headerRow, Map<String, Map<String, Integer>> sheetCellIndex, int idxCell, String parentName, SegmentConfiguration sc, SXSSFSheet currentSheet) {
        String segmentName = (parentName.length() > 0 ? parentName + "." : "") + sc.getName();
        if (!sc.isMultiple()) {
            Map<String, Integer> cellIndex = sheetCellIndex.get(currentSheet.getSheetName());
            if (cellIndex == null) {
                cellIndex = new HashMap<>();
            }
            idxCell = createSegmentHeader(headerRow, cellIndex, idxCell, sc, segmentName);
            List<SegmentConfiguration> segmentConfigurations = sc.getSegments();
            for (SegmentConfiguration child : segmentConfigurations) {
                createExcelHeader(wb, headerRow, sheetCellIndex, idxCell, segmentName, child, currentSheet);
            }
            sheetCellIndex.put(currentSheet.getSheetName(), cellIndex);
        } else {
            SXSSFSheet sheetSegment = wb.getSheet(segmentName);
            if (sheetSegment == null) {
                sheetSegment = wb.createSheet(segmentName);
                SXSSFRow hr = sheetSegment.createRow(0);
                SXSSFCell fx = hr.createCell(0);
                fx.setCellValue("File Name");
                Map<String, Integer> cellIndex = sheetCellIndex.get(sheetSegment.getSheetName());
                if (cellIndex == null) {
                    cellIndex = new HashMap<>();
                }
                int idxCellSheet = createSegmentHeader(hr, cellIndex, 1, sc, segmentName);
                List<SegmentConfiguration> segmentConfigurations = sc.getSegments();
                for (SegmentConfiguration child : segmentConfigurations) {
                    createExcelHeader(wb, headerRow, sheetCellIndex, idxCellSheet, segmentName, child, sheetSegment);
                }
                sheetCellIndex.put(sheetSegment.getSheetName(), cellIndex);
            }
        }
        return idxCell;
    }

    private int createSegmentHeader(SXSSFRow headerResults, Map<String, Integer> cellIndex, int idxCell, SegmentConfiguration sc, String segmentName) {
        SXSSFCell scell = headerResults.createCell(idxCell);
        scell.setCellValue(segmentName);
        cellIndex.put("SEGMENT." + segmentName, idxCell);
        idxCell++;
        List<CaptureConfiguration> captureConfigurations = sc.getCaptureConfigurations();
        List<CaptureConfiguration> segmentCaptureConfigurations = sc.getSentenceCaptureConfigurations();
        idxCell = getCapturesHeader(headerResults, cellIndex, idxCell, segmentName, captureConfigurations, segmentCaptureConfigurations);
        return idxCell;
    }

    private int getCapturesHeader(SXSSFRow headerResults, Map<String, Integer> cellIndex, int idxCell, String segmentName, List<CaptureConfiguration> captureConfigurations, List<CaptureConfiguration> segmentCaptureConfigurations) {
        for (CaptureConfiguration capture : captureConfigurations) {
            idxCell = getCaptureHeader(headerResults, cellIndex, idxCell, segmentName, capture);
            List<CaptureConfiguration> scaptureConfigurations = capture.getSubCaptures();
            if (scaptureConfigurations != null) {
                idxCell = getCapturesHeader(headerResults, cellIndex, idxCell, segmentName, scaptureConfigurations, new ArrayList<>());
            }
        }
        for (CaptureConfiguration capture : segmentCaptureConfigurations) {
            idxCell = getCaptureHeader(headerResults, cellIndex, idxCell, segmentName, capture);
            List<CaptureConfiguration> scaptureConfigurations = capture.getSubCaptures();
            if (scaptureConfigurations != null) {
                idxCell = getCapturesHeader(headerResults, cellIndex, idxCell, segmentName, scaptureConfigurations, new ArrayList<>());
            }
        }
        return idxCell;
    }

    private int getCaptureHeader(SXSSFRow headerResults, Map<String, Integer> cellIndex, int idxCell, String segmentName, CaptureConfiguration capture) {
        if (capture.isTemporary()) {
            return idxCell;
        }
        if (capture.isStartPeriod()) {
            String captureName = "SegmentDuration";
            String key = segmentName + "." + captureName;
            if (!cellIndex.containsKey(key)) {
                SXSSFCell cell = headerResults.createCell(idxCell);
                cell.setCellValue(captureName);
                cellIndex.put(key, idxCell);
                idxCell++;
            }
        }

        String captureName = capture.getName();
        String key = segmentName + "." + captureName;
        if (!cellIndex.containsKey(key)) {
            SXSSFCell cell = headerResults.createCell(idxCell);
            cell.setCellValue(captureName);
            cellIndex.put(key, idxCell);
            idxCell++;
        }

        return idxCell;
    }

    /**
     * Scrive l'excel
     *
     * @param fos output stream
     * @throws IOException Eccezione di input/output
     */
    public void write(FileOutputStream fos) throws IOException {
        wb.write(fos);
    }

}
