# Train Station

### Demo application based on Apache Pulsar
The purpose of this application is to show how to build a simple event-driven application on top of Apache Pulsar. 
This application is written in functional Scala with Tagless Final style and ZIO is used as the main effect.

#### Testing
In order to run unit tests:
```sbt
sbt test
```

#### Running
This repository contains a `docker-compose` file, which includes 3 services: 
* Apache Pulsar
* Zurich train station
* Bern train station

First, build docker images of the service by running
```sbt
sbt docker:publishLocal
```

When you have successfully built docker images you can start environment: 
```sbt
docker-compose up -d
```
This will run services in the background. Both train stations will connect to Apache Pulsar. 
Services are starting much faster than Apache Pulsar so they will retry until it is ready. 
Train station service is ready when you see a log message:
```[2020-09-30T19:10:52.064Z] Started train station Bern```

#### Calling service endpoints
To test that services are working correctly you can send HTTP requests:
```
curl --request POST \
  --url http://localhost:8082/departure \
  --header 'content-type: application/json' \
  --data '{
	"id": "123",
	"to": "Bern",
	"time": "2020-12-03T10:15:30.00Z",
	"actual": "2020-12-03T10:15:30.00Z"
}'
```
This will create a departing train from Zurich to Bern. 
In order to mark train as arrived send another HTTP request: 
```
curl --request POST \
  --url http://localhost:8081/arrival \
  --header 'content-type: application/json' \
  --data '{
	"trainId": "123",
	"time": "2020-12-03T10:15:30.00Z"
}'
```

#### Analytics - TODO (WIP) 
This service also contains a Pulsar Function, which saves emitted events to a database. 
This is not yet ready to be reviewed.