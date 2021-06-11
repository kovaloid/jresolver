package com.koval.resolver.processor.documentation.convert.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.documents4j.api.DocumentType;

import com.koval.resolver.processor.documentation.bean.MediaType;
import com.koval.resolver.processor.documentation.convert.FileConverter;


public class RtfToPdfConverter implements FileConverter throws FileNotFoundException {
  private static final Logger LOGGER = LoggerFactory.getLogger(WordToPdfFileConverter.class);

  @Override
  public void convert(final String inputFilePath, final String outputFilePath) {
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      InputStream in = new BufferedInputStream (new FileInputStream(inputFilePath));
      IConverter converter = LocalConverter.builder()
        .workerPool(20, 25, 2, TimeUnit.SECONDS)
        .processTimeout(5, TimeUnit.SECONDS)
        .build();
      Future<Boolean> conversion = converter
        .convert(in).as(DocumentType.RTF)
        .to(bo).as(DocumentType.PDF)
        .prioritizeWith(1000)
        .schedule();
      try (OutputStream outputStream = new FileOutputStream(outputFilePath)) {
          conversion.get();
          bo.writeTo(outputStream);
          in.close();
          bo.close();
      } catch (IOException | InterruptedException | ExecutionException e) {
          LOGGER.error("Could not convert rtf file " + inputFilePath + " to pdf " + outputFilePath, e);
      }
  }
}

  @Override
  public MediaType getConvertibleType(MediaType docType) {
    return MediaType.RTF;
  }
