/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2014-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.scaladsl

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.Future
import org.apache.pekko
import pekko.stream._

object SourceWithContext {

  /**
   * Creates a SourceWithContext from a regular source that operates on a tuple of `(data, context)` elements.
   */
  def fromTuples[Out, CtxOut, Mat](source: Source[(Out, CtxOut), Mat]): SourceWithContext[Out, CtxOut, Mat] =
    new SourceWithContext(source)
}

/**
 * A source that provides operations which automatically propagate the context of an element.
 * Only a subset of common operations from [[FlowOps]] is supported. As an escape hatch you can
 * use [[FlowWithContextOps.via]] to manually provide the context propagation for otherwise unsupported
 * operations.
 *
 * Can be created by calling [[Source.asSourceWithContext]]
 */
final class SourceWithContext[+Out, +Ctx, +Mat] private[stream] (delegate: Source[(Out, Ctx), Mat])
    extends GraphDelegate(delegate)
    with FlowWithContextOps[Out, Ctx, Mat] {
  override type ReprMat[+O, +C, +M] = SourceWithContext[O, C, M @uncheckedVariance]

  override def via[Out2, Ctx2, Mat2](viaFlow: Graph[FlowShape[(Out, Ctx), (Out2, Ctx2)], Mat2]): Repr[Out2, Ctx2] =
    new SourceWithContext(delegate.via(viaFlow))

  override def unsafeDataVia[Out2, Mat2](viaFlow: Graph[FlowShape[Out, Out2], Mat2]): Repr[Out2, Ctx] =
    SourceWithContext.fromTuples(Source.fromGraph(GraphDSL.createGraph(delegate) { implicit b => d =>
      import GraphDSL.Implicits._

      val unzip = b.add(Unzip[Out, Ctx]())
      val zipper = b.add(Zip[Out2, Ctx]())

      d ~> unzip.in

      unzip.out0.via(viaFlow) ~> zipper.in0
      unzip.out1              ~> zipper.in1

      SourceShape(zipper.out)
    }))

  override def viaMat[Out2, Ctx2, Mat2, Mat3](flow: Graph[FlowShape[(Out, Ctx), (Out2, Ctx2)], Mat2])(
      combine: (Mat, Mat2) => Mat3): SourceWithContext[Out2, Ctx2, Mat3] =
    new SourceWithContext(delegate.viaMat(flow)(combine))

  /**
   * Context-preserving variant of [[pekko.stream.scaladsl.Source.withAttributes]].
   *
   * @see [[pekko.stream.scaladsl.Source.withAttributes]]
   */
  override def withAttributes(attr: Attributes): SourceWithContext[Out, Ctx, Mat] =
    new SourceWithContext(delegate.withAttributes(attr))

  /**
   * Context-preserving variant of [[pekko.stream.scaladsl.Source.mapMaterializedValue]].
   *
   * @see [[pekko.stream.scaladsl.Source.mapMaterializedValue]]
   */
  def mapMaterializedValue[Mat2](f: Mat => Mat2): SourceWithContext[Out, Ctx, Mat2] =
    new SourceWithContext(delegate.mapMaterializedValue(f))

  /**
   * Transforms this stream. Works very similarly to [[#mapAsync]] but with an additional
   * partition step before the transform step. The transform function receives the an individual
   * stream entry and the calculated partition value for that entry.
   *
   * @since 1.1.0
   * @see [[#mapAsync]]
   * @see [[#mapAsyncPartitionedUnordered]]
   */
  def mapAsyncPartitioned[T, P](parallelism: Int)(
      extractPartition: Out => P)(f: (Out, P) => Future[T]): SourceWithContext[T, Ctx, Mat] = {
    MapAsyncPartitioned.mapSourceWithContextOrdered(this, parallelism)(extractPartition)(f)
  }

  /**
   * Transforms this stream. Works very similarly to [[#mapAsyncUnordered]] but with an additional
   * partition step before the transform step. The transform function receives the an individual
   * stream entry and the calculated partition value for that entry.
   *
   * @since 1.1.0
   * @see [[#mapAsyncUnordered]]
   * @see [[#mapAsyncPartitioned]]
   */
  def mapAsyncPartitionedUnordered[T, P](parallelism: Int)(
      extractPartition: Out => P)(f: (Out, P) => Future[T]): SourceWithContext[T, Ctx, Mat] = {
    MapAsyncPartitioned.mapSourceWithContextUnordered(this, parallelism)(extractPartition)(f)
  }

  /**
   * Connect this [[pekko.stream.scaladsl.SourceWithContext]] to a [[pekko.stream.scaladsl.Sink]],
   * concatenating the processing steps of both.
   */
  def to[Mat2](sink: Graph[SinkShape[(Out, Ctx)], Mat2]): RunnableGraph[Mat] =
    delegate.toMat(sink)(Keep.left)

  /**
   * Connect this [[pekko.stream.scaladsl.SourceWithContext]] to a [[pekko.stream.scaladsl.Sink]],
   * concatenating the processing steps of both.
   */
  def toMat[Mat2, Mat3](sink: Graph[SinkShape[(Out, Ctx)], Mat2])(combine: (Mat, Mat2) => Mat3): RunnableGraph[Mat3] =
    delegate.toMat(sink)(combine)

  /**
   * Connect this [[pekko.stream.scaladsl.SourceWithContext]] to a [[pekko.stream.scaladsl.Sink]] and run it.
   * The returned value is the materialized value of the `Sink`.
   *
   * Note that the `ActorSystem` can be used as the implicit `materializer` parameter to use the
   * [[pekko.stream.SystemMaterializer]] for running the stream.
   */
  def runWith[Mat2](sink: Graph[SinkShape[(Out, Ctx)], Mat2])(implicit materializer: Materializer): Mat2 =
    delegate.runWith(sink)

  /**
   * Stops automatic context propagation from here and converts this to a regular
   * stream of a pair of (data, context).
   */
  def asSource: Source[(Out, Ctx), Mat] = delegate

  def asJava[JOut >: Out, JCtx >: Ctx, JMat >: Mat]: javadsl.SourceWithContext[JOut, JCtx, JMat] =
    new javadsl.SourceWithContext(this)
}
