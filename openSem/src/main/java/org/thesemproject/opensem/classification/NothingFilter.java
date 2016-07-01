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
package org.thesemproject.opensem.classification;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filtro che non fa nulla E' utilizzato dal MyAnalyzer in funzione della
 * lingua. Dato che sono stati pensati un numero di filtri fissi per ogni lingua
 * quando una lingua ha meno passi da fare i passi rimanenti vengono fatti
 * attraverso il nothingfilter
 */
public class NothingFilter extends FilteringTokenFilter {

    /**
     * Istanzia il filtro
     *
     * @param in stream di token
     */
    public NothingFilter(TokenStream in) {
        super(in);

    }

    /**
     * Accetta qualsiasi token
     *
     * @return @throws IOException
     */
    @Override
    protected boolean accept() throws IOException {

        return true;
    }

}
