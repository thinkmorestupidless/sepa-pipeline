package xeffe.sepa.process

import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.clusterstreamlet.AkkaClusterStreamlet
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import xeffe.sepa.data.{ControlFile, MonitoringFile}

class TransactionProcessor extends AkkaClusterStreamlet {

  val monitoringFileInlet = AvroInlet[MonitoringFile]("monitoring-in")
  val transactionInlet = AvroInlet[ControlFile]("transaction-in")

  val shape = StreamletShape.withInlets(monitoringFileInlet, transactionInlet)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(monitoringFileInlet).to(Sink.ignore)
    }
  }
}
