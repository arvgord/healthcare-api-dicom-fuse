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

import com.google.dicomwebfuse.mount.DicomFuseConfigurator;
import com.google.dicomwebfuse.mount.DicomFuseTestMount;
import com.google.dicomwebfuse.mount.Mount;
import com.google.dicomwebfuse.parser.FuseTestArguments;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class DicomFuseTest {

  private static final Logger LOGGER = LogManager.getLogger();

  public static void main(String[] args) {
    changeConsoleLoggingLevel();
    FuseTestArguments fuseTestArguments = new FuseTestArguments();
    DicomFuseConfigurator.configureDicomFuse(args, fuseTestArguments);
    if (!fuseTestArguments.help) {
      Mount<FuseTestArguments> mount = new DicomFuseTestMount();
      mount.mountDicomFuseFS(fuseTestArguments);
    }
  }

  private static void changeConsoleLoggingLevel() {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig rootLoggerConfig = config.getRootLogger();
    rootLoggerConfig.removeAppender("Console");
    rootLoggerConfig.addAppender(config.getAppender("Console"), Level.ERROR, null);
    ctx.updateLoggers();
  }
}
