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
 * Tokenizer filter che esclude parole di lunghezza inferiore a 3 caratteri e
 * numeri
 */
public class MyFilter extends FilteringTokenFilter {

    private final CharTermAttribute termAtt = (CharTermAttribute) this.addAttribute(CharTermAttribute.class);
    private Pattern numbers = Pattern.compile("^[0-9]*$");

    /**
     * Costruisce il filtro
     *
     * @param in stream di token
     */
    public MyFilter(TokenStream in) {
        super(in);

    }

    /**
     * Analizza un token e decide se accettarlo o meno escludendo le parole con
     * meno di 3 caratteri e i numeri
     *
     * @return true se il token è accettato
     * @throws IOException Eccezione di input/output
     */
    @Override
    protected boolean accept() throws IOException {
        String word = String.valueOf(this.termAtt.buffer());
        word = word.substring(0, this.termAtt.length());
        if (word.length() < 3) {
            return false;
        }
        if (numbers.matcher(word).matches()) {
            return false;
        }

        return true;
    }

}
