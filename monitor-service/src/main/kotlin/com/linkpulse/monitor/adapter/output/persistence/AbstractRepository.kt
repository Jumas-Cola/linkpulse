package com.linkpulse.monitor.adapter.output.persistence

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

abstract class AbstractRepository {
    protected suspend fun <T> dbQuery(block: suspend () -> T): T =
        suspendTransaction { block() }
}