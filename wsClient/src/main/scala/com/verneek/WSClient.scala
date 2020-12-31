package com.verneek

import akka.actor.ActorSystem
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import scala.concurrent.duration._

import scala.concurrent.Future
import akka.actor.Cancellable
// import akka.actor.Status.Success
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext
import scala.util.Random

object WSClient {

  def initClient(
      id: String,
      host: String,
      max: Long
  )(implicit system: ActorSystem) = {
    import system.dispatcher

    // print each incoming strict text message
    val printSink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict =>
          println(message.text)
        case _ =>
        // ignore other message types
      }

    var counter = 0

    val timeStamp = DateTime.now.toIsoDateTimeString()
    val source: Source[Message, NotUsed] =
      Source
        .fromIterator(() => (0L to max).toIterator)
        .map(el =>
          TextMessage.Strict(
            s"""{ "wsId":"$id", "id":$el, "time":"${DateTime.now
              .toIsoDateTimeString()}", "temp":${Random
              .between(96, 105)}, "beats":${Random.between(60, 120)} }"""
          )
        )

    // and it is completed when the stream completes
    val flow: Flow[Message, Message, Future[Done]] =
      Flow.fromSinkAndSourceMat(printSink, source)(Keep.left)

    // upgradeResponse is a Future[WebSocketUpgradeResponse] that
    // completes or fails when the connection succeeds or fails
    // and closed is a Future[Done] representing the stream completion from above
    val (upgradeResponse, closed) =
      Http().singleWebSocketRequest(
        WebSocketRequest(host),
        flow
      )

    val connected = upgradeResponse.map { upgrade =>
      // just like a regular http request we can access response status which is available via upgrade.response.status
      // status code 101 (Switching Protocols) indicates that server support WebSockets
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        throw new RuntimeException(
          s"Connection for $id failed: ${upgrade.response.status}"
        )
      }
    }

    connected.onComplete(println)
    closed
  }

  private def futureToFutureTry[T](f: Future[T])(implicit
      ex: ExecutionContext
  ): Future[Try[T]] =
    f.map(Success(_)).recover { case x => Failure(x) }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher

    val futures = (0 to 10)
      .map(id => initClient(s"$id", "ws://echo.websocket.org", 100))
      .toSeq

    Future
      .sequence(futures.map(futureToFutureTry(_)))
      .onComplete(_ => system.terminate())
    // val seqq = Future.sequence(futures.map(_.transform(e => Success(e))))
    // val seq = Future.sequence(futures.map(_.transform(Success(_))))
  }
}
