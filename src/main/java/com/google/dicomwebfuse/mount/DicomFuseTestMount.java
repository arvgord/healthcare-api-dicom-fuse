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

package com.google.dicomwebfuse.mount;

import com.google.dicomwebfuse.dao.FuseDao;
import com.google.dicomwebfuse.entities.cache.CacheTime;
import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.fuse.DicomFuseFS;
import com.google.dicomwebfuse.fuse.Parameters;
import com.google.dicomwebfuse.fuse.cacher.DownloadCacher;
import com.google.dicomwebfuse.parser.FuseTestArguments;
import com.google.dicomwebfuse.performancetest.PerformanceTest;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import jnr.ffi.Platform.OS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DicomFuseTestMount extends Mount<FuseTestArguments> {

  private static final int TIMEOUT = 10; // sec
  private static final int DELAY = 1; // sec
  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  Parameters setParameters(FuseTestArguments arguments, FuseDao fuseDao, OS os) {
    // Set maximum cacheTime and cacheSize for tests
    CacheTime cacheTime = new CacheTime(Integer.MAX_VALUE, Integer.MAX_VALUE);
    long cacheSize = Integer.MAX_VALUE;
    return new Parameters(fuseDao, os, arguments, cacheTime, cacheSize);
  }

  @Override
  void startTestIfPresent(DicomFuseFS dicomFuseFS, Parameters parameters,
      DownloadCacher downloadCacher) {
    Instant instant = Instant.now().plusSeconds(TIMEOUT);
    Runnable testRunnable = () -> {
      while (instant.isAfter(Instant.now().minusSeconds(1))) {
        if (dicomFuseFS.isDicomFuseMounted()) {
          try {
            TimeUnit.SECONDS.sleep(DELAY);
            PerformanceTest performanceTest = new PerformanceTest(parameters, downloadCacher);
            performanceTest.startPerformanceTest();
          } catch (IOException | DicomFuseException | InterruptedException e) {
            LOGGER.error("Test error!", e);
          } finally {
            dicomFuseFS.umount();
          }
          return;
        } else if (instant.isBefore(Instant.now())) {
          LOGGER.error("Timeout test error. DICOMFuse is not mounted!");
          return;
        }
      }
    };
    new Thread(testRunnable).start();
  }
}
