package carly.ingestor

import akka.stream.scaladsl.RunnableGraph
import carly.data.CallRecord
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._

class CallRecordEgress extends AkkaStreamlet {
  val inlet: AvroInlet[CallRecord] = AvroInlet[CallRecord]("in")
  val shape: StreamletShape           = StreamletShape.withInlets(inlet)

  val LogLevel = RegExpConfigParameter(
    "log-level",
    "Provide one of the following log levels, debug, info, warning or error",
    "^debug|info|warning|error$",
    Some("debug")
  )

  val MsgPrefix = StringConfigParameter("msg-prefix", "Provide a prefix for the log lines", Some("valid-logger"))

  override def configParameters: Vector[ConfigParameter] = Vector(LogLevel, MsgPrefix)

  override def createLogic: RunnableGraphStreamletLogic = new RunnableGraphStreamletLogic() {
    val logF: String => Unit = streamletConfig.getString(LogLevel.key).toLowerCase match {
      case "debug"   => system.log.debug _
      case "info"    => system.log.info _
      case "warning" => system.log.warning _
      case "error"   => system.log.error _
    }

    val msgPrefix: String = streamletConfig.getString(MsgPrefix.key)

    def log(data: CallRecord): Unit =
      logF(s"$msgPrefix ${data.user}")

    private def flow =
      FlowWithCommittableContext[CallRecord]
        .map { cdr =>
          log(cdr)
          cdr
        }

    def runnableGraph: RunnableGraph[_] = {
      sourceWithOffsetContext(inlet).via(flow).to(committableSink)
    }
  }
}
