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
package org.thesemproject.opensem.utils;

import java.text.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Utility per la gestione e il parsing delle date
 */
public class DateUtils {

    /**
     * Formato base per la data
     */
    public static final DateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY);

    /**
     * Patterns che possono corrispondere ad un modo di scrivere una data
     */
    public static final Pattern PATTERNS[] = {Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^(0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^(19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])$"),
        Pattern.compile("^([1-9])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^([1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^(19|20)\\d\\d[- /.](0[1-9])[- /.](0[1-9]|[12][0-9]|3[01])$"),
        Pattern.compile("^(19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.]([1-9])$"),
        Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.]\\d\\d$"),
        Pattern.compile("^(0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])[- /.]\\d\\d$"),
        Pattern.compile("^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[012])(19|20)\\d\\d$"),
        Pattern.compile("^([1-9])[- /.]([1-9])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^(19|20)\\d\\d(0[1-9]|1[012])([0-9])([0-9])$"),
        Pattern.compile("^([1-9])[- /.](0[1-9]|[12][0-9]|3[01])[- /.]\\d\\d$"),
        Pattern.compile("^([1-9])[- /.](0[1-9]|1[012])[- /.]\\d\\d$"),
        Pattern.compile("^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[012])\\d\\d$"),
        Pattern.compile("^(0[1-9]|1[012])[- /.](19|20)\\d\\d$"),
        Pattern.compile("^(0[1-9]|[12][0-9]|3[01])[- /.]([1-9])[- /.](19|20)\\d\\d$")

    };

    /**
     * Modi di scrivere gennaio
     */
    public static final String GEN = "janu(a|á)r|genn?aio|january|(gen|jan)(\\.)?";

    /**
     * Modi di scrivere febbraio
     */
    public static final String FEB = "febb?raio|february|febru(a|ár)|(feb|febbr|febb)(\\.)?";

    /**
     * Modi di scrivere marzo
     */
    public static final String MAR = "marec|marzo|march|mar(\\.)?";

    /**
     * Modi di scrivere aprile
     */
    public static final String APR = "apr(\\.)?|aprile|april|apri(\\.)?";

    /**
     * Modi di scrivere maggio
     */
    public static final String MAG = "m(a|á)j|magg?io|may|mag(\\.)?";

    /**
     * Modi di scrivere giugno
     */
    public static final String GIU = "j(u|ú)n|giugno|june|(giu|jun)(\\.)?";

    /**
     * Modi di scrivere luglio
     */
    public static final String LUG = "j(u|ú)l|luglio|july|(jul|lug)(\\.)?";

    /**
     * Modi di scrivere agosto
     */
    public static final String AGO = "agosto|august|(ago|augu|agu|aug)(\\.)?";

    /**
     * Modi di scrivere settembre
     */
    public static final String SET = "septemb(ra|er)|sett?embre|september|(set|sep|sett|sept)(\\.)?";

    /**
     * Modi di scrivere ottobre
     */
    public static final String OTT = "ott?obre|october|okt(o|ó)ber|(ott|okt|oct)(\\.)?";

    /**
     * Modi di scrivere novembre
     */
    public static final String NOV = "novembre|november|nov(\\.)?";

    /**
     * Modi di scrivere dicembre
     */
    public static final String DIC = "dicembre|december|(dic|dec)(\\.)?";

    private static final ConcurrentHashMap<String, String> STRING_FORMAT_CACHE = new ConcurrentHashMap<>();

    /**
     * Prova a parsare una stringa cercando di interpretarla come data e ritorna
     * una Stringa contenente una data ben formattata
     *
     * @param stringDate data da parsare
     * @return data ben formattata
     */
    public static String parseString(String stringDate) {
        if (stringDate == null) {
            return null;
        }
        stringDate = stringDate.toLowerCase();
        stringDate = stringDate.replace("à","a");
        stringDate = stringDate.replace("è","e");
        stringDate = stringDate.replace("é","e");
        stringDate = stringDate.replace("ì","i");
        stringDate = stringDate.replace("ò","o");
        stringDate = stringDate.replace("ù","u");
        stringDate = stringDate.replaceAll("\\b("+GEN+")\\b", "01");
        stringDate = stringDate.replaceAll("\\b("+FEB+")\\b", "02");
        stringDate = stringDate.replaceAll("\\b("+MAR+")\\b", "03");
        stringDate = stringDate.replaceAll("\\b("+APR+")\\b", "04");
        stringDate = stringDate.replaceAll("\\b("+MAG+")\\b", "05");
        stringDate = stringDate.replaceAll("\\b("+GIU+")\\b", "06");
        stringDate = stringDate.replaceAll("\\b("+LUG+")\\b", "07");
        stringDate = stringDate.replaceAll("\\b("+AGO+")\\b", "08");
        stringDate = stringDate.replaceAll("\\b("+SET+")\\b", "09");
        stringDate = stringDate.replaceAll("\\b("+OTT+")\\b", "10");
        stringDate = stringDate.replaceAll("\\b("+NOV+")\\b", "11");
        stringDate = stringDate.replaceAll("\\b("+DIC+")\\b", "12");
        stringDate = stringDate.replace(". ", ".").replace("/ ","/");
        String ret = STRING_FORMAT_CACHE.get(stringDate);
        if (ret != null) {
            return ret;
        }

        int pos = stringDate.indexOf("00:00:00");
        if (pos != -1) {
            stringDate = stringDate.substring(0, pos).trim();
        }
        String str = null;
        stringDate = stringDate.trim();
        int len = stringDate.length();
        switch (len) {
            case 10:
                if (PATTERNS[0].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + '/' + stringDate.substring(3, 5) + '/' + stringDate.substring(6);
                } else if (PATTERNS[1].matcher(stringDate).matches()) {
                    str = stringDate.substring(3, 5) + '/' + stringDate.substring(0, 2) + '/' + stringDate.substring(6);
                } else if (PATTERNS[2].matcher(stringDate).matches()) {
                    str = stringDate.substring(8) + '/' + stringDate.substring(5, 7) + '/' + stringDate.substring(0, 4);
                }
                break;
            case 9:
                if (PATTERNS[3].matcher(stringDate).matches()) {
                    str = '0' + stringDate.substring(0, 1) + '/' + stringDate.substring(2, 4) + '/' + stringDate.substring(5);
                } else if (PATTERNS[4].matcher(stringDate).matches()) {
                    str = stringDate.substring(2, 4) + "/0" + stringDate.substring(0, 1) + '/' + stringDate.substring(5);
                } else if (PATTERNS[5].matcher(stringDate).matches()) {      //2010/1/23
                    str = stringDate.substring(7) + "/0" + stringDate.substring(5, 6) + '/' + stringDate.substring(0, 4);
                } else if (PATTERNS[6].matcher(stringDate).matches()) {
                    str = '0' + stringDate.substring(8) + '/' + stringDate.substring(5, 7) + '/' + stringDate.substring(0, 4);
                } else if (PATTERNS[16].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + "/0" + stringDate.substring(3, 4) + '/' + stringDate.substring(5);
                }
                break;
            case 8:
                if (PATTERNS[7].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + '/' + stringDate.substring(3, 5) + "/20" + stringDate.substring(6);
                } else if (PATTERNS[8].matcher(stringDate).matches()) {
                    str = stringDate.substring(3, 5) + '/' + stringDate.substring(0, 2) + "/20" + stringDate.substring(6);
                } else if (PATTERNS[9].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + '/' + stringDate.substring(2, 4) + stringDate.substring(4);
                } else if (PATTERNS[10].matcher(stringDate).matches()) {
                    str = '0' + stringDate.substring(0, 1) + "/0" + stringDate.substring(2, 3) + "/" + stringDate.substring(4);
                } else if (PATTERNS[11].matcher(stringDate).matches()) {
                    str = stringDate.substring(6) + '/' + stringDate.substring(4, 6) + '/' + stringDate.substring(0, 4);
                }
                break;
            case 7:
                if (PATTERNS[12].matcher(stringDate).matches()) {
                    str = stringDate.substring(2, 4) + "/0" + stringDate.substring(0, 1) + "/20" + stringDate.substring(5);
                } else if (PATTERNS[13].matcher(stringDate).matches()) {
                    str = '0' + stringDate.substring(0, 1) + '/' + stringDate.substring(2, 4) + '/' + stringDate.substring(5);
                } else if (PATTERNS[15].matcher(stringDate).matches()) {
                    str = "01/" + stringDate.substring(0, 2) + '/' + stringDate.substring(3);
                }
                break;
            case 6:
                if (PATTERNS[7].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + '/' + stringDate.substring(3, 5) + "/20" + stringDate.substring(4);
                } else if (PATTERNS[8].matcher(stringDate).matches()) {
                    str = stringDate.substring(3, 5) + '/' + stringDate.substring(0, 2) + "/20" + stringDate.substring(4);
                } else if (PATTERNS[14].matcher(stringDate).matches()) {
                    str = stringDate.substring(0, 2) + '/' + stringDate.substring(2, 4) + "/20" + stringDate.substring(4);
                }
                break;
            default:
                break;
        }
        if (str != null) {
            STRING_FORMAT_CACHE.put(stringDate, str);
        }
        if (str == null) {
            return stringDate;
        }
        return str;
    }

    /**
     * Parsa una stringa come se fosse una data e ritorna un oggetto tipo data
     *
     * @param stringDate stringa da parsare
     * @return data parsata (null se trova errori)
     */
    public static Date parseDate(String stringDate) {
        String str = parseString(stringDate);
        if (str != null) {
            try {
                return DATEFORMAT.parse(str);
            } catch (ParseException ex) {
                return null;
            }
        }
        return null;
    }

    private static long getDifferenceMillis(Date from, Date to) {
        if (from == null) {
            return 0;
        }
        if (to == null) {
            to = new Date();
        }
        return (to.getTime() - from.getTime());
    }

    /**
     * Calcola la differenza in giorni tra due date
     *
     * @param from data da
     * @param to data a
     * @return differenza in giorni
     */
    public static double getDifferenceDays(Date from, Date to) {
        return Math.round((getDifferenceMillis(from, to) / (1000 * 60 * 60 * 24)) * 100.0) / 100.0;
    }

    /**
     * Calcola la differenza in anni tra due date
     *
     * @param from data da
     * @param to data a
     * @return differenza in anni
     */
    public static double getDifferenceYears(Date from, Date to) {
        return Math.round((getDifferenceDays(from, to) / 365) * 100.0) / 100.0;
    }

    /**
     * Calcola la differenza in anni (valore assoluto) tra due date
     *
     * @param from data da
     * @param to data a
     * @return differenza in anni
     */
    public static int getDifferenceYears(int from, int to) {
        return (to - from) + 1;
    }

    /**
     * Calcola la differenza in giorni tra due "anni"
     *
     * @param from anno inizio
     * @param to anno fine
     * @return differenza in giorni
     */
    public static double getDifferenceDays(int from, int to) {
        return getDifferenceYears(from, to) * 365;
    }

    /**
     * Elenco degli anni tra due date
     *
     * @param from data da
     * @param to data a
     * @return elenco anni
     */
    public static String[] getYears(Date from, Date to) {
        return getInterval(from, to, Calendar.YEAR, "yyyy");
    }

    /**
     * Elenco anni tra due anni
     *
     * @param from anno inizio
     * @param to anno fine
     * @return elenco anni
     * @throws Exception Eccezione
     */
    public static String[] getYears(int from, int to) throws Exception {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String start = "01-01-" + from;
        String stop = "31-12-" + to;
        return getInterval(formatter.parse(start), formatter.parse(stop), Calendar.YEAR, "yyyy");
    }

    /**
     * mesi tra due anni
     *
     * @param from anno inizio
     * @param to anno fine
     * @return elenco dei mesi
     * @throws Exception Eccezione
     */
    public static String[] getMonths(int from, int to) throws Exception {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String start = "01-01-" + from;
        String stop = "31-12-" + to;
        return getInterval(formatter.parse(start), formatter.parse(stop), Calendar.MONTH, "yyyy-MM");
    }

    /**
     * elenco dei mesi tra due date
     *
     * @param from data da
     * @param to data a
     * @return elenco dei mesi
     */
    public static String[] getMonths(Date from, Date to) {
        return getInterval(from, to, Calendar.MONTH, "yyyy-MM");
    }

    /**
     * Ritorna l'intervallo tra due date
     *
     * @param from data da
     * @param to data a
     * @param field campo calendar
     * @param format format della data
     * @return lista oggetti rappresentati dal field
     */
    public static String[] getInterval(Date from, Date to, int field, String format) {
        List<String> ret = new ArrayList<>();
        if (from == null) {
            return null;
        }
        if (to == null) {
            to = new Date();
        }
        DateFormat formatter = new SimpleDateFormat(format);

        Calendar beginCalendar = Calendar.getInstance();
        Calendar finishCalendar = Calendar.getInstance();
        beginCalendar.setTime(from);
        finishCalendar.setTime(to);
        while (beginCalendar.before(finishCalendar)) {
            String date = formatter.format(beginCalendar.getTime()).toUpperCase();
            ret.add(date);
            beginCalendar.add(field, 1);
        }
        String[] retArray = new String[ret.size()];
        for (int i = 0; i < ret.size(); i++) {
            retArray[i] = ret.get(i);
        }
        return retArray;
    }
    
    public static void main (String args[]) {
    
        System.out.println(DateUtils.parseString("april 1990"));
        
    }
}
