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

package org.apache.pekko.projection.testkit.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.ApiMayChange
import pekko.projection.OffsetVerification
import pekko.projection.javadsl.VerifiableSourceProvider
import pekko.projection.testkit.internal.TestSourceProviderImpl
import pekko.util.FunctionConverters._

@ApiMayChange
object TestSourceProvider {

  /**
   * A [[TestSourceProvider]] is used to supply an arbitrary stream of envelopes to a [[TestProjection]]
   *
   * @param sourceEvents - a [[pekko.stream.javadsl.Source]] of envelopes
   * @param extractOffset - a user-defined function to extract the offset from an envelope
   */
  def create[Offset, Envelope](
      sourceEvents: pekko.stream.javadsl.Source[Envelope, NotUsed],
      extractOffset: java.util.function.Function[Envelope, Offset]): TestSourceProvider[Offset, Envelope] =
    new TestSourceProviderImpl[Offset, Envelope](
      sourceEvents = sourceEvents.asScala,
      extractOffsetFn = extractOffset.asScala,
      extractCreationTimeFn = (_: Envelope) => 0L,
      verifyOffsetFn = (_: Offset) => OffsetVerification.VerificationSuccess,
      startSourceFromFn = (_: Offset, _: Offset) => false,
      allowCompletion = false)
}

abstract class TestSourceProvider[Offset, Envelope] extends VerifiableSourceProvider[Offset, Envelope] {

  /**
   * A user-defined function to extract the event creation time from an envelope.
   */
  def withExtractCreationTimeFunction(
      extractCreationTimeFn: java.util.function.Function[Envelope, Long]): TestSourceProvider[Offset, Envelope]

  /**
   * Allow the [[sourceEvents]] Source to complete or stay open indefinitely.
   */
  def withAllowCompletion(allowCompletion: Boolean): TestSourceProvider[Offset, Envelope]

  /**
   * A user-defined function to verify offsets.
   */
  def withOffsetVerification(offsetVerificationFn: java.util.function.Function[Offset, OffsetVerification])
      : TestSourceProvider[Offset, Envelope]

  /**
   * A user-defined function to compare the last offset returned by the offset store with the offset in the source
   * to filter out previously processed offsets.
   *
   * First parameter: Last offset processed. Second parameter this envelope's offset from [[sourceEvents]].
   */
  def withStartSourceFrom(startSourceFromFn: java.util.function.BiFunction[Offset, Offset, java.lang.Boolean])
      : TestSourceProvider[Offset, Envelope]
}
