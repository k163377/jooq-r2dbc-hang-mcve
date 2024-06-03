package org.wrongwrong

import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.jooq.generated.tables.records.AccountsRecord
import org.jooq.generated.tables.references.ACCOUNTS
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class HangTest {
    companion object {
        const val SIZE = 10000
        val INFINITE_DATETIME: OffsetDateTime = OffsetDateTime.parse("2200-12-31T23:59:59Z")
    }

    val factory =
        ConnectionFactories.get("r2dbc:postgresql://jooq-16669-root:jooq-16669-root@localhost:5432/jooq-16669-db")
    val factoryCtxt = DSL.using(factory)
    val connectionCtxt = runBlocking { DSL.using(factory.create().awaitSingle()) }

    fun resetTestData(ctxt: DSLContext) = runBlocking {
        ctxt.deleteFrom(ACCOUNTS).awaitSingle()

        val ids = (0 until (SIZE / 2)).map { UUID.randomUUID() }
        val now = OffsetDateTime.now()

        (0 until SIZE).map {
            AccountsRecord().apply {
                id = ids[it / 2]
                validFrom = now
                validTo = INFINITE_DATETIME
                transactFrom = now
                transactTo = if (it % 2 == 0) now else INFINITE_DATETIME
                email = "$it@example.com"
            }
        }
            .chunked(1000)
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
    }.apply { println("beforeAll completed") }

    @Test
    fun test() {
        resetTestData(connectionCtxt)
    }
}
