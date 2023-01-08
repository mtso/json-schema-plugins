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

  public JsonNode getJsonSchemaNode(JsonNode root, Ref ref) throws IOException {
    if (ref.isLocalRef()) {
      return jsonSchemaFactory.getSchema(root).getRefSchemaNode(ref.getFragment());

    } else if (ref.getPath() != null && !ref.getPath().contains("schema")) {
      String filepath;
      if (ref.getPath().startsWith("/")) {
        filepath = ref.getPath();
      } else {
        filepath = rootDirectory.getAbsolutePath() + "/" + ref.getPath();
      }
      File file = new File(filepath);
      FileInputStream fis = new FileInputStream(file);
      return new ObjectMapper().readTree(fis);
    } else {
      final JsonSchema schema = getJsonSchema(ref.getPath());

      if (null == ref.getFragment()) {
        return schema.getSchemaNode();
      } else {
        return schema.getRefSchemaNode(ref.getFragment());
      }
    }
  }

  public void dereference(final JsonNode schema) throws IOException {
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

        final JsonNode schemaNode = getJsonSchemaNode(schema, ref);

        if (schemaNode != null && schemaNode.isObject()) {
          final ObjectNode schemaNodeObj = (ObjectNode) schemaNode;
          dereference(schemaNodeObj);

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
