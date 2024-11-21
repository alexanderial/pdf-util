package com.smc.pdfutil.service;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CompressServiceTest {
    private static final String BASE_PATH = System.getProperty("user.dir") + File.separator + "data" + File.separator;
    private static final String PDF1_PATH = BASE_PATH + "PDF_1.pdf";
    private static final String PDF2_PATH = BASE_PATH + "PDF_2.pdf";
    private static final String OUTPUT_PATH = BASE_PATH + "output" + File.separator;

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
    public void testCompress() {
        String outputPath = OUTPUT_PATH + "compress.zip";

        try {
            List<File> inputFiles = new ArrayList<File> ();
            inputFiles.add(new File(BASE_PATH + "big_pdf.pdf"));
            File outputFile = new File(outputPath);
            CompressService.compress(inputFiles, outputFile, 20 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
