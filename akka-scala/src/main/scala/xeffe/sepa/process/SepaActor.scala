package xeffe.sepa.process

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

  private val numberOfShards = 10

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

      val response = transaction match {
        case Some(x) =>
          become(completed)
          Yep(file, x)
        case None =>
          become(receiving(Some(file), transaction))
          Nope
      }

      sender() ! response


    case transaction: Transaction =>

      val response = monitoringFile match {
        case Some(x) =>
          become(completed)
          Yep(x, transaction)
        case None =>
          become(receiving(monitoringFile, Some(transaction)))
          Nope
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
