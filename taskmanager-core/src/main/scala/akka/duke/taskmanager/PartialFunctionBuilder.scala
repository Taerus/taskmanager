package akka.duke.taskmanager

import scala.collection.immutable.Vector


/**
 * A builder class to chain partial functions
 *
 */
class PartialFunctionBuilder[A, B] {

  type PF = PartialFunction[A, B]

  private var pfs: Vector[PF] = Vector.empty

  def +=(pf: PF): Unit =
    pfs = pfs :+ pf

  def result(): PF =
    pfs.foldLeft[PF](Map.empty) { _ orElse _ }
}
