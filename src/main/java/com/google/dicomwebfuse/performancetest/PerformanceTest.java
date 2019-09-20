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

public class PerformanceTest {

  private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private final Parameters parameters;
  private final DownloadCacher downloadCacher;

  public PerformanceTest(Parameters parameters, DownloadCacher downloadCacher) {
    this.parameters = parameters;
    this.downloadCacher = downloadCacher;
  }

  public void startPerformanceTest() throws IOException, DicomFuseException {
    Path inputStore = parameters.getUploadStore();
    List<Path> inputDicomFiles = Files.walk(inputStore)
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());
    System.out.println("Found " + inputDicomFiles.size() + " files");
    startSingleFilePerformanceTest(parameters, inputDicomFiles);
    startMultithreadedPerformanceTest(parameters, inputDicomFiles);
  }

  private void startSingleFilePerformanceTest(Parameters parameters, List<Path> inputDicomFiles)
      throws IOException {
    for (int i = 1; i <= parameters.getIterations(); i++) {
      System.out.println("\nStart iteration " + i);
      downloadCacher.clearCache();

//      printHeapMemoryUsage();

      // Start download test
      Path firstTestFile = inputDicomFiles.get(0);
      Path tempFile1 = Files.createTempFile("test-", ".dcm");
      long startTime1 = System.currentTimeMillis();
      Files.copy(firstTestFile, tempFile1, StandardCopyOption.REPLACE_EXISTING);
      long endTime1 = System.currentTimeMillis();
      Metrics downloadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime1)
          .endTime(endTime1);
      System.out.println("Download and read latency " + downloadMetrics.getLatency() + " ms");
      System.out.println(String.format("Megabytes download and read per second from cache %.2f MB/s", downloadMetrics.getTransmissionRate()));

      // Start copy test
      Path tempFile2 = Files.createTempFile("temp-", ".dcm");
      long startTime2 = System.currentTimeMillis();
      Files.copy(firstTestFile, tempFile2, StandardCopyOption.REPLACE_EXISTING);
//      printHeapMemoryUsage();
      long endTime2 = System.currentTimeMillis();
      Metrics copyFromCacheMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime2)
          .endTime(endTime2);
      System.out.println("Copy from cache latency " + copyFromCacheMetrics.getLatency() + " ms");
      System.out.println(String.format(
          "Megabytes read per second %.2f MB/s", copyFromCacheMetrics.getTransmissionRate()));
      Files.delete(tempFile2);

      // Start upload test
      Path outputStore = parameters.getUploadStore();
      Path outputFile = outputStore.resolve(tempFile1.getFileName());
      long startTime3 = System.currentTimeMillis();
      Files.copy(tempFile1, outputFile, StandardCopyOption.REPLACE_EXISTING);
      //      printHeapMemoryUsage();
      long endTime3 = System.currentTimeMillis();
      Metrics uploadMetrics = Metrics.forConfiguration(Files.size(tempFile1))
          .startTime(startTime3)
          .endTime(endTime3);
      System.out.println("Upload latency " + uploadMetrics.getLatency() + " ms");
      System.out.println(String.format(
          "Megabytes upload per second %.2f MB/s", uploadMetrics.getTransmissionRate()));
//      printHeapMemoryUsage();
    }
  }

  private void startMultithreadedPerformanceTest(Parameters parameters, List<Path> inputDicomFiles)
      throws DicomFuseException {
    System.out.println("\nStart uploading test with parallel request using " + parameters.getMaxThreads() + " threads");
    ExecutorService executorService = Executors.newFixedThreadPool(parameters.getMaxThreads());
    List<Future<Metrics>> futureList = new ArrayList<>();
    List<Metrics> results = new ArrayList<>();
    for (int i = 0; i < inputDicomFiles.size(); i++) {
      Path inputFile = inputDicomFiles.get(i);

      Callable<Metrics> callable = () -> {
        Path tempFile = Files.createTempFile("test-", ".dcm");
        Files.copy(inputFile, tempFile, StandardCopyOption.REPLACE_EXISTING);
        Path outputFile = parameters.getUploadStore().resolve(UUID.randomUUID().toString());
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
      executorService.submit(callable);
    }
    for (Future<Metrics> future : futureList) {
      try {
        results.add(future.get());
      } catch (InterruptedException | ExecutionException e) {
        throw new DicomFuseException(e);
      }
      executorService.shutdown();
    }
    double commonTransmissionRate = 0;
    for (Metrics metrics : results) {
      System.out.println(String.format("Transmission rate %.2f MB/s", metrics.getTransmissionRate()));
      commonTransmissionRate += metrics.getTransmissionRate();
    }
    System.out.println(String.format("Average transmission rate %.2f MB/s", commonTransmissionRate/results.size()));
  }

  private void printHeapMemoryUsage() {
    System.out.println(String.format("Used heap memory: %.2f GB",
        (double) memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024 * 1024)));
  }
}
