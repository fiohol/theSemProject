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

import org.bson.Document;

import java.util.HashSet;

/**
 * Utilities per gestire il Document base di MongoDB
 */
public class BSonUtils {

    /**
     * Field speciale per idenificare il timestamp
     */
    public static final String TIMESTAMP = "@timestamp";

    /**
     * Field per identificare il testo principale del documento
     */
    public static final String TEXT = "@text";

    /**
     * Field per identificare la sorgente dati
     */
    public static final String SOURCE = "@source";

    /**
     * Field per identificare il codice crc
     */
    public static final String CRC = "@crc32";

    /**
     * Field per identificare l'ID del documento
     */
    public static final String ENVIRONMENT_ID = "@environmentId";

    /**
     * Field per identificare l'id del documento
     */
    public static final String ID = "_id";

    /**
     * Ritorna l'ID del documento
     *
     * @param doc Document
     * @return id documento
     */
    public static String getId(Document doc) {
        return String.valueOf(doc.get(ID));
    }

    /**
     * Imposta l'ID di documento
     *
     * @param doc Document
     * @param id id del documento
     */
    public static void setId(Document doc, String id) {
        doc.remove(ID);
        doc.append(ID, new Document("$oid", id));
    }

    
    /**
     * Ritorna il testo del documento
     *
     * @param bson Document MongoDB
     * @return testo del documento
     */
    public static String getText(Document bson) {
        return (String) bson.get(TEXT);
    }
}
