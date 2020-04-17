package xeffe.sepa.process

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.util.Timeout
import cloudflow.akkastream.scaladsl.FlowWithCommittableContext
import cloudflow.clusterstreamlet.RunnableGraphClusterStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import xeffe.sepa.SepaActor
import xeffe.sepa.data.{ControlFile, MonitoringFile, ProcessableTransaction}

import scala.concurrent.duration._

class SepaCluster {

  val monitoringFileInlet = AvroInlet[MonitoringFile]("monitoring-in")
  val transactionInlet = AvroInlet[ControlFile]("transaction-in")

  val out = AvroOutlet[ProcessableTransaction]("out")

  val shape = StreamletShape.withInlets(monitoringFileInlet, transactionInlet)

  override def createLogic = new RunnableGraphClusterStreamletLogic() {
    def runnableGraph = sourceWithOffsetContext(monitoringFileInlet).via(flow).to(committableSink(out))

    val carRegion: ActorRef = ClusterSharding(context.system).start(
      typeName = "Counter",
      entityProps = Props[SepaActor],
      settings = ClusterShardingSettings(context.system),
      extractEntityId = SepaActor.extractEntityId,
      extractShardId = SepaActor.extractShardId)

//    val treeActor = system.actorOf(TreeModelActor.props(true, 8081, carRegion), "tree-actor")

    implicit val timeout: Timeout = 3.seconds
    def flow = FlowWithCommittableContext[ConnectedCarERecord]
      .map(carRecord ⇒ {
        log.info("Stream Has Record ID: " + carRecord.carId)
        carRecord
      })
      .mapAsync(1)(msg ⇒ (carRegion ? msg).mapTo[ConnectedCarERecord])
      .map(carRecord ⇒
        Driver(carRecord.timestamp, carRecord.driver, carRecord.speed, carRecord.status))
  }
}
