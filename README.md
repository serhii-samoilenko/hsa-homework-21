# Highload Software Architecture 8 Lesson 20 Homework

Database: Replication
---

## Test project setup

The demo is written in Kotlin and uses Gradle as a build tool.
Postgres sharding setup is defined in the [docker-compose.yaml](docker-compose.yaml) file.

The `com.example.DemoKt.runDemo` function is used to run all the scenarios.
It also writes the [REPORT.md](reports/REPORT.md) file as it executes the scenarios.

## How to build and run

Start the docker-compose setup:

```shell script
docker-compose up -d
```

Build and run demo application (Requires Java 11+)

```shell script
./gradlew run
```

## Scenarios

Each setup is tested with the following scenario:

1. Create a table and set up sharding if needed
2. Insert 1 million rows into the table
3. Run the insertion of additional random rows for a minute, measure performance
4. Run the selection of random rows for a minute, measure performance

### Scenario 1: Plain table on one PostgreSQL instance

This scenario is used as a baseline for comparison. It creates a table on a single PostgreSQL instance. The table is not sharded.

Achieved 3715.388 ops/sec for inserts and 45.771 ops/sec for selects.

### Scenario 2: Vertical sharding by category on one PostgreSQL instance

This scenario creates a table on a single PostgreSQL instance. The table is sharded by category using inherited tables and rules.

Achieved 3627.635 ops/sec for inserts and 115.887 ops/sec for selects.

In percentage, the performance is 97.6% of the plain table for inserts and 253.2% for selects.

This happens because the inserts are slower due to the rules and selects are faster because the query planner can use the rules to optimize the query.

### Scenario 3: Horizontal sharding using Citus

This scenario creates a table on a Citus cluster. The table is sharded by category using Citus.

Achieved 3252.129 ops/sec for inserts and 111.746 ops/sec for selects.

In percentage, the performance is 87.5% of the plain table for inserts and 243.8% for selects.

This happens because the inserts are slower due to the overhead of the Citus cluster and selects are faster because the query planner can use the Citus cluster to optimize the query.

Performance could be better if the Citus worker nodes were on different machines.
