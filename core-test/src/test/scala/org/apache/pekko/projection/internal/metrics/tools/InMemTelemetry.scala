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

package org.apache.pekko.projection.internal.metrics.tools

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function

import scala.util.control.NoStackTrace

import org.apache.pekko
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.Extension
import pekko.actor.typed.ExtensionId
import pekko.projection.ProjectionId
import pekko.projection.internal.Telemetry

/**
 */
class InMemTelemetry(projectionId: ProjectionId, system: ActorSystem[_]) extends Telemetry {
  private val instruments: InMemInstruments = InMemInstrumentsRegistry(system).forId(projectionId)

  import instruments._

  startedInvocations.incrementAndGet()

  override def failed(cause: Throwable): Unit = {
    failureInvocations.incrementAndGet()
    lastFailureThrowable.set(cause)
  }

  override def stopped(): Unit =
    stoppedInvocations.incrementAndGet()

  override def beforeProcess[Envelope](envelope: Envelope, creationTimeInMillis: Long): AnyRef = {
    creationTimestampInvocations.incrementAndGet()
    lastCreationTimestamp.set(creationTimeInMillis)
    ExternalContext()
  }

  override def afterProcess(externalContext: AnyRef): Unit = {
    afterProcessInvocations.incrementAndGet()
    val readyTimestampNanos = externalContext.asInstanceOf[ExternalContext].readyTimestampNanos
    lastServiceTimeInNanos.set(System.nanoTime() - readyTimestampNanos)
  }

  override def onOffsetStored(numberOfEnvelopes: Int): Unit = {
    onOffsetStoredInvocations.incrementAndGet()
    offsetsSuccessfullyCommitted.addAndGet(numberOfEnvelopes)
  }

  override def error(cause: Throwable): Unit = {
    lastErrorThrowable.set(cause)
    errorInvocations.incrementAndGet()
  }

}

case class ExternalContext(readyTimestampNanos: Long = System.nanoTime())

case object TelemetryException extends RuntimeException("Oh, no! Handler errored.") with NoStackTrace

object InMemInstrumentsRegistry extends ExtensionId[InMemInstrumentsRegistry] {
  override def createExtension(system: ActorSystem[_]): InMemInstrumentsRegistry = new InMemInstrumentsRegistry(system)
}
class InMemInstrumentsRegistry(system: ActorSystem[_]) extends Extension {
  private val instrumentMap = new ConcurrentHashMap[ProjectionId, InMemInstruments]()
  def forId(projectionId: ProjectionId): InMemInstruments = {
    instrumentMap.computeIfAbsent(projectionId,
      new function.Function[ProjectionId, InMemInstruments] {
        override def apply(t: ProjectionId): InMemInstruments = new InMemInstruments
      })
  }

  // these are added to use the constructor argument and keep the AkkaDisciplinePlugin happy
  val observedActorSystem = new AtomicReference[ActorSystem[_]](null)
  observedActorSystem.set(system)
}

class InMemInstruments() {

  val creationTimestampInvocations = new AtomicInteger(0)
  val lastCreationTimestamp = new AtomicLong()

  val afterProcessInvocations = new AtomicInteger(0)
  val lastServiceTimeInNanos = new AtomicLong()
  val offsetsSuccessfullyCommitted = new AtomicInteger(0)
  val onOffsetStoredInvocations = new AtomicInteger(0)
  val errorInvocations = new AtomicInteger(0)
  val lastErrorThrowable = new AtomicReference[Throwable](null)

  val startedInvocations = new AtomicInteger(0)

  val stoppedInvocations = new AtomicInteger(0)
  val failureInvocations = new AtomicInteger(0)
  val lastFailureThrowable = new AtomicReference[Throwable](null)

}
