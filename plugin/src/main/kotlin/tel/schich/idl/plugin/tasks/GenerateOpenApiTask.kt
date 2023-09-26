package tel.schich.idl.plugin.tasks

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import tel.schich.idl.generator.openapi.OpenApiGenerator

@CacheableTask
abstract class GenerateOpenApiTask : IdlGenerateTask() {
    init {
        output.convention(project.layout.buildDirectory.map { it.dir("generated-openapi") })
    }

    @TaskAction
    private fun generate() {
        generate(OpenApiGenerator())
    }
}