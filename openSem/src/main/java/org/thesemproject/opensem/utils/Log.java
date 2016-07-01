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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * La classe gestisce il logging di piattaforma attraverso Log4J
 */
public final class Log {
    /**
     * Logger di classe
     */
    private static final Logger logger = LogManager.getLogger(Log.class);

    /**
     * Livello di stack
     */
    private final static int stackLevel = 3;

    /**
     * Scrive sul log un messaggio con livello INFO
     *
     * @param msg messaggio da scrivere
     */
    public static void info(final String msg) {
        logger.info(prepare(msg));
    }

    /**
     * Scrive sul log un messaggio con livello DEBUG
     *
     * @param msg messaggio da scrivere
     */
    public static void debug(final String msg) {
        logger.debug(prepare(msg));
    }

    /**
     * Scrive sul log un messaggio con livello ERROR
     *
     * @param msg messaggio da scrivere
     */
    public static void error(final String msg) {
        logger.error(prepare(msg));
    }

    /**
     * Scrive sul log un messaggio con livello ERROR
     *
     * @param msg messaggio
     * @param obj oggetto da stampare
     */
    public static void error(final String msg, final Object obj) {
        error(msg);
        error(String.valueOf(obj));
    }

    /**
     * Scrive sul log un messaggio con livello WARNING
     *
     * @param msg messaggio da scrivere
     */
    public static void warning(final String msg) {
        logger.warn(prepare(msg));
    }

    /**
     * Scrive sul log un messaggio con livello WARNING
     *
     * @param msg messaggio da scrivere
     */
    public static void warn(final String msg) {
        logger.warn(prepare(msg));
    }

    /**
     * Scrive sul log un messaggio con livello WARNING
     *
     * @param msg messaggio
     * @param obj oggetto da stampare
     */
    public static void warn(final String msg, final Object obj) {
        warn(prepare(msg));
        warn(String.valueOf(obj));
    }

    /**
     * Scrive sul log un messaggio con livello FATAL
     *
     * @param msg messaggio
     */
    public static void fatal(final String msg) {
        logger.fatal(prepare(msg));
    }

    /**
     * Prepara il messaggio per essere stampato aggiungendo il path di chiamata
     *
     * @param msg messaggio da stampare
     * @return messaggio arricchito del path
     */
    private static String prepare(final String msg) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length > stackLevel) {
            String className = stackTraceElements[stackLevel].getClassName();
            int pos = className.lastIndexOf(".");
            className = (pos != -1) ? className.substring(pos + 1) : className;
            return "[" + stackTraceElements[stackLevel].getLineNumber() + "][" + className + "].[" + stackTraceElements[stackLevel].getMethodName() + "]: " + msg;
        }
        return msg;
    }

    /**
     * Gestisce la stampa dello stacktrace di una eccezione sul log di ERROR
     *
     * @param e eccezione da stampare
     */
    public static void printStackTrace(final Throwable e) {
        final StackTraceElement[] st = e.getStackTrace();
        String msg = "\n----------Exception-----------\n";
        msg += String.valueOf(e) + "\n";
        for (StackTraceElement stackTraceElement : st) {
            msg = msg + stackTraceElement + "\n";
        }
        Throwable cause = e.getCause();
        if (cause != null) {
            final StackTraceElement[] stCause = cause.getStackTrace();
            msg += "----- Caused by --------------";
            for (StackTraceElement stackTraceElement : stCause) {
                msg = msg + stackTraceElement + "\n";
            }
            msg += "----- Caused by --------------";
        }
        msg += "----------Exception-----------";
        logger.error(msg);
    }

     
}

