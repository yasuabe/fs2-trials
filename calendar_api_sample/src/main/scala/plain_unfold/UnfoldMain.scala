package plain_unfold

import java.time.{LocalDate, Month}

import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.option._
import com.google.api.services.calendar.model.{Event, Events}
import misc.CalendarApiOps._

object UnfoldMain {
  def unfoldr[B, A](f: B => Option[(A, B)], b: B): Stream[A] = f(b) match {
    case Some((a, newB)) => a #:: unfoldr(f, newB)
    case None            => Stream.empty
  }
  def eventStream(f: EventsFunc): Stream[Event] = {
    val g: Option[Events] => Option[Events] = {                    // 前回の f 実行結果にマッチさせる
      case Some(e) if !e.hasNext => None                           // 前回結果があるがトークンがなければ終了
      case prev                  => f(prev >>= (_.nextToken)).some // unfoldr を継続
    }
    unfoldr[Option[Events], Events](g(_).map(e => (e, e.some)), None)
      .flatMap(_.items.toStream)
  }
  def main(args: Array[String]): Unit = {
    import Month._
    val calendarId = "ja.japanese#holiday@group.v.calendar.google.com"
    val start      = LocalDate.of(2000, JANUARY, 1)
    val end        = LocalDate.of(2020, DECEMBER, 31)

    eventStream(calendar.retrieveEvents(calendarId, _.setStartDate(start).setEndDate(end)))
      .take(12)
      .map(e => s"start:${e.getStart}, summary: ${e.getSummary}")
      .foreach(println)
  }
}
