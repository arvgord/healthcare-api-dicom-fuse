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

package com.google.dicomwebfuse.fuse;

import com.google.dicomwebfuse.dao.FuseDao;
import com.google.dicomwebfuse.entities.CloudConf;
import com.google.dicomwebfuse.entities.cache.CacheTime;
import com.google.dicomwebfuse.parser.FuseArguments;
import com.google.dicomwebfuse.parser.FuseTestArguments;
import java.nio.file.Path;
import jnr.ffi.Platform.OS;

public class Parameters {

  private final FuseDao fuseDAO;
  private final OS os;
  private final CloudConf cloudConf;
  private final CacheTime cacheTime;
  private final long cacheSize;
  private final boolean enableDeletion;
  private final Path downloadStore;
  private final Path uploadStore;
  private final int maxThreads;
  private final int iterations;

  public Parameters(FuseDao fuseDAO, OS os, FuseArguments fuseArguments) {
    this.fuseDAO = fuseDAO;
    this.os = os;
    this.cloudConf = fuseArguments.cloudConf;
    this.cacheTime = fuseArguments.cacheTime;
    this.cacheSize = fuseArguments.cacheSize;
    this.enableDeletion = fuseArguments.enableDeletion;
    this.downloadStore = null;
    this.uploadStore = null;
    this.maxThreads = 0;
    this.iterations = 0;
  }

  public Parameters(FuseDao fuseDAO, OS os, FuseTestArguments fuseTestArguments,
      CacheTime cacheTime, long cacheSize) {
    this.fuseDAO = fuseDAO;
    this.os = os;
    this.cloudConf = fuseTestArguments.cloudConf;
    this.enableDeletion = fuseTestArguments.enableDeletion;
    this.downloadStore = fuseTestArguments.downloadStore;
    this.uploadStore = fuseTestArguments.uploadStore;
    this.maxThreads = fuseTestArguments.maxTreads;
    this.iterations = fuseTestArguments.iterations;
    this.cacheTime = cacheTime;
    this.cacheSize = cacheSize;
  }

  public FuseDao getFuseDAO() {
    return fuseDAO;
  }

  public CloudConf getCloudConf() {
    return cloudConf;
  }

  public CacheTime getCacheTime() {
    return cacheTime;
  }

  public long getCacheSize() {
    return cacheSize;
  }

  boolean isEnableDeletion() {
    return enableDeletion;
  }

  OS getOs() {
    return os;
  }

  public Path getDownloadStore() {
    return downloadStore;
  }

  public Path getUploadStore() {
    return uploadStore;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public int getIterations() {
    return iterations;
  }
}
