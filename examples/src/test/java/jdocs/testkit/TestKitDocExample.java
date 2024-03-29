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

package jdocs.testkit;

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.projection.Projection;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.javadsl.Handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

// #testkit-import
import org.apache.pekko.projection.testkit.javadsl.TestSourceProvider;
import org.junit.ClassRule;
import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.projection.testkit.javadsl.ProjectionTestKit;

// #testkit-import

// #testkit-duration
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// #testkit-duration

// #testkit-assertion-import
import static org.junit.Assert.assertEquals;

// #testkit-assertion-import

// #testkit-testprojection

import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.projection.testkit.javadsl.TestProjection;

// #testkit-testprojection

public class TestKitDocExample {

  static class CartView {
    final String id;

    CartView(String id) {
      this.id = id;
    }
  }

  static class CartCheckoutRepository {
    public CompletionStage<CartView> findById(String id) {
      return CompletableFuture.completedFuture(new CartView(id));
    }
  }

  // #testkit
  @ClassRule static final TestKitJunitResource testKit = new TestKitJunitResource();
  ProjectionTestKit projectionTestKit = ProjectionTestKit.create(testKit.system());
  // #testkit

  Projection<String> projection = TestProjection.create(null, null, null);

  CartCheckoutRepository cartCheckoutRepository = new CartCheckoutRepository();

  void illustrateTestKitRun() {
    // #testkit-run
    projectionTestKit.run(
        projection,
        () ->
            cartCheckoutRepository
                .findById("abc-def")
                .toCompletableFuture()
                .get(1, TimeUnit.SECONDS));
    // #testkit-run
  }

  void illustrateTestKitRunWithMaxAndInterval() {
    // #testkit-run-max-interval
    projectionTestKit.run(
        projection,
        Duration.ofSeconds(5),
        Duration.ofMillis(300),
        () ->
            cartCheckoutRepository
                .findById("abc-def")
                .toCompletableFuture()
                .get(1, TimeUnit.SECONDS));
    // #testkit-run-max-interval
  }

  void illustrateTestKitRunWithTestSink() {

    // #testkit-sink-probe
    projectionTestKit.runWithTestSink(
        projection,
        sinkProbe -> {
          sinkProbe.request(1);
          sinkProbe.expectNext(Done.getInstance());
          cartCheckoutRepository.findById("abc-def").toCompletableFuture().get(1, TimeUnit.SECONDS);
        });

    // #testkit-sink-probe
  }

  void illustrateTestKitTestProjection() {
    Handler<Pair<Integer, String>> handler = null;

    // #testkit-testprojection
    List<Pair<Integer, String>> testData =
        Stream.of(Pair.create(0, "abc"), Pair.create(1, "def")).collect(Collectors.toList());

    Source<Pair<Integer, String>, NotUsed> source = Source.from(testData);

    Function<Pair<Integer, String>, Integer> extractOffsetFn =
        (Pair<Integer, String> env) -> env.first();

    TestSourceProvider<Integer, Pair<Integer, String>> sourceProvider =
        TestSourceProvider.create(source, extractOffsetFn);

    Projection<Pair<Integer, String>> projection =
        TestProjection.create(ProjectionId.of("test", "00"), sourceProvider, () -> handler);

    projectionTestKit.run(
        projection,
        () -> {
          // assert logic ...
        });
    // #testkit-testprojection
  }

  // #fixme
  // FIXME: Java example
  // #fixme
}
