package ai.vier.cvg.shared.bargein

sealed interface BargeInOptions {
    data class Advanced(val value: AdvancedBargeInOptions) : BargeInOptions

    data class Legacy(val value: LegacyEnableBargeIn) : BargeInOptions
}
