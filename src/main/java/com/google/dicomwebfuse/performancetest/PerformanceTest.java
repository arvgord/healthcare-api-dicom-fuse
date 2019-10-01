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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import jnr.ffi.Platform.OS;

public class PerformanceTest {

  private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private static final int BYTES_IN_GIBIBYTE = 1024 * 1024 * 1024;
  private final Parameters parameters;
  private final DownloadCacher downloadCacher;
  private final ExecutorService executorService;

  public PerformanceTest(Parameters parameters, DownloadCacher downloadCacher) {
    this.parameters = parameters;
    this.downloadCacher = downloadCacher;
    executorService = Executors.newFixedThreadPool(parameters.getMaxThreads());
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

      // Start downloading a single test file
      Path firstTestFile = inputDicomFiles.get(0);
      Path tempFile1 = Files.createTempFile("test-", ".dcm");
      tempFile1.toFile().deleteOnExit();
      long startTime1 = System.currentTimeMillis();
      Files.copy(firstTestFile, tempFile1, StandardCopyOption.REPLACE_EXISTING);
      long endTime1 = System.currentTimeMillis();
      Metrics downloadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime1)
          .endTime(endTime1);
      System.out.printf("%-50s%-8.2f%s%n", "File size is",
          downloadMetrics.getFileSizeInMebibyte(), "MiB");
      // Skip some test for Windows
      if (parameters.getOs() != OS.WINDOWS) {
        System.out.printf("%-50s%-8d%s%n", "Download and read time",
            downloadMetrics.getElapsedTimeInMilliseconds(), "ms");
        System.out.printf("%-50s%-8.2f%s%n", "Download and read rate per second",
            downloadMetrics.getTransmissionRateInMibPerSec(), "MiB/s");

        // Start copying a single test file from local cache
        Path tempFile2 = Files.createTempFile("temp-", ".dcm");
        long startTime2 = System.currentTimeMillis();
        Files.copy(firstTestFile, tempFile2, StandardCopyOption.REPLACE_EXISTING);
        long endTime2 = System.currentTimeMillis();
        Metrics copyFromCacheMetrics = Metrics.forConfiguration(Files.size(tempFile2))
            .startTime(startTime2)
            .endTime(endTime2);
        System.out.printf("%-50s%-8d%s%n", "Copy time from local cache",
            copyFromCacheMetrics.getElapsedTimeInMilliseconds(), "ms");
        System.out.printf("%-50s%-8.2f%s%n", "Read rate per second from local cache",
            copyFromCacheMetrics.getTransmissionRateInMibPerSec(), "MiB/s");
        Files.delete(tempFile2);
      }

      // Start uploading a single test file
      Path outputStore = parameters.getUploadStore();
      Path outputFile = outputStore.resolve(UUID.randomUUID().toString());
      long startTime3 = System.currentTimeMillis();
      Files.copy(tempFile1, outputFile);
      long endTime3 = System.currentTimeMillis();
      Metrics uploadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime3)
          .endTime(endTime3);
      System.out.printf("%-50s%-8d%s%n", "Write and upload time",
          uploadMetrics.getElapsedTimeInMilliseconds(), "ms");
      System.out.printf("%-50s%-8.2f%-8s%-8.2f%s%n", "Average transmission rate",
          uploadMetrics.getTransmissionRateInMibPerSec(), "MiB/s",
          uploadMetrics.getTransmissionRateInGibPerHour(), "GiB/h");

      System.out.printf("%-50s%-8.2f%s%n", "Used heap memory",
          (double) memoryMXBean.getHeapMemoryUsage().getUsed() / BYTES_IN_GIBIBYTE, "GiB");
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

    List<Path> tempFiles = new ArrayList<>();
    List<Future<Void>> copyFutureList = new ArrayList<>();
    for (Path inputDicomFile : inputDicomFiles) {
      Callable<Void> callable = () -> {
        Path tempFile = Files.createTempFile("test-", ".dcm");
        tempFile.toFile().deleteOnExit();
        Files.copy(inputDicomFile, tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFiles.add(tempFile);
        return null;
      };
      Future<Void> future = executorService.submit(callable);
      copyFutureList.add(future);
    }
    for (Future<Void> future : copyFutureList) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new DicomFuseException(e);
      }
    }
    try {
      for (int i = 1; i <= parameters.getIterations(); i++) {
        System.out.println();
        System.out.printf("Start iteration %d%n", i);
        List<Future<Metrics>> metricsFutureList = new ArrayList<>();
        List<Metrics> results = new ArrayList<>();
        long commonStartTime = System.currentTimeMillis();
        for (Path tempFile : tempFiles) {
          Callable<Metrics> callable = () -> {
            Path outputFile = parameters.getUploadStore().resolve(UUID.randomUUID().toString());
            long startTime = System.currentTimeMillis();
            Files.copy(tempFile, outputFile);
            long endTime = System.currentTimeMillis();
            return Metrics.forConfiguration(Files.size(tempFile))
                .startTime(startTime)
                .endTime(endTime);
          };
          Future<Metrics> future = executorService.submit(callable);
          metricsFutureList.add(future);
        }
        for (Future<Metrics> future : metricsFutureList) {
          try {
            results.add(future.get());
          } catch (InterruptedException | ExecutionException e) {
            throw new DicomFuseException(e);
          }
        }
        long commonEndTime = System.currentTimeMillis();
        long fileSizesInBytes = 0;
        for (Metrics metrics : results) {
          fileSizesInBytes += metrics.getFileSizeInBytes();
        }
        Metrics commonMetrics = Metrics.forConfiguration(fileSizesInBytes)
            .startTime(commonStartTime)
            .endTime(commonEndTime);
        System.out.printf("%-50s%-8.2f%s%n", "Total mebibytes uploaded",
            commonMetrics.getFileSizeInMebibyte(), "MiB");
        System.out.printf("%-50s%-8d%s%n", "Write and upload time",
            commonMetrics.getElapsedTimeInMilliseconds(), "ms");
        System.out.printf("%-50s%-8.2f%-8s%-8.2f%s%n", "Average transmission rate",
            commonMetrics.getTransmissionRateInMibPerSec(), "MiB/s",
            commonMetrics.getTransmissionRateInGibPerHour(), "GiB/h");
      }
    } finally {
      executorService.shutdown();
    }
  }
}
