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
import com.beust.jcommander.converters.LongConverter;
import com.google.dicomwebfuse.entities.cache.CacheTime;

public class FuseArguments extends MainArguments {

  @Parameter(
      names = {"--cacheTime", "-t"},
      descriptionKey = "option.fuse.cacheTime",
      order = 5,
      converter = CacheTimeConverter.class,
      validateWith = CacheTimePositiveValidator.class
  )
  public CacheTime cacheTime = new CacheTime(60, 300);

  @Parameter(
      names = {"--cacheSize", "-s"},
      descriptionKey = "option.fuse.cacheSize",
      order = 6,
      converter = LongConverter.class,
      validateWith = CacheSizePositiveValidator.class
  )
  public long cacheSize = 10000;
}