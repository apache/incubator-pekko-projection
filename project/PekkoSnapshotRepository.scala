/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

import sbt.Keys._
import sbt._

/**
 * This plugins conditionally adds Akka snapshot repository.
 */
object PekkoSnapshotRepositories extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  // If using a snapshot version of either Pekko or Pekko Connectors, add both snapshot repos
  // in case there are transitive dependencies to other snapshot artifacts
  override def projectSettings: Seq[Def.Setting[_]] = {
    resolvers ++= (sys.props
      .get("build.pekko.version")
      .orElse(sys.props.get("build.connectors.kafka.version")) match {
      case Some(_) =>
        Seq(
          // akka and alpakka-kafka use Sonatype's snapshot repo
          "Apache Nexus Snapshots".at("https://repository.apache.org/content/repositories/snapshots/"))
      case None => Seq.empty
    })
  }
}
