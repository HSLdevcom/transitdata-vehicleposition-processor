# transitdata-vehicleposition-processor

transitdata-vehicleposition-processor converts [HFP messages](https://digitransit.fi/en/developers/apis/4-realtime-api/vehicle-positions-2/) into [GTFS-RT vehicle positions](https://developers.google.com/transit/gtfs-realtime/guides/vehicle-positions). Optionally, it is also possible to add passenger count data from APC messages to the GTFS-RT vehicle positions.

This project is part of [transitdata pipeline](https://github.com/HSLdevcom/transitdata). 

| Branch    | Build status |
|-----------|--------|
| `master`  | [![Test and create Docker image](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml)
| `develop` | [![Test and create Docker image](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml/badge.svg?branch=develop)](https://github.com/HSLdevcom/transitdata-vehicleposition-processor/actions/workflows/test-and-build.yml)

## Building

Building this project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common), which is available from GitHub Packages. Make sure you have configured access to GitHub Packages.

### Locally

1. `mvn compile`
2. `mvn package`

### Docker

* Use [this script](./build-image.sh) to build the Docker image

## Running

### Dependencies

* Pulsar

### Environment variables

* `PROCESSOR_VEHICLE_POSITION_MAX_TIME_DIFFERENCE`: maximum time difference of the HFP timestamp relative to the current time
* `ADDED_TRIPS_ENABLED_MODES`: transport modes for which GTFS-RT ADDED trips are enabled (i.e. whether to create vehicle positions for all vehicles when more than one vehicle is serving the same trip)
* `PASSENGER_COUNT_ENABLED_VEHICLES`: whether passenger count data should be used in the GTFS-RT vehicle positions
