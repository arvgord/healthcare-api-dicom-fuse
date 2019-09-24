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

import com.beust.jcommander.JCommander;
import com.google.dicomwebfuse.parser.MainArguments;
import java.util.ResourceBundle;

public class DicomFuseConfigurator {

  public static <T extends MainArguments> void configureDicomFuse(String[] args, T arguments) {

    JCommander jCommander = JCommander.newBuilder()
        .addObject(arguments)
        .resourceBundle(ResourceBundle.getBundle("cli-messages"))
        .programName("DICOMFuse")
        .build();
    jCommander.parse(args);

    if (arguments.help) {
      jCommander.usage();
    }
  }
}
