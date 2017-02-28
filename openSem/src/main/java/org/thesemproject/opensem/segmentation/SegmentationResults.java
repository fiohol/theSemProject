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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.segmentation.functions.Durations;
import org.thesemproject.opensem.utils.DateUtils;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce i risultati di un processo di segmentazione su un segmento.
 */
public class SegmentationResults implements Serializable {

    /**
     * Formattatore per i numeri
     */
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.ITALY));
    private static final Map<String, DecimalFormat> DECIMAL_FORMAT_CACHE = new ConcurrentHashMap<>();
    List<String> sentencies;
    Map<SegmentConfiguration, List<SegmentationResults>> subsentencies;
    Map<CaptureConfiguration, String> captureConfigurationResults;
    Map<String, String> captureResults;
    private final List<String> lines;
    private final List<ClassificationPath> classificationPaths;
    private final Set<String> paths;
    private boolean isClassifiedByCapture;

    private Date startDate;
    private Date endDate;
    private int startYear;
    private int endYear;
    private boolean endYearNotPresent;

    /**
     * Ritorna il path di classificazione
     *
     * @return Lista dei path di classificazione. Ogni elemento della lista è un
     * percorso di classificazione su un albero
     */
    public List<ClassificationPath> getClassificationPaths() {
        return classificationPaths;
    }

    /**
     * Crea un segmentationResults
     */
    public SegmentationResults() {
        sentencies = new ArrayList<>();
        subsentencies = new LinkedHashMap<>();
        captureConfigurationResults = new LinkedHashMap<>();
        captureResults = new LinkedHashMap<>();
        lines = new ArrayList<>();
        classificationPaths = new ArrayList<>();
        paths = new HashSet<>();
        isClassifiedByCapture = false;
        startDate = null;
        endDate = null;
        startYear = -1;
        endYear = -1;
        endYearNotPresent = false;

    }

    /**
     * Ritorna la lista delle frasi (sentencies)
     *
     * @return Lista di frasi
     */
    public List<String> getSentencies() {
        return sentencies;
    }

    /**
     * Ritorna le sotto sentenze come mappa di Configurazione Segmento e Lista
     * di SegmentationResult. Viene applicato ai sottosegmenti di un segmento
     * (Organizzazione gerarchica)
     *
     * @return mappa {SegmentConfiguration, Lista di segmentation result}
     */
    public Map<SegmentConfiguration, List<SegmentationResults>> getSubsentencies() {
        return subsentencies;
    }

    /**
     * Imposta i risultati di una sottosegmentazione
     *
     * @param subsentencies risultato della sottosegmentazione
     */
    public void setSubsentencies(Map<SegmentConfiguration, List<SegmentationResults>> subsentencies) {
        this.subsentencies = subsentencies;
    }

    /**
     * Aggiunge una frase
     *
     * @param sentence sentenza
     */
    public void addSentence(String sentence) {
        this.sentencies.add(sentence);
    }

    /**
     * Ritorna le righe di un testo
     *
     * @return righe come lista di stringhe
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Aggiunge una linea al segment result
     *
     * @param line linea da aggiungere
     */
    public void addLine(String line) {
        lines.add(line);
    }

    /**
     * Aggiunge il risultato di una cattura
     *
     * @param captureConfiguration configurazione della cattura
     * @param value valore catturato
     */
    public void addCaptureResult(CaptureConfiguration captureConfiguration, String value) {
        addCaptureResult(captureConfiguration, value, false);
    }

    /**
     * Aggiunge il risultato di una cattura
     *
     * @since 1.2
     * @param captureConfiguration configurazione della cattura
     * @param value valore catturato
     * @param override true se si vuole andare in sovrascrittura
     */
    public void addCaptureResult(CaptureConfiguration captureConfiguration, String value, boolean override) {
        if (value == null) {
            return;
        }
        value = value.trim();
        //Sistemo i tipi
        String type = captureConfiguration.getType();
        String format = captureConfiguration.getFormat();
        boolean toFormat = format.length() > 0;
        Formatter formatter;
        formatter = new Formatter();
        if ("date".equalsIgnoreCase(type)) {
            //Devo validare che sia una data
            if (toFormat) {
                Date date = DateUtils.parseDate(value);
                if (date == null) {
                    return;
                }
                try {
                    Formatter formatter1 = formatter.format(format, date);
                    value = formatter1.toString();
                    setPeriodFromDate(captureConfiguration, date);
                } catch (Exception e) {
                    LogGui.printException(e);
                    return;
                }
            } else {
                value = DateUtils.parseString(value);
                if (value == null) {
                    return;
                } else {
                    setPeriodFromDate(captureConfiguration, DateUtils.parseDate(value));
                }
            }
        } else if ("integer".equalsIgnoreCase(type)) {
            //Devo validare che sia un numero
            try {
                int integer = Integer.parseInt(value);
                if (toFormat) {
                    value = formatter.format(format, integer).toString();
                } else {
                    value = String.valueOf(integer);
                }
                if (integer > 1000) { //Anni 
                    setPeriodFromInt(captureConfiguration, integer);
                }
            } catch (NumberFormatException e) {
                return;
            }

        } else if ("real".equalsIgnoreCase(type)) {
            //Devo validare che sia un numero
            try {
                double real = Double.parseDouble(value);
                if (toFormat) {
                    value = formatter.format(format, real).toString();
                } else {
                    value = DECIMAL_FORMAT.format(real);
                }
            } catch (NumberFormatException e) {
                return;
            }

        } else if ("number".equalsIgnoreCase(type)) {
            //Devo validare che sia un numero
            try {
                double real = Double.parseDouble(value.replace(",", ".").replace(" ", ""));
                if (toFormat) {
                    DecimalFormat myFormatter = DECIMAL_FORMAT_CACHE.get(format);
                    if (myFormatter == null) {
                        try {
                            myFormatter = new DecimalFormat(format, new DecimalFormatSymbols(Locale.ITALY));
                            DECIMAL_FORMAT_CACHE.put(format, myFormatter);
                        } catch (Exception e) {
                            LogGui.info(e.getLocalizedMessage());
                            return;
                        }
                    }
                    value = myFormatter.format(real);

                } else {
                    value = DECIMAL_FORMAT.format(real);
                }
            } catch (NumberFormatException e) {
                return;
            }

        } else if ("boolean".equalsIgnoreCase(type)) {
            if (value.length() > 0) {
                value = toFormat ? format : "Y";
            }
        } else if ("text".equalsIgnoreCase(type)) {
            if (toFormat) {
                if (format.contains("@clean")) {
                    value = value.replace(" ", "").replace(".", "").replace(";", "").replace(":", "");
                } else {
                    value = formatter.format(format, value).toString();
                }
            }
        }
        value = value.trim();
        captureConfigurationResults.put(captureConfiguration, value);
        String old = captureResults.get(captureConfiguration.getName());
        if (old != null && !override) {
            if (!old.equalsIgnoreCase(value) && !old.contains(value + ", ") && !old.endsWith(", " + value)) {
                captureResults.put(captureConfiguration.getName(), old + ", " + value);
            }
        } else {
            captureResults.put(captureConfiguration.getName(), value);
        }
    }

    /**
     * Ritorna il risultato delle catture
     *
     * @return risultati delle catture come configurazione fattura e valore
     * catturato
     */
    public Map<CaptureConfiguration, String> getCaptureConfigurationResults() {
        return captureConfigurationResults;
    }

    /**
     * Ritorna il risultato delle catture
     *
     * @return risultati delle catture come nome della cattura e valore della
     * cattura. Dato che ci possono essere più capture configuration con lo
     * stesso nome cattura, non necessariamente il risultato di questa chiamata
     * sarà uguale al precedente metodo,
     */
    public Map<String, String> getCaptureResults() {
        return captureResults;
    }

    void addClassificationPath(List<ClassificationPath> path) {
        if (path == null) {
            return;
        }
        for (ClassificationPath cp : path) {
            String small = cp.toSmallClassString();
            if (!paths.contains(small)) {
                this.classificationPaths.add(cp);
                this.paths.add(small);
            }
        }
        //this.classificationPaths.addAll(path);

    }

    void setClassifyByCapture(boolean b) {
        this.isClassifiedByCapture = b;
    }

    boolean isClassifyByCapture() {
        return isClassifiedByCapture;
    }

    /**
     * Rimuove la configuraizone di una cattura. Usata per le catture temporanee
     *
     * @param captureConfiguration configurazione di una cattura
     */
    public void removeCaptureConfigurationResults(CaptureConfiguration captureConfiguration) {
        if (captureConfiguration == null) {
            return;
        }
        captureConfigurationResults.remove(captureConfiguration);
        captureResults.remove(captureConfiguration.getName());
    }

    /**
     * Rimuove una cattura e tutti i suoi effetti
     *
     * @since 1.4
     * @param captureName cattura da rimuovere
     */
    public void removeCaptureConfigurationResults(String captureName) {
        if (captureName == null) {
            return;
        }
        captureResults.remove(captureName);
        Set<CaptureConfiguration> toRemove = new HashSet<>();
        for (CaptureConfiguration cc : captureConfigurationResults.keySet()) {
            if (cc.getName().equals(captureName)) {
                toRemove.add(cc);
            }
        }
        for (CaptureConfiguration cc : toRemove) {
            if (cc.getClassificationPath() != null) {
                classificationPaths.remove(cc.getClassificationPath());
                paths.remove(cc.getClassificationPath().toSmallClassString());
                if (classificationPaths.isEmpty()) {
                    isClassifiedByCapture = false;
                }
            }
            captureConfigurationResults.remove(cc);
        }
    }

    /**
     * Ritorna gli anni in cui il segmentResult ha valori.
     *
     * @return lista degli anni
     */
    public String[] getYears() {
        checkEndPeriod();
        if (startDate != null) {
            return DateUtils.getYears(startDate, endDate);
        }
        return null;
    }

    /**
     * Ritorna i mesi in cui il segmentResult ha valori. Se tra le catture del
     * segmento esiste almeno una cattura di "Inizio periodo" valorizzato, il
     * sistema calcola in automatico gli anni e i mesi di copertura del
     * segmento. Se c'è anche una cattura di "fine periodo" la durata è
     * calcolata come distanza tra inizio e fine periodo. Altrimenti tra inizio
     * periodo e data attuale.
     *
     * @return elenco dei mesi coperti dal segmento
     */
    public String[] getMonths() {
        checkEndPeriod();
        if (startDate != null) {
            return DateUtils.getMonths(startDate, endDate);
        }
        return null;
    }

    /**
     * Ritorna la durata in anni del segmento
     *
     * @return durata in anni (con parte decimale) del segmento
     */
    public double getDurationYears() {
        checkEndPeriod();
        if (startDate != null) {
            return DateUtils.getDifferenceYears(startDate, endDate);
        }
        return 0;
    }

    /**
     * Ritorna la durata in anni del segmento come Anno Fine - Anno Inizio + 1
     *
     * @return durata assoluta in anni del segmento
     */
    public int getIntDurationYears() {
        checkEndPeriod();
        if (startDate != null) {
            return DateUtils.getDifferenceYears(startYear, endYear);
        }
        return 0;
    }

    private void setPeriodFromDate(CaptureConfiguration captureConfiguration, Date date) {
        if (date == null) {
            return;
        }
        if (captureConfiguration.isStartPeriod()) {
            startDate = new Date(date.getTime());
            if (startYear == -1) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                startYear = cal.get(Calendar.YEAR);
            }
        }
        if (captureConfiguration.isEndPeriod()) {
            endDate = new Date(date.getTime());
            if (endYear == -1) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                endYear = cal.get(Calendar.YEAR);
            }
        }
    }

    private void setPeriodFromInt(CaptureConfiguration captureConfiguration, int date) {
        if (date < 1000) {
            return;
        }
        if (captureConfiguration.isStartPeriod()) {
            if (startDate == null) {
                startDate = DateUtils.parseDate("01/01/" + date);
            }
            startYear = date;
        }
        if (captureConfiguration.isEndPeriod()) {
            if (endDate == null) {
                endDate = DateUtils.parseDate("31/12/" + date);
            }
            endYear = date;
        }
    }

    private void checkEndPeriod() {
        if (endDate == null) {
            endDate = new Date();
            endYearNotPresent = true;
        }
        if (endYear == -1) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            endYear = cal.get(Calendar.YEAR);
            endYearNotPresent = true;
        }
    }

    /**
     * Ritorna le durate del segmento
     *
     * @return Durate del segmento
     */
    public Durations getDurations() {
        double duration = getDurationYears();
        if (duration > 0) {
            Durations ret = new Durations();
            ret.setDurationYears(duration);
            ret.setIntDurationYears(getIntDurationYears());
            ret.setYears(getYears());
            ret.setMonths(getMonths());
            ret.setIsEndYearExtracted(!endYearNotPresent);
            return ret;
        }
        return null;
    }

    /**
     * Imposta la data di fine di un segmento sottoforma di data completa.
     * Questa funzionalità permette di andare in correzione anche dopo la
     * segmentazione sulla data di fine periodo Se il sistema, infatti non
     * riesce a calcolare la data di fine, assume come data di finel la data
     * attuale. Attraverso questo medoto è possibile correggere il comportamento
     *
     * @param endDate data di fine del segmento
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        endYear = cal.get(Calendar.YEAR);
    }

    /**
     * Imposta la data di fine di un segmento sottoforma di anno di fine.
     * Automaticamente il sistema imposta la data reale di fine come 31/12/Anno
     *
     * @param endYear anno di fine del segmento
     */
    public void setEndYear(int endYear) {
        this.endYear = endYear;
        endDate = DateUtils.parseDate("31/12/" + endYear);
    }

    /**
     * Ritorna il testo del segmentation result
     *
     * @since 1.2
     * @return testo
     */
    public String getText() {
        StringBuilder text = new StringBuilder();
        lines.stream().forEach((line) -> {
            text.append(line).append(SegmentEngine.CR);
        });
        return text.toString();
    }

}
