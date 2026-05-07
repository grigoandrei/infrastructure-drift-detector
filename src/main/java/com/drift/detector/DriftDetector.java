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
        String keyField = findKeyField(desired, actual);
        boolean[] actualMatched = new boolean[actual.size()];

        for (int i = 0; i < desired.size(); i++) {
            Object desiredVal = desired.get(i);
            int matchIndex = findMatch(desiredVal, actual, actualMatched, keyField);

            if (matchIndex == -1) {
                results.add(new DriftResult(path + "[" + i + "]", DriftType.REMOVED, desiredVal, null));
            } else {
                actualMatched[matchIndex] = true;
                Object actualVal = actual.get(matchIndex);
                String indexedPath = path + "[" + i + "]";

                if (desiredVal instanceof Map && actualVal instanceof Map) {
                    compare((Map<String, Object>) desiredVal, (Map<String, Object>) actualVal, indexedPath, results);
                } else if (desiredVal instanceof List && actualVal instanceof List) {
                    compareLists((List<Object>) desiredVal, (List<Object>) actualVal, indexedPath, results);
                }
            }
        }

        for (int i = 0; i < actual.size(); i++) {
            if (!actualMatched[i]) {
                results.add(new DriftResult(path + "[" + i + "]", DriftType.ADDED, null, actual.get(i)));
            }
        }
    }

    private int findMatch(Object desiredVal, List<Object> actual, boolean[] matched, String keyField) {
        if (desiredVal instanceof Map && keyField != null) {
            Object desiredKey = ((Map<?, ?>) desiredVal).get(keyField);
            for (int i = 0; i < actual.size(); i++) {
                if (!matched[i] && actual.get(i) instanceof Map) {
                    Object actualKey = ((Map<?, ?>) actual.get(i)).get(keyField);
                    if (equals(desiredKey, actualKey)) return i;
                }
            }
        }
        for (int i = 0; i < actual.size(); i++) {
            if (!matched[i] && deepEquals(desiredVal, actual.get(i))) return i;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private boolean deepEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> mapA = (Map<String, Object>) a;
            Map<String, Object> mapB = (Map<String, Object>) b;
            if (!mapA.keySet().equals(mapB.keySet())) return false;
            for (String key : mapA.keySet()) {
                if (!deepEquals(mapA.get(key), mapB.get(key))) return false;
            }
            return true;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> listA = (List<Object>) a;
            List<Object> listB = (List<Object>) b;
            if (listA.size() != listB.size()) return false;
            boolean[] used = new boolean[listB.size()];
            for (Object itemA : listA) {
                boolean found = false;
                for (int i = 0; i < listB.size(); i++) {
                    if (!used[i] && deepEquals(itemA, listB.get(i))) {
                        used[i] = true;
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        return a.equals(b);
    }

    private String findKeyField(List<Object> desired, List<Object> actual) {
        if (desired.isEmpty() || !(desired.get(0) instanceof Map)) return null;
        for (String candidate : List.of("name", "id", "key")) {
            if (allMapsHaveField(desired, candidate) && allMapsHaveField(actual, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean allMapsHaveField(List<Object> list, String field) {
        for (Object item : list) {
            if (!(item instanceof Map) || !((Map<?, ?>) item).containsKey(field)) return false;
        }
        return true;
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
