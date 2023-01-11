package io.mtso.jsonschema;

import org.gradle.api.Action;
import org.gradle.api.tasks.Nested;

public abstract class JsonschemaExtension {
  @Nested
  public abstract PrepareExtension getPrepare();

  public void prepare(Action<? super PrepareExtension> action) {
    action.execute(getPrepare());
  }
}
