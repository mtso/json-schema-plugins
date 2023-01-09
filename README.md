# JSON Schema Plugin

## Install

```groovy
plugins {
    id 'io.mtso.jsonschema.prepare'
}
```

### Legacy Install

```groovy
buildscript {
    repositories {
        maven {
            name = 'localPluginRepository'
            url System.properties['user.home'] + '/dev/repo/local-plugin-repository'
        }
    }
    dependencies {
        classpath "io.mtso:jsonschema:1.0-SNAPSHOT"
    }
}

apply plugin: 'io.mtso.jsonschema.prepare'
```

## Task: `prepareSchemas`

De-references JSON schemas in the 'from' directory. And copies the de-referenced schemas to the 'into' directory.

```groovy
jsonschema {
    prepare {
        from = layout.projectDirectory.dir("schemas/endpoints")
        into = layout.buildDirectory.dir("expanded-schemas")
        exclude "**/shared/**"
    }
}

compileJava {
    dependsOn prepareSchemas
}
```
