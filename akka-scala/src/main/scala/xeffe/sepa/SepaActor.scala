package xeffe.sepa

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import xeffe.sepa.data.{MonitoringFile, Transaction}

trait SepaActorResponse
case object Nope extends SepaActorResponse
case class Yep(monitoringFile: MonitoringFile, transaction: Transaction) extends SepaActorResponse

object SepaActor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case file: MonitoringFile ⇒ (file.transaction.txId, file)
    case tx: Transaction => (tx.txId, tx)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case file: MonitoringFile ⇒ (file.transaction.txId.hashCode % numberOfShards).toString
    case tx: Transaction => (tx.txId.hashCode % numberOfShards).toString
  }
}

class SepaActor extends Actor with ActorLogging {
  import context.become

  override def receive: Receive = receiving(None, None)

  def receiving(monitoringFile: Option[MonitoringFile], transaction: Option[Transaction]): Receive = {

    case file: MonitoringFile =>
      become(completed)

      val response = transaction match {
        case Some(x) => Yep(file, x)
        case None => Nope
      }

      sender() ! response


    case transaction: Transaction =>
      become(completed)

      val response = monitoringFile match {
        case Some(x) => Yep(x, transaction)
        case None => Nope
      }

      sender() ! response
  }

  def completed: Receive = {

    case _: MonitoringFile =>
      sender ! Nope

    case _: Transaction =>
      sender ! Nope
  }
}
