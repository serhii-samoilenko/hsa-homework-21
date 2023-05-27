# Database: Sharding report

Testing performance of different sharding strategies.

For each case, the same table will be used:

```sql
CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    category_id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL
);
```

## No sharding

First, we will test the performance of a single database.

Preparing the database:

`books table prepared`

#### Starting daemon to constantly populate master:

```sql
INSERT INTO test.users (name) VALUES ('${UUID.randomUUID()}');
```

