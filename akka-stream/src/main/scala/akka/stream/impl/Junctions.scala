/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.impl

import akka.stream.impl.StreamLayout.Module
import akka.stream.scaladsl.FlexiRoute.RouteLogic
import akka.stream.scaladsl.FlexiMerge.MergeLogic
import akka.stream.scaladsl.MergePreferred
import akka.stream.{ Attributes, Inlet, Outlet, Shape, InPort, OutPort, UniformFanInShape, UniformFanOutShape, FanOutShape2 }
import akka.event.Logging.simpleName

/**
 * INTERNAL API
 */
private[stream] object Junctions {

  import Attributes._

  sealed trait JunctionModule extends Module {
    override def subModules: Set[Module] = Set.empty

    override def replaceShape(s: Shape): Module =
      if (s.getClass == shape.getClass) this
      else throw new UnsupportedOperationException("cannot change the shape of a " + simpleName(this))
  }

  // note: can't be sealed as we have boilerplate generated classes which must extend FaninModule/FanoutModule
  private[akka] trait FanInModule extends JunctionModule
  private[akka] trait FanOutModule extends JunctionModule

  final case class MergeModule[T](
    shape: UniformFanInShape[T, T],
    override val attributes: Attributes = name("merge")) extends FanInModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = MergeModule(shape.deepCopy(), attributes)
  }

  final case class BroadcastModule[T](
    shape: UniformFanOutShape[T, T],
    eagerCancel: Boolean,
    override val attributes: Attributes = name("broadcast")) extends FanOutModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = BroadcastModule(shape.deepCopy(), eagerCancel, attributes)
  }

  final case class MergePreferredModule[T](
    shape: MergePreferred.MergePreferredShape[T],
    override val attributes: Attributes = name("preferred")) extends FanInModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = MergePreferredModule(shape.deepCopy(), attributes)
  }

  final case class FlexiMergeModule[T, S <: Shape](
    shape: S,
    flexi: S ⇒ MergeLogic[T],
    override val attributes: Attributes) extends FanInModule {

    require(shape.outlets.size == 1, "FlexiMerge can have only one output port")

    override def withAttributes(attributes: Attributes): Module = copy(attributes = attributes)

    override def carbonCopy: Module = FlexiMergeModule(shape.deepCopy().asInstanceOf[S], flexi, attributes)
  }

  final case class FlexiRouteModule[T, S <: Shape](
    shape: S,
    flexi: S ⇒ RouteLogic[T],
    override val attributes: Attributes) extends FanOutModule {

    require(shape.inlets.size == 1, "FlexiRoute can have only one input port")

    override def withAttributes(attributes: Attributes): Module = copy(attributes = attributes)

    override def carbonCopy: Module = FlexiRouteModule(shape.deepCopy().asInstanceOf[S], flexi, attributes)
  }

  final case class BalanceModule[T](
    shape: UniformFanOutShape[T, T],
    waitForAllDownstreams: Boolean,
    override val attributes: Attributes = name("broadcast")) extends FanOutModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = BalanceModule(shape.deepCopy(), waitForAllDownstreams, attributes)
  }

  final case class UnzipModule[A, B](
    shape: FanOutShape2[(A, B), A, B],
    override val attributes: Attributes = name("unzip")) extends FanOutModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = UnzipModule(shape.deepCopy(), attributes)
  }

  final case class ConcatModule[T](
    shape: UniformFanInShape[T, T],
    override val attributes: Attributes = name("concat")) extends FanInModule {

    override def withAttributes(attr: Attributes): Module = copy(attributes = attr)

    override def carbonCopy: Module = ConcatModule(shape.deepCopy(), attributes)
  }

}
