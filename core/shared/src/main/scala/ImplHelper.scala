package react.impls.helper

import react.ReactiveLibrary
import react.ReactiveLibrary._
import cats.syntax.flatMap._


object NonCancelable extends Cancelable {
  override def kill(): Unit = {}

  import language.implicitConversions
  implicit def unitToCancelable(x: Unit): Cancelable = NonCancelable
}

trait DefaultSignalObject {
  self: ReactiveLibrary =>

  final object Signal extends SignalCompanionObject[Signal, TrackDependency] {
    override def Const[A](value: A): Signal[A] = Var(value)

    override def apply[A](fun: (TrackDependency) => A): Signal[A] =
      throw new UnsupportedOperationException(s"library ${self.implementationName} doesn't support Signal.apply")

    override def breakPotentiallyLongComputation()(implicit td: TrackDependency): Unit =
      throw new UnsupportedOperationException()
  }
}

trait DefaultEventObject {
  self: ReactiveLibrary =>

  final object Event extends EventCompanionObject[Event] {
    override def Never: Event[Nothing] = EventSource[Nothing]
  }
}

trait DefaultReassignableVar {
  self: ReactiveLibrary =>
  class ReassignableVar[A] private (constr: Var[Signal[A]]) extends ReassignableVarTrait[A, Signal, TrackDependency] {

    private lazy val self: Signal[A] = unsafeImplicits.signalApplicative.flatten(constr)

    override def update(newValue: A): Unit = constr update (Signal.Const(newValue))

    override def now: A = self.now

    override def observe(f: (A) => Unit): Cancelable = self.observe(f)

    override def subscribe(s: Signal[A]): Unit = constr update s

    override def toSignal: Signal[A] = self

    override def apply()(implicit trackDependency: TrackDependency): A = self()
  }

  override object ReassignableVar extends ReassignableVarCompanionObject[ReassignableVar, Signal] {
    override def apply[A](init: A): ReassignableVar[A] = new ReassignableVar(Var(Signal.Const(init)))

    override def apply[A](init: Signal[A]): ReassignableVar[A] = new ReassignableVar(Var(init))
  }
}

trait ReactiveLibraryImplementationHelper {
  self: ReactiveLibrary =>
  override protected type VolatileHelper = Any
}
