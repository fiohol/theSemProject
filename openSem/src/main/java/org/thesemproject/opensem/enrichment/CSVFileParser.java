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
package org.thesemproject.opensem.enrichment;

import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.utils.DateUtils;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.plexus.util.StringUtils;

/**
 * Impelementa il parsing di un file CSV basato su org.apache.commons.csv
 */
public class CSVFileParser {

    /**
     * Ritorna il parser costruito secondo i parametri passati per leggere il
     * file
     *
     * @param fileName nome del file da parsare
     * @param delimiter delimitatore delle colonne
     * @param quoting carattere di quoting
     * @param escape carattere di escaping
     * @param lineSeparator separatore di righe
     * @param charset charset UTF o ISO
     * @return Parser per il file CSV
     */
    public static CSVParser getParser(String fileName, String delimiter, String quoting, String escape, String lineSeparator, String charset) {
        File csvData = new File(fileName);
        if (!csvData.exists()) {
            int pos = fileName.replace('\\', '/').lastIndexOf('/');
            if (pos != -1) {
                fileName = fileName.substring(pos + 1);
                csvData = new File("./" + fileName);
            }
        }
        if (csvData.exists()) {
            try {

                CSVFormat format;
                if (delimiter != null && delimiter.length() > 0) {
                    if (delimiter.length() == 2 && delimiter.startsWith("\\")) {
                        if ("\\t".equalsIgnoreCase(delimiter)) {
                            format = CSVFormat.newFormat('\t');
                        } else {
                            format = CSVFormat.newFormat(delimiter.charAt(0));
                        }
                    } else {
                        format = CSVFormat.newFormat(delimiter.charAt(0));
                    }
                } else {
                    format = CSVFormat.newFormat(CSVFormat.DEFAULT.getDelimiter());
                }
                if (quoting != null && quoting.length() > 0) {
                    format = format.withQuote(quoting.charAt(0));
                }
                if (escape != null && escape.length() > 0) {
                    format = format.withEscape(escape.charAt(0));
                }
                if (lineSeparator != null && lineSeparator.length() > 0) {
                    format = format.withRecordSeparator(lineSeparator);
                }
                return CSVParser.parse(csvData, Charset.forName(charset), format);
            } catch (Exception e) {
                LogGui.printException(e);
            }
        }
        return null;
    }

    /**
     * Parsa un file csv e cerca di identificare i tipi di dati per ogni colonna
     *
     * @param fileName nome del file
     * @param delimiter delimitatore di colonna
     * @param quoting caratteri di quoting
     * @param escape carattere di escaping
     * @param lineSeparator separatore di linea
     * @param charset UTF o ISO
     * @return Lista di tipi per ogni colonna (numeric, date, text, boolean)
     */
    public static List<String[]> getFiledsType(String fileName, String delimiter, String quoting, String escape, String lineSeparator, String charset) {
        List<String[]> ret = new ArrayList<>();
        Set<String> fields = new HashSet<>();

        CSVParser parser = getParser(fileName, delimiter, quoting, escape, lineSeparator, charset);
        int count = 0;
        for (CSVRecord csvRecord : parser) {
            if (count == 0) {
                for (String field : csvRecord) {
                    if (field.trim().length() == 0) {
                        field = "Field";
                    }
                    String original = field.trim();
                    int fCount = 1;
                    while (fields.contains(field)) {
                        field = original + "_" + fCount;
                        fCount++;
                    }
                    field = field.trim();
                    fields.add(field);
                    String[] fieldDescriptor = new String[4];
                    fieldDescriptor[0] = String.valueOf(ret.size());
                    fieldDescriptor[1] = field;
                    fieldDescriptor[2] = "text";
                    fieldDescriptor[3] = String.valueOf(ret.size() + 1);
                    ret.add(fieldDescriptor);
                }
            } else {
                int pos = 0;
                for (String field : csvRecord) {
                    if (pos < ret.size()) {
                        String[] fieldDescriptor = ret.get(pos);
                        fieldDescriptor[2] = identifyType(field);
                        ret.set(pos, fieldDescriptor);

                    }
                    pos++;
                }
            }
            count++;
            if (count == 2) {
                break;
            }
        }
        return ret;
    }

    private static String identifyType(String field) {
        if (StringUtils.isNumeric(field)) {
            return "number";
        }
        if (DateUtils.parseDate(field) != null) {
            return "date";
        }
        if ("true".equalsIgnoreCase(field) || "false".equalsIgnoreCase(field)) {
            return "boolean";
        }
        return "text";
    }

}
