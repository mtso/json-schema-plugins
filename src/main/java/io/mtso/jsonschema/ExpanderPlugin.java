package io.mtso.jsonschema;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.RefValidator;
import com.networknt.schema.SpecVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.archive.compression.URIBuilder;
import org.gradle.api.provider.Property;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

public class ExpanderPlugin implements Plugin<Project> {
    interface ExpanderPluginExtension {

        DirectoryProperty getFrom();

//        Property<Directory> getFrom();
//        CopySpec getFrom();
//        Property<String> getFrom();
//        Property<String> getInto();
        DirectoryProperty getInto();
    }

    @Override
    public void apply(Project project) {


        ExpanderPluginExtension extension =
            project.getExtensions().create("expander", ExpanderPluginExtension.class);

        project.task("hello").doLast(task -> {


            try {
                URI uri = new URI("file:./schema/foo.json#/bar");

                task.getLogger().lifecycle(String.format("scheme: %s", uri.getScheme()));
                task.getLogger().lifecycle(String.format("frag: %s", uri.getFragment()));
                String result = "file:./schema/foo.json#/bar".replace("#" + uri.getRawFragment(), "");
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

            final Dereferencer dereferencer = new Dereferencer(
                extension.getFrom().get().getAsFile(),
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4), task);

            tree.visit(new SchemaDirectoryVisitor(dereferencer, task, intoFile, project));

//            tree.visit(new FileVisitor() {
//                @Override
//                public void visitDir(FileVisitDetails fileVisitDetails) {
//////                    task.getLogger().lifecycle(String.format("visitDir %s", fileVisitDetails.getFile().getName()));
////
////                    FileInputStream fis;
////                    try {
////                        FileInputStream fis = new FileInputStream(fileVisitDetails.getFile());
////                        String data = new String(fis.readAllBytes(), Charset.defaultCharset());
////                        task.getLogger().lifecycle("visit");
////                    } catch (final IOException e) {
////                        throw new RuntimeException(e);
////                    }
//
//
//
////                     fileVisitDetails.getFile()
////
////
////                    getClass().getResourceAsStream()
//                    task.getLogger().lifecycle(String.format("visitDir %s",
//                        fileVisitDetails.getRelativePath()
////                        fileVisitDetails.getFile().getParent(),
////                        fileVisitDetails.getFile().getName()
//                    ));
////                    task.getLogger().lifecycle(String.format("visitDir %s", fileVisitDetails.getFile().getPath()));
//                }
//
//                @Override
//                public void visitFile(FileVisitDetails fileVisitDetails) {
//
//                    String data;
//                    try {
////                        FileInputStream fis = new FileInputStream(fileVisitDetails.getFile());
//                        data = new String(Files.readAllBytes(fileVisitDetails.getFile().toPath()));
////                        Files.readAllBytes(fileVisitDetails.getFile().toPath());
//                        task.getLogger().lifecycle("visit");
//                    } catch (final IOException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
//                            .getSchema(data);
//
//                    schema.initializeValidators();
//                    schema.getRefSchemaNode("#/definitions");
//
//                    task.getLogger().lifecycle(String.format("visitFile %s %s",
//                        fileVisitDetails.getRelativePath(), schema
//
////                        fileVisitDetails.getFile().getParent(),
////                        fileVisitDetails.getFile().getName()
//                    ));
//                }
//            });
        });
    }
}
