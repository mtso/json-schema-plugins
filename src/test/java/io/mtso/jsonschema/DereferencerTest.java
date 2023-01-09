package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class DereferencerTest {
  @Test
  public void testDereference() throws IOException {
    Dereferencer dereferencer =
        new Dereferencer(null, JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4), null);
    final ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
    schemaNode.put("type", "string");
    dereferencer.dereference(schemaNode);
  }
}
