package io.mtso.jsonschema;

import org.gradle.api.Action;
import org.gradle.api.tasks.Nested;

public abstract class JsonschemasExtension {
  @Nested
  public abstract GenerateExtension getGenerate();

  public void generate(Action<? super GenerateExtension> action) {
    action.execute(getGenerate());
  }
}
