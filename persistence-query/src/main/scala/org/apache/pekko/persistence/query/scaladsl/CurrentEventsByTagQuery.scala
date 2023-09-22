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
import pekko.persistence.query.{ EventEnvelope, Offset }
import pekko.stream.scaladsl.Source

/**
 * A plugin may optionally support this query by implementing this trait.
 */
trait CurrentEventsByTagQuery extends ReadJournal {

  /**
   * Same type of query as [[pekko.persistence.query.scaladsl.EventsByTagQuery#eventsByTag EventsByTagQuery#eventsByTag]] but the event stream
   * is completed immediately when it reaches the end of the "result set". Depending
   * on journal implementation, this may mean all events up to when the query is
   * started, or it may include events that are persisted while the query is still
   * streaming results. For eventually consistent stores, it may only include all
   * events up to some point before the query is started.
   */
  def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed]

}
