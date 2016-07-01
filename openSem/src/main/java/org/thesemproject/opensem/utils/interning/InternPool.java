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
package org.thesemproject.opensem.utils.interning;

/**
 * Classe per gestire la versione internizzata di un oggetto
 * <p/>
 * Dettaglio dell'esempio implementato si trovano all'indirizzo
 * <a href="https://javax0.wordpress.com/2014/03/11/object-interning/">https://javax0.wordpress.com/2014/03/11/object-interning/</a>
 *
 * @param <T> class type
 */

public class InternPool<T> {
    /**
     * Pool di valori internizzati (weak perche' ogni tot dimentica)
     */
    private final WeakPool<T> pool = new WeakPool<T>();

    /**
     * Ritorna la versione internizzata di un oggetto ed eventualmente lo internizza
     * E' sincronizzato per gestire il multithreading
     *
     * @param object oggetto da internizzare
     * @return versione internizzata dell'oggetto
     */
    public synchronized T intern(T object) {
        if (object == null) return null;
        T res = pool.get(object);
        if (res == null) {
            pool.put(object);
            res = object;
        }
        return res;
    }
}