package io.mtso.jsonschema;

import org.gradle.api.file.DirectoryProperty;

public interface ExpanderPluginExtension {
  DirectoryProperty getFrom();

  DirectoryProperty getInto();
}
