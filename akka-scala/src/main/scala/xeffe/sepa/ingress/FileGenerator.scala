package xeffe.sepa.ingress

import java.util.UUID

import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Source}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroOutlet
import xeffe.sepa.data.{ControlFile, MonitoringFile, Transaction}

import scala.concurrent.duration._

/**
 * Simulates the Monitoring files and Control files received by the system.
 *
 * Monitoring files represent a single Transaction
 * Control files contain a list of 1 or more Transactions
 *
 * Transactions can be received in any order - Monitoring file first or Control file first.
 */
class FileGenerator extends AkkaStreamlet {

  val monitoringFileOutlet = AvroOutlet[MonitoringFile]("monitoring-files")
  val controlFileOutlet = AvroOutlet[ControlFile]("control-files")

  val shape = StreamletShape.withOutlets(monitoringFileOutlet, controlFileOutlet)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    override def runnableGraph(): RunnableGraph[_] =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        // Source
        val transactions = Source.tick(10 seconds, 100 milliseconds, NotUsed)
          .map { _ =>
            val txId = UUID.randomUUID().toString
            Transaction(txId, "", "", 0)
          }

        // Sinks
        val monitoringFilesOut = plainSink(monitoringFileOutlet)
        val controlFilesOut = plainSink(controlFileOutlet)

        // Structure
        val balance = builder.add(Balance[MonitoringFile](2))
        val merge = builder.add(Merge[MonitoringFile](2))
        val broadcast = builder.add(Broadcast[Transaction](2))

        // Flows
        val txToMonitoringFile = Flow[Transaction].map { tx =>
          MonitoringFile(tx, s"$tx.id")
        }
        val delayedMonitoringFile = Flow[MonitoringFile].delay(5 seconds)
        val groupTx = Flow[Transaction].grouped(5)
        val txSeqToControlFile = Flow[Seq[Transaction]].map { txSeq =>
          val ctrlId = UUID.randomUUID().toString
          ControlFile(ctrlId, s"$ctrlId.json", txSeq)
        }

        // The Graph

        transactions ~> broadcast ~> txToMonitoringFile ~> balance ~> delayedMonitoringFile ~> merge ~> monitoringFilesOut
                                                           balance                          ~> merge
                        broadcast ~> groupTx ~> txSeqToControlFile ~> controlFilesOut

        ClosedShape
      })
  }
}
