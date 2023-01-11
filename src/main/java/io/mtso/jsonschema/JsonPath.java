package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public class JsonPath {
  public static JsonNode get(final JsonNode node, final String jsonPath) {
    if (Objects.isNull(node)) {
      return null;
    }
    if (Objects.isNull(jsonPath)) {
      return node;
    }

    JsonNode curr = node;
    final String[] parts = jsonPath.split("(?<!\\\\)\\.");
    for (final String part : parts) {
      if ("$".equals(part)) {
        continue;
      }

      if (part.matches("\\[\\d+]$")) {
        final String arrayKey = part.replaceAll("\\[\\d+\\]$", "");
        if (!"$".equals(arrayKey)) {
          curr = curr.path(arrayKey);
        }
        final String arrayIndexStr = part.replaceAll(".*\\[(\\d+)\\]$", "$1");
        int arrayIndex = Integer.parseInt(arrayIndexStr);
        curr = curr.path(arrayIndex);
      } else {
        curr = curr.path(part);
      }
    }
    return curr;
  }
}
