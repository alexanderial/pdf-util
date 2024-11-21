package com.smc.pdfutil.service;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.pdfbox.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CompressService {
    private static final Logger log = LoggerFactory.getLogger(CompressService.class);

    public static void compress(List<File> inputFiles, File outputFile) throws IOException {
        try (ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(outputFile)) {
            compress(inputFiles, zipStream);
        }
    }

    public static void compress(List<File> inputFiles, File outputFile, long splitSize) throws IOException {
        try (ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(outputFile, splitSize * 1024)) {
            compress(inputFiles, zipStream);
        }
    }

    private static void compress(List<File> inputFiles, ZipArchiveOutputStream zipStream) throws IOException {
        for (File inputFile: inputFiles) {
            ArchiveEntry entry = zipStream.createArchiveEntry(inputFile, inputFile.getName());
            zipStream.putArchiveEntry(entry);
            if (inputFile.isFile()) {
                try (FileInputStream inputStream = new FileInputStream(inputFile)) {
                    IOUtils.copy(inputStream, zipStream);
                }
            }
            zipStream.closeArchiveEntry();
        }
    }
}
