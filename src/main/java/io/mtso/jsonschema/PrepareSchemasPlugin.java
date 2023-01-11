package io.mtso.jsonschema;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class PrepareSchemasPlugin implements Plugin<Project> {
  public static final String JSONSCHEMA_EXTENSION_NAME = "jsonschema";
  public static final String PREPARE_SCHEMAS_TASK_NAME = "prepareSchemas";

  @Override
  public void apply(final Project project) {
    final JsonschemaExtension extension =
        project.getExtensions().create(JSONSCHEMA_EXTENSION_NAME, JsonschemaExtension.class);

    final TaskProvider<PrepareSchemasTask> task =
        project.getTasks().register(PREPARE_SCHEMAS_TASK_NAME, PrepareSchemasTask.class);

    project.afterEvaluate(
        (p) -> {
          task.configure(
              (t) -> {
                t.getPrepare().getFrom().set(extension.getPrepare().getFrom());
                t.getPrepare().getInto().set(extension.getPrepare().getInto());
                t.getPrepare().getExcludes().addAll(extension.getPrepare().getExcludes());

                t.getPrepare()
                    .getValidate()
                    .getExamplePattern()
                    .set(extension.getPrepare().getValidate().getExamplePattern());
                t.getPrepare()
                    .getValidate()
                    .getSchemaFileExtension()
                    .set(extension.getPrepare().getValidate().getSchemaFileExtension());
                t.getPrepare()
                    .getValidate()
                    .getIncludes()
                    .addAll(extension.getPrepare().getValidate().getIncludes());
                t.getPrepare()
                    .getValidate()
                    .getExcludes()
                    .addAll(extension.getPrepare().getValidate().getExcludes());
              });
        });
  }
}
