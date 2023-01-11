package io.mtso.jsonschema;

import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;

public abstract class PrepareExtension {
  @Internal private final List<String> excludes = new LinkedList<>();

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  abstract DirectoryProperty getFrom();

  @OutputDirectory
  abstract DirectoryProperty getInto();

  @Inject
  public PrepareExtension() {}

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
