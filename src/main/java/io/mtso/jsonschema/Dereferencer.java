package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import org.gradle.api.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        JsonSchema schema = jsonSchemaFactory.getSchema(new FileInputStream(file));
        schemas.put(path, schema);
        return schema;
    }

    public void dereference(JsonNode schema) throws IOException {
        for (JsonNode node : schema.findParents("$ref")) {
            if (node.isObject()) {
                ObjectNode parent = (ObjectNode) node;

                final String refString = node.path("$ref").textValue();
                final Ref ref = new Ref(refString);

                if (ref.isLocalRef()) {
                    JsonNode schemaNode = jsonSchemaFactory.getSchema(schema).getRefSchemaNode(ref.getFragment());
                    if (schemaNode.isObject()) {

                        ObjectNode schemaNodeObj = (ObjectNode) schemaNode;

                        dereference(schemaNodeObj);

                        parent.remove("$ref");

                        Iterator<String> names = schemaNodeObj.fieldNames();
                        while (names.hasNext()) {
                            String name = names.next();
                            parent.set(name, schemaNodeObj.get(name));
                        }
                    }
                    continue;
                }

                final JsonSchema schema1 = getJsonSchema(ref.getPath());

//                task.getLogger().lifecycle(String.format("getting root=%s schema=%s fragment=%s", schema, schema1, ref.getFragment()));
                JsonNode schemaNode;
                if (null == ref.getFragment()) {
                    schemaNode = schema1.getSchemaNode();
                } else {
                    schemaNode = schema1.getRefSchemaNode(ref.getFragment());
                }

                if (schemaNode.isObject()) {

                    ObjectNode schemaNodeObj = (ObjectNode) schemaNode;

                    dereference(schemaNodeObj);

                    parent.remove("$ref");

                    Iterator<String> names = schemaNodeObj.fieldNames();
                    while (names.hasNext()) {
                        String name = names.next();
                        parent.set(name, schemaNodeObj.get(name));
                    }
                }
            }

        }
    }
}
