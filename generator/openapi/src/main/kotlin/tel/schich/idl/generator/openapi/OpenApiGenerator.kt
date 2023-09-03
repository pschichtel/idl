package tel.schich.idl.generator.openapi

import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.runner.command.JvmInProcessGenerator

class OpenApiGenerator : JvmInProcessGenerator {
    override fun generate(request: GenerationRequest): GenerationResult {
        return GenerationResult.Success(emptyList())
    }
}