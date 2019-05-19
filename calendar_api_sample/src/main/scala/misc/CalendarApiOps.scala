package misc

import java.io.{File, InputStreamReader}
import java.time.LocalDate
import java.util.Collections

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.model.{Event, Events}
import com.google.api.services.calendar.{Calendar, CalendarScopes}

object CalendarApiOps {
  import collection.JavaConverters._

  implicit class EventsOps(val e: Events) extends AnyVal {
    def nextToken: Option[PageToken] = Option(e.getNextPageToken)
    def hasNext:   Boolean        = e.getNextPageToken != null
    def items:     Seq[Event]     = e.getItems.asScala
  }
  implicit class GoogleDateTime(date: LocalDate) {
    import java.time._
    def toGoogleDateTime = new DateTime(date.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli)
  }
  type EventsReq      = Calendar#Events#List
  type EventsModifier = EventsReq => EventsReq
  type PageToken      = String
  type EventsFunc     = Option[PageToken] => Events

  implicit class EventListOps(val e: EventsReq) extends AnyVal {
    def setTokenOrNull(token: Option[PageToken]): EventsReq = e.setPageToken(token.orNull)
    def setStartDate(d: LocalDate): EventsReq = e.setTimeMin(d.toGoogleDateTime)
    def setEndDate(d: LocalDate):   EventsReq = e.setTimeMax(d.toGoogleDateTime)
  }
  private val jacksonFactory = JacksonFactory.getDefaultInstance
  private val httpTransport  = GoogleNetHttpTransport.newTrustedTransport()
  private val scopes         = Collections.singletonList(CalendarScopes.CALENDAR_EVENTS)
  private val appName        = "Google Calendar API with Scala Stream"

  def credential: Credential = {
    val in   = this.getClass.getResourceAsStream("/credentials.json")
    val sec  = GoogleClientSecrets.load(jacksonFactory, new InputStreamReader(in))
    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jacksonFactory, sec, scopes)
      .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
      .setAccessType("offline")
      .build()
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build()

    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }
  val calendar: Calendar = new Calendar.Builder(httpTransport, jacksonFactory, credential)
    .setApplicationName(appName)
    .build()

  implicit class CalendarOps(val cal: Calendar) extends AnyVal {
    def retrieveEvents(calendarId: String, modifier: EventsModifier = identity): EventsFunc =
      maybeToken => {
        println("********* calling API ***********") // 確認用
        modifier(cal.events().list(calendarId))
          .setTokenOrNull(maybeToken)
          .setMaxResults(5)
          .execute()
      }
  }
}
