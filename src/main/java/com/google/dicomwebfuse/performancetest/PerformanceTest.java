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

import com.google.dicomwebfuse.fuse.Parameters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class PerformanceTest {

  public static void startPerformanceTest(Parameters parameters) throws IOException {
    Path inputStore = parameters.getUploadStore();
    List<Path> inputFiles = Files.walk(inputStore)
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());
    Path firstTestFile = inputFiles.get(0);
    Path instanceTempFile = Files.createTempFile("temp-", ".dcm");
    long startTime = System.currentTimeMillis();
    Files.copy(firstTestFile, instanceTempFile, StandardCopyOption.REPLACE_EXISTING);
    long endTime = System.currentTimeMillis();
    long fileSize = Files.size(instanceTempFile);
    long latency = endTime - startTime;
    System.out.println("Downloading latency " + latency + " ms");
    System.out.println("Megabytes read per second " + (double) fileSize / (double) latency / 1048.576 + " MB/s");
    Path instanceTempFile2 = Files.createTempFile("temp-", ".dcm");
    long startTime1 = System.currentTimeMillis();
    Files.copy(firstTestFile, instanceTempFile2, StandardCopyOption.REPLACE_EXISTING);
    long endTime1 = System.currentTimeMillis();
    long latency1 = endTime1 - startTime1;
    System.out.println("Downloading latency " + latency1 + " ms");
    System.out.println("Megabytes read per second " + (double) fileSize / (double) latency1 / 1048.576 + " MB/s");
  }
}
