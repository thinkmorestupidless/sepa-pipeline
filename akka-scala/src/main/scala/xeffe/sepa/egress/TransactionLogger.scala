package xeffe.sepa.egress

import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import xeffe.sepa.data.ProcessableTransaction

class TransactionLogger extends AkkaStreamlet {

  val in = AvroInlet[ProcessableTransaction]("in")

  val shape = StreamletShape(in)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in).to(Sink.foreach(println))
    }
  }
}
