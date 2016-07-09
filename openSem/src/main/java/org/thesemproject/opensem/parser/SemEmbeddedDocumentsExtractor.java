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

import org.thesemproject.opensem.gui.LogGui;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;

/**
 * Estrattore di contenuti binari da un documento. Viene usato dal
 * DocumentParser
 */
public class SemEmbeddedDocumentsExtractor extends ParsingEmbeddedDocumentExtractor {

    private final Detector detector;
    private final TikaConfig config;
    private final Map<String, byte[]> embedded;
    private final Map<String, BufferedImage> embeddedImages;
    private int fileCount = 0;

    /**
     * Istanzia l'oggetto
     *
     * @param context contesto di parsing
     * @param parser parser
     */
    public SemEmbeddedDocumentsExtractor(ParseContext context, AutoDetectParser parser) {
        super(context);
        detector = ((AutoDetectParser) parser).getDetector();
        config = TikaConfig.getDefaultConfig();
        fileCount = 0;
        embedded = new LinkedHashMap<>();
        embeddedImages = new LinkedHashMap<>();
    }

    /**
     * Ritorna sempre true (ovvero che il contenuto è parsabile)
     *
     * @param metadata metadati
     * @return true
     */
    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        return true;
    }

    /**
     * Processa i contenuti
     *
     * @since 1.1: aggiunto il trattamento dei TIFF
     * @param stream stream binario del contenuto
     * @param handler handler
     * @param metadata metadati del documento
     * @param outputHtml necessario per l'override del metodo ma mai usato
     * @throws SAXException eccezione
     * @throws IOException Eccezione di input/output
     */
    @Override
    public void parseEmbedded(InputStream stream, org.xml.sax.ContentHandler handler, Metadata metadata, boolean outputHtml) throws SAXException, IOException {
        String name = "Content" + fileCount++;
        MediaType contentType = detector.detect(stream, metadata);
        if (contentType != null) {
            try {
                name += config.getMimeRepository().forName(contentType.toString()).getExtension();
            } catch (MimeTypeException e) {
                LogGui.printException(e);
            }
        }
        byte[] bytes = IOUtils.toByteArray(stream);
        embedded.put(name, bytes);
        if (name.toLowerCase().endsWith("jpg") || name.toLowerCase().endsWith("tiff") || name.toLowerCase().endsWith("tif") || name.toLowerCase().endsWith("png") || name.toLowerCase().endsWith("gif")) {

            BufferedImage pl = ImageIO.read(new ByteArrayInputStream(bytes));
            if (pl != null) {
                if ((pl.getWidth() > 32 && pl.getHeight() > 32)) //No Icone
                {
                    embeddedImages.put(name, pl);
                }
            }

        }
    }

    /**
     * Ritorna la mappa dei contenuti
     *
     * @return mappa nome del contenuto (come se fosse un file), array di byte
     */
    public Map<String, byte[]> getEmbeddedContent() {
        return embedded;
    }

    /**
     * Ritorna la mappa di tutte le immagini
     *
     * @return mappa nome del contenuto, BufferedImage
     */
    public Map<String, BufferedImage> getEmbeddedImages() {
        return embeddedImages;
    }
}
