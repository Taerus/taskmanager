package akka.duke.taskmanager

import akka.actor.Actor


/** An extended actor to chain the receive definitions across inheritance.
  *
  * {{{
  *   class A extends ComposableActor {
  *     receiveBuilder += {
  *       case "Foo" => println("foo")
  *     }
  *   }
  *
  *   class B extends A {
  *     receiveBuilder += {
  *       case "Bar" => println("bar")
  *     }
  *   }
  * }}}
  *
  * is equivalent to :
  * {{{
  *   class A extends Actor {
  *     def receive = {
  *       case "Foo" => println("foo")
  *     }
  *   }
  *
  *   class B extends A {
  *     receive = super.receive orElse {
  *       case "Bar" => println("bar")
  *     }
  *   }
  * }}}
  *
  * and the actor `B` behavior on receive will be :
  * {{{
  *   case "Foo" => println("foo")
  *   case "Bar" => println("bar")
  * }}}
  *
  */
trait ComposableActor extends Actor {

  protected lazy val receiveBuilder = new PartialFunctionBuilder[Any, Unit]

  final def receive = receiveBuilder.result()

  final def become(behavior: PartialFunction[Any, Unit]) {
    context.become(receive orElse behavior)
  }

  final def become(behavior: PartialFunction[Any, Unit], discardOld: Boolean) {
    context.become(receive orElse behavior, discardOld)
  }

  final def unBecome() {
    context.unbecome()
  }

}
