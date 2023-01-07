package io.mtso.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SchemaDirectoryVisitor implements FileVisitor {
    Dereferencer dereferencer;
    Task task;
    File intoFile;
    Project project;

    public SchemaDirectoryVisitor(Dereferencer dereferencer, Task task, File intoFile, Project project) {
        this.dereferencer = dereferencer;
        this.task = task;
        this.intoFile = intoFile;
        this.project = project;
    }

    @Override
    public void visitDir(FileVisitDetails fileVisitDetails) {

    }

    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        if (!fileVisitDetails.getName().endsWith("schema.json")) {
            return;
        }

        task.getLogger().lifecycle(String.format("expanding schema %s", fileVisitDetails.getFile().toPath()));
        String data;
        try {
            data = new String(Files.readAllBytes(fileVisitDetails.getFile().toPath()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
            .getSchema(data);
        JsonNode schemaNode = schema.getSchemaNode();
        try {
            dereferencer.dereference(schemaNode);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String result = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schemaNode);
            Path path = intoFile.toPath().resolve(Paths.get(fileVisitDetails.getRelativePath().getPathString()));

//            task.getLogger().lifecycle(String.format("parent %s", path.getParent()));
//            task.getLogger().lifecycle(String.format("writing %s", path));

            project.mkdir(path.getParent());
            Files.write(path, result.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
//        task.getLogger().lifecycle(String.format("visited %s", schemaNode));


    }
}
