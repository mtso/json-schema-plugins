package io.mtso.jsonschema;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;

public class PrepareSchemasPlugin implements Plugin<Project> {
  public static final String JSONSCHEMA_EXTENSION_NAME = "jsonschema";
  public static final String PREPARE_SCHEMAS_TASK_NAME = "prepareSchemas";

  @Override
  public void apply(final Project project) {
    final JsonschemaExtension extension =
        project.getExtensions().create(JSONSCHEMA_EXTENSION_NAME, JsonschemaExtension.class);

    project
        .task(PREPARE_SCHEMAS_TASK_NAME)
        .doFirst(
            task -> {
              final FileTree tree = extension.getGenerate().getFrom().getAsFileTree();
              if (tree.isEmpty()) {
                throw new InvalidUserDataException(
                    "'from' directory is empty: " + extension.getGenerate().getFrom().getAsFile());
              }

              final File intoFile = extension.getGenerate().getInto().get().getAsFile();
              project.mkdir(intoFile);

              final Dereferencer dereferencer =
                  new Dereferencer(
                      extension.getGenerate().getFrom().get().getAsFile(),
                      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4),
                      task);

              tree.visit(
                  new SchemaDirectoryVisitor(
                      dereferencer, task, intoFile, extension.getGenerate()));
            });
  }
}
