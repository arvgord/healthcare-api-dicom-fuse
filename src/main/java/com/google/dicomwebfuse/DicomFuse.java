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

package com.google.dicomwebfuse;

import com.google.dicomwebfuse.exception.DicomFuseException;
import com.google.dicomwebfuse.mount.DicomFuseConfigurator;
import com.google.dicomwebfuse.mount.DicomFuseMount;
import com.google.dicomwebfuse.mount.Mount;
import com.google.dicomwebfuse.parser.FuseArguments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DicomFuse {

  private static final Logger LOGGER = LogManager.getLogger();

  public static void main(String[] args) {
    FuseArguments fuseArguments = new FuseArguments();
    DicomFuseConfigurator.configureDicomFuse(args, fuseArguments);
    if (!fuseArguments.help) {
      Mount<FuseArguments> mount = new DicomFuseMount();
      try {
        mount.mountDicomFuseFS(fuseArguments);
      } catch (DicomFuseException e) {
        LOGGER.error("DICOMFuse error!", e);
      }
    }
  }
}
