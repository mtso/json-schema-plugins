package io.mtso.jsonschema;

import java.util.LinkedList;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Nested;

public abstract class PrepareExtension {

  private final List<String> excludes = new LinkedList<>();

  abstract DirectoryProperty getFrom();

  abstract DirectoryProperty getInto();

  private JsonschemaExtension jsonschemasExtension;

  public PrepareExtension(Project project) {}

  public void from(Directory from) {
    getFrom().set(from);
  }

  public void from(Directory from, Action<PrepareExtension> closure) {
    getFrom().set(from);
    closure.execute(this);
  }

  public void into(Directory into) {
    getInto().set(into);
  }

  public void exclude(String pattern) {
    this.excludes.add(pattern);
  }

  public List<String> getExcludes() {
    return excludes;
  }

  @Nested
  public abstract ValidateExtension getValidate();

  public void validate(Action<? super ValidateExtension> action) {
    action.execute(getValidate());
  }
}
