package io.mtso.jsonschema;

import java.util.LinkedList;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;

public abstract class GenerateExtension {
  abstract DirectoryProperty getFrom();

  private List<String> excludes = new LinkedList<>();

  abstract DirectoryProperty getInto();

  private JsonschemasExtension jsonschemasExtension;

  public GenerateExtension(Project project) {}

  public void from(Directory from) {
    getFrom().set(from);
  }

  public void from(Directory from, Action<GenerateExtension> closure) {
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
}
