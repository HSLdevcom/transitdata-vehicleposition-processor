# transitdata-vehicleposition-processor

| Branch    | Status |
|-----------|--------|
| `master`  | [![Test and create Docker image](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml)
| `develop` | [![Test and create Docker image](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml/badge.svg?branch=develop)](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml)

This project is part of [transitdata pipeline](https://github.com/HSLdevcom/transitdata).  
`transitdata-vehicleposition-processor` generates [GTFS-RT vehicle positions](https://developers.google.com/transit/gtfs-realtime/guides/vehicle-positions) from [HFP messages](https://digitransit.fi/en/developers/apis/4-realtime-api/vehicle-positions-2/).

## Building

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common).

### Locally

1. `mvn compile`
2. `mvn package`

### Docker

* Use [this script](./build-image.sh) to build the Docker image

