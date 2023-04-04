/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2020-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.projection.internal

import org.apache.pekko.annotation.InternalApi

/**
 * INTERNAL API
 */
@InternalApi
private[projection] trait InternalProjection {

  /**
   * INTERNAL API
   */
  @InternalApi
  private[projection] def offsetStrategy: OffsetStrategy
}
