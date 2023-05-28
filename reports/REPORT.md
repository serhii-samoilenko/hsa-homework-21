# Database: Sharding report

Testing performance of different sharding strategies.

## No sharding

First, testing the performance without sharding.

Creating simple table

```sql
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    category_id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL
);
```

Running test scenario...

### Running inserts for 1m with concurrency 10

`222927 ops in 1m 0.001s - 3715.388 ops/sec`

### Running selects for 1m with concurrency 10

`2750 ops in 1m 0.082s - 45.771 ops/sec`

## Sharding by category

Now, testing the performance with sharding by category.

Creating partitioned table

```sql
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    category_id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL
);
CREATE TABLE books_0 (CHECK (category_id = 0)) INHERITS (books);
CREATE TABLE books_1 (CHECK (category_id = 1)) INHERITS (books);
CREATE TABLE books_2 (CHECK (category_id = 2)) INHERITS (books);
CREATE TABLE books_3 (CHECK (category_id = 3)) INHERITS (books);
CREATE RULE books_insert_to_category_0 AS ON INSERT TO books WHERE ( category_id = 0 ) DO INSTEAD INSERT INTO books_0 VALUES (NEW.*);
CREATE RULE books_insert_to_category_1 AS ON INSERT TO books WHERE ( category_id = 1 ) DO INSTEAD INSERT INTO books_1 VALUES (NEW.*);
CREATE RULE books_insert_to_category_2 AS ON INSERT TO books WHERE ( category_id = 2 ) DO INSTEAD INSERT INTO books_2 VALUES (NEW.*);
CREATE RULE books_insert_to_category_3 AS ON INSERT TO books WHERE ( category_id = 3 ) DO INSTEAD INSERT INTO books_3 VALUES (NEW.*);
```

Running test scenario...

### Running inserts for 1m with concurrency 10

`217669 ops in 1m 0.003s - 3627.635 ops/sec`

### Running selects for 1m with concurrency 10

`6959 ops in 1m 0.05s - 115.887 ops/sec`

## Sharding using citus

Now, testing the performance with sharding using citus.

Creating partitioned table managed by citus

Partitioning column must be part of the primary key

```sql
CREATE EXTENSION IF NOT EXISTS citus;
CREATE TABLE books (
    id BIGSERIAL,
    category_id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    PRIMARY KEY (id, category_id)
);
```

```sql
SELECT create_distributed_table('books', 'category_id');
```

Running test scenario...

### Running inserts for 1m with concurrency 10

`195131 ops in 1m 0.001s - 3252.129 ops/sec`

### Running selects for 1m with concurrency 10

`6709 ops in 1m 0.038s - 111.746 ops/sec`

