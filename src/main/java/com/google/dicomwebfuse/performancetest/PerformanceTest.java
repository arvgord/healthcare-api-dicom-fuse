// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.dicomwebfuse.performancetest;

import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.fuse.Parameters;
import com.google.dicomwebfuse.fuse.cacher.DownloadCacher;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class PerformanceTest {

  private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private static final int BYTES_IN_GIBIBYTE = 1024 * 1024 * 1024;
  private final Parameters parameters;
  private final DownloadCacher downloadCacher;

  public PerformanceTest(Parameters parameters, DownloadCacher downloadCacher) {
    this.parameters = parameters;
    this.downloadCacher = downloadCacher;
  }

  public void startPerformanceTest() throws IOException, DicomFuseException {
    System.out.println("Test started");
    System.out.println("Searching for files in the input folder ...");
    Path inputStore = parameters.getDownloadStore();
    List<Path> inputDicomFiles = Files.walk(inputStore)
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());
    if (inputDicomFiles.size() == 0) {
      throw new DicomFuseException("Error! Download store is empty!");
    }
    System.out.printf("Found %d files %n", inputDicomFiles.size());
    startSingleFilePerformanceTest(parameters, inputDicomFiles);
    startMultithreadedPerformanceTest(parameters, inputDicomFiles);
  }

  private void startSingleFilePerformanceTest(Parameters parameters, List<Path> inputDicomFiles)
      throws IOException {
    System.out.println("Started test for single file");
    for (int i = 1; i <= parameters.getIterations(); i++) {
      System.out.println();
      System.out.printf("%s%d%n", "Start iteration ", i);
      downloadCacher.clearCache();

      // Start download single file test
      Path firstTestFile = inputDicomFiles.get(0);
      Path tempFile1 = Files.createTempFile("test-", ".dcm");
      tempFile1.toFile().deleteOnExit();
      long startTime1 = System.currentTimeMillis();
      Files.copy(firstTestFile, tempFile1, StandardCopyOption.REPLACE_EXISTING);
      long endTime1 = System.currentTimeMillis();
      Metrics downloadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime1)
          .endTime(endTime1);
      System.out
          .printf("%-50s%-8.2f%s%n", "File size is", downloadMetrics.getFileSizeInMebibyte(), "MiB");
      System.out
          .printf("%-50s%-8d%s%n", "Download and read latency", downloadMetrics.getLatency(), "ms");
      System.out.printf("%-50s%-8.2f%s%n", "Download and read rate per second",
          downloadMetrics.getTransmissionRateInMibPerSec(), "MiB/s");

      // Start copy single file from local cache test
      Path tempFile2 = Files.createTempFile("temp-", ".dcm");
      long startTime2 = System.currentTimeMillis();
      Files.copy(firstTestFile, tempFile2, StandardCopyOption.REPLACE_EXISTING);
      long endTime2 = System.currentTimeMillis();
      Metrics copyFromCacheMetrics = Metrics.forConfiguration(Files.size(tempFile2))
          .startTime(startTime2)
          .endTime(endTime2);
      System.out.printf("%-50s%-8d%s%n", "Copy latency from local cache",
          copyFromCacheMetrics.getLatency(), "ms");
      System.out.printf("%-50s%-8.2f%s%n", "Read rate per second from local cache",
          copyFromCacheMetrics.getTransmissionRateInMibPerSec(), "MiB/s");
      Files.delete(tempFile2);

      // Start upload single file test
      Path outputStore = parameters.getUploadStore();
      Path outputFile = outputStore.resolve(tempFile1.getFileName());
      long startTime3 = System.currentTimeMillis();
      Files.copy(tempFile1, outputFile, StandardCopyOption.REPLACE_EXISTING);
      long endTime3 = System.currentTimeMillis();
      Metrics uploadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime3)
          .endTime(endTime3);
      System.out.printf("%-50s%-8d%s%n", "Upload latency", uploadMetrics.getLatency(), "ms");
      System.out.printf("%-50s%-8.2f%-8s%-8.2f%s%n", "Average transmission rate",
          uploadMetrics.getTransmissionRateInMibPerSec(), "MiB/s", uploadMetrics.getTransmissionRateInGibPerHour(), "GiB/h");

      printHeapMemoryUsage();
    }
  }

  private void startMultithreadedPerformanceTest(Parameters parameters, List<Path> inputDicomFiles)
      throws DicomFuseException {
    // Start multithreaded upload test
    System.out.println();
    System.out.println();
    int filesCount = inputDicomFiles.size();
    System.out.printf("Started upload test for %d files, using %d threads", filesCount,
        parameters.getMaxThreads());
    System.out.println();
    ExecutorService executorService = Executors.newFixedThreadPool(parameters.getMaxThreads());
    for (int i = 1; i <= parameters.getIterations(); i++) {
      System.out.println();
      System.out.printf("Start iteration %d %n", i);
      downloadCacher.clearCache();
      List<Future<Metrics>> futureList = new ArrayList<>();
      List<Metrics> results = new ArrayList<>();
      for (Path inputFile : inputDicomFiles) {
        Callable<Metrics> callable = () -> {
          Path tempFile = Files.createTempFile("test-", ".dcm");
          Files.copy(inputFile, tempFile, StandardCopyOption.REPLACE_EXISTING);
          Path outputFile = parameters.getUploadStore().resolve(tempFile.getFileName());
          long startTime = System.currentTimeMillis();
          Files.copy(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
          long endTime = System.currentTimeMillis();
          Metrics uploadMetrics = Metrics.forConfiguration(Files.size(tempFile))
              .startTime(startTime)
              .endTime(endTime);
          Files.delete(tempFile);
          return uploadMetrics;
        };
        Future<Metrics> future = executorService.submit(callable);
        futureList.add(future);
      }
      for (Future<Metrics> future : futureList) {
        try {
          results.add(future.get());
        } catch (InterruptedException | ExecutionException e) {
          throw new DicomFuseException(e);
        }
      }
      double commonTransmissionRateInMibPerSec = 0;
      double commonTransmissionRateInGibPerHour = 0;
      double fileSizesInMib = 0;
      for (Metrics metrics : results) {
        commonTransmissionRateInMibPerSec += metrics.getTransmissionRateInMibPerSec();
        commonTransmissionRateInGibPerHour += metrics.getTransmissionRateInGibPerHour();
        fileSizesInMib += metrics.getFileSizeInMebibyte();
      }
      double averageTransmissionRateMibPerSec = commonTransmissionRateInMibPerSec / filesCount;
      double averageTransmissionRateGibPerHour = commonTransmissionRateInGibPerHour / filesCount;
      System.out.printf("%-50s%-8.2f%s%n", "Total mebibytes uploaded",
          fileSizesInMib, "MiB");
      System.out.printf("%-50s%-8.2f%-8s%-8.2f%s%n", "Average transmission rate",
          averageTransmissionRateMibPerSec, "MiB/s", averageTransmissionRateGibPerHour, "GiB/h");
    }
    executorService.shutdown();
  }

  private void printHeapMemoryUsage() {
    System.out.printf("%-50s%-8.2f%s%n", "Used heap memory",
        (double) memoryMXBean.getHeapMemoryUsage().getUsed() / BYTES_IN_GIBIBYTE, "GiB");
  }
}
