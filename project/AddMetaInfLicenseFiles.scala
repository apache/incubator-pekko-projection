/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt.AutoPlugin

import sbt.Keys._
import sbt._
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin.autoImport._

object AddMetaInfLicenseFiles extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = ApacheSonatypePlugin

  override lazy val projectSettings = Seq(
    apacheSonatypeDisclaimerFile := Some((LocalRootProject / baseDirectory).value / "DISCLAIMER"))

}
