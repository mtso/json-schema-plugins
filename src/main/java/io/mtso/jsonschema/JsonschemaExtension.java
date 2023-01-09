package io.mtso.jsonschema;

import org.gradle.api.Action;
import org.gradle.api.tasks.Nested;

public abstract class JsonschemaExtension {
  @Nested
  public abstract PrepareExtension getGenerate();

  public void prepare(Action<? super PrepareExtension> action) {
    action.execute(getGenerate());
  }
}
