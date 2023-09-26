package tel.schich.idl.runner.generate

import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult

interface JvmInProcessGenerator {
    fun generate(request: GenerationRequest): GenerationResult
}