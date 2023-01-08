package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;

public class RefWalker {
  @FunctionalInterface
  public interface RefReplacer {
    JsonNode replaceRef(Ref ref, JsonNode root) throws IOException;
  }

  RefReplacer replacer;
  JsonNode root;

  public RefWalker(RefReplacer replacer, JsonNode root) {
    this.replacer = replacer;
    this.root = root;
  }

  public static boolean isRefObject(JsonNode field) {
    if (null == field) {
      return false;
    }

    if (field.isObject()) {
      return field.has("$ref");
    } else {
      return false;
    }
  }

  public static String getRefString(JsonNode field) {

    if (null == field) {
      return null;
    }

    if (field.isObject()) {
      return field.path("$ref").textValue();
    } else {
      return null;
    }
  }

  public void walk(JsonNode current) throws IOException {
    if (null == current) {
      return;
    }

    if (current.isObject()) {
      ObjectNode obj = (ObjectNode) current;
      Iterator<String> names = obj.fieldNames();
      while (names.hasNext()) {
        String fieldName = names.next();
        JsonNode value = obj.get(fieldName);

        if (isRefObject(value)) {
          JsonNode schema = replacer.replaceRef(new Ref(getRefString(value)), root);
          obj.set(fieldName, schema);
        } else {
          walk(obj.get(fieldName));
        }
      }
    } else if (current.isArray()) {
      ArrayNode arr = (ArrayNode) current;

      for (int i = 0; i < arr.size(); i++) {
        JsonNode value = arr.get(i);

        if (isRefObject(value)) {
          JsonNode schema = replacer.replaceRef(new Ref(getRefString(value)), root);
          arr.set(i, schema);
        } else {
          walk(value);
        }
      }
    }
  }

  public JsonNode walkNested(JsonNode nested) throws IOException {
    if (isRefObject(nested)) {
      JsonNode schema = replacer.replaceRef(new Ref(getRefString(nested)), root);
      return schema;
    } else {
      walk(nested);
    }
    return nested;
  }

  public void walk() throws IOException {
    if (isRefObject(root)) {
      JsonNode schema = replacer.replaceRef(new Ref(getRefString(root)), root);
      root = schema;
    } else {
      walk(root);
    }
  }

  public JsonNode getJsonNode() {
    return root;
  }
}
