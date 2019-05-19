package with_fs2

import java.time.{LocalDate, Month}
import java.util.concurrent.Executors

import cats.effect._
import cats.instances.option._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.option._
import com.google.api.services.calendar.model.{Event, Events}
import fs2.{Stream, io, text}
import misc.CalendarApiOps._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

trait Program[F[_]] {
  implicit val SyncF: Sync[F]
  implicit val ContextShiftF: ContextShift[F]

  def blockingEC: Resource[F, ExecutionContextExecutorService] =
    Resource.make(
      SyncF.delay(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2)))
    )(ec => SyncF.delay(ec.shutdown()))

  def eventStream(f: EventsFunc): Stream[F, Event] = {
    val g: Option[Events] => Option[Events] = {
      case Some(e) if !e.hasNext => None
      case prev                  => f(prev >>= (_.nextToken)).some
    }
    Stream.unfold[F, Option[Events], Events](None)(g(_).map(e => (e, e.some)))
          .flatMap(e => Stream(e.items: _*))
  }
  def program(calendarId: String)(modifier: EventsModifier): Stream[F, Unit] =
    Stream.resource(blockingEC) flatMap { ec =>
      eventStream(calendar.retrieveEvents(calendarId, modifier))
        .take(12)
        .map(e => s"start:${e.getStart}, summary: ${e.getSummary}\n")
        .through(text.utf8Encode)
        .through(io.stdout(ec))
    }
}

object FS2main extends IOApp with Program[IO] {
  import Month._
  val SyncF:         Sync[IO]         = Sync[IO]
  val ContextShiftF: ContextShift[IO] = ContextShift[IO]

  val calendarId = "ja.japanese#holiday@group.v.calendar.google.com"
  val start      = LocalDate.of(2000, JANUARY, 1)
  val end        = LocalDate.of(2020, DECEMBER, 31)

  def run(args: List[String]): IO[ExitCode] =
    program(calendarId)(_.setStartDate(start).setEndDate(end))
      .compile
      .drain
      .as(ExitCode.Success)
}

