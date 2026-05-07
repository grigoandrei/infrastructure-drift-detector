package com.drift.detector;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DriftDetectorTest {

    private final DriftDetector detector = new DriftDetector();

    @Test
    void reorderedListOfMapsWithKeyField_noDrift() {
        Map<String, Object> desired = Map.of("security_groups", List.of(
                Map.of("name", "web-sg", "port", 443),
                Map.of("name", "db-sg", "port", 5432)
        ));
        Map<String, Object> actual = Map.of("security_groups", List.of(
                Map.of("name", "db-sg", "port", 5432),
                Map.of("name", "web-sg", "port", 443)
        ));

        List<DriftResult> results = detector.detect(desired, actual);
        assertTrue(results.isEmpty(), "Reordered list with same content should produce no drift");
    }

    @Test
    void reorderedListOfPrimitives_noDrift() {
        Map<String, Object> desired = Map.of("ports", List.of(80, 443, 22));
        Map<String, Object> actual = Map.of("ports", List.of(443, 22, 80));

        List<DriftResult> results = detector.detect(desired, actual);
        assertTrue(results.isEmpty(), "Reordered primitive list should produce no drift");
    }

    @Test
    void missingElementInActual_reportedAsRemoved() {
        Map<String, Object> desired = Map.of("items", List.of(
                Map.of("name", "a"), Map.of("name", "b")
        ));
        Map<String, Object> actual = Map.of("items", List.of(
                Map.of("name", "a")
        ));

        List<DriftResult> results = detector.detect(desired, actual);
        assertEquals(1, results.size());
        assertEquals(DriftType.REMOVED, results.get(0).type());
    }

    @Test
    void extraElementInActual_reportedAsAdded() {
        Map<String, Object> desired = Map.of("items", List.of(
                Map.of("name", "a")
        ));
        Map<String, Object> actual = Map.of("items", List.of(
                Map.of("name", "a"), Map.of("name", "b")
        ));

        List<DriftResult> results = detector.detect(desired, actual);
        assertEquals(1, results.size());
        assertEquals(DriftType.ADDED, results.get(0).type());
    }

    @Test
    void matchedByKeyButValueDiffers_reportedAsModified() {
        Map<String, Object> desired = Map.of("items", List.of(
                Map.of("name", "web-sg", "port", 443)
        ));
        Map<String, Object> actual = Map.of("items", List.of(
                Map.of("name", "web-sg", "port", 8080)
        ));

        List<DriftResult> results = detector.detect(desired, actual);
        assertEquals(1, results.size());
        assertEquals(DriftType.MODIFIED, results.get(0).type());
        assertNotNull(results.get(0).path());
        assertTrue(results.get(0).path().contains("port"));
    }

    @Test
    void identicalLists_noDrift() {
        Map<String, Object> desired = Map.of("tags", List.of("prod", "us-east-1"));
        Map<String, Object> actual = Map.of("tags", List.of("prod", "us-east-1"));

        List<DriftResult> results = detector.detect(desired, actual);
        assertTrue(results.isEmpty());
    }

    @Test
    void emptyLists_noDrift() {
        Map<String, Object> desired = Map.of("items", List.of());
        Map<String, Object> actual = Map.of("items", List.of());

        List<DriftResult> results = detector.detect(desired, actual);
        assertTrue(results.isEmpty());
    }

    @Test
    void nestedMapComparison_detectsModified() {
        Map<String, Object> desired = Map.of("db", Map.of("version", "15.4", "multi_az", true));
        Map<String, Object> actual = Map.of("db", Map.of("version", "15.2", "multi_az", true));

        List<DriftResult> results = detector.detect(desired, actual);
        assertEquals(1, results.size());
        assertEquals("db.version", results.get(0).path());
        assertEquals(DriftType.MODIFIED, results.get(0).type());
    }
}
