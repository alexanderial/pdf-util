package com.smc.pdfutil.service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class PdfServiceTest {
	private static final String BASE_PATH = System.getProperty("user.dir") + File.separator + "data" + File.separator;
	private static final String PDF1_PATH = BASE_PATH + "PDF_1.pdf";
	private static final String PDF2_PATH = BASE_PATH + "PDF_2.pdf";
	private static final String OUTPUT_PATH = BASE_PATH + "output" + File.separator;
	
	private static final String EN_PDF1_PATH = BASE_PATH + "encrypted_PDF_1.pdf";
	private static final String OWNER_PWD = "OWNER_PWD";
	private static final String USER_PWD = "USER_PWD";
	
	private static final String PDF1_PAGE1_TEXT = "PDF 1 Page 1 Line 1\r\n"
												+ "PDF 1 Page 1 Line 2\r\n"
												+ "PDF 1 Page 1 Line 3\r\n"
												+ "PDF 1 Page 1 Line 4\r\n"
												+ "PDF 1 Page 1 Line 5\r\n";
	private static final String PDF1_PAGE2_TEXT = "PDF 1 Page 2 Line 1\r\n"
												+ "PDF 1 Page 2 Line 2\r\n"
												+ "PDF 1 Page 2 Line 3\r\n"
												+ "PDF 1 Page 2 Line 4\r\n"
												+ "PDF 1 Page 2 Line 5\r\n";
	private static final String PDF2_PAGE1_TEXT = "PDF 2 Page 1 Line 1\r\n"
												+ "PDF 2 Page 1 Line 2\r\n"
												+ "PDF 2 Page 1 Line 3\r\n"
												+ "PDF 2 Page 1 Line 4\r\n"
												+ "PDF 2 Page 1 Line 5\r\n";
	private static final String PDF2_PAGE2_TEXT = "PDF 2 Page 2 Line 1\r\n"
												+ "PDF 2 Page 2 Line 2\r\n"
												+ "PDF 2 Page 2 Line 3\r\n"
												+ "PDF 2 Page 2 Line 4\r\n"
												+ "PDF 2 Page 2 Line 5\r\n";
	private static final String PDF1_TEXT = PDF1_PAGE1_TEXT + PDF1_PAGE2_TEXT;
	private static final String PDF2_TEXT = PDF2_PAGE1_TEXT + PDF2_PAGE2_TEXT;
	
	@BeforeClass
	public static void setUp() {
		File outputDir = new File(OUTPUT_PATH);
		if (outputDir.exists() || outputDir.mkdir()) {
			File[] files = outputDir.listFiles();
			if (null != files) {
				for (File f : files) {
					assertTrue(f.delete());
				}
			}
		}
	}

	@AfterClass
	public static void tearDown() {
	}

	@Test
	public void testCreate() {
		String outputPath = OUTPUT_PATH + "create_output.pdf";

		try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.create(0, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals("", PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testRead() {
		try (FileInputStream inputStream1 = new FileInputStream(PDF1_PATH);
			 FileInputStream inputStream2 = new FileInputStream(PDF2_PATH)) {
			assertEquals(PDF1_TEXT, PdfService.read(inputStream1));
			assertEquals(PDF2_TEXT, PdfService.read(inputStream2));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}
	@Test
	public void testSplit() {
		String outputPath = OUTPUT_PATH + "split_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.split(inputStream, new int[] {1}, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_PAGE2_TEXT, PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testMerge() {
		String outputPath = OUTPUT_PATH + "merge_output.pdf";

		try (FileInputStream inputStream1 = new FileInputStream(PDF1_PATH);
			 FileInputStream inputStream2 = new FileInputStream(PDF2_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			Map<InputStream, String> map = new HashMap<> ();
			map.put(inputStream1, "");
			map.put(inputStream2, "");
			PdfService.merge(map.entrySet().stream(), outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			String resultStr = PdfService.read(resultStream);
			assertTrue(resultStr.contains(PDF1_TEXT));
			assertTrue(resultStr.contains(PDF2_TEXT));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testMergeEncrypted() {
		String outputPath = OUTPUT_PATH + "merge_encrypted_output.pdf";

		try (FileInputStream inputStream1 = new FileInputStream(EN_PDF1_PATH);
			 FileInputStream inputStream2 = new FileInputStream(PDF2_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			Map<InputStream, String> map = new HashMap<> ();
			map.put(inputStream1, USER_PWD);
			map.put(inputStream2, "");
			PdfService.merge(map.entrySet().stream(), outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			String resultStr = PdfService.read(resultStream);
			assertTrue(resultStr.contains(PDF1_TEXT));
			assertTrue(resultStr.contains(PDF2_TEXT));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEncrypt() {
		String outputPath = OUTPUT_PATH + "encrypt_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.encrypt(inputStream, OWNER_PWD, USER_PWD, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertNull(PdfService.read(resultStream));
		} catch (InvalidPasswordException e) {
			e.printStackTrace();
			assertEquals("Cannot decrypt PDF, the password is incorrect", e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream, OWNER_PWD));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream, USER_PWD));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testDraw() {
		String outputPath = OUTPUT_PATH + "draw_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileInputStream imgStream = new FileInputStream(BASE_PATH + "sign.png");
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.draw(inputStream, 0, imgStream, 2f, 3f, 2f, 1f, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testDrawLongImage() {
		String outputPath = OUTPUT_PATH + "draw_long_image_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileInputStream imgStream = new FileInputStream(BASE_PATH + "long_image.png");
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.draw(inputStream, 0, imgStream, 2f, 3f, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testDrawWithNullPDF() {
		String outputPath = OUTPUT_PATH + "draw_null_pdf_output.pdf";

		try (FileInputStream imgStream = new FileInputStream(BASE_PATH + "sign.png");
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.draw(null, 0, imgStream, 2f, 3f, 2f, 1f, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals("\r\n", PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testCapture() {
		String outputPath = OUTPUT_PATH + "capture_output.png";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.capture(inputStream, 0, 1, 2, 3, 4, 600f, outputStream);
			assertTrue(new File(outputPath).exists());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testCaptureFull() {
		String outputPath = OUTPUT_PATH + "capture_full_output.png";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.capture(inputStream, 0, 600f, outputStream);
			assertTrue(new File(outputPath).exists());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testWatermark() {
		String outputPath = OUTPUT_PATH + "watermark_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileInputStream watermarkStream = new FileInputStream(BASE_PATH + "watermark.pdf");
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.watermark(inputStream, watermarkStream, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testWatermarkEncrypted() {
		String outputPath = OUTPUT_PATH + "watermark_encrypted_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH);
			 FileInputStream watermarkStream = new FileInputStream(BASE_PATH + "encrypted_watermark.pdf");
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			PdfService.watermark(inputStream, null, watermarkStream, OWNER_PWD, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream resultStream = new FileInputStream(outputPath)) {
			assertEquals(PDF1_TEXT, PdfService.read(resultStream));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetInfo() {
		try (FileInputStream inputStream = new FileInputStream(PDF1_PATH)) {
			Map<String, String> infoMap = PdfService.getInfo(inputStream);
			assertEquals("Author_Value", infoMap.get("Author"));
			assertEquals("Creator_Value", infoMap.get("Creator"));
			assertEquals("Keywords_Value", infoMap.get("Keywords"));
			assertEquals("Producer_Value", infoMap.get("Producer"));
			assertEquals("Subject_Value", infoMap.get("Subject"));
			assertEquals("Title_Value", infoMap.get("Title"));
			assertEquals("False", infoMap.get("Trapped"));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testSetInfo() {
		try (FileInputStream inputStream = new FileInputStream(PDF2_PATH)) {
			Map<String, String> infoMap = PdfService.getInfo(inputStream);
			assertNull(infoMap.get("Author"));
			assertNull(infoMap.get("Creator"));
			assertNull(infoMap.get("Keywords"));
			assertNull(infoMap.get("Producer"));
			assertNull(infoMap.get("Subject"));
			assertEquals("PDF_2", infoMap.get("Title"));
			assertNull(infoMap.get("Trapped"));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		String outputPath = OUTPUT_PATH + "setInfo_output.pdf";

		try (FileInputStream inputStream = new FileInputStream(PDF2_PATH);
			 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
			HashMap<String, String> infoMap = new HashMap<>();
			infoMap.put("Author", "Author_Value");
			infoMap.put("Creator", "Creator_Value");
			infoMap.put("Keywords", "Keywords_Value");
			infoMap.put("Producer", "Producer_Value");
			infoMap.put("Subject", "Subject_Value");
			infoMap.put("Title", "Title_Value");
			infoMap.put("Trapped", "False");
			infoMap.put("Custom_Key", "Custom_Value");
			PdfService.setInfo(inputStream, infoMap, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}

		try (FileInputStream inputStream = new FileInputStream(outputPath)) {
			Map<String, String> infoMap = PdfService.getInfo(inputStream);
			assertEquals("Author_Value", infoMap.get("Author"));
			assertEquals("Creator_Value", infoMap.get("Creator"));
			assertEquals("Keywords_Value", infoMap.get("Keywords"));
			assertEquals("Producer_Value", infoMap.get("Producer"));
			assertEquals("Subject_Value", infoMap.get("Subject"));
			assertEquals("Title_Value", infoMap.get("Title"));
			assertEquals("False", infoMap.get("Trapped"));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}
}
