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

package com.google.dicomwebfuse.parser;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.BooleanConverter;
import com.beust.jcommander.converters.PathConverter;
import com.google.dicomwebfuse.entities.CloudConf;
import java.nio.file.Path;

public abstract class MainArguments {

  @Parameter(
      names = {"--datasetAddr", "-a"},
      descriptionKey = "option.main.datasetAddr",
      order = 0,
      required = true,
      converter = CloudConfigurationConverter.class
  )
  public CloudConf cloudConf;

  @Parameter(
      names = {"--mountPath", "-p"},
      descriptionKey = "option.main.mountPath",
      order = 1,
      required = true,
      converter = PathConverter.class
  )
  public Path mountPath;

  @Parameter(
      names = {"--keyFile", "-k"},
      descriptionKey = "option.main.keyPath",
      order = 2,
      converter = PathConverter.class
  )
  public Path keyPath;

  @Parameter(
      names = {"--help", "-h"},
      help = true,
      descriptionKey = "option.main.help",
      order = 3
  )
  public boolean help = false;

  @Parameter(
      names = {"--enableDeletion", "-d"},
      descriptionKey = "option.main.enableDeletion",
      order = 4,
      converter = BooleanConverter.class
  )
  public boolean enableDeletion = true;
}
