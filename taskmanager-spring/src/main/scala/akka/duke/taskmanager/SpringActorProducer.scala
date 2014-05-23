package akka.duke.taskmanager

import akka.actor.{Actor, IndirectActorProducer}
import org.springframework.context.ApplicationContext


class SpringActorProducer(actorName: String) extends IndirectActorProducer {

  override def produce(): Actor = {
    Context[ApplicationContext]("springContext").getBean(actorName, classOf[Actor])
  }

  override def actorClass: Class[_ <: Actor] = {
    Context[ApplicationContext]("springContext").getType(actorName).asInstanceOf[Class[_ <: Actor]]
  }

}
