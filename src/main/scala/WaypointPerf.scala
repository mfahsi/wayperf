import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.LocalDate
import scala.concurrent.duration._

class WaypointPerf extends Simulation {

  private val httpProtocol = http
    .baseUrl("https://waypointcapital.io")
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/116.0")

  private val headers_1 = Map(
    "Sec-Fetch-Dest" -> "document",
    "Sec-Fetch-Mode" -> "navigate",
    "Sec-Fetch-Site" -> "none",
    "Sec-Fetch-User" -> "?1"
  )

  val csvFeeder = csv("getposts.csv").circular
  val languages = csv("languages.csv").random

  val startDate = LocalDate.parse("2023-07-01")
  val daysToLoop = 10

  val scn = scenario("WaypointLoad")
    .repeat(daysToLoop, "dayCounter") {
      exec(session => {
        val currentDay = startDate.plusDays(session("dayCounter").validate[Int].map(_.toLong).toOption.getOrElse(0L))
        session.set("currentDate", currentDay.toString)
      })
        .feed(csvFeeder)
        .feed(languages)
        .exec(
          http("request_0")
            .get(s"""/wp-json/ai/v1/posts?lang=${"${language}"}&per_page=40&offset=${"${offset}"}&after=${"${currentDate}"}T${"${after}"}&&before=${"${currentDate}"}T${"${before}"}""")
            .headers(headers_1) // Use the provided headers for the HTTP request
        )
        .exec(
          http("request_0")
            .get(s"""/wp-json/ai/v1/posts?lang=english&per_page=40&offset=0&after=${"${currentDate}"}T${"${after}"}&&before=${"${currentDate}"}T${"${before}"}""")
            .headers(headers_1) // Use the provided headers for the HTTP request
        )
    }

  setUp(
    scn.inject(
      rampUsersPerSec(0) to 3 during (20 seconds) // Adjust the ramp-up duration as necessary
    ).protocols(httpProtocol)
  )
}
