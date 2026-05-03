# Drift Infrastructure Detector

A Java CLI tool that detects configuration drift by comparing a **desired state** (YAML) against an **actual state** (YAML) and reports the differences.

## Why YAML?

- Supports comments — annotate *why* a value is expected
- Human-readable for editing infrastructure configs
- Superset of JSON — accepts JSON files as-is

## Usage

```bash
java -jar drift-detector.jar --desired desired.yaml --actual actual.yaml
```

### Example

**desired.yaml**
```yaml
# Production database config
database:
  engine: postgres
  version: "15.4"
  storage_gb: 100
  multi_az: true
  backup_retention_days: 7

security_groups:
  - name: web-sg
    ingress:
      - port: 443
        cidr: "0.0.0.0/0"
```

**actual.yaml**
```yaml
database:
  engine: postgres
  version: "15.2"
  storage_gb: 50
  multi_az: false
  backup_retention_days: 7

security_groups:
  - name: web-sg
    ingress:
      - port: 443
        cidr: "0.0.0.0/0"
      - port: 22
        cidr: "0.0.0.0/0"
```

**Output**
```
DRIFT DETECTED — 3 difference(s) found

  MODIFIED  database.version              expected: "15.4"    actual: "15.2"
  MODIFIED  database.storage_gb           expected: 100       actual: 50
  MODIFIED  database.multi_az             expected: true      actual: false
  ADDED     security_groups[0].ingress[1] unexpected entry in actual state
```

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                      CLI (Main)                      │
│         Parses args, orchestrates the flow           │
└──────────────┬───────────────────────┬───────────────┘
               │                       │
               ▼                       ▼
┌──────────────────────┐  ┌──────────────────────────┐
│     YamlLoader       │  │     DriftReporter        │
│                      │  │                          │
│ Reads & parses YAML  │  │ Formats drift results    │
│ into a unified tree  │  │ for console output       │
│ (Map<String, Object>)│  │                          │
└──────────┬───────────┘  └──────────▲───────────────┘
           │                         │
           ▼                         │
┌──────────────────────────────────────────────────────┐
│                  DriftDetector                        │
│                                                      │
│ Recursively compares desired vs actual trees.        │
│ Produces a List<DriftResult> with:                   │
│   - path      (dotted notation, e.g. "db.version")  │
│   - type      (ADDED, REMOVED, MODIFIED)             │
│   - expected  (value from desired state)             │
│   - actual    (value from actual state)              │
└──────────────────────────────────────────────────────┘
```

### Core Classes

| Class | Responsibility |
|-------|---------------|
| `Main` | CLI entry point. Parses `--desired` and `--actual` args, wires components together. |
| `YamlLoader` | Loads a YAML file into `Map<String, Object>` using SnakeYAML. |
| `DriftDetector` | Deep recursive comparison of two maps. Handles nested maps, lists, and primitives. |
| `DriftResult` | Value object: `path`, `type` (enum: `ADDED`, `REMOVED`, `MODIFIED`), `expected`, `actual`. |
| `DriftReporter` | Takes `List<DriftResult>` and prints a formatted console report. |

### Comparison Rules

| Scenario | Behavior |
|----------|----------|
| Key in desired, missing in actual | `REMOVED` drift |
| Key in actual, missing in desired | `ADDED` drift |
| Both present, values differ | `MODIFIED` drift |
| Both present, values equal | No drift |
| Nested maps | Recurse, building dotted path |
| Lists | Index-based comparison (order-sensitive) |
| Actual list longer than desired | Each extra element → `ADDED` |
| Desired list longer than actual | Each missing element → `REMOVED` |

## Project Structure

```
drift-infrastructure-detector/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/drift/detector/
    │   ├── Main.java
    │   ├── YamlLoader.java
    │   ├── DriftDetector.java
    │   ├── DriftResult.java
    │   ├── DriftType.java          # enum: ADDED, REMOVED, MODIFIED
    │   └── DriftReporter.java
    └── test/java/com/drift/detector/
        ├── DriftDetectorTest.java
        └── YamlLoaderTest.java
```

## Dependencies

| Dependency | Purpose |
|------------|---------|
| [SnakeYAML](https://github.com/snakeyaml/snakeyaml) `2.2` | YAML parsing |
| [JUnit 5](https://junit.org/junit5/) `5.10.x` | Testing |

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/drift-detector-1.0.0.jar --desired desired.yaml --actual actual.yaml

# Run tests
mvn test
```

## Suggested Build Order

1. **`DriftType`** — Simple enum, no dependencies
2. **`DriftResult`** — Value object, depends only on `DriftType`
3. **`YamlLoader`** + **`YamlLoaderTest`** — File I/O, test with sample YAML files
4. **`DriftDetector`** + **`DriftDetectorTest`** — Core logic, test with nested maps/lists
5. **`DriftReporter`** — Formatting, test manually with sample `DriftResult` lists
6. **`Main`** — Wire everything together, add arg parsing

## Future Enhancements

- [ ] JSON input support (auto-detect by file extension)
- [ ] `--output json` flag for machine-readable output
- [ ] `--ignore-paths` flag to exclude specific paths from comparison
- [ ] Exit code: `0` = no drift, `1` = drift detected (for CI/CD pipelines)
- [ ] Key-based array matching (match list items by a field like `name` instead of index)
- [ ] Severity levels (critical vs warning drift)
