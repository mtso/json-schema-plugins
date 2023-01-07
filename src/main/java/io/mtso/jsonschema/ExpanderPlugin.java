package io.mtso.jsonschema;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;

public class ExpanderPlugin implements Plugin<Project> {
  interface ExpanderPluginExtension {

    DirectoryProperty getFrom();

    DirectoryProperty getInto();
  }

  @Override
  public void apply(Project project) {

    ExpanderPluginExtension extension =
        project.getExtensions().create("expander", ExpanderPluginExtension.class);

    project.getExtensions().getByName("expander");

    project
        .task("hello")
        .doLast(
            task -> {
              try {
                URI uri = new URI("file:./schema/foo.json#/bar");

                task.getLogger().lifecycle(String.format("scheme: %s", uri.getScheme()));
                task.getLogger().lifecycle(String.format("frag: %s", uri.getFragment()));
                String result =
                    "file:./schema/foo.json#/bar".replace("#" + uri.getRawFragment(), "");
                task.getLogger().lifecycle(String.format("result: %s", result));

              } catch (URISyntaxException e) {

              }

              task.getLogger().lifecycle("io.mtso.jsonschema.ExpanderPlugin");
              task.getLogger().lifecycle(String.format("from: %s", extension.getFrom()));
              task.getLogger().lifecycle(String.format("into: %s", extension.getInto()));

              FileTree tree = extension.getFrom().get().getAsFileTree();
              extension.getFrom().get().getAsFile();
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
