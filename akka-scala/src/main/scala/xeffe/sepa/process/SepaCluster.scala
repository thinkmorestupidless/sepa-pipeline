package xeffe.sepa.process

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.kafka.ConsumerMessage.Committable
import akka.pattern.ask
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, Merge, RunnableGraph}
import akka.util.Timeout
import cloudflow.akkastream.scaladsl.{FlowWithCommittableContext, RunnableGraphStreamletLogic}
import cloudflow.akkastream.{AkkaStreamlet, Clustering}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import xeffe.sepa.data.{MonitoringFile, ProcessableTransaction, Transaction}

import scala.concurrent.duration._

class SepaCluster extends AkkaStreamlet with Clustering {

  val monitoringFileInlet = AvroInlet[MonitoringFile]("monitoring-files")
  val transactionInlet = AvroInlet[Transaction]("transactions")

  val out = AvroOutlet[ProcessableTransaction]("out")

  val shape = StreamletShape.withInlets(monitoringFileInlet, transactionInlet).withOutlets(out)

  override def createLogic = new RunnableGraphStreamletLogic() {

    system.logConfiguration()

    override def runnableGraph(): RunnableGraph[_] =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val monitoringFileSource = sourceWithOffsetContext(monitoringFileInlet)
        val transactionSource = sourceWithOffsetContext(transactionInlet)
        val merge = builder.add(Merge[(ProcessableTransaction, Committable)](2))
        val sink = committableSink(out)

        monitoringFileSource ~> monitoringFileFlow ~> merge ~> sink
        transactionSource    ~> transactionFlow    ~> merge

        ClosedShape
      })

    val txRegion: ActorRef = ClusterSharding(context.system).start(
      typeName = "SepaTransactions",
      entityProps = Props[SepaActor],
      settings = ClusterShardingSettings(context.system),
      extractEntityId = SepaActor.extractEntityId,
      extractShardId = SepaActor.extractShardId)

    implicit val timeout: Timeout = 3.seconds

    def transactionFlow = FlowWithCommittableContext[Transaction]
      .map( tx => {
        log.info(s"Processing Transaction: ${tx.txId}")
        tx
      })
      .mapAsync(1)(msg ⇒ (txRegion ? msg).mapTo[SepaActorResponse])
      .collect {
        case Yep(_, tx) => ProcessableTransaction(tx)
      }

    def monitoringFileFlow = FlowWithCommittableContext[MonitoringFile]
      .map(file ⇒ {
        log.info("Processing Monitoring File: " + file.fileName)
        file
      })
      .mapAsync(1)(msg ⇒ (txRegion ? msg).mapTo[SepaActorResponse])
      .collect {
        case Yep(_, tx) => ProcessableTransaction(tx)
      }
  }
}
