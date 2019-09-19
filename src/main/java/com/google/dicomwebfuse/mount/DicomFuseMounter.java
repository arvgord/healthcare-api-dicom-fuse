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

import com.google.dicomwebfuse.auth.AuthAdc;
import com.google.dicomwebfuse.dao.FuseDao;
import com.google.dicomwebfuse.dao.FuseDaoImpl;
import com.google.dicomwebfuse.dao.http.HttpClientFactory;
import com.google.dicomwebfuse.dao.http.HttpClientFactoryImpl;
import com.google.dicomwebfuse.entities.cache.CacheTime;
import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.fuse.AccessChecker;
import com.google.dicomwebfuse.fuse.DicomFuseFS;
import com.google.dicomwebfuse.fuse.Parameters;
import com.google.dicomwebfuse.parser.FuseArguments;
import com.google.dicomwebfuse.parser.FuseTestArguments;
import com.google.dicomwebfuse.parser.MainArguments;
import com.google.dicomwebfuse.performancetest.PerformanceTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class DicomFuseMounter {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final int INITIAL_DELAY = 2; // sec
  private static final int DELAY = 1; // sec
  private static int timeout = 10;

  <T extends MainArguments> void mountDicomFuse(T arguments) {
    AuthAdc authAdc = createAuth(arguments.keyPath);
    HttpClientFactory httpClientFactory = new HttpClientFactoryImpl();
    FuseDao fuseDAO = new FuseDaoImpl(authAdc, httpClientFactory);
    OS os = Platform.getNativePlatform().getOS();

    Parameters parameters;
    if (arguments instanceof FuseTestArguments) {
      // Set maximum cacheTime for performance tests
      CacheTime cacheTime = new CacheTime(Integer.MAX_VALUE, Integer.MAX_VALUE);
      // Set maximum cacheSize for performance tests
      long cacheSize = Integer.MAX_VALUE;
      parameters = new Parameters(fuseDAO, os, (FuseTestArguments) arguments, cacheTime, cacheSize);
    } else {
      parameters = new Parameters(fuseDAO, os, (FuseArguments) arguments);
    }
    DicomFuseFS dicomFuseFS = new DicomFuseFS(parameters);

    checkAccess(parameters);

    String[] fuseMountOptions = setFuseMountOptions(os);

    if (arguments instanceof FuseTestArguments) {
      ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      Runnable runnable = () -> {
        if (dicomFuseFS.isDicomFuseMounted()){
          try {
            PerformanceTest.startPerformanceTest(parameters);
          } catch (IOException e) {
            LOGGER.error("Error!", e);
          }
          dicomFuseFS.umount();
          executor.shutdown();
        } else {
          timeout--;
          if (timeout < 0) {
            executor.shutdown();
            LOGGER.error("Performance test error! DICOMFuse not mounted!");
          }
        }
      };
      executor.scheduleWithFixedDelay(runnable, INITIAL_DELAY, DELAY, TimeUnit.SECONDS);
    }
    try {
      dicomFuseFS.mount(arguments.mountPath, true, false, fuseMountOptions);
    } finally {
      dicomFuseFS.umount();
    }
  }

  private void checkAccess(Parameters parameters) {
    AccessChecker accessChecker = new AccessChecker(parameters);
    try {
      accessChecker.check();
    } catch (DicomFuseException e) {
      if (e.getStatusCode() == 403) {
        LOGGER.error("Please check your Project name, Location, Dataset name in "
            + "--datasetAddr argument. Check that Project and Dataset exist. Also, check the role "
            + "for current account service key. The role should be Healthcare DICOM Editor.", e);
      } else {
        LOGGER.error("AccessChecker error!", e);
      }
      System.exit(0);
    }
  }

  private AuthAdc createAuth(Path keyPath) {
    try {
      return new AuthAdc(keyPath);
    } catch (IOException e) {
      LOGGER.error("AuthAdc error!", e);
      System.exit(0);
      return null;
    }
  }

  private String[] setFuseMountOptions(OS os) {
    ArrayList<String> mountOptions = new ArrayList<>();
    if (os == OS.DARWIN || os == OS.LINUX) {
      Path userHome = Paths.get(System.getProperty("user.home"));
      try {
        mountOptions.add("-ouid=" + Files.getAttribute(userHome, "unix:uid"));
        mountOptions.add("-ogid=" + Files.getAttribute(userHome, "unix:gid"));
      } catch (IOException e) {
        LOGGER.error("Error!", e);
        System.exit(0);
      }
    }

    if (os == OS.DARWIN) {
      // set OSXFuse mount options see: https://github.com/osxfuse/osxfuse/wiki/Mount-options
      // set the timeout in seconds for which a negative lookup will be cached
      mountOptions.add("-onegative_timeout=4"); // in seconds
      // because DICOMFuse has the internal cache, is disabled the external cache
      mountOptions.add("-onolocalcaches");
      // .DS_Store and ._ files are not used
      mountOptions.add("-onoappledouble");
      // set defer_permissions
      mountOptions.add("-odefer_permissions");
      // set name
      mountOptions.add("-ovolname=DICOMFuse");
    }

    if (os == OS.LINUX) {
      // set libfuse mount options
      // set filesystem name
      mountOptions.add("-ofsname=DicomFuse");
      //set the timeout in seconds for which a negative lookup will be cached
      mountOptions.add("-onegative_timeout=4");
      // because DICOMFuse has the internal cache, is disabled the external cache
      mountOptions.add("-oattr_timeout=0");
      mountOptions.add("-oac_attr_timeout=0");
      mountOptions.add("-oentry_timeout=0");
    }

    if (os == OS.WINDOWS) {
      // set WinFps mount options
      // set filesystem name
      mountOptions.add("-ofsname=DicomFuse");
      //set the timeout in seconds for which a negative lookup will be cached
      mountOptions.add("-onegative_timeout=4");
      // because DICOMFuse has the internal cache, is disabled the external cache
      mountOptions.add("-oattr_timeout=0");
      mountOptions.add("-oac_attr_timeout=0");
      mountOptions.add("-oentry_timeout=0");
    }
    return mountOptions.toArray(new String[0]);
  }
}
