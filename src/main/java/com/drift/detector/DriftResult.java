package com.drift.detector;

public record DriftResult(String path, DriftType type, Object expected, Object actual) {}
