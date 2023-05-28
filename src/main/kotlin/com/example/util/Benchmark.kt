import com.example.util.ConnectionPool
import java.sql.ResultSet
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Benchmark(private val connectionPool: ConnectionPool) {

    data class Result(
        val count: Long,
        val duration: Duration,
    ) {
        private fun opsPerSecond() = round(1000.0 * count * 1000.0 / duration.inWholeMilliseconds) / 1000.0
        override fun toString() = "$count ops in $duration - ${opsPerSecond()} ops/sec"
    }

    fun benchmarkSelects(duration: Duration, concurrency: Int, querySupplier: () -> String): Result =
        benchmark(duration, concurrency) { Runnable { query(querySupplier()) } }

    fun benchmarkInserts(duration: Duration, concurrency: Int, querySupplier: () -> String): Result =
        benchmark(duration, concurrency) { Runnable { execute(querySupplier()) } }

    private fun benchmark(
        duration: Duration,
        concurrency: Int,
        taskSupplier: () -> Runnable,
    ): Result {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration.inWholeMilliseconds
        val executor = Executors.newFixedThreadPool(concurrency) as ThreadPoolExecutor

        while (System.currentTimeMillis() < endTime) {
            val totalCount = executor.activeCount + executor.queue.size
            val capacity = concurrency - totalCount - 1
            for (i in 0 until capacity) {
                executor.submit(taskSupplier())
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, MINUTES)
        val actualTime = System.currentTimeMillis() - startTime
        return Result(executor.completedTaskCount, actualTime.milliseconds)
    }

    private fun execute(sql: String): Duration {
        val start = System.currentTimeMillis()
        connectionPool.connection().use { connection ->
            connection.createStatement().executeUpdate(sql)
        }
        val end = System.currentTimeMillis()
        return (end - start).milliseconds
    }

    private fun query(sql: String) {
        connectionPool.connection().use { connection ->
            connection.createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
            ).apply { fetchSize = 2000 }
                .executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        // do nothing
                    }
                    rs.close()
                }
        }
    }
}
