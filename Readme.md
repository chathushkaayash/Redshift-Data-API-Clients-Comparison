# Redshift Data API Performance Comparison

This project aims to compare the performance of the Redshift Data API Java SDK's synchronous and asynchronous clients. The comparison is done using two separate Spring Boot applications, one for each client type, running inside Docker containers. Performance metrics are collected using JMeter.

## Project Structure

- **redshift-data-api-sync/**: Spring Boot project using the synchronous Redshift Data API client.
- **redshift-data-api-async/**: Spring Boot project using the asynchronous Redshift Data API client.
- **jmeter/**: Contains JMeter test plans and configurations for performance testing.

## Prerequisites

- Java 17
- Gradle
- Docker
- JMeter
- AWS credentials configured for accessing Redshift Data API

## Steps to Build and Test

### 1. Build the Projects

Navigate to each project directory and build the respective jar files.

#### Synchronous Client Project:

```bash
cd redshift-data-api-sync
gradle clean build
```

#### Asynchronous Client Project:

```bash
cd redshift-data-api-async
gradle clean build
```

### 2. Create Docker Images

#### Build Docker Image for Synchronous Client:

```bash
cd redshift-data-api-sync
docker build -t redshift-data-sync .
```

#### Build Docker Image for Asynchronous Client:

```bash
cd redshift-data-api-async
docker build -t redshift-data-async .
```

### 3. Run Docker Containers

#### Run the Synchronous Client Container:

```bash
docker run -p 9080:8080 --cpus="2" --memory="4g" -d redshift-data-sync
```

#### Run the Asynchronous Client Container:

```bash
docker run -p 9081:8080 --cpus="2" --memory="4g" -d redshift-data-async
```

### 4. Run JMeter Tests

Use the provided JMeter test plan to measure performance. Update the parameters as required.

#### Test the Synchronous Client:

```bash
jmeter -n -t jmeter/aws-redshift-tests.jmx -l results/sync.jtl -Jusers=50 -Jduration=1200 -Jport=9080
```

#### Test the Asynchronous Client:

```bash
jmeter -n -t jmeter/aws-redshift-tests.jmx -l results/async.jtl -Jusers=50 -Jduration=1200 -Jport=9081
```

### 5. Analyze Results

- JMeter results are saved in the `results/` directory.
- Compare `sync.jtl` and `async.jtl` using JMeter GUI or any performance analysis tool.

## Notes

1. Ensure your AWS credentials have sufficient permissions to access the Redshift Data API.
2. Modify the `application.properties` files in each project to include necessary configurations (e.g., AWS region, database, query parameters).
3. Monitor resource utilization during tests to ensure no bottlenecks from the host machine.
