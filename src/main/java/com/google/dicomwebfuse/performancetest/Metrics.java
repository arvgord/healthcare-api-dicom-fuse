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

package com.google.dicomwebfuse.performancetest;

public class Metrics {

  private static final int BYTES_IN_MEBIBYTE = 1024 * 1024;
  private static final int MILLISECONDS_IN_SECONDS = 1000;
  private long fileSize; // in bytes
  private long startTime; // ms
  private long endTime; // ms

  static Metrics forConfiguration(long fileSize) {
    Metrics metrics = new Metrics();
    metrics.fileSize = fileSize;
    return metrics;
  }

  Metrics startTime(long startTime) {
    this.startTime = startTime;
    return this;
  }

  Metrics endTime(long endTime) {
    this.endTime = endTime;
    return this;
  }

  long getFileSizeInBytes() {
    return fileSize;
  }

  double getTransmissionRate() {
    return getFileSizeInMebibyte() / getLatencyInSeconds();
  }

  long getLatency() {
    return endTime - startTime;
  }

  private double getLatencyInSeconds() {
    return (double) getLatency() / MILLISECONDS_IN_SECONDS;
  }

  private double getFileSizeInMebibyte() {
    return (double) fileSize / BYTES_IN_MEBIBYTE;
  }

  @Override
  public String toString() {
    return "Metrics{" +
        "fileSize=" + fileSize +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        '}';
  }

  private Metrics() {}
}
