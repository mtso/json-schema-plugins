package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Task;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

public class SchemaDirectoryVisitor implements FileVisitor {
  private final Dereferencer dereferencer;
  private final Task task;
  private final File intoFile;
  private final GenerateExtension extension;

  public SchemaDirectoryVisitor(
      final Dereferencer dereferencer,
      final Task task,
      final File intoFile,
      final GenerateExtension extension) {
    this.dereferencer = dereferencer;
    this.task = task;
    this.intoFile = intoFile;
    this.extension = extension;
  }

  @Override
  public void visitDir(final FileVisitDetails fileVisitDetails) {}

  @Override
  public void visitFile(final FileVisitDetails fileVisitDetails) {
    if (!fileVisitDetails.getName().endsWith(".json")) {
      return;
    }

    final boolean anyExcluded =
        extension.getExcludes().stream()
            .anyMatch(
                pattern ->
                    FileSystems.getDefault()
                        .getPathMatcher("glob:" + pattern)
                        .matches(fileVisitDetails.getFile().toPath()));

    if (anyExcluded) {
      return;
    }

    final String data;
    try {
      data = new String(Files.readAllBytes(fileVisitDetails.getFile().toPath()));
    } catch (final IOException e) {
      throw new GradleScriptException("Failed to read data", e);
    }

    final JsonNode schemaNode;

    if (fileVisitDetails.getFile().getPath().endsWith("schema.json")) {
      task.getLogger()
          .lifecycle(String.format("expanding schema %s", fileVisitDetails.getFile().toPath()));

      final JsonSchema schema =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(data);

      try {
        schemaNode = dereferencer.dereference(schema.getSchemaNode());
      } catch (final IOException e) {
        throw new GradleScriptException("Failed to dereference", e);
      }

    } else {
      task.getLogger()
          .lifecycle(String.format("copying file %s", fileVisitDetails.getFile().toPath()));

      try {
        schemaNode = new ObjectMapper().readTree(data);
      } catch (final IOException e) {
        throw new GradleScriptException("failed to read json file", e);
      }
    }

    try {
      final byte[] b =
          new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(schemaNode);
      final Path path =
          intoFile.toPath().resolve(Paths.get(fileVisitDetails.getRelativePath().getPathString()));

      task.getProject().mkdir(path.getParent());
      Files.write(path, b, StandardOpenOption.CREATE);
    } catch (final IOException e) {
      throw new GradleScriptException("Failed to write file", e);
    }
  }
}
