package io.mtso.jsonschema;

import java.util.LinkedList;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public abstract class ValidateExtension {

  public abstract Property<String> getExamplePattern();

  public abstract Property<String> getSchemaFileExtension();

  private final List<String> includes = new LinkedList<>();
  private final List<String> excludes = new LinkedList<>();

  public ValidateExtension(Project project) {}

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
