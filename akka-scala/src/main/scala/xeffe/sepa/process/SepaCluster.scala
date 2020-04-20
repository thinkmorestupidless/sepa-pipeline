package xeffe.sepa.process

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern.ask
import akka.util.Timeout
import cloudflow.akkastream.scaladsl.{FlowWithCommittableContext, RunnableGraphStreamletLogic}
import cloudflow.akkastream.{AkkaStreamlet, Clustering}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import xeffe.sepa.data.{MonitoringFile, ProcessableTransaction, Transaction}
import xeffe.sepa.{SepaActor, SepaActorResponse, Yep}

import scala.concurrent.duration._

class SepaCluster extends AkkaStreamlet with Clustering {

  val monitoringFileInlet = AvroInlet[MonitoringFile]("monitoring-files")
  val transactionInlet = AvroInlet[Transaction]("transactions")

  val out = AvroOutlet[ProcessableTransaction]("out")

  val shape = StreamletShape.withInlets(monitoringFileInlet, transactionInlet).withOutlets(out)

  override def createLogic = new RunnableGraphStreamletLogic() {
    def runnableGraph = sourceWithOffsetContext(monitoringFileInlet).via(monitoringFileFlow).to(committableSink(out))

    val txRegion: ActorRef = ClusterSharding(context.system).start(
      typeName = "SepaTransactions",
      entityProps = Props[SepaActor],
      settings = ClusterShardingSettings(context.system),
      extractEntityId = SepaActor.extractEntityId,
      extractShardId = SepaActor.extractShardId)

    implicit val timeout: Timeout = 3.seconds

    def monitoringFileFlow = FlowWithCommittableContext[MonitoringFile]
      .map(file ⇒ {
        log.info("Processing Monitoring File: " + file.fileName)
        file
      })
      .mapAsync(1)(msg ⇒ (txRegion ? msg).mapTo[SepaActorResponse])
      .map { response =>
        println(response)
        response
      }
      .collect {
        case Yep(_, tx) => ProcessableTransaction(tx)
      }
  }
}
