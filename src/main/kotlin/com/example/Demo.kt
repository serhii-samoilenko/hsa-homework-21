package com.example

import com.example.util.ConnectionPool
import com.example.util.Database
import com.example.util.Docker
import com.example.util.Report

/**
 * Database: Sharding demo
 */
fun runDemo() {
    val r = Report("REPORT.md")
    val docker = Docker()
    r.h1("Database: Sharding report")
    r.text("Testing performance of different sharding strategies.")

    val booksTableSql = """
        CREATE TABLE IF NOT EXISTS books (
            id BIGSERIAL PRIMARY KEY,
            category_id INT NOT NULL,
            author VARCHAR(255) NOT NULL,
            title VARCHAR(255) NOT NULL
        )
    """.trimIndent()

    r.text("For each case, the same table will be used:")
    r.sql(booksTableSql)

    r.h2("No sharding")
    r.text("First, we will test the performance of a single database.")
    r.text("Preparing the database:")
    docker.waitForContainer("standalone-postgres")

    val standalone = Database(ConnectionPool("jdbc:postgresql://localhost:5433/", "postgres", "postgres"))
    standalone.execute(booksTableSql, "TRUNCATE TABLE books")
    r.code("books table prepared")

    r.h4("Starting daemon to constantly populate master:")
    r.sql("INSERT INTO test.users (name) VALUES ('\${UUID.randomUUID()}')")

//    standalone.execute("INSERT INTO test.users (name) VALUES ('${UUID.randomUUID()}')")

    r.writeToFile()
}
