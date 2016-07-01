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
import java.text.NumberFormat;

/**
 *
 * Gestisce le informazioni di sistema
 */
public class SystemInfo {

    private Runtime runtime = Runtime.getRuntime();

    private static NumberFormat format = NumberFormat.getInstance();

    /**
     * Ritorna le informazione del sistema
     * @return informazioni del sistema
     */
    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.osInfo());
        sb.append("\n");
        sb.append(this.memInfo());
        return sb.toString();
    }

    /**
     * Ritorna il nome del sistema operativo
     * @return nome del sistema operativo
     */
    public String OSname() {
        return System.getProperty("os.name");
    }

    /**
     * Ritorna la versione del sistema operativo
     * @return versione del sistema operativo
     */
    public String OSversion() {
        return System.getProperty("os.version");
    }

    /**
     * Ritorna l'architettura hardware
     * @return architettura hardware
     */
    public String OsArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Ritorna la memoria totale (in byte)
     * @return memoria totale
     */
    public long totalMem() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * Ritorna la memoria usata
     * @return memoria usata
     */
    public long usedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Ritorna le informazioni sulla memoria
     * @return informazioni sulla memoria
     */
    public String memInfo() {

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        sb.append("Free memory: ");
        sb.append(format.format(freeMemory / 1024));
        sb.append("\n");
        sb.append("Allocated memory: ");
        sb.append(format.format(allocatedMemory / 1024));
        sb.append("\n");
        sb.append("Max memory: ");
        sb.append(format.format(maxMemory / 1024));
        sb.append("\n");
        sb.append("Total free memory: ");
        sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
        return sb.toString();

    }

    /**
     * Ritorna il riassunto della memoria allocata
     * @return riassunto memoria allocata
     */
    public String getAllocatedMemorySummary() {

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long totalFreeMemory = (freeMemory + (maxMemory - allocatedMemory));
        long totalAllocatedMemory = maxMemory - totalFreeMemory;
        double perc = ((double)totalAllocatedMemory / (double)maxMemory) * 100d;
        sb.append("Memory: ").append(format.format(totalAllocatedMemory/1024)).append("/").append(format.format(maxMemory/1024)).append(" ").append(format.format(perc)).append("%");
        return sb.toString();
    }

    /**
     * Ritorna le informazioni di sistema operativo
     * @return informazioni di sistema operativo
     */
    public String osInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: ");
        sb.append(this.OSname());
        sb.append("\n");
        sb.append("Version: ");
        sb.append(this.OSversion());
        sb.append("\n");
        sb.append("Arch: ");
        sb.append(this.OsArch());
        sb.append("\n");
        sb.append("Available processors (cores): ");
        sb.append(runtime.availableProcessors());
        return sb.toString();
    }

    /**
     * Ritorna le informazioni sul disco corrento
     * @return informazioni sul disco
     */
    public String DiskInfo() {
        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();
        StringBuilder sb = new StringBuilder();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            sb.append("File system root: ");
            sb.append(root.getAbsolutePath());
            sb.append("\n");
            sb.append("Total space (bytes): ");
            sb.append(root.getTotalSpace());
            sb.append("\n");
            sb.append("Free space (bytes): ");
            sb.append(root.getFreeSpace());
            sb.append("\n");
            sb.append("Usable space (bytes): ");
            sb.append(root.getUsableSpace());
            sb.append("\n");
        }
        return sb.toString();
    }
}
