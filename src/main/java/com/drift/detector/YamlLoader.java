package com.drift.detector;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YamlLoader {

    public Map<String, Object> load(String filePath) throws IOException {
        try (InputStream is = Files.newInputStream(Path.of(filePath))) {
            return new Yaml().load(is);
        }
    }
}