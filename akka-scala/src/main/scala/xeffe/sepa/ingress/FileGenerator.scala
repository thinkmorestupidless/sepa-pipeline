package xeffe.sepa.ingress

import java.util.UUID

import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Source}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroOutlet
import xeffe.sepa.data.{ControlFile, MonitoringFile, Transaction}

import scala.concurrent.duration._

class FileGenerator extends AkkaStreamlet {

  val monitoringFileOutlet = AvroOutlet[MonitoringFile]("monitoring-files")
  val controlFileOutlet = AvroOutlet[ControlFile]("control-files")

  val shape = StreamletShape.withOutlets(monitoringFileOutlet, controlFileOutlet)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    override def runnableGraph(): RunnableGraph[_] =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val monitoringFilesOut = plainSink(monitoringFileOutlet)
        val controlFilesOut = plainSink(controlFileOutlet)
        val monitoringFileSource =
          Source.tick(10 seconds, 1 second, NotUsed)
            .map { _ =>
              val txId = UUID.randomUUID().toString
              MonitoringFile(Transaction(txId, "", "", 0), s"txId.json")
            }

        monitoringFileSource ~> monitoringFilesOut

        ClosedShape
      })
  }
}
