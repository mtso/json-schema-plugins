package io.mtso.jsonschema;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;

import org.gradle.api.GradleScriptException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;

public class ExpanderPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {

    final ExpanderPluginExtension extension =
        project.getExtensions().create("expander", ExpanderPluginExtension.class);

    project
        .task("hello")
        .doLast(
            task -> {
              task.getLogger().lifecycle("io.mtso.expander");
              FileTree tree = extension.getFrom().get().getAsFileTree();
              if (tree.isEmpty()) {
                  throw new InvalidUserDataException("'from' directory is empty: " + extension.getFrom().getAsFile());
              }

//              task.getLogger().lifecycle(String.format("what? %s", extension.getFrom().get()));
//              extension.getFrom().get().getAsFile();
              File intoFile = extension.getInto().get().getAsFile();
              project.mkdir(intoFile);

              final Dereferencer dereferencer =
                  new Dereferencer(
                      extension.getFrom().get().getAsFile(),
                      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4),
                      task);


              tree.visit(new SchemaDirectoryVisitor(dereferencer, task, intoFile, project));
            });
  }
}
