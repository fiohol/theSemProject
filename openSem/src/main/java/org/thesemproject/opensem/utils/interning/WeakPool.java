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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Gestsce il pool di oggetti internizzati
 *
 * Per ulteriori dettagli visitare
 * <a href="https://javax0.wordpress.com/2014/03/11/object-interning/">https://javax0.wordpress.com/2014/03/11/object-interning/</a>
 *
 * @param <T> tipo di oggetti
 */
class WeakPool<T> {

    /**
     * Cache lasca degli oggetti internizzati
     */
    private final WeakHashMap<T, WeakReference<T>> pool = new WeakHashMap<>();

    /**
     * Ritorna la versione internizzata dell'oggetto
     *
     * @param object oggetto
     * @return valore internizzato
     */
    public T get(T object) {
        final T res;
        WeakReference<T> ref = pool.get(object);
        if (ref != null) {
            res = ref.get();
        } else {
            res = null;
        }
        return res;
    }

    /**
     * Memorizza la versione internizzata dell'oggetto
     *
     * @param object oggetto da internizzare
     */
    public void put(T object) {
        pool.put(object, new WeakReference<T>(object));
    }
}
