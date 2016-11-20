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
 *
 * @author The Sem Project
 */
public class StringUtils {

    /**
     * Mette la prima lettera maiuscola e tutto il respo in minuscolo
     *
     * @param str stringa da elaborare
     * @return stringa elaborata.
     */
    public static String firstUpper(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        StringBuffer sb = new StringBuffer();
        return sb.append(str.substring(0, 1).toUpperCase()).append(str.substring(1).toLowerCase()).toString();

    }
}
