# JSON Schema Plugin

## Install

```groovy
plugins {
    id 'io.mtso.jsonschema.prepare' version '1.1.0'
}
```

See: https://plugins.gradle.org/plugin/io.mtso.jsonschema.prepare

## Task: `prepareSchemas`

De-references JSON schemas in the 'from' directory. And copies the de-referenced schemas to the 'into' directory.

### `build.gradle` Configuration

```groovy
jsonschema {
    prepare {
        from = layout.projectDirectory.dir("schemas/endpoints")
        into = layout.buildDirectory.dir("expanded-schemas")
        exclude "**/shared/**"

        validate {
            include "**schema.json"
            include "**example.*.json"
            exclude "**openapi.schema.json"

            // Optional, defaults to value below.
            examplePattern = '^(.*\\.)example\\.[a-zA-Z_\\d]+(\\.json)$'
            schemaFileExtension = 'schema.json'
        }
    }
}

compileJava {
    dependsOn prepareSchemas
}
```

## Development

Set version in `build.gradle` to `9999-SNAPSHOT`. Then run: `./gradlew publish`
to publish the plugin to the local plugin repository.

In the local using project, add this to `build.gradle`:
```groovy
buildscript {
    repositories {
        maven {
            name = 'localPluginRepository'
            url System.properties['user.home'] + '/dev/repo/local-plugin-repository'
        }
    }
    dependencies {
        classpath "io.mtso:jsonschema:9999-SNAPSHOT"
    }
}

apply plugin: 'io.mtso.jsonschema.prepare'
```
