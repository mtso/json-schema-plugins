package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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

  private JsonSchema getJsonSchema(String path) throws FileNotFoundException {
    if (schemas.containsKey(path)) {
      return schemas.get(path);
    }

    // find file
    String filepath;
    if (path.startsWith("/")) {
      filepath = path;
    } else {
      filepath = rootDirectory.getAbsolutePath() + "/" + path;
    }
    File file = new File(filepath);
//    task.getLogger().lifecycle(String.format("getJsonSchema %s", file));
    JsonSchema schema = jsonSchemaFactory.getSchema(new FileInputStream(file));
    schemas.put(path, schema);
    return schema;
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

  public JsonNode getJsonSchemaNode(JsonNode root, Ref ref) throws IOException {
    if (ref.isLocalRef()) {
      return digRef(root, ref.getFragment());
//      return jsonSchemaFactory.getSchema(root).getRefSchemaNode(ref.getFragment());

    } else if (ref.getPath() != null && !ref.getPath().contains("schema")) {
      String filepath;
      if (ref.getPath().startsWith("/")) {
        filepath = ref.getPath();
      } else {
        filepath = rootDirectory.getAbsolutePath() + "/" + ref.getPath();
      }
      File file = new File(filepath);
      FileInputStream fis = new FileInputStream(file);
      JsonNode dataNode = new ObjectMapper().readTree(fis);
      if (null != ref.getFragment()) {
        JsonNode refNode = digRef(dataNode, ref.getFragment());
        RefWalker rw = new RefWalker(this::replaceRef, dataNode);
        rw.walk(refNode);
        JsonNode newRoot = rw.getJsonNode();
        return digRef(newRoot, ref.getFragment());
      } else {
        RefWalker rw = new RefWalker(this::replaceRef, dataNode);
        rw.walk();
        return rw.getJsonNode();
//        return dataNode;
      }
    } else {
      final JsonSchema schema = getJsonSchema(ref.getPath());

      if (null == ref.getFragment()) {
        JsonNode schemaNode = schema.getSchemaNode();
        dereferenceInner(schemaNode, schemaNode);
        return schemaNode;
      } else {
        JsonNode schemaNode = schema.getSchemaNode();
        JsonNode refNode = digRef(schemaNode, ref.getFragment());
        dereferenceInner(schemaNode, refNode);
        return refNode;
      }
    }
  }

  public void dereferenceInner(final JsonNode root, final JsonNode schema) throws IOException {
    for (final JsonNode node : schema.findParents("$ref")) {
      if (node.isObject()) {
        final ObjectNode parent = (ObjectNode) node;

        final String refString = parent.path("$ref").textValue();
        if (refString == null) {
//          parent.remove("$ref");
          continue;
//          task.getLogger().lifecycle(String.format("why null: %s", node.path("$ref")));
        }
        final Ref ref = new Ref(refString);

        final JsonNode schemaNode = getJsonSchemaNode(root, ref);

        if (schemaNode != null && schemaNode.isObject()) {
          final ObjectNode schemaNodeObj = (ObjectNode) schemaNode;
          dereferenceInner(root, schemaNodeObj);

          parent.remove("$ref");
          final Iterator<String> names = schemaNodeObj.fieldNames();
          while (names.hasNext()) {
            final String name = names.next();
            parent.set(name, schemaNodeObj.get(name));
          }
        }
      }
    }
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

  public void dereference(final JsonNode root) throws IOException {
    for (final JsonNode node : root.findParents("$ref")) {
      if (node.isObject()) {
        final ObjectNode parent = (ObjectNode) node;

        final String refString = parent.path("$ref").textValue();
        if (refString == null) {
//          parent.remove("$ref");
          continue;
//          task.getLogger().lifecycle(String.format("why null: %s", node.path("$ref")));
        }
        final Ref ref = new Ref(refString);

        final JsonNode schemaNode = getJsonSchemaNode(root, ref);

        if (schemaNode != null && schemaNode.isObject()) {
          final ObjectNode schemaNodeObj = (ObjectNode) schemaNode;
          dereferenceInner(root, schemaNodeObj);

          parent.remove("$ref");
          final Iterator<String> names = schemaNodeObj.fieldNames();
          while (names.hasNext()) {
            final String name = names.next();
            parent.set(name, schemaNodeObj.get(name));
          }
        }
      }
    }
  }
}
