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
import com.google.dicomwebfuse.fuse.DicomFuseFS;
import com.google.dicomwebfuse.fuse.Parameters;
import com.google.dicomwebfuse.fuse.cacher.DownloadCacher;
import com.google.dicomwebfuse.parser.FuseArguments;
import jnr.ffi.Platform.OS;

public class DicomFuseMount extends Mount<FuseArguments> {

  @Override
  Parameters setParameters(FuseArguments arguments, FuseDao fuseDao, OS os) {
    return new Parameters(fuseDao, os, arguments);
  }

  @Override
  void startTestIfPresent(DicomFuseFS dicomFuseFS, Parameters parameters,
      DownloadCacher downloadCacher) {}
}
