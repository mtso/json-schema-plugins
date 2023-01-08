package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.gradle.api.Task;

public class Dereferencer {
  Map<String, JsonSchema> schemas = new HashMap<>();
  Map<String, JsonNode> jsonFiles = new HashMap<>();
  File rootDirectory;
  JsonSchemaFactory jsonSchemaFactory;
  Task task;

  public Dereferencer(File rootDirectory, JsonSchemaFactory jsonSchemaFactory, Task task) {
    this.rootDirectory = rootDirectory;
    this.jsonSchemaFactory = jsonSchemaFactory;
    this.task = task;
  }

  public static JsonNode digRef(JsonNode root, String ref) {
    JsonNode curr = root;
    String[] parts = ref.split("/");
    int partIdx = 0;
    while (partIdx < parts.length) {
      String part = parts[partIdx];
      partIdx++;

      if (!"#".equals(part)) {
        curr = curr.path(part);
      }
    }
    return curr;
  }

  public JsonNode replaceRef(final Ref ref, final JsonNode root) throws IOException {
    if (ref.isLocalRef()) {
      final JsonNode node = digRef(root, ref.getFragment());
      final RefWalker refWalker = new RefWalker(this::replaceRef, root);
      return refWalker.walkNested(node);

    } else {

      final String filepath;
      if (ref.getPath().startsWith("/")) {
        filepath = ref.getPath();
      } else {
        filepath = rootDirectory.getAbsolutePath() + "/" + ref.getPath();
      }

      final JsonNode derefedDataNode;

      if (jsonFiles.containsKey(filepath)) {
        derefedDataNode = jsonFiles.get(filepath);
      } else {
        final File file = new File(filepath);
        final FileInputStream fis = new FileInputStream(file);
        final JsonNode dataNode = new ObjectMapper().readTree(fis);
        jsonFiles.put(filepath, dataNode);

        final RefWalker refWalker = new RefWalker(this::replaceRef, dataNode);
        refWalker.walk();
        derefedDataNode = refWalker.getJsonNode();
        jsonFiles.put(filepath, derefedDataNode);
      }

      if (ref.getFragment() == null) {
        return derefedDataNode;
      } else {
        return digRef(derefedDataNode, ref.getFragment());
      }
    }
  }

  public JsonNode dereference2(final JsonNode root) throws IOException {
    RefWalker rw = new RefWalker(this::replaceRef, root);
    rw.walk();
    return rw.getJsonNode();
  }
}
