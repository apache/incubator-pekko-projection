/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2020-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.projection.jdbc.docs

import scala.io.Source

import org.apache.pekko
import pekko.projection.jdbc.internal.Dialect
import pekko.projection.jdbc.internal.H2Dialect
import pekko.projection.jdbc.internal.MSSQLServerDialect
import pekko.projection.jdbc.internal.MySQLDialect
import pekko.projection.jdbc.internal.OracleDialect
import pekko.projection.jdbc.internal.PostgresDialect
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OffsetSchemaSpec extends AnyWordSpecLike with Matchers {
  "Documentation for JDBC OffsetStore Schema" should {

    "match the version used in code (H2)" in {
      val fromDialect =
        asFileContent((opt, s) => H2Dialect(opt, s"${s}_offset_store", s"${s}_management", lowerCase = true), "h2")
      val fromSqlFile = Source.fromResource("create-table-h2.sql").mkString

      normalize(fromSqlFile) shouldBe normalize(fromDialect)
    }

    "match the version used in code (PostgreSQL)" in {
      val fromDialect =
        asFileContent(
          (opt, s) => PostgresDialect(opt, s"${s}_offset_store", s"${s}_management", lowerCase = true),
          "postgres")
      val fromSqlFile = Source.fromResource("create-table-postgres.sql").mkString
      normalize(fromSqlFile) shouldBe normalize(fromDialect)
    }

    "match the version used in code (PostgreSQL uppercase)" in {
      val fromDialect =
        asFileContent(
          (opt, s) => PostgresDialect(opt, s"${s}_offset_store", s"${s}_management", lowerCase = false),
          "postgres-uppercase")
      val fromSqlFile = Source.fromResource("create-table-postgres-uppercase.sql").mkString
      normalize(fromSqlFile) shouldBe normalize(fromDialect)
    }

    "match the version used in code (MSSQL Server)" in {
      val fromDialect =
        asFileContent((opt, s) => MSSQLServerDialect(opt, s"${s}_offset_store", s"${s}_management"), "mssql")
      val fromSqlFile = Source.fromResource("create-table-mssql.sql").mkString
      normalize(fromSqlFile) shouldBe normalize(fromDialect)
    }

    "match the version used in code (MySQL)" in {
      val fromDialect = asFileContent((opt, s) => MySQLDialect(opt, s"${s}_offset_store", s"${s}_management"), "mysql")
      val fromSqlFile = Source.fromResource("create-table-mysql.sql").mkString
      normalize(fromSqlFile) shouldBe normalize(fromDialect)
    }

    "match the version used in code (Oracle)" in {
      val fromDialect =
        asFileContent((opt, s) => OracleDialect(opt, s"${s}_offset_store", s"${s}_management"), "oracle")
      val fromSqlFile = Source.fromResource("create-table-oracle.sql").mkString
      normalizeCaseSensitive(fromSqlFile) shouldBe normalizeCaseSensitive(fromDialect)
    }

  }

  private def asFileContent(dialect: (Option[String], String) => Dialect, dialectCode: String): String = {
    val dialectStatements = {
      val d = dialect(None, "pekko_projection")
      d.createTableStatements ++ d.createManagementTableStatements
    }
    s"""
         |#create-table-$dialectCode
         |${dialectStatements.mkString("\n")}
         |#create-table-$dialectCode
         |""".stripMargin
  }

  private def normalize(in: String): String = {
    normalizeCaseSensitive(in).toLowerCase // ignore case
  }

  private def normalizeCaseSensitive(in: String): String = {
    in.replaceAll("[\\s]++", " ") // ignore multiple blankspaces
      .trim // trim edges
  }
}
