package io.mtso.jsonschema;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class PrepareSchemasTask extends DefaultTask {
  @Nested
  public abstract PrepareExtension getPrepare();

  @TaskAction
  public void prepareSchemas() {
    final FileTree tree = this.getPrepare().getFrom().getAsFileTree();
    if (tree.isEmpty()) {
      throw new InvalidUserDataException(
          "'from' directory is empty: " + this.getPrepare().getFrom().getAsFile());
    }

    final File intoFile = this.getPrepare().getInto().get().getAsFile();
    this.getProject().mkdir(intoFile);

    final Dereferencer dereferencer =
        new Dereferencer(
            this.getPrepare().getFrom().get().getAsFile(),
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4),
            this);

    tree.visit(new SchemaDirectoryVisitor(dereferencer, this, intoFile, this.getPrepare()));

    new ExampleValidator(this.getPrepare().getValidate(), this)
        .validateExamples(this.getPrepare().getInto().get().getAsFile());
  }
}
