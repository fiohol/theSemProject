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

/**
 * Boolean final per essere utilizzato nei thread
 */
public class FinalBoolean {
    /**
     * boolean
     */
    boolean ret;

    /**
     * costruttore
     *
     * @param ret valore di inizializzazione
     */
    public FinalBoolean(boolean ret) {
        this.ret = ret;
    }

    /**
     * Ritorna il valore del boolean
     *
     * @return boolean
     */
    public boolean getValue() {
        return ret;
    }

    /**
     * Imposta il valore del boolean
     *
     * @param ret valore del boolean
     */
    public void setValue(boolean ret) {
        this.ret = ret;
    }

}
