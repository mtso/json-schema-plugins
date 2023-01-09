# JSON Schema Plugin

## Install

```groovy
plugins {
    id 'io.mtso.jsonschema.prepare'
}
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
