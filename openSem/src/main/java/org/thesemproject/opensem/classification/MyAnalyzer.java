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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianStemFilter;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.cz.CzechStemFilter;
import org.apache.lucene.analysis.de.GermanLightStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter.Builder;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter.StemmerOverrideMap;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.stempel.StempelFilter;
import org.apache.lucene.analysis.stempel.StempelStemmer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.tr.TurkishLowerCaseFilter;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.analysis.util.CharArrayMap.EntryIterator;
import org.apache.lucene.util.CharsRefBuilder;
import org.egothor.stemmer.Trie;
import org.tartarus.snowball.ext.DutchStemmer;
import org.tartarus.snowball.ext.RomanianStemmer;
import org.tartarus.snowball.ext.RussianStemmer;
import org.tartarus.snowball.ext.TurkishStemmer;

/**
 * Analizzatore sintattico multilingua E' stato ottenuto mettendo assieme i
 * sorgenti degli analizzatori per lingua presenti in lucene ed adattandoli per
 * utilizzare il MyFilter
 */
public class MyAnalyzer extends StopwordAnalyzerBase {

    /**
     * File delle stopwords di default
     */
    public static final String DEFAULT_STOPWORD_FILE = "italian_stop.txt";
    private static final CharArraySet DEFAULT_ARTICLES_IT = CharArraySet.unmodifiableSet(new CharArraySet(Arrays.asList(new String[]{"c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell", "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"}), true));

    /**
     * Articoli di default francesi
     */
    public static final CharArraySet DEFAULT_ARTICLES_FR = CharArraySet.unmodifiableSet(new CharArraySet(Arrays.asList(new String[]{"l", "m", "t", "qu", "n", "s", "j", "d", "c", "jusqu", "quoiqu", "lorsqu", "puisqu"}), true));
    private final CharArraySet stemExclusionSet;
    private final Trie stemTable;
    private final StemmerOverrideMap stemdict;
    private final String language;

    /**
     * Inizializza l'analizzatore sintattico per lingua
     *
     * @param language lingua dell'analizzatore
     */
    public MyAnalyzer(String language) {
        this(language, getDefaultStopSet(language), CharArraySet.EMPTY_SET, DefaultSetHolder.DEFAULT_STEM_DICT);
    }

    /**
     * Inizializza l'analizzatore sintattico per lingua con un set di stopwords
     *
     * @param language lingua dell'analizzatore
     * @param stopwords set di stop words per lingua
     */
    public MyAnalyzer(String language, CharArraySet stopwords) {
        this(language, stopwords, CharArraySet.EMPTY_SET, DefaultSetHolder.DEFAULT_STEM_DICT);
    }

    /**
     * Inizializza l'analizzatore sintattico per lingua
     *
     * @param language lingua
     * @param stopwords stop words
     * @param stemExclusionSet elenco dei termini che non deve essere sottoposto
     * a stemming
     * @param stemOverrideDict dizionario dei termini in overriding
     */
    public MyAnalyzer(String language, CharArraySet stopwords, CharArraySet stemExclusionSet, CharArrayMap<String> stemOverrideDict) {
        super(stopwords);
        this.language = language;
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
        this.stemTable = DefaultSetHolder.DEFAULT_TABLE;
        if (stemOverrideDict.isEmpty()) {
            this.stemdict = null;
        } else {
            Builder builder = new Builder(false);
            EntryIterator iter = stemOverrideDict.entrySet().iterator();
            CharsRefBuilder spare = new CharsRefBuilder();

            while (iter.hasNext()) {
                char[] ex = iter.nextKey();
                spare.copyChars(ex, 0, ex.length);
                builder.add(spare.get(), (CharSequence) iter.currentValue());
            }

            try {
                this.stemdict = builder.build();
            } catch (IOException var8) {
                throw new RuntimeException("can not build stem dict", var8);
            }
        }

    }

    /**
     * Elenco delle lingue conosciute dal classificatore
     */
    public static String[] languages = {"it", "bg", "br", "cz", "en", "de", "es", "fr", "nl", "pl", "pt", "ru", "ro", "tr", "sk"};

    /**
     * Elenco delle lingue come set
     */
    public static Set<String> languagesSet = new HashSet<String>(Arrays.asList(languages));

    /**
     * Ritorna il set di stop words di default per una lingua
     *
     * @param language lingua
     * @return set di stop words
     */
    public static CharArraySet getDefaultStopSet(String language) {
        try {
            if ("en".equalsIgnoreCase(language)) {
                return StandardAnalyzer.STOP_WORDS_SET;
            } else if ("es".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "spanish_stop.txt", StandardCharsets.UTF_8));
            } else if ("fr".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "french_stop.txt", StandardCharsets.UTF_8));
            } else if ("de".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "german_stop.txt", StandardCharsets.UTF_8));
            } else if ("pl".equalsIgnoreCase(language)) {
                return WordlistLoader.getWordSet(IOUtils.getDecodingReader(PolishAnalyzer.class, "stopwords.txt", StandardCharsets.UTF_8), "#");
            } else if ("pt".equalsIgnoreCase(language) || "br".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "portuguese_stop.txt", StandardCharsets.UTF_8));
            } else if ("it".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "italian_stop.txt", StandardCharsets.UTF_8));
            } else if ("cz".equalsIgnoreCase(language) || "sk".equalsIgnoreCase(language)) {
                return WordlistLoader.getWordSet(IOUtils.getDecodingReader(CzechAnalyzer.class, "stopwords.txt", StandardCharsets.UTF_8), "#");
            } else if ("tr".equalsIgnoreCase(language)) {
                return TurkishAnalyzer.loadStopwordSet(false, TurkishAnalyzer.class, "stopwords.txt", "#");
            } else if ("ru".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "russian_stop.txt", StandardCharsets.UTF_8));
            } else if ("ro".equalsIgnoreCase(language)) {
                return RomanianAnalyzer.loadStopwordSet(false, RomanianAnalyzer.class, "stopwords.txt", "#");
            } else if ("bg".equalsIgnoreCase(language)) {
                return BulgarianAnalyzer.loadStopwordSet(false, BulgarianAnalyzer.class, "stopwords.txt", "#");
            } else if ("nl".equalsIgnoreCase(language)) {
                return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "dutch_stop.txt", StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            throw new RuntimeException("Unable to load default stopword set");
        }
        return StandardAnalyzer.STOP_WORDS_SET;

    }

    /**
     * Crea i componets per l'analizzatore. Per ogni lingua combina i filtri
     * necessari per processarla
     *
     * @param fieldName field da processare
     * @return Token Components
     */
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        Object source = new StandardTokenizer();
        StandardFilter result = new StandardFilter((TokenStream) source);
        TokenFilter result2 = new NothingFilter(result);
        TokenFilter result1 = new NothingFilter(result);
        if ("en".equalsIgnoreCase(language)) {
            result2 = new EnglishPossessiveFilter(result);
        } else if ("fr".equalsIgnoreCase(language)) {
            result2 = new ElisionFilter(result, DEFAULT_ARTICLES_FR);
        } else if ("it".equalsIgnoreCase(language)) {
            result2 = new ElisionFilter(result, DEFAULT_ARTICLES_IT);
        }
        TokenFilter result3 = ("tr".equalsIgnoreCase(language)) ? new TurkishLowerCaseFilter(result2) : new LowerCaseFilter(result2);
        Object result4 = new MyFilter(new StopFilter(result3, this.stopwords));
        if (!this.stemExclusionSet.isEmpty()) {
            result4 = new SetKeywordMarkerFilter((TokenStream) result4, this.stemExclusionSet);
        }

        if ("en".equalsIgnoreCase(language)) {
            result1 = new PorterStemFilter((TokenStream) result4);
        } else if ("es".equalsIgnoreCase(language)) {
            result1 = new SpanishLightStemFilter((TokenStream) result4);
        } else if ("fr".equalsIgnoreCase(language)) {
            result1 = new FrenchLightStemFilter((TokenStream) result4);
        } else if ("de".equalsIgnoreCase(language)) {
            GermanNormalizationFilter result41 = new GermanNormalizationFilter((TokenStream) result4);
            result1 = new GermanLightStemFilter(result41);
        } else if ("pl".equalsIgnoreCase(language)) {
            result1 = new StempelFilter((TokenStream) result4, new StempelStemmer(this.stemTable));
        } else if ("pt".equalsIgnoreCase(language) || "br".equalsIgnoreCase(language)) {
            result1 = new PortugueseLightStemFilter((TokenStream) result4);
        } else if ("it".equalsIgnoreCase(language)) {
            result1 = new ItalianLightStemFilter((TokenStream) result4);
        } else if ("cz".equalsIgnoreCase(language) || "sk".equalsIgnoreCase(language)) {
            result1 = new CzechStemFilter((TokenStream) result4);
        } else if ("tr".equalsIgnoreCase(language)) {
            result1 = new SnowballFilter((TokenStream) result4, new TurkishStemmer());
        } else if ("ru".equalsIgnoreCase(language)) {
            result1 = new SnowballFilter((TokenStream) result4, new RussianStemmer());
        } else if ("ro".equalsIgnoreCase(language)) {
            result1 = new SnowballFilter((TokenStream) result4, new RomanianStemmer());
        } else if ("bg".equalsIgnoreCase(language)) {
            result1 = new BulgarianStemFilter((TokenStream) result4);
        } else if ("nl".equalsIgnoreCase(language)) {
            if (this.stemdict != null) {
                result4 = new StemmerOverrideFilter((TokenStream) result4, this.stemdict);
            }
            result1 = new SnowballFilter((TokenStream) result4, new DutchStemmer());
        }
        return new Analyzer.TokenStreamComponents((Tokenizer) source, result1);

    }

    private static class DefaultSetHolder {

        static final CharArraySet DEFAULT_STOP_SET;
        static final Trie DEFAULT_TABLE;
        static final CharArrayMap<String> DEFAULT_STEM_DICT;

        static {
            try {

                DEFAULT_STOP_SET = WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "italian_stop.txt", StandardCharsets.UTF_8));
                DEFAULT_STEM_DICT = new CharArrayMap(4, false);
                DEFAULT_STEM_DICT.put("fiets", "fiets");
                DEFAULT_STEM_DICT.put("bromfiets", "bromfiets");
                DEFAULT_STEM_DICT.put("ei", "eier");
                DEFAULT_STEM_DICT.put("kind", "kinder");
                DEFAULT_TABLE = StempelStemmer.load(PolishAnalyzer.class.getResourceAsStream("stemmer_20000.tbl"));
            } catch (IOException var1) {
                throw new RuntimeException("Unable to load default stopword set");
            }
        }

        private DefaultSetHolder() {

        }
    }

}
