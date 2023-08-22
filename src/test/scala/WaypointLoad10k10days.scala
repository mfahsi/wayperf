import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.Predef._
import io.gatling.core.feeder.{FeederStrategy, SourceFeederBuilder}
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.javaapi.core.FeederBuilder

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.SystemProperties
import examplesimulation.util.TestConfig


class WaypointLoad10k10days extends Simulation with TestConfig {

  private val httpProtocol = http
    .baseUrl("https://waypointcapital.io")
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/116.0")

  private val headers = Map(
    "Sec-Fetch-Dest" -> "document",
    "Sec-Fetch-Mode" -> "navigate",
    "Sec-Fetch-Site" -> "none",
    "Sec-Fetch-User" -> "?1"
  )

  val langFeeder = Array("english", "german", "spanish", "french")
 
  val scn = scenario("WaypointLoad")
    .feed(langFeeder)
    .during(10 days) {
      exec(session => {
        val day = session.startDate
        val afterTime = s"${day}T00:00:00.000"
        val beforeTime = s"${day}T00:00:10.000"
        session.setAll("afterTime" -> afterTime, "beforeTime" -> beforeTime)
      })
      .asLongAs(session => {
        val currTime = session("afterTime").as[String]
        currTime.take(8) == session.startDate.toString
      }) {
        exec(http("fetch_posts")
          .get(s"/wp-json/ai/v1/posts?lang=${"$"}{lang}&per_page=40&offset=0&after=${"$"}{afterTime}&&before=${"$"}{beforeTime}")
          .headers(headers))
          .pause(10)
          .exec(session => {
            val currAfterTime = session("afterTime").as[String]
            val currBeforeTime = session("beforeTime").as[String]
            val updatedAfterTime = increaseTimeBySeconds(currAfterTime, 10)
            val updatedBeforeTime = increaseTimeBySeconds(currBeforeTime, 10)
            session.setAll("afterTime" -> updatedAfterTime, "beforeTime" -> updatedBeforeTime)
          })
      }
    }

  def increaseTimeBySeconds(time: String, seconds: Int): String = {
    val sdf = new java.text.SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS")
    val date = sdf.parse(time)
    val cal = java.util.Calendar.getInstance()
    cal.setTime(date)
    cal.add(java.util.Calendar.SECOND, seconds)
    sdf.format(cal.getTime)
  }

  val injectionSteps = Iterator.iterate(10)(_ * 2).takeWhile(_ <= 50000).map(i => rampUsers(i) during (1 minute)).toList

  setUp(scn.inject(injectionSteps).protocols(httpProtocol))
}

