package xeffe.sepa.ingress

import akka.stream.scaladsl.{RunnableGraph, Source}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import xeffe.sepa.data.{ControlFile, Transaction}

class ControlFileIngress extends AkkaStreamlet {

  val in = AvroInlet[ControlFile]("in")
  val out = AvroOutlet[Transaction]("out")

  val shape = StreamletShape(in, out)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in)
        .flatMapConcat { control =>
          Source(control.transactions.toList)
        }
        .to(plainSink(out))
    }
  }
}
