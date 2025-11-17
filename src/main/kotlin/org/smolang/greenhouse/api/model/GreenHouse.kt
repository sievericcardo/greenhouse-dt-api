package org.smolang.greenhouse.api.model

class GreenHouse(
    val greenhouseId: String,
    val sections: List<Section>,
    val waterBuckets: List<WaterBucket>,
    val lightSensor: LightSensor,
    val temperatureHumiditySensor: TemperatureHumiditySensor
)