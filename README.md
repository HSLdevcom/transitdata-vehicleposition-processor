# transitdata-vehicleposition-processor

| Branch    | Status |
|-----------|--------|
| `master`  | [![Build Status](https://travis-ci.org/HSLdevcom/transitdata-vehicleposition-processor.svg?branch=master)](https://travis-ci.org/HSLdevcom/transitdata-vehicleposition-processor)
| `develop` | [![Build Status](https://travis-ci.org/HSLdevcom/transitdata-vehicleposition-processor.svg?branch=develop)](https://travis-ci.org/HSLdevcom/transitdata-vehicleposition-processor)

This project is part of [transitdata pipeline](https://github.com/HSLdevcom/transitdata).  
`transitdata-vehicleposition-processor` generates [GTFS-RT vehicle positions](https://developers.google.com/transit/gtfs-realtime/guides/vehicle-positions) from [HFP messages](https://digitransit.fi/en/developers/apis/4-realtime-api/vehicle-positions-2/).

## Building

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common).

### Locally

1. `mvn compile`
2. `mvn package`

### Docker

* Use [this script](./build-image.sh) to build the Docker image

