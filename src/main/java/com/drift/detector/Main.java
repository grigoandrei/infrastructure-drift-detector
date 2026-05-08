package com.drift.detector;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String desiredPath = null;
        String actualPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--desired" -> desiredPath = args[++i];
                case "--actual" -> actualPath = args[++i];
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
                }
            }
        }

        if (desiredPath == null || actualPath == null) {
            System.err.println("Usage: drift-detector --desired <file> --actual <file>");
            System.exit(1);
        }

        var loader = new YamlLoader();
        var desired = loader.load(desiredPath);
        var actual = loader.load(actualPath);

        var detector = new DriftDetector();
        List<DriftResult> results = detector.detect(desired, actual);

        if (results.isEmpty()) {
            System.out.println("No drift detected.");
        } else {
            System.out.println("DRIFT DETECTED — " + results.size() + " difference(s) found\n");
            for (DriftResult r : results) {
                String label = String.format("  %-10s %-40s", r.type(), r.path());
                switch (r.type()) {
                    case MODIFIED -> System.out.println(label + "expected: " + r.expected() + "  actual: " + r.actual());
                    case REMOVED -> System.out.println(label + "missing from actual state");
                    case ADDED -> System.out.println(label + "unexpected in actual state");
                }
            }
        }

        System.exit(results.isEmpty() ? 0 : 1);
    }
}