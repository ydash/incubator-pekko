/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2015-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.query.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Source

/**
 * A plugin ReadJournal may optionally support this query by implementing this trait.
 */
trait PagedPersistenceIdsQuery extends ReadJournal {

  /**
   * Get the current persistence ids.
   *
   * Not all plugins may support in database paging, and may simply use drop/take Pekko streams operators
   * to manipulate the result set according to the paging parameters.
   *
   * @param afterId The ID to start returning results from, or [[scala.None]] to return all ids. This should be an id
   *                returned from a previous invocation of this command. Callers should not assume that ids are
   *                returned in sorted order.
   * @param limit The maximum results to return. Use Long.MaxValue to return all results. Must be greater than zero.
   * @return A source containing all the persistence ids, limited as specified.
   */
  def currentPersistenceIds(afterId: Option[String], limit: Long): Source[String, NotUsed]
}
