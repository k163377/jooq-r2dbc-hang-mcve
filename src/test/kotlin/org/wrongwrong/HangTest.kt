package org.wrongwrong

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.TableField
import org.jooq.generated.tables.records.AccountsRecord
import org.jooq.generated.tables.references.ACCOUNTS
import org.jooq.generated.tables.references.CORPORATIONS
import org.jooq.generated.tables.references.NATURAL_PERSONS
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.min
import org.jooq.impl.DSL.name
import org.junit.jupiter.api.Test
import org.wrongwrong.HangTest.Companion.INFINITE_DATETIME
import java.time.OffsetDateTime
import java.util.*
import kotlin.system.measureTimeMillis

fun <T : TableField<R, OffsetDateTime?>, R : Record> biTemporalCurrentStatusCondition(
    validFrom: T,
    validTo: T,
    transactTo: T,
): Condition {
    return field(validFrom.name).le(DSL.currentOffsetDateTime())
        .and(field(validTo.name).gt(DSL.currentOffsetDateTime()))
        .and(field(transactTo.name).eq(INFINITE_DATETIME))
}

fun <T : TableField<R, OffsetDateTime?>, R : Record> biTemporalValidStatusesCondition(transactTo: T): Condition {
    return transactTo.eq(INFINITE_DATETIME)
}

class HangTest {
    companion object {
        const val SIZE = 20000000
        val INFINITE_DATETIME: OffsetDateTime = OffsetDateTime.parse("2200-12-31T23:59:59Z")

        const val TEMP_ACCOUNTS = "temp_accounts"
        const val ACCOUNT_ID = "account_id"
        const val CREATED_AT = "created_at"
    }

    val factory =
        ConnectionFactories.get("r2dbc:postgresql://jooq-16669-root:jooq-16669-root@localhost:5432/jooq-16669-db")
    val factoryCtxt = DSL.using(factory, SQLDialect.POSTGRES)
    val connectionPoolContext =
        ConnectionPoolConfiguration.builder(factory).build().let { DSL.using(ConnectionPool(it), SQLDialect.POSTGRES) }
    val connectionCtxt = runBlocking { DSL.using(factory.create().awaitSingle(), SQLDialect.POSTGRES) }

    fun resetTestData(ctxt: DSLContext) = runBlocking {
        ctxt.deleteFrom(ACCOUNTS).awaitSingle()

        val ids = (0 until (SIZE / 2)).map { UUID.randomUUID() }
        val now = OffsetDateTime.now()

        (0 until SIZE)
            .asSequence()
            .map {
                AccountsRecord().apply {
                    id = ids[it / 2]
                    validFrom = now
                    validTo = INFINITE_DATETIME
                    transactFrom = now
                    transactTo = if (it % 2 == 0) now else INFINITE_DATETIME
                    email = "$it@example.com"
                }
            }
            .chunked(10000)
            .forEach {
                ctxt.insertInto(ACCOUNTS)
                    .columns(
                        ACCOUNTS.ID,
                        ACCOUNTS.VALID_FROM,
                        ACCOUNTS.VALID_TO,
                        ACCOUNTS.TRANSACT_FROM,
                        ACCOUNTS.TRANSACT_TO,
                        ACCOUNTS.EMAIL
                    )
                    .valuesOfRows(
                        it.map { row ->
                            DSL.row(row.id, row.validFrom, row.validTo, row.transactFrom, row.transactTo, row.email)
                        }
                    )
                    .awaitSingle()
            }
    }.apply { println("$SIZE records created") }

    fun select(ctxt: DSLContext) = ctxt.with(TEMP_ACCOUNTS)
        .`as`(
            DSL.select(ACCOUNTS.ID.`as`(ACCOUNT_ID), min(ACCOUNTS.VALID_FROM).`as`(CREATED_AT))
                .from(ACCOUNTS)
                .where(biTemporalValidStatusesCondition(ACCOUNTS.TRANSACT_TO))
                .groupBy(ACCOUNTS.ID)
        )
        .select(ACCOUNTS.asterisk())
        .from(ACCOUNTS)
        .innerJoin(TEMP_ACCOUNTS)
        .on(ACCOUNTS.ID.eq(field(name(TEMP_ACCOUNTS, ACCOUNT_ID), UUID::class.java)))
        .leftJoin(NATURAL_PERSONS)
        .on(ACCOUNTS.CONSUMER_RECORD_ID.eq(NATURAL_PERSONS.CONSUMER_RECORD_ID))
        .leftJoin(CORPORATIONS)
        .on(ACCOUNTS.CONSUMER_RECORD_ID.eq(CORPORATIONS.CONSUMER_RECORD_ID))
        .orderBy(field(name(TEMP_ACCOUNTS, CREATED_AT)).desc())
        .limit(500)
        .asFlow()

    fun repeatSelectTest(ctxt: DSLContext) {
        println("start repeatSearch\n")

        (1..10).forEach {
            runBlocking {
                val time = measureTimeMillis {
                    val result = select(ctxt).toList()
                    println(result.size)
                }

                println("${String.format("%03d", it)}: $time")
                println()
            }
        }
    }

    @Test
    fun test() {
        try {
            resetTestData(connectionPoolContext)
            repeatSelectTest(connectionPoolContext)
            // resetTestData(connectionCtxt)
            // repeatSelectTest(connectionCtxt)
            // repeatSelectTest(factoryCtxt)
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
}
