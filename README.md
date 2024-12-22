# GreenHouse DT API

Simulation Driver for the GreenHouse adaptation experiment.

## Installation

The green-house-dt-api is designed to be run with Docker. To install the API, clone the repository, and download the jar and SMOL resources from the release page and then build the docker image.

```bash
docker build -t green-house-dt-api .
```

Otherwise, it can be run from the jar file under the `build/libs` directory.

## Configuration

The API can be configured using the following environment variables:

- `DOMAIN_PREFIX_URI`: The prefix URI for the domain ontology. Default is `http://www.smolang.org/greenhouseDT#`.
- `EXTRA_PREFIXES`: Extra prefixes to be used in the ontology. Default is `ast,http://www.smolang.org/greenhouseDT#`.
- `INFLUX_TOKEN`: The token for the InfluxDB instance.
- `INFLUX_URL`: The URL for the InfluxDB instance.
- `SMOL_PATH`: The path to the SMOL files to be loaded. Default when executed from docker is to be set to `SMOL/Greenhouse_ctrl.smol;SMOL/Greenhouse_data.smol;SMOL/Greenhouse_health.smol;SMOL/Greenhouse_plants.smol;SMOL/Greenhouse_pumps.smol;SMOL/Greenhouse_pots.smol;SMOL/Greenhouse_shelves.smol;SMOL/GreenHouse.smol`.

## Usage

The API gets executed as a Spring Boot application. The set of endpoints are visibile under swagger-ui at `http://localhost:8080/swagger-ui.html`.
