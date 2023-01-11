package io.mtso.jsonschema;

import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

public abstract class ValidateExtension {

  @Internal
  public abstract Property<String> getExamplePattern();

  @Internal
  public abstract Property<String> getSchemaFileExtension();

  @Internal private final List<String> includes = new LinkedList<>();

  @Internal private final List<String> excludes = new LinkedList<>();

  @Inject
  public ValidateExtension() {}

  public void include(String pattern) {
    this.includes.add(pattern);
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void exclude(String pattern) {
    this.excludes.add(pattern);
  }

  public List<String> getExcludes() {
    return excludes;
  }
}
