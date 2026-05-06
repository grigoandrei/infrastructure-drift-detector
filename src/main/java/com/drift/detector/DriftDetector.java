package com.drift.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DriftDetector {

    public List<DriftResult> detect(Map<String, Object> desired, Map<String, Object> actual) {
        List<DriftResult> results = new ArrayList<>();
        compare(desired, actual, "", results);
        return results;
    }

    @SuppressWarnings("unchecked")
    private void compare(Map<String, Object> desired, Map<String, Object> actual, String prefix, List<DriftResult> results) {
        for (String key : desired.keySet()) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object expectedVal = desired.get(key);
            Object actualVal = actual.get(key);

            if (!actual.containsKey(key)) {
                results.add(new DriftResult(path, DriftType.REMOVED, expectedVal, null));
            } else if (expectedVal instanceof Map && actualVal instanceof Map) {
                compare((Map<String, Object>) expectedVal, (Map<String, Object>) actualVal, path, results);
            } else if (expectedVal instanceof List && actualVal instanceof List) {
                compareLists((List<Object>) expectedVal, (List<Object>) actualVal, path, results);
            } else if (!equals(expectedVal, actualVal)) {
                results.add(new DriftResult(path, DriftType.MODIFIED, expectedVal, actualVal));
            }
        }

        for (String key : actual.keySet()) {
            if (!desired.containsKey(key)) {
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                results.add(new DriftResult(path, DriftType.ADDED, null, actual.get(key)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void compareLists(List<Object> desired, List<Object> actual, String path, List<DriftResult> results) {
        int minSize = Math.min(desired.size(), actual.size());
        for (int i = 0; i < minSize; i++) {
            Object expectedVal = desired.get(i);
            Object actualVal = actual.get(i);
            String indexedPath = path + "[" + i + "]";

            if (expectedVal instanceof Map && actualVal instanceof Map) {
                compare((Map<String, Object>) expectedVal, (Map<String, Object>) actualVal, indexedPath, results);
            } else if (expectedVal instanceof List && actualVal instanceof List) {
                compareLists((List<Object>) expectedVal, (List<Object>) actualVal, indexedPath, results);
            } else if (!equals(expectedVal, actualVal)) {
                results.add(new DriftResult(indexedPath, DriftType.MODIFIED, expectedVal, actualVal));
            }
        }

        for (int i = minSize; i < actual.size(); i++) {
            results.add(new DriftResult(path + "[" + i + "]", DriftType.ADDED, null, actual.get(i)));
        }
        for (int i = minSize; i < desired.size(); i++) {
            results.add(new DriftResult(path + "[" + i + "]", DriftType.REMOVED, desired.get(i), null));
        }
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
