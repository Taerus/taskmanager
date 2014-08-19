package akka.duke.taskmanager.macros

import scala.annotation.StaticAnnotation
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.Context


class SimpleWorker extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro SimpleWorker.simpleWorker_impl
}

object SimpleWorker {

  def simpleWorker_impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def genWorker(classDef: ClassDef): List[Tree] = {
      val ClassDef(mods, name, tparams, Template(parents, self, body)) = classDef

      val strName = name.toString
      val actorName = newTypeName(strName.capitalize + "Worker")
      val callName = newTermName(strName + "Call")
      val actorRef = newTermName("ref" + strName.capitalize)
      val responseMsgName = newTermName(strName.capitalize + "Result")
      val responseProcName = newTermName(strName + "ResponseProcedure")

      val generated = mutable.MutableList.empty[Tree]

      val newParents: List[Tree] = {
        if(parents.map(_.toString()) == List("scala.AnyRef")) {
          List(tq"akka.actor.Actor")
        } else if(parents.map(_.toString()).forall(t => !t.endsWith("Actor"))) {
          parents :+ tq"akka.actor.Actor"
        } else {
          c.warning(c.enclosingPosition, "Your receive method will be overriden")
          parents
        }
      }

      val newBody = mutable.MutableList.empty[Tree]

      var actions = mutable.MutableList.empty[DefDef]
      var reactions = mutable.MutableList.empty[DefDef]

      var routConf: Tree = null

      body.collect {
        case ddef @ DefDef(defMods, defName, defTparams, defvparams, defTpt, defRhs) if defName.toString == "action" =>
          val newDdef = DefDef(defMods, defName, defTparams, List(defvparams.flatten), defTpt, defRhs)
          newBody += newDdef
          actions += ddef

        case ddef @ DefDef(_, defName, _, _, _, _) if defName.toString == "reaction" =>
          reactions += ddef

        case vdef @ ValDef(_, valName, tpt, value) if valName.toString == "routerConfig" =>
          routConf = value

        case tree =>
          newBody += tree
      }

      if(actions.isEmpty) c.abort(c.enclosingPosition, strName + ": action definition is missing")
      else if(actions.size > 1) c.error(c.enclosingPosition, strName + ": mutiple action definitions, one expected")
      val action = actions.head
      if(action.vparamss.isEmpty) c.error(c.enclosingPosition, strName + ": the action definition must have a parameter list")

      if(reactions.size > 1) c.error(c.enclosingPosition, strName + ": mutiple reaction definitions, one expected")
      val reaction = reactions.headOption
      reaction.foreach{ r =>
        if(r.vparamss.isEmpty) c.error(c.enclosingPosition, strName + ": the reaction definition must have a parameter list")
        if(r.vparamss.size > 1) c.error(c.enclosingPosition, strName + ": to many parameter list in the reaction definition, one expected")
      }


      val callReceive: Tree = {
        val paramTypes = tq"(..${action.vparamss.flatten.map(p => c.typeCheck(p).children.head)})"
        val doAction = action.vparamss.flatten.size match {
          case 0 => q"action()"
          case 1 => q"action(params.asInstanceOf[$paramTypes])"
          case _ => q"(action _).tupled(params.asInstanceOf[$paramTypes])"
        }
        val caseClause = if(reaction.isEmpty) {
          cq"params => $doAction"
        } else {
          val responseMsg = reaction.get.vparamss.head.size match {
            case 0 | 1  => q"$responseMsgName"
            case _      => q"$responseMsgName.tupled"
          }
          if(reaction.get.vparamss.head.isEmpty) {
            cq""" params =>
              $doAction
              sender ! $responseMsg()
            """
          } else {
            cq"params => sender ! $responseMsg($doAction)"
          }
        }
        q"""
          def receive = {
            case $caseClause
          }
        """
      }
      newBody += callReceive

      val workerDef: Tree = ClassDef(mods, actorName, tparams, Template(newParents, self, newBody.toList))

      val createWorker: Tree = {
        var props: Tree =  q"akka.actor.Props(new $actorName())"
        if(routConf != null) props = q"$props.withRouter($routConf)"
        q"val $actorRef = actorRefFactory.actorOf($props, $strName)"
      }

      val callMethod: Tree = {
        DefDef(Modifiers(), callName, action.tparams, action.vparamss, tq"scala.Unit", q"""$actorRef ! (..${action.vparamss.flatten.map(_.name)})""")
      }

      generated ++= List(
        workerDef,
        createWorker,
        callMethod
      )

      reaction.foreach { r =>
        val responseMessage: Tree = {
          if(r.vparamss.head.isEmpty) {
            q"case class ${responseMsgName.toTypeName}()"
          } else {
            q"case class ${responseMsgName.toTypeName}(..${r.vparamss.head})"
          }
        }

        val responseFunction: Tree = {
          if(r.vparamss.head.isEmpty) {
            q"def $responseProcName() { ..${r.rhs} }"
          } else {
            q"def $responseProcName(..${r.vparamss.head}) { ..${r.rhs} }"
          }
        }

        val responseReceive: Tree = {
          val caseClause = if(r.vparamss.head.isEmpty) {
            cq"_: ${responseMsgName.toTypeName} => $responseProcName()"
          } else {
            cq"$responseMsgName(..${r.vparamss.head.map(p => Bind(p.name, Ident(nme.WILDCARD)))}) => $responseProcName(..${r.vparamss.head.map(_.name)})"
          }

          q"workerReceive += { case $caseClause }"
        }

        generated ++= List(
          responseMessage,
          responseFunction,
          responseReceive
        )
      }


////    uncomment to see generated code
//      val n = 120
//      val l1 = (n - strName.length)/2 - 1
//      val l2 = n - 2 - l1 - strName.length
//      println("#"*n + "\n#" + " "*l1 + strName + " "*l2 + "#\n" + "#"*n + "\n")
//      println(generated.map(t => show(t)).mkString("\n\n"))
//      println("\n" + "-"*n + "\n")
//      println(generated.map(t => showRaw(t)).mkString("\n\n"))
//      println("\n" + "="*n)
//      println()

      generated.toList
    }

    val inputs = annottees.map(_.tree).toList
    val expandees = inputs match {
      case (classDef: ClassDef) :: _ => genWorker(classDef)
      case _ => inputs
    }

    c.Expr[Any](Block(expandees, Literal(Constant(()))))
  }

}