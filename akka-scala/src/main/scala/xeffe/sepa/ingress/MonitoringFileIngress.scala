package xeffe.sepa.ingress

import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import xeffe.sepa.data.MonitoringFile

class MonitoringFileIngress extends AkkaStreamlet {

  val in = AvroInlet[MonitoringFile]("in")
  val out = AvroOutlet[MonitoringFile]("out")

  val shape = StreamletShape(in, out)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in).to(plainSink(out))
    }
  }
}
