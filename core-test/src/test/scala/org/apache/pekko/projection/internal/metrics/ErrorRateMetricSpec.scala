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

package org.apache.pekko.projection.internal.metrics

import scala.concurrent.duration._

import org.apache.pekko
import pekko.projection.HandlerRecoveryStrategy
import pekko.projection.ProjectionId
import pekko.projection.internal.AtLeastOnce
import pekko.projection.internal.AtMostOnce
import pekko.projection.internal.ExactlyOnce
import pekko.projection.internal.FlowHandlerStrategy
import pekko.projection.internal.GroupedHandlerStrategy
import pekko.projection.internal.SingleHandlerStrategy
import pekko.projection.internal.metrics.tools.InMemInstrumentsRegistry
import pekko.projection.internal.metrics.tools.InternalProjectionStateMetricsSpec
import pekko.projection.internal.metrics.tools.InternalProjectionStateMetricsSpec.Envelope
import pekko.projection.internal.metrics.tools.InternalProjectionStateMetricsSpec.TelemetryTester
import pekko.projection.internal.metrics.tools.TestHandlers

abstract class ErrorRateMetricSpec extends InternalProjectionStateMetricsSpec {
  implicit var projectionId: ProjectionId = null

  before {
    projectionId = genRandomProjectionId()
  }

  def instruments(implicit projectionId: ProjectionId) = InMemInstrumentsRegistry(system).forId(projectionId)
  val defaultNumberOfEnvelopes = 6

  def detectNoError(numberOfEnvelopes: Int = defaultNumberOfEnvelopes): Any = {
    instruments.offsetsSuccessfullyCommitted.get should be(numberOfEnvelopes)
    instruments.errorInvocations.get should be(0)
  }

  def detectSomeErrors(expectedErrorCount: Int): Any = {
    instruments.errorInvocations.get should be(expectedErrorCount)
    if (instruments.lastErrorThrowable.get() != null)
      instruments.lastErrorThrowable.get().getMessage should be("Oh, no! Handler errored.")
  }

}

class ErrorRateMetricAtLeastOnceSpec extends ErrorRateMetricSpec {

  "A metric reporting event handler errors" must {
    // at-least-once
    " in `at-least-once` with singleHandler" must {
      "report nothing in happy scenarios" in {
        val numOfEnvelopes = 20
        val tt: TelemetryTester =
          new TelemetryTester(AtLeastOnce(), SingleHandlerStrategy(TestHandlers.single), numOfEnvelopes)
        runInternal(tt.projectionState) {
          detectNoError(numOfEnvelopes)
        }
      }
      "report errors in flaky handlers" in {
        val single = TestHandlers.singleWithErrors(1, 1, 1, 1, 2, 2, 3, 4, 5)
        val tt = new TelemetryTester(
          AtLeastOnce(recoveryStrategy = Some(HandlerRecoveryStrategy.retryAndFail(maxRetries, 30.millis))),
          SingleHandlerStrategy(single))

        runInternal(tt.projectionState) {
          detectSomeErrors(9)
        }
      }
    }
    " in `at-least-once` with groupedHandler" must {
      "report nothing in happy scenarios" in {
        val tt = new TelemetryTester(AtLeastOnce(), GroupedHandlerStrategy(TestHandlers.grouped))

        runInternal(tt.projectionState) {
          detectNoError()
        }
      }
      "report errors in flaky handlers" in {
        val grouped = TestHandlers.groupedWithErrors(2, 3, 3, 5)
        val tt = new TelemetryTester(
          AtLeastOnce(recoveryStrategy = Some(HandlerRecoveryStrategy.retryAndFail(maxRetries, 30.millis))),
          GroupedHandlerStrategy(grouped))

        runInternal(tt.projectionState) {
          detectSomeErrors(4)
        }
      }
    }
    " in `at-least-once` with flowHandler" must {
      "report nothing in happy scenarios" in {
        val tt =
          new TelemetryTester(AtLeastOnce(), FlowHandlerStrategy[Envelope](TestHandlers.flow))

        runInternal(tt.projectionState) {
          detectNoError()
        }
      }
      "report errors in flaky handlers" in {
        val flow = TestHandlers.flowWithErrors(2, 3, 6)
        val tt = new TelemetryTester(AtLeastOnce(), FlowHandlerStrategy[Envelope](flow))
        runInternal(tt.projectionState) {
          detectSomeErrors(3)
        }
      }
    }

  }
}

class ErrorRateMetricExactlyOnceSpec extends ErrorRateMetricSpec {

  "A metric reporting event handler errors" must {

    // exactly-once
    " in `exactly-once` with singleHandler" must {
      "report nothing in happy scenarios" in {
        val tt = new TelemetryTester(ExactlyOnce(), SingleHandlerStrategy(TestHandlers.single))

        runInternal(tt.projectionState) {
          detectNoError()
        }
      }
      "report errors in flaky handlers" in {
        val single = TestHandlers.singleWithErrors(2, 3, 4, 4, 4, 4, 4, 4, 4, 5, 6, 6)
        val tt = new TelemetryTester(
          ExactlyOnce(recoveryStrategy = Some(HandlerRecoveryStrategy.retryAndFail(maxRetries, 30.millis))),
          SingleHandlerStrategy(single))

        runInternal(tt.projectionState) {
          detectSomeErrors(12)
        }
      }
    }
    " in `exactly-once` with groupedHandler" must {
      "report nothing in happy scenarios" in {
        val grouped = TestHandlers.grouped
        val groupHandler: GroupedHandlerStrategy[Envelope] = GroupedHandlerStrategy(grouped)
        val tt = new TelemetryTester(ExactlyOnce(), groupHandler)

        runInternal(tt.projectionState) {
          detectNoError()
        }
      }
      "report errors in flaky handlers" in {
        val groupedWithFailures = TestHandlers.groupedWithErrors(1, 2, 5)
        val tt = new TelemetryTester(
          ExactlyOnce(recoveryStrategy = Some(HandlerRecoveryStrategy.retryAndFail(maxRetries, 30.millis))),
          GroupedHandlerStrategy(groupedWithFailures))

        runInternal(tt.projectionState) {
          detectSomeErrors(3)
        }
      }
    }

  }
}

class ErrorRateMetricAtMostOnceSpec extends ErrorRateMetricSpec {

  "A metric reporting event handler errors" must {

    // at-most-once
    " in `at-most-once` with singleHandler" must {
      "report nothing in happy scenarios" in {
        val tt = new TelemetryTester(AtMostOnce(), SingleHandlerStrategy(TestHandlers.single))

        runInternal(tt.projectionState) {
          detectNoError()
        }
      }
      "report nothing in case of failure" in {
        val single = TestHandlers.singleWithErrors(1, 2, 4, 6)
        val tt = new TelemetryTester(
          AtMostOnce(recoveryStrategy = Some(HandlerRecoveryStrategy.skip)),
          SingleHandlerStrategy(single))

        runInternal(tt.projectionState) {
          detectSomeErrors(4)
        }
      }
    }

  }

}
