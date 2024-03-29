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

//#guideProjectionHandler
package docs.guide

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success

import org.apache.pekko
import pekko.Done
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.scaladsl.LoggerOps
import pekko.projection.eventsourced.EventEnvelope
import pekko.projection.scaladsl.Handler
import org.slf4j.LoggerFactory

object ItemPopularityProjectionHandler {
  val LogInterval = 10
}

class ItemPopularityProjectionHandler(tag: String, system: ActorSystem[_], repo: ItemPopularityProjectionRepository)
    extends Handler[EventEnvelope[ShoppingCartEvents.Event]]() {
  import ShoppingCartEvents._

  private var logCounter: Int = 0
  private val log = LoggerFactory.getLogger(getClass)
  private implicit val ec: ExecutionContext = system.executionContext

  /**
   * The Envelope handler to process events.
   */
  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    val processed = envelope.event match {
      case ItemAdded(_, itemId, quantity)                            => repo.update(itemId, quantity)
      case ItemQuantityAdjusted(_, itemId, newQuantity, oldQuantity) => repo.update(itemId, newQuantity - oldQuantity)
      case ItemRemoved(_, itemId, oldQuantity)                       => repo.update(itemId, 0 - oldQuantity)
      case _: CheckedOut                                             => Future.successful(Done) // skip
    }
    processed.onComplete {
      case Success(_) => logItemCount(envelope.event)
      case _          => ()
    }
    processed
  }

  /**
   * Log the popularity of the item in every `ItemEvent` every `LogInterval`.
   */
  private def logItemCount(event: Event): Unit = event match {
    case itemEvent: ItemEvent =>
      logCounter += 1
      if (logCounter == ItemPopularityProjectionHandler.LogInterval) {
        logCounter = 0
        val itemId = itemEvent.itemId
        repo.getItem(itemId).foreach {
          case Some(count) =>
            log.infoN("ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]", tag, itemId, count)
          case None =>
            log.info2("ItemPopularityProjectionHandler({}) item popularity for '{}': [0]", tag, itemId)
        }
      }
    case _ => ()
  }

}
//#guideProjectionHandler
