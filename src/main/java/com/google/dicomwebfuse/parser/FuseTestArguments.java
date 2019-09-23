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
import com.beust.jcommander.converters.IntegerConverter;
import com.beust.jcommander.converters.PathConverter;
import java.nio.file.Path;

public class FuseTestArguments extends MainArguments {

  @Parameter(
      names = {"--downloadStore"},
      descriptionKey = "option.fuseTest.downloadStore",
      order = 5,
      required = true,
      converter = PathConverter.class
  )
  public Path downloadStore;

  @Parameter(
      names = {"--uploadStore"},
      descriptionKey = "option.fuseTest.uploadStore",
      order = 6,
      required = true,
      converter = PathConverter.class
  )
  public Path uploadStore;

  @Parameter(
      names = {"--maxTreads"},
      descriptionKey = "option.fuseTest.maxThreads",
      order = 7,
      converter = IntegerConverter.class
  )
  public int maxTreads = 5;

  @Parameter(
      names = {"--iterationCount"},
      descriptionKey = "option.fuseTest.iterationCount",
      order = 8,
      converter = IntegerConverter.class
  )
  public int iterations = 3;
}
