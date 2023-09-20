/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.pdfbox.examples.pdmodel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Creates a simple PDF/A document.
 */
public final class PDFCreator
{
    private PDFCreator()
    {
    }
    
    public static void main(String[] args) throws IOException, TransformerException, TesseractException
    {
//        if (args.length != 3)
//        {
//            System.err.println("usage: " + PDFCreator.class.getName() +
//                    " <output-file> <Message> <ttf-file>");
//            System.exit(1);
//        }

        String file = "/home/gbr/Música/pdfa-teste.pdf";
        String message = "Conteudo do PDF/A";
        String fontfile = "/home/gbr/Música/Roboto/Roboto-Regular.ttf";
        String inputFilePath = "/home/gbr/Música/testetexto.png";
        String dataPathLanguage = "/home/gbr/Música/Tesseract/tessdata/";
        String pdfComImagemPath = "/home/gbr/Música/pdfComImagem.pdf";
        String outputDirectory = "/home/gbr/Música/";
        String fullText = "";
    
        try (PDDocument doc = new PDDocument())
        {
        	
            PDPage page = new PDPage();
            doc.addPage(page);
            
            Tesseract tesseract = new Tesseract();
            tesseract.setTessVariable("user_defined_dpi", "100");
            tesseract.setLanguage("por");
            tesseract.setDatapath(dataPathLanguage);
            
            File arquivoComImagem = new File(pdfComImagemPath);
            
            PDDocument documentoComImagem = Loader.loadPDF(arquivoComImagem);
            
            
            // Itera através das páginas do documento original
            int pageNumber = 0;
            for (PDPage pagina : documentoComImagem.getPages()) {
                pageNumber++;

                // Obtém a lista de recursos da página
                PDResources resources = pagina.getResources();

                // Itera através dos recursos em busca de objetos XObject (imagens)
                for (COSName cosName : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(cosName);
                    if (xObject instanceof PDImageXObject) {
                        // É uma imagem
                        PDImageXObject imageXObject = (PDImageXObject) xObject;

                        // Converte a imagem em um BufferedImage
                        BufferedImage bufferedImage = imageXObject.getImage();

                        // Cria um novo arquivo para a imagem
                        String imageFileName = outputDirectory + "imagem_" + pageNumber + "_" + cosName.getName() + ".png";
                        File imageFile = new File(imageFileName);

                        // Salva a imagem em arquivo
                        ImageIO.write(bufferedImage, "png", imageFile);
                        
                        fullText = tesseract.doOCR(imageFile);
                    }
                }
            }

            // load the font as this needs to be embedded
            PDFont font = PDType0Font.load(doc, new File(fontfile));
            
            // A PDF/A file needs to have the font embedded if the font is used for text rendering
            // in rendering modes other than text rendering mode 3.
            //
            // This requirement includes the PDF standard fonts, so don't use their static PDFType1Font classes such as
            // PDFType1Font.HELVETICA.
            //
            // As there are many different font licenses it is up to the developer to check if the license terms for the
            // font loaded allows embedding in the PDF.
            // 
            if (!font.isEmbedded())
            {
            	throw new IllegalStateException("PDF/A compliance requires that all fonts used for"
            			+ " text rendering in rendering modes other than rendering mode 3 are embedded.");
            }
            
            
            String textoAjustado = fullText.replace("º", "o").replace("ª", "a");
            textoAjustado = textoAjustado.replace("\n", " "); // Substituir '\n' por espaço em branco
            
            
            System.out.println(fullText);
            System.out.println(textoAjustado);
            
            
            // create a page with the message
            try (PDPageContentStream contents = new PDPageContentStream(doc, page))
            {
//            	float pageHeight = page.getMediaBox().getHeight();
//            	
//                contents.beginText();
//                
//                contents.setFont(font, 12);
//                contents.newLineAtOffset(0, pageHeight - 20);
//                
//                contents.showText(textoAjustado);
//                
//                contents.endText();
            	
            	float pageHeight = page.getMediaBox().getHeight();
                float margin = 20; // Margem esquerda
                float yPosition = pageHeight - 20; // Posição vertical inicial
                float maxWidth = page.getMediaBox().getWidth() - 2 * margin; // Largura máxima da linha
                contents.setFont(font, 12);

                String[] lines = textoAjustado.split("\n"); // Divide o texto em linhas

                for (String line : lines) {
                    List<String> words = Arrays.asList(line.split(" ")); // Divide a linha em palavras
                    StringBuilder currentLine = new StringBuilder();

                    for (String word : words) {
                        float textWidth = font.getStringWidth(currentLine + " " + word) / 1000 * 12;
                        
                        // Verifica se a palavra cabe na linha atual
                        if (textWidth <= maxWidth) {
                            if (currentLine.length() > 0) {
                                currentLine.append(" ");
                            }
                            currentLine.append(word);
                        } else {
                            // A palavra não cabe na linha atual, então desenha a linha atual e passa para a próxima linha
                            contents.beginText();
                            contents.newLineAtOffset(margin, yPosition);
                            contents.showText(currentLine.toString());
                            contents.endText();
                            
                            yPosition -= 15; // Mova para a próxima linha
                            currentLine = new StringBuilder(word); // Inicie uma nova linha com a palavra atual
                        }
                    }

                    // Desenha a última linha da página
                    contents.beginText();
                    contents.newLineAtOffset(margin, yPosition);
                    contents.showText(currentLine.toString());
                    contents.endText();
                    
                    yPosition -= 15; // Mova para a próxima linha
                }
            }

            // add XMP metadata
            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            
            try
            {
                DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
                dc.setTitle(file);
                
                PDFAIdentificationSchema id = xmp.createAndAddPDFAIdentificationSchema();
                id.setPart(1);
                id.setConformance("B");
                
                XmpSerializer serializer = new XmpSerializer();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                serializer.serialize(xmp, baos, true);

                PDMetadata metadata = new PDMetadata(doc);
                metadata.importXMPMetadata(baos.toByteArray());
                doc.getDocumentCatalog().setMetadata(metadata);
            }
            catch(BadFieldValueException e)
            {
                // won't happen here, as the provided value is valid
                throw new IllegalArgumentException(e);
            }

            // sRGB output intent
//            InputStream colorProfile = PDFCreator.class.getResourceAsStream(
//                    "/org/apache/pdfbox/resources/pdfa/sRGB.icc");
//            PDOutputIntent intent = new PDOutputIntent(doc, colorProfile);
//            intent.setInfo("sRGB IEC61966-2.1");
//            intent.setOutputCondition("sRGB IEC61966-2.1");
//            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
//            intent.setRegistryName("http://www.color.org");
//            doc.getDocumentCatalog().addOutputIntent(intent);

            doc.save(file, CompressParameters.NO_COMPRESSION);
        }
    }
}












