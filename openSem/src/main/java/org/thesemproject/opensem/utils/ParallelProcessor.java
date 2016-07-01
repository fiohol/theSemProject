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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.thesemproject.opensem.gui.LogGui;

/**
 * Questa classe gestisce l'esecuzione in parallelo di piu' threads
 * parallelizzabili distribuendoli sui processori del sistema (o su un numero di
 * thread paralleli deciso dall'utente)
 * <p/>
 * Per gestire le cose si usa il componente standard Java ExecutorService come
 * dettagliatamente descritto nella documentazione ufficiale Oracle
 * <p/>
 * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html">https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html</a>
 * <p/>
 * Esempio di utilizzo:
 * <pre>
 * final ParallelProcessor executor = new ParallelProcessor(Constants.PROCESSORS, 5); //5 minuti al massimo per una aggregazione
 * for (int j = 0; j < Constants.PROCESSORS; j++) {
 *  executor.add(new Runnable() {
 *  public void run() {
 *  for (; ; ) {
 *  // quello che c'e' da fare con tanto di condizione di uscita
 *  }
 * });
 * }
 * executor.waitTermination();
 * </pre>
 */
public class ParallelProcessor {

    /**
     * Esecutore del processo
     */
    private ExecutorService executor;

    /**
     * Timeout del processo
     */
    private int timeout;

    /**
     * Inizializza il sistema
     *
     * @param threads numero di thread contemporanei
     * @param timeout tempo massimo di esecuzione
     */
    public ParallelProcessor(int threads, int timeout) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.timeout = timeout;
    }

    /**
     * Aggiunge un thread al parallel processor
     *
     * @param runnable thread da aggiungere
     */
    public void add(Runnable runnable) {
        try {
            LogGui.info("Add new runnable");
            executor.execute(runnable);
        } catch (Exception e) {
            LogGui.info("Error on Parallel Processor " + e);
            LogGui.info("Shutdown executor....");
            executor.shutdown();
        }
    }

    /**
     * Attende finche' tutti thread sono terminati.
     */
    public void waitTermination() {
        LogGui.info("Block for new thread");
        executor.shutdown();
        try {
            LogGui.info("Wait for termination...");
            executor.awaitTermination(timeout, TimeUnit.MINUTES);
            // interrupt();
            LogGui.info("Execution terminated...");
        } catch (InterruptedException e) {
            LogGui.info("Interrupted: " + e);
            LogGui.printException(e);
        }
    }

    /**
     * Interrompe l'esecuzione dei thread
     */
    public void interrupt() {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
