package com.google.dicomwebfuse.performancetest;

public class Metrics {

  private static final int BYTES_IN_MEBIBYTE = 1024 * 1024;
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

  long getLatency() {
    return endTime - startTime;
  }

  double getLatencyInSeconds() {
    return (double) getLatency() / 1000;
  }

  double getFileSizeInMebibyte() {
    return (double) fileSize / BYTES_IN_MEBIBYTE;
  }

  long getFileSizeInBytes() {
    return fileSize;
  }

  double getTransmissionRate() {
    return getFileSizeInMebibyte() / getLatencyInSeconds();
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
