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

import com.google.api.client.http.HttpStatusCodes;
import com.google.dicomwebfuse.auth.AuthAdc;
import com.google.dicomwebfuse.dao.FuseDao;
import com.google.dicomwebfuse.dao.FuseDaoImpl;
import com.google.dicomwebfuse.dao.http.HttpClientFactory;
import com.google.dicomwebfuse.dao.http.HttpClientFactoryImpl;
import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.fuse.AccessChecker;
import com.google.dicomwebfuse.fuse.DicomFuseFS;
import com.google.dicomwebfuse.fuse.Parameters;
import com.google.dicomwebfuse.fuse.cacher.DownloadCacher;
import com.google.dicomwebfuse.parser.MainArguments;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

public abstract class Mount<T extends MainArguments> {

  abstract Parameters setParameters(T arguments, FuseDao fuseDao, OS os);
  abstract void startTestIfPresent(DicomFuseFS dicomFuseFS, Parameters parameters, DownloadCacher downloadCacher);

  public void mountDicomFuseFS(T arguments) throws DicomFuseException {
    AuthAdc authAdc = createAuth(arguments.keyPath);
    HttpClientFactory httpClientFactory = new HttpClientFactoryImpl();
    FuseDao fuseDAO = new FuseDaoImpl(authAdc, httpClientFactory);
    OS os = Platform.getNativePlatform().getOS();

    Parameters parameters = setParameters(arguments, fuseDAO, os);

    DownloadCacher downloadCacher = new DownloadCacher(parameters);
    DicomFuseFS dicomFuseFS = new DicomFuseFS(parameters, downloadCacher);

    checkAccess(parameters);

    startTestIfPresent(dicomFuseFS, parameters, downloadCacher);

    String[] fuseMountOptions = setFuseMountOptions(os);

    try {
      dicomFuseFS.mount(arguments.mountPath, true, false, fuseMountOptions);
    } finally {
      dicomFuseFS.umount();
    }
  }

  private void checkAccess(Parameters parameters) throws DicomFuseException {
    AccessChecker accessChecker = new AccessChecker(parameters);
    try {
      accessChecker.check();
    } catch (DicomFuseException e) {
      if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN) {
        throw new DicomFuseException("Please check your Project name, Location, Dataset name in "
            + "--datasetAddr argument. Check that Project and Dataset exist. Also, check the role "
            + "for current account service key. The role should be Healthcare DICOM Editor.", e);
      } else {
        throw new DicomFuseException("AccessChecker error!", e);
      }
    }
  }

  private AuthAdc createAuth(Path keyPath) throws DicomFuseException {
    try {
      return new AuthAdc(keyPath);
    } catch (IOException e) {
      throw new DicomFuseException("AuthAdc error!", e);
    }
  }

  private String[] setFuseMountOptions(OS os) throws DicomFuseException {
    ArrayList<String> mountOptions = new ArrayList<>();
    if (os == OS.DARWIN || os == OS.LINUX) {
      Path userHome = Paths.get(System.getProperty("user.home"));
      try {
        mountOptions.add("-ouid=" + Files.getAttribute(userHome, "unix:uid"));
        mountOptions.add("-ogid=" + Files.getAttribute(userHome, "unix:gid"));
      } catch (IOException e) {
        throw new DicomFuseException("Set DICOMFuse options error!", e);
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
      mountOptions.add("-ofsname=DICOMFuse");
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
      mountOptions.add("-ofsname=DICOMFuse");
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
