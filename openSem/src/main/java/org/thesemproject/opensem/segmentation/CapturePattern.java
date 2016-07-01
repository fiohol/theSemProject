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

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Questo oggetto rappresenta un pattern di cattura. Un pattern di cattura è
 * composto dal pattern regex, la posizione rispetto al pattern del token che
 * bisogna catturare, valore di normalizzazione
 */
public class CapturePattern implements Serializable {

    int position;
    Pattern pattern;
    String fixValue;

    /**
     * Definisce un pattern di cattura
     *
     * @param position posizione
     * @param pattern pattern
     * @param fixValue normalizzazione
     */
    public CapturePattern(int position, Pattern pattern, String fixValue) {
        this.position = position;
        this.pattern = pattern;
        if (fixValue == null) {
            fixValue = "";
        }
        this.fixValue = fixValue;
    }

    /**
     * Ritorna la posizione di cattura. Se è 0 significa che tutto quanto matcha
     * il pattern viene catturato. Se è maggiore di zero viene catturato quanto
     * il blocco indicato cattura. Per esempio se il pattern è (1-9)([A-Z]) e il
     * testo è 3A se la posizione è 0 la cattura varrà "3A" se è 1 la cattura
     * varrà "3" se 2 la cattura varrà "A"
     *
     * @return posizione di cattura
     */
    public int getPosition() {
        return position;
    }

    /**
     * Imposta la posizione da cattura
     *
     * @param position posizione di cattura
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Ritorna il pattern di cattura
     *
     * @return pattern di cattura
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Imposta il pattern di cattura
     *
     * @param pattern pattern di cattura
     */
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Ritorna il valore di formattazione o di normalizzazione
     *
     * @return valore di formattazione
     */
    public String getFixValue() {
        return fixValue;
    }

    /**
     * Imposta il valore di normalizzazione o il pattern di formattazione
     *
     * @param fixValue valore di formattazione
     */
    public void setFixValue(String fixValue) {
        this.fixValue = fixValue;
    }

}
