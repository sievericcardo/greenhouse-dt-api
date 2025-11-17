package org.smolang.greenhouse.api.config

data class WateringStrategyConfig(
    val strategies: Map<String, StrategyDefinition> = emptyMap(),
    val activeStrategy: String = "default"
)

data class StrategyDefinition(
    val name: String = "",
    val description: String = "",
    val durations: MoistureDurations = MoistureDurations()
)

data class MoistureDurations(
    val thirsty: Int = 5,
    val moist: Int = 2,
    val overwatered: Int = 0,
    val unknown: Int = 2
)
