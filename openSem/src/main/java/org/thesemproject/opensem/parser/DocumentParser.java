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
package org.thesemproject.opensem.parser;

import org.thesemproject.opensem.classification.MyAnalyzer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.utils.interning.InternPool;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Parser basato su TIKA per estrarre contenuto da documenti
 */
public class DocumentParser {

    AutoDetectParser adp;

    InternPool intern;

    /**
     * Istanzia il parser
     */
    public DocumentParser() {
        adp = new AutoDetectParser();
        intern = new InternPool();
    }

    /**
     * Istanzia il parser
     *
     * @param intern internizzatore
     */
    public DocumentParser(InternPool intern) {
        adp = new AutoDetectParser();
        this.intern = intern;
    }

    /**
     * Estrare il testo da un file
     *
     * @param file file da parsare
     * @return testo estratto dal file. null se nulla è stato estratto
     */
    public String getTextFromFile(File file) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, file.toString());
            BodyContentHandler handler = new BodyContentHandler(1000000);
            adp.parse(file.toURI().toURL().openStream(), handler, metadata, new ParseContext());
            return handler.toString();
        } catch (IOException | SAXException | TikaException ex) {
            return "!ERROR: " + ex.getLocalizedMessage();
        }
    }

    /**
     * Ritorna i contenuti non testo presenti in un file. In un PDF o in un Word
     * ci spossono essere immagini, altri testi, musica...
     *
     * @param file file da cui estrarre i contenuti
     * @return mappa con il nome del contenuto e byte del binario del contenuto.
     * Il nome del contenuto è dato da un progressivo seguito dall'estensione
     * corrispondente al tipo del file
     */
    public Map<String, byte[]> getInlinesContentFromFile(File file) {
        try {
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            ContentHandler handler = new BodyContentHandler(-1);
            context.set(Parser.class, adp);
            PDFParserConfig pdc = new PDFParserConfig();
            pdc.setExtractInlineImages(true);
            context.set(PDFParserConfig.class, pdc);
            SemEmbeddedDocumentsExtractor ex = new SemEmbeddedDocumentsExtractor(context, adp);
            context.set(EmbeddedDocumentExtractor.class, ex);
            adp.parse(file.toURI().toURL().openStream(), handler, metadata, context);
            return ex.getEmbeddedContent();
        } catch (IOException | SAXException | TikaException ex) {
            return null;
        }
    }

    /**
     * Ritorna tutte le immagini contenute in un file
     *
     * @param file file da processare
     * @return mappa con il nome assegnato dal sistema all'immagine e contenuto
     * binario
     */
    public Map<String, BufferedImage> getImagesFromFile(File file) {
        try {
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            ContentHandler handler = new BodyContentHandler(-1);
            context.set(Parser.class, adp);
            PDFParserConfig pdc = new PDFParserConfig();
            pdc.setExtractInlineImages(true);
            context.set(PDFParserConfig.class, pdc);
            SemEmbeddedDocumentsExtractor ex = new SemEmbeddedDocumentsExtractor(context, adp);
            context.set(EmbeddedDocumentExtractor.class, ex);
            adp.parse(file.toURI().toURL().openStream(), handler, metadata, context);
            return ex.getEmbeddedImages();
        } catch (IOException | SAXException | TikaException ex) {
            LogGui.printException(ex);
            return null;
        }
    }

    /**
     * Estrae l'immgine più grande (in termini di megapixel) contenuta in un
     * file
     *
     * @param file file da processare
     * @return immagine o null se il documento non contiene immagini
     */
    public BufferedImage getLargestImageFromFile(File file) {
        Map<String, BufferedImage> images = getImagesFromFile(file);
        if (images == null) {
            return null;
        }
        BufferedImage ret = null;
        for (BufferedImage image : images.values()) {
            if (ret == null) {
                ret = image;
            }
            int pixels = ret.getHeight() * ret.getWidth();
            int imagesPixels = image.getHeight() * image.getWidth();
            if (imagesPixels > pixels) {
                ret = image;
            }
        }
        return ret;
    }

    /**
     * Ritorna una vista HTML sul file processato. L'estrazione del testo fa
     * perdere di fatto la formattazione contenuta nel word o nel PDF. La
     * versione HTML è una versione processabile ma che mantiene il formato
     *
     * @param file file da parsare
     * @return vista HTML formattato del contenuto del documento (privato delle
     * immagini)
     */
    public String getHtmlFromFile(File file) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
            handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            handler.setResult(new StreamResult(out));
            ExpandedTitleContentHandler handler1 = new ExpandedTitleContentHandler(handler);
            adp.parse(file.toURI().toURL().openStream(), handler1, new Metadata());
            return new String(out.toByteArray(), "UTF-8").replaceAll("<img .*?</img>", "").replaceAll("<img .*?/>", "");
        } catch (TransformerConfigurationException | IllegalArgumentException | IOException | SAXException | TikaException ex) {
            return "!ERROR: " + ex.getLocalizedMessage();
        }
    }

    /**
     * Cerca di identificare la lingua in cui è scritto un testo (attraverso
     * TIKA). In caso di indecisione ritorna Italiano
     *
     * @param text testo di cui identificare la lingua
     * @return lingua del documento
     */
    public String getLanguageFromText(String text) {
        LanguageIdentifier identifier = new LanguageIdentifier(text);
        String lang = identifier.getLanguage();
        if (!MyAnalyzer.languagesSet.contains(lang)) {
            lang = "it";
        }
        return (String) intern.intern(lang);
    }

}
