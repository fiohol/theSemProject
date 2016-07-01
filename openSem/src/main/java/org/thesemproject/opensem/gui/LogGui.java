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

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.thesemproject.opensem.utils.Log;

/**
 * Gestisce il log grafico e il log su Log4J
 *
 */
public class LogGui {

    private static javax.swing.JTextArea jTextArea;
    private static JLabel memInfo;
    private static SystemInfo si = new SystemInfo();

    /**
     * Stampa un messaggio sul log, Quando la textarea che rappresenta il log
     * supera le 200 righe rimuove le righe precedenti. Contemporaneamente
     * stampa il log sullo streaming Log4J
     *
     * @param message messaggio da stampare sul log
     */
    public static void info(String message) {
        if (jTextArea != null) {
            if (jTextArea.getLineCount() > 200) {
                StringBuffer old = new StringBuffer(jTextArea.getText());
                jTextArea.setText(old.substring(old.length() / 2));
            }
            jTextArea.append("\n" + message);
            Log.debug(message);
            jTextArea.setCaretPosition(jTextArea.getText().length() - 1);
            updateMemoryInfo();
        } else {
            Log.info(message);
        }
    }

    /**
     * Imposta l'oggetto textarea che conterrà il log
     *
     * @param jTextArea oggetto dove scrivere il log
     */
    public static void setjTextArea(JTextArea jTextArea) {
        LogGui.jTextArea = jTextArea;
    }

    /**
     * Imposta la JLabel che deve contenere le informazioni di memoria
     *
     * @param label label che rappresenterà lo stato della memoria
     */
    public static void setMemInfo(JLabel label) {
        memInfo = label;
    }

    /**
     * Stampa un'eccezione
     *
     * @param e eccezione da stampare
     */
    public static void printException(Exception e) {
        if (jTextArea != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
            }
            LogGui.info("ECCEZIONE: " + e.getMessage() + "\n" + sw.toString());
        } else {
            Log.printStackTrace(e);
        }
    }

    /**
     * Stampa le informazione di sistema e la memoria
     */
    public static void printSystemInfo() {
        info("\n---------------");
        info(si.info());
        info(si.getAllocatedMemorySummary());
        info("---------------\n");
    }

    /**
     * Stampa il riassunto della memoria
     */
    public static void printMemorySummary() {
        info(si.getAllocatedMemorySummary());
    }

    /**
     * Ritorna le informazione sulla compact memory
     *
     * @return informazioni sulla memoria
     */
    public static String getCompactMemInfo() {
        return si.getAllocatedMemorySummary();
    }

    /**
     * Aggiorna le informazioni di memoria sulla label
     */
    public static void updateMemoryInfo() {
        if (memInfo != null) {
            memInfo.setText(LogGui.getCompactMemInfo());
        }
    }

}
