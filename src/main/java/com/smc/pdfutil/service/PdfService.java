package com.smc.pdfutil.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfService {
	private static final Logger log = LoggerFactory.getLogger(PdfService.class);

	private static final float DEF_DPI = 72f;
	private static final float IMG_DPI = 96f;

	/**
	 * Create a new blank PDF. The new PDF will at least contain a single page.
	 * @param pageNum Page number that new PDF will contain
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void create(int pageNum, OutputStream outputStream) throws IOException {
		log.info("Create PDF");
		log.debug("Params: pageNum={}", pageNum);
		try (PDDocument doc = new PDDocument()) {
			for (int i = 0; i < Math.max(pageNum, 1); i++) {
				PDPage page = new PDPage();
				doc.addPage(page);
			}
			doc.save(outputStream);
		} catch (IOException e) {
			log.error("Create PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Read the text from given PDF.
	 * @param inputStream InputStream to the PDF
	 * @return Text of the PDF
	 */
	public static String read(InputStream inputStream) throws IOException {
		return read(inputStream, null);
	}

	/**
	 * Read the text from given PDF.
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @return Text of the PDF
	 */
	public static String read(InputStream inputStream, String pwd) throws IOException {
		log.info("Reading PDF");
		String str;
		try (PDDocument doc = PDDocument.load(inputStream, pwd)) {
			str = new PDFTextStripper().getText(doc);
		} catch (IOException e) {
			log.error("Reading PDF Exception: ", e);
			throw(e);
		}
		return str;
	}

	/**
	 * Split specified pages from given PDF and put into a new PDF.
	 * @param inputStream InputStream to the PDF
	 * @param pagesIdx Index of pages subjected to split
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void split(InputStream inputStream, int[] pagesIdx, OutputStream outputStream) throws IOException {
		split(inputStream, null, pagesIdx, outputStream);
	}

	/**
	 * Split specified pages from given PDF and put into a new PDF.
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param pagesIdx Index of pages subjected to split
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void split(InputStream inputStream, String pwd, int[] pagesIdx, OutputStream outputStream) throws IOException {
		log.info("Splitting PDF");
		log.debug("Params: pagesIdx={}", Arrays.toString((pagesIdx)));
		try (PDDocument doc = PDDocument.load(inputStream, pwd);
			 PDDocument dest = new PDDocument()) {
			for (int pageIdx: pagesIdx) {
				dest.addPage(doc.getPage(pageIdx));
			}
			dest.save(outputStream);
		} catch (IOException e) {
			log.error("Splitting PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Merge multiple PDF into a new PDF
	 * @param inputStream Map of InputStream to the PDF and corresponding password (if any)
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void merge(Stream<Map.Entry<InputStream, String>> inputStream, OutputStream outputStream) throws IOException {
		log.info("Merging PDF");
		try (PDDocument dest = new PDDocument()) {
			PDFMergerUtility merger = new PDFMergerUtility();
			for (Iterator<Map.Entry<InputStream, String>> i = inputStream.iterator(); i.hasNext();) {
				Map.Entry<InputStream, String> source = i.next();
				try (InputStream is = source.getKey();
					 PDDocument doc = PDDocument.load(is, source.getValue())) {
					merger.appendDocument(dest, doc);
				}
			}
			dest.save(outputStream);
		} catch (IOException e) {
			log.error("Merging PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Encrypt the PDF
	 * @param inputStream InputStream to the PDF
	 * @param ownerPwd New password to decrypt the PDF in owner level
	 * @param userPwd New password to decrypt the PDF in user level
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void encrypt(InputStream inputStream, String ownerPwd, String userPwd, OutputStream outputStream) throws IOException {
		encrypt(inputStream, null, ownerPwd, userPwd, outputStream);
	}

	/**
	 * Encrypt the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param ownerPwd New password to decrypt the PDF in owner level
	 * @param userPwd New password to decrypt the PDF in user level
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void encrypt(InputStream inputStream, String pwd, String ownerPwd, String userPwd, OutputStream outputStream) throws IOException {
		log.info("Encrypting PDF");
		try (PDDocument doc = PDDocument.load(inputStream, pwd)) {
			AccessPermission ap = new AccessPermission();
			ap.setCanAssembleDocument(false);
			ap.setCanExtractContent(false);
			ap.setCanExtractForAccessibility(false);
			ap.setCanModify(false);
			ap.setCanModifyAnnotations(false);
			ap.setCanPrint(false);
			ap.setCanPrintDegraded(false);

			StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPwd, userPwd, ap);
			spp.setEncryptionKeyLength(256);

			doc.protect(spp);

			doc.save(outputStream);
		} catch (IOException e) {
			log.error("Encrypting PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Draw the specified image to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pageIdx Index of the page to draw the image
	 * @param imgStream InputStream to the image
	 * @param x X coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param y Y coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void draw(InputStream inputStream, int pageIdx, InputStream imgStream, float x, float y, OutputStream outputStream) throws IOException {
		draw(inputStream, null, pageIdx, imgStream, x, y, -1, -1, outputStream);
	}

	/**
	 * Draw the specified image to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pageIdx Index of the page to draw the image
	 * @param imgStream InputStream to the image
	 * @param x X coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param y Y coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param width Width (inch) of the image
	 * @param height Height (inch) of the image
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void draw(InputStream inputStream, int pageIdx, InputStream imgStream, float x, float y, float width, float height, OutputStream outputStream) throws IOException {
		draw(inputStream, null, pageIdx, imgStream, x, y, width, height, outputStream);
	}

	/**
	 * Draw the specified image to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param pageIdx Index of the page to draw the image
	 * @param imgStream InputStream to the image
	 * @param x X coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param y Y coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void draw(InputStream inputStream, String pwd, int pageIdx, InputStream imgStream, float x, float y, OutputStream outputStream) throws IOException {
		draw(inputStream, pwd, pageIdx, imgStream, x, y, -1, -1, outputStream);
	}

	/**
	 * Draw the specified image to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param pageIdx Index of the page to draw the image
	 * @param imgStream InputStream to the image
	 * @param x X coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param y Y coordinate (inch) of the drawing position, starting from upper-left corner
	 * @param width Width (inch) of the image
	 * @param height Height (inch) of the image
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void draw(InputStream inputStream, String pwd, int pageIdx, InputStream imgStream, float x, float y, float width, float height, OutputStream outputStream) throws IOException {
		log.info("Drawing img to PDF");
		log.debug("Params: pageIdx={}, x={}, y={}, width={}, height={}", pageIdx, x, y, width, height);
		try (PDDocument doc = inputStream == null ? new PDDocument() : PDDocument.load(inputStream, pwd)) {
			if (inputStream == null) {
				for (int i = 0; i < Math.max(pageIdx, 1); i++) {
					PDPage page = new PDPage();
					doc.addPage(page);
				}
			}
			PDPage page = doc.getPage(pageIdx);
			PDPageContentStream content = new PDPageContentStream(doc, page, AppendMode.APPEND, false, true);
			BufferedImage nativeImg = ImageIO.read(imgStream);
			PDImageXObject img = LosslessFactory.createFromImage(doc, nativeImg);
			PDRectangle rect = page.getCropBox();

			width = width < 0? rect.getWidth() / DEF_DPI - x: width;
			height = height < 0? rect.getHeight() / DEF_DPI - y: height;
			float scale = Collections.min(Arrays.asList(width * DEF_DPI / nativeImg.getWidth(), height * DEF_DPI / nativeImg.getHeight(), 1f));
			if (scale > 1f) {
				scale = 1f;
			}
			y = page.getCropBox().getUpperRightY() / DEF_DPI - y - (img.getHeight() * scale / DEF_DPI);

			content.drawImage(img, x * DEF_DPI, y * DEF_DPI, nativeImg.getWidth() * scale, nativeImg.getHeight() * scale);
			content.close();
			doc.save(outputStream);
		} catch (IOException e) {
			log.error("Drawing img to PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Capture the specified area of the PDF and convert into image(png)
	 * @param inputStream InputStream to the PDF
	 * @param pageIdx Index of page to capture
	 * @param dpi The DPI of the output image
	 * @param outputStream OutputStream to the output image(png)
	 */
	public static void capture(InputStream inputStream, int pageIdx, float dpi, OutputStream outputStream) throws IOException {
		capture(inputStream, null, pageIdx, -1, -1, -1, -1, dpi, outputStream);
	}

	/**
	 * Capture the specified area of the PDF and convert into image(png)
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param pageIdx Index of page to capture
	 * @param dpi The DPI of the output image
	 * @param outputStream OutputStream to the output image(png)
	 */
	public static void capture(InputStream inputStream, String pwd, int pageIdx, float dpi, OutputStream outputStream) throws IOException {
		capture(inputStream, pwd, pageIdx, -1, -1, -1, -1, dpi, outputStream);
	}

	/**
	 * Capture the specified area of the PDF and convert into image(png)
	 * @param inputStream InputStream to the PDF
	 * @param pageIdx Index of page to capture
	 * @param x X coordinate (inch) of the capture area, starting from upper-left corner
	 * @param y Y coordinate (inch) of the capture area, starting from upper-left corner
	 * @param width Width (inch) of the capture area
	 * @param height Height (inch) of the capture area
	 * @param dpi The DPI of the output image
	 * @param outputStream OutputStream to the output image(png)
	 */
	public static void capture(InputStream inputStream, int pageIdx, float x, float y, float width, float height, float dpi, OutputStream outputStream) throws IOException {
		capture(inputStream, null, pageIdx, x, y, width, height, dpi, outputStream);
	}

	/**
	 * Capture the specified area of the PDF and convert into image(png)
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param pageIdx Index of page to capture
	 * @param x X coordinate (inch) of the capture area, starting from upper-left corner
	 * @param y Y coordinate (inch) of the capture area, starting from upper-left corner
	 * @param width Width (inch) of the capture area
	 * @param height Height (inch) of the capture area
	 * @param dpi The DPI of the output image
	 * @param outputStream OutputStream to the output image(png)
	 */
	public static void capture(InputStream inputStream, String pwd, int pageIdx, float x, float y, float width, float height, float dpi, OutputStream outputStream) throws IOException {
		log.info("Cutting img from PDF");
		log.debug("Params: pageIdx={}, x={}, y={}, width={}, height={}, dpi={}", pageIdx, x, y, width, height, dpi);
		try (PDDocument doc = PDDocument.load(inputStream, pwd)) {
			BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(pageIdx, dpi);
			if (x >= 0 && y >=0 && width > 0 && height > 0) {
				int _x = Math.round(x * dpi),
						_y = Math.round(y * dpi),
						_w = Math.round(width * dpi),
						_h = Math.round(height * dpi);
				img = img.getSubimage(_x, _y, _w, _h);
			}
			ImageIO.write(img, "PNG", outputStream);
		} catch (IOException e) {
			log.error("Cutting PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Add watermark to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param watermarkStream InputStream to the watermark PDF
	 * @param outputStream OutputStream to the output PDF
	 */
	 public static void watermark(InputStream inputStream, InputStream watermarkStream, OutputStream outputStream) throws IOException {
		watermark(inputStream, null, watermarkStream, null, outputStream);
	 }

	/**
	 * Add watermark to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param watermarkStream InputStream to the watermark PDF
	 * @param watermarkPwd Password to decrypt the watermark PDF
	 * @param outputStream OutputStream to the output PDF
	 */
	 public static void watermark(InputStream inputStream, String pwd, InputStream watermarkStream, String watermarkPwd, OutputStream outputStream) throws IOException {
		log.info("Adding watermark to PDF");
		try (PDDocument doc = PDDocument.load(inputStream, pwd);
			 PDDocument watermarkDoc = PDDocument.load(watermarkStream, watermarkPwd);
			 Overlay overlay = new Overlay()) {
			overlay.setInputPDF(doc);
			overlay.setAllPagesOverlayPDF(watermarkDoc);
			overlay.setOverlayPosition(Overlay.Position.FOREGROUND);
			overlay.overlay(new HashMap<> ());
			doc.save(outputStream);
		} catch (IOException e) {
			log.error("Adding watermark to PDF Exception: ", e);
			throw(e);
		}
	}

	/**
	 * Get meta info from the PDF
	 * @param inputStream
	 * @return Map that includes meta info of the PDF
	 */
	public static Map<String, String> getInfo(InputStream inputStream) throws IOException {
	 	return getInfo(inputStream, null);
	}

	/**
	 * Get meta info from the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @return Map that includes meta info of the PDF
	 */
	public static Map<String, String> getInfo(InputStream inputStream, String pwd) throws IOException {
	 	log.info("Getting Info from PDF");
		Map<String, String> infoMap = new HashMap<>();
	 	try (PDDocument doc = PDDocument.load(inputStream, pwd)) {
	 		PDDocumentInformation info = doc.getDocumentInformation();
	 		infoMap.put("Author", info.getAuthor());
	 		infoMap.put("Creator", info.getCreator());
	 		infoMap.put("Keywords", info.getKeywords());
	 		infoMap.put("Producer", info.getProducer());
	 		infoMap.put("Subject", info.getSubject());
			infoMap.put("Title", info.getTitle());
			infoMap.put("Trapped", info.getTrapped());
		} catch (IOException e) {
	 		log.error("Getting Info from PDF Exception: ", e);
	 		throw(e);
		}
	 	return infoMap;
	}

	/**
	 * Set meta info to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param infoMap HashMap to store info of the meta info
	 *                defined keys include: Author, Creator, Keywords, Producer, Subject, Title, Trapped
	 *                other keys will be added into custom properties
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void setInfo(InputStream inputStream, HashMap<String, String> infoMap, OutputStream outputStream) throws IOException {
		setInfo(inputStream, null, infoMap, outputStream);
	}

	/**
	 * Set meta info to the PDF
	 * @param inputStream InputStream to the PDF
	 * @param pwd Password to decrypt the PDF
	 * @param infoMap HashMap to store info of the meta info
	 *                defined keys include: Author, Creator, Keywords, Producer, Subject, Title, Trapped
	 *                other keys will be added into custom properties
	 * @param outputStream OutputStream to the output PDF
	 */
	public static void setInfo(InputStream inputStream, String pwd, HashMap<String, String> infoMap, OutputStream outputStream) throws IOException {
		log.info("Setting Info to PDF");
		try (PDDocument doc = PDDocument.load(inputStream, pwd)) {
			PDDocumentInformation info = doc.getDocumentInformation();
			for (Map.Entry<String, String> entry : infoMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				switch (key.toUpperCase()) {
					case "AUTHOR":
						info.setAuthor(value);
						break;
					case "CREATOR":
						info.setCreator(value);
						break;
					case "KEYWORDS":
						info.setKeywords(value);
						break;
					case "PRODUCER":
						info.setProducer(value);
						break;
					case "SUBJECT":
						info.setSubject(value);
						break;
					case "TITLE":
						info.setTitle(value);
						break;
					case "TRAPPED":
						if ("True".equals(value) || "False".equals(value))
							info.setTrapped(value);
						else
							info.setTrapped("Unknown");
						break;
					default:
						info.setCustomMetadataValue(key, value);
						break;
				}
			}
			doc.setDocumentInformation(info);
			doc.save(outputStream);
		} catch (IOException e) {
			log.error("Setting Info to PDF Exception: ", e);
			throw(e);
		}
	}
}