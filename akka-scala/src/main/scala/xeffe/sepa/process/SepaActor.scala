package xeffe.sepa.process

import akka.actor.{ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import xeffe.sepa.data.{MonitoringFile, Transaction}

trait SepaActorResponse
case object Nope extends SepaActorResponse
case class Yep(monitoringFile: MonitoringFile, transaction: Transaction) extends SepaActorResponse

case class MonitoringFileAdded(file: MonitoringFile)
case class TransactionAdded(tx: Transaction)

case class SepaState(monitoringFile: Option[MonitoringFile], transaction: Option[Transaction]) {
  def isComplete() =
    monitoringFile.isDefined && transaction.isDefined
}

object SepaState {
  def apply(): SepaState =
    SepaState(None, None)
}

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

class SepaActor extends PersistentActor with ActorLogging {

  override def journalPluginId = "jdbc-journal"

  var state: SepaState = SepaState()

  def reply(state: SepaState, replyTo: ActorRef): Unit = {
    if (state.isComplete()) {
      replyTo ! Yep(state.monitoringFile.get, state.transaction.get)
    } else {
      replyTo ! Nope
    }
  }

  override def receiveRecover: Receive = {

    case fileAdded: MonitoringFileAdded =>
      state = state.copy(monitoringFile = Some(fileAdded.file))

    case txAdded: TransactionAdded =>
      state = state.copy(transaction = Some(txAdded.tx))
  }

  override def receiveCommand: Receive = {

    case file: MonitoringFile =>
      val replyTo = sender()

      state.monitoringFile.fold {
        persist(MonitoringFileAdded(file)) { evt =>
          state = state.copy(monitoringFile = Some(evt.file))
          reply(state, replyTo)
        }
      } { _ =>
        reply(state, replyTo)
      }

    case transaction: Transaction =>
      val replyTo = sender()

      state.transaction.fold {
        persist(TransactionAdded(transaction)) { evt =>
          state = state.copy(transaction = Some(evt.tx))
          reply(state, replyTo)
        }
      } { _ =>
        reply(state, replyTo)
      }
  }

  override def persistenceId: String = self.path.name
}
