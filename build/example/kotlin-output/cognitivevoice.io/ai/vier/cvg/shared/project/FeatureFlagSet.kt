package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class FeatureFlagSet(val value: Set<FeatureFlag>)
