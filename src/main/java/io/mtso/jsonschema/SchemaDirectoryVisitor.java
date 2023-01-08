package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

public class SchemaDirectoryVisitor implements FileVisitor {
  Dereferencer dereferencer;
  Task task;
  File intoFile;
  Project project;

  public SchemaDirectoryVisitor(
      Dereferencer dereferencer, Task task, File intoFile, Project project) {
    this.dereferencer = dereferencer;
    this.task = task;
    this.intoFile = intoFile;
    this.project = project;
  }

  @Override
  public void visitDir(FileVisitDetails fileVisitDetails) {}

  @Override
  public void visitFile(FileVisitDetails fileVisitDetails) {
    if (!fileVisitDetails.getName().endsWith(".json")) {
      return;
    }
    if (fileVisitDetails.getFile().getPath().contains("endpoints/shared/")) {
      return;
    }

    String data;
    try {
      data = new String(Files.readAllBytes(fileVisitDetails.getFile().toPath()));
    } catch (final IOException e) {
      throw new GradleScriptException("Failed to read data", e); // + e.getMessage());
    }

    JsonNode schemaNode;

    if (fileVisitDetails.getFile().getPath().endsWith("schema.json")) {

      task.getLogger()
          .lifecycle(String.format("expanding schema %s", fileVisitDetails.getFile().toPath()));

      JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(data);
      schemaNode = schema.getSchemaNode();
      try {
        dereferencer.schemas = new HashMap<>();
        schemaNode = dereferencer.dereference2(schemaNode);
      } catch (final IOException e) {
        throw new GradleScriptException("Failed to dereference", e);
      }
    } else { // if (fileVisitDetails.getFile().getPath().endsWith(".json")) {

      task.getLogger()
          .lifecycle(String.format("copying file %s", fileVisitDetails.getFile().toPath()));

      try {
        schemaNode = new ObjectMapper().readTree(data);
      } catch (final IOException e) {
        throw new GradleScriptException("failed to read json file", e);
      }
      //    } else {
      // write as regular file
    }

    try {
      //      new ObjectMapper().writer .writerWithDefaultPrettyPrinter().write
      //      String result =
      //          new
      // ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schemaNode);
      byte[] b = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(schemaNode);
      Path path =
          intoFile.toPath().resolve(Paths.get(fileVisitDetails.getRelativePath().getPathString()));

      //            task.getLogger().lifecycle(String.format("parent %s", path.getParent()));
      //            task.getLogger().lifecycle(String.format("writing %s", path));

      project.mkdir(path.getParent());
      Files.write(path, b, StandardOpenOption.CREATE);
    } catch (final IOException e) {
      throw new GradleScriptException("Failed to write file", e);
    }
    //        task.getLogger().lifecycle(String.format("visited %s", schemaNode));

  }
}
