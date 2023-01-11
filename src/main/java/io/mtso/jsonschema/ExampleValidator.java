package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.gradle.api.GradleScriptException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;

public class ExampleValidator {
  private static final String DEFAULT_EXAMPLE_PATH_PATTERN =
      "^(.*\\.)example\\.[a-zA-Z_\\d]+(\\.json)$";
  private static final String DEFAULT_SCHEMA_FILE_EXTENSION = "schema.json";

  private final ValidateExtension validateExtension;
  private final Task task;
  private final String examplePathPattern;
  private final String schemaFileExtension;

  public ExampleValidator(final ValidateExtension validateExtension, final Task task) {
    this.validateExtension =
        Objects.requireNonNull(validateExtension, "ValidateExtension required");
    this.task = Objects.requireNonNull(task, "Task required");
    this.examplePathPattern =
        validateExtension.getExamplePattern().getOrElse(DEFAULT_EXAMPLE_PATH_PATTERN);
    this.schemaFileExtension =
        validateExtension
            .getSchemaFileExtension()
            .getOrElse(DEFAULT_SCHEMA_FILE_EXTENSION)
            .replaceAll("^\\.", "");
  }

  public void validateExamples(@Nonnull final File directory) {
    final Predicate<Path> includes =
        (path) ->
            validateExtension.getIncludes().isEmpty()
                || validateExtension.getIncludes().stream()
                    .anyMatch(
                        glob ->
                            FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(path));

    final Predicate<Path> excludes =
        (path) ->
            validateExtension.getExcludes().stream()
                .noneMatch(
                    glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(path));

    try {
      final List<Path> files =
          Files.walk(Paths.get(directory.toURI()))
              .filter(Files::isRegularFile)
              .filter(file -> file.toString().endsWith(".json"))
              .filter(includes)
              .filter(excludes)
              .collect(Collectors.toList());

      buildSchemaToExamplesMap(files).forEach(this::validateJsonFilesOrDie);
    } catch (final IOException e) {
      throw new GradleScriptException("Failed to validate", e);
    }
  }

  static List<ObjectNode> formatReport(
      @Nonnull final Set<ValidationMessage> report, @Nonnull final JsonNode context) {
    return report.stream()
        .map(
            (vm) -> {
              final JsonNode value = JsonPath.get(context, vm.getPath());
              final ObjectNode obj = JsonNodeFactory.instance.objectNode();
              obj.set("value", value);
              obj.put("type", vm.getType());
              obj.put("code", vm.getCode());
              obj.put("path", vm.getPath());
              obj.put("schemaPath", vm.getSchemaPath());
              obj.set("arguments", JsonNodeFactory.instance.pojoNode(vm.getArguments()));
              obj.set("details", JsonNodeFactory.instance.pojoNode(vm.getDetails()));
              obj.put("message", vm.getMessage());
              return obj;
            })
        .collect(Collectors.toList());
  }

  private void validateJsonFilesOrDie(
      @Nonnull final Path schemaPath, @Nonnull final List<Path> examplePaths) {

    final JsonSchema jsonSchema;
    try {
      jsonSchema =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
              .getSchema(new FileInputStream(schemaPath.toFile()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to read schema file", e);
    }

    final Map<Path, JsonNode> exampleFilePathToJsonMap = new HashMap<>();

    examplePaths.forEach(
        examplePath -> {
          try {
            exampleFilePathToJsonMap.put(
                examplePath,
                new ObjectMapper().readTree(new FileInputStream(examplePath.toFile())));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    final Map<String, Exception> pathToExceptionMap = new HashMap<>();

    task.getLogger()
        .lifecycle(
            String.format(
                "Found %s example(s) for JSON schema file: %s",
                exampleFilePathToJsonMap.size(), schemaPath));

    exampleFilePathToJsonMap.forEach(
        (filePath, json) -> {
          try {
            task.getLogger().lifecycle("Validating example JSON file: " + filePath);
            final Set<ValidationMessage> report = jsonSchema.validate(json);
            if (!report.isEmpty()) {
              final List<ObjectNode> formattedReport = formatReport(report, json);
              final String result =
                  new ObjectMapper()
                      .writerWithDefaultPrettyPrinter()
                      .writeValueAsString(formattedReport);
              pathToExceptionMap.put(filePath.toString(), new InvalidUserDataException(result));
            }
          } catch (final Exception e) {
            pathToExceptionMap.put(filePath.toString(), e);
          }
        });

    pathToExceptionMap.forEach(
        (path, e) -> {
          task.getLogger().lifecycle("Invalid JSON file: " + path);
          task.getLogger().lifecycle(e.getMessage());
        });

    if (!pathToExceptionMap.isEmpty()) {
      throw new GradleScriptException("Failed to validate examples", new Exception());
    }
  }

  private Map<Path, List<Path>> buildSchemaToExamplesMap(@Nonnull final List<Path> jsonFiles) {

    final Map<Path, List<Path>> schemaToExamplesMap = new HashMap<>();
    // Collect all the schema files.
    jsonFiles.stream()
        .filter(path -> path.toString().endsWith(schemaFileExtension))
        .forEach(path -> schemaToExamplesMap.put(path, new LinkedList<>()));
    // Associate all the example files with their corresponding schema files.
    jsonFiles.stream()
        .filter(path -> path.toString().matches(examplePathPattern))
        .forEach(
            examplePath -> {
              final Path schemaPath = getSchemaPathForExamplePath(examplePath);

              if (schemaToExamplesMap.containsKey(schemaPath)) {
                schemaToExamplesMap.get(schemaPath).add(examplePath);
              } else {
                throw new RuntimeException(
                    "No schema file found for JSON example file: " + examplePath.toString());
              }
            });

    return schemaToExamplesMap;
  }

  private Path getSchemaPathForExamplePath(@Nonnull final Path examplePath) {

    final String examplePathPattern =
        validateExtension.getExamplePattern().getOrElse(DEFAULT_EXAMPLE_PATH_PATTERN);
    final Matcher matcher = Pattern.compile(examplePathPattern).matcher(examplePath.toString());

    if (matcher.matches()) {
      return Paths.get(matcher.group(1) + schemaFileExtension);
    } else {
      throw new RuntimeException("Invalid example file pattern: " + examplePath.toString());
    }
  }
}
