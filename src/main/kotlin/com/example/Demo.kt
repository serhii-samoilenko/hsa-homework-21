package com.example

import Benchmark
import com.example.util.ConnectionPool
import com.example.util.Database
import com.example.util.Docker
import com.example.util.Report
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Database: Sharding demo
 */
fun runDemo() {
    val r = Report("REPORT.md")
    val docker = Docker()
    r.h1("Database: Sharding report")
    r.text("Testing performance of different sharding strategies.")

    val scenario = { pool: ConnectionPool ->

        val initialCount = 1_000_000
        val chunkSize = 1000
        val categories = 4
        val duration = 10.seconds
        val concurrency = 10

        r.text("Running test scenario...")
        val db = Database(pool)

        println("Inserting $initialCount records...")
        sequence { var i = 0; while (i < initialCount) yield(i++) }
            .chunked(chunkSize)
            .forEach { chunk: List<Int> ->
                db.execute(
                    "INSERT INTO books (category_id, author, title) VALUES " + chunk.joinToString(", ") {
                        "(${randomCategory(categories)}, '${randomName()}', '${randomName()}')"
                    },
                )
            }

        val benchmark = Benchmark(pool)

        r.h3("Running inserts for $duration with concurrency $concurrency")
        val insertsResult = benchmark.benchmarkInserts(duration, concurrency) {
            "INSERT INTO books (category_id, author, title) VALUES (${randomCategory(categories)}, '${randomName()}', '${randomName()}')"
        }
        r.code(insertsResult.toString())

        r.h3("Running selects for $duration with concurrency $concurrency")
        val selectsResult = benchmark.benchmarkSelects(duration, concurrency) {
            "SELECT * FROM books WHERE author = '${randomName()}'"
        }
        r.code(selectsResult.toString())
    }

    r.h2("No sharding")
    docker.waitForContainer("postgres")
    r.text("First, testing the performance without sharding.")

    with(ConnectionPool("jdbc:postgresql://localhost:5432/", "postgres", "postgres")) {
        val db = Database(this)
        db.execute("DROP TABLE IF EXISTS books CASCADE")

        r.text("Creating simple table")
        db.execute(
            """
            CREATE TABLE books (
                id BIGSERIAL PRIMARY KEY,
                category_id INT NOT NULL,
                author VARCHAR(255) NOT NULL,
                title VARCHAR(255) NOT NULL
            )
            """.trimIndent(),
            report = r,
        )
        scenario(this)
    }

    r.h2("Sharding by category")
    docker.waitForContainer("postgres-partitioned")
    r.text("Now, testing the performance with sharding by category.")

    with(ConnectionPool("jdbc:postgresql://localhost:5433/", "postgres", "postgres")) {
        val db = Database(this)
        db.execute("DROP TABLE IF EXISTS books CASCADE")

        r.text("Creating partitioned table")
        db.execute(
            """
            CREATE TABLE books (
                id BIGSERIAL PRIMARY KEY,
                category_id INT NOT NULL,
                author VARCHAR(255) NOT NULL,
                title VARCHAR(255) NOT NULL
            )
            """.trimIndent(),
            "CREATE TABLE books_0 (CHECK (category_id = 0)) INHERITS (books)",
            "CREATE TABLE books_1 (CHECK (category_id = 1)) INHERITS (books)",
            "CREATE TABLE books_2 (CHECK (category_id = 2)) INHERITS (books)",
            "CREATE TABLE books_3 (CHECK (category_id = 3)) INHERITS (books)",
            "CREATE RULE books_insert_to_category_0 AS ON INSERT TO books " +
                "WHERE ( category_id = 0 ) DO INSTEAD INSERT INTO books_0 VALUES (NEW.*)",
            "CREATE RULE books_insert_to_category_1 AS ON INSERT TO books " +
                "WHERE ( category_id = 1 ) DO INSTEAD INSERT INTO books_1 VALUES (NEW.*)",
            "CREATE RULE books_insert_to_category_2 AS ON INSERT TO books " +
                "WHERE ( category_id = 2 ) DO INSTEAD INSERT INTO books_2 VALUES (NEW.*)",
            "CREATE RULE books_insert_to_category_3 AS ON INSERT TO books " +
                "WHERE ( category_id = 3 ) DO INSTEAD INSERT INTO books_3 VALUES (NEW.*)",
            report = r,
        )
        scenario(this)
    }

    r.h2("Sharding using citus")
    docker.waitForContainer("citus-master")
    r.text("Now, testing the performance with sharding using citus.")

    with(ConnectionPool("jdbc:postgresql://localhost:5434/", "postgres", "postgres")) {
        val db = Database(this)
        db.execute("DROP TABLE IF EXISTS books CASCADE")

        r.text("Creating partitioned table managed by citus")
        r.text("Partitioning column must be part of the primary key")
        db.execute(
            "CREATE EXTENSION IF NOT EXISTS citus",
            """
            CREATE TABLE books (
                id BIGSERIAL,
                category_id INT NOT NULL,
                author VARCHAR(255) NOT NULL,
                title VARCHAR(255) NOT NULL,
                PRIMARY KEY (id, category_id)
            )
            """.trimIndent(),
            report = r,
        )
        db.query(
            "SELECT create_distributed_table('books', 'category_id')",
            report = r,
        )
        scenario(this)
    }
    r.writeToFile()
}

fun randomName(): String = UUID.randomUUID().toString()

fun randomCategory(categories: Int): Int = Random.nextInt(categories)
