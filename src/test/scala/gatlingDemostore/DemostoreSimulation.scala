package gatlingDemostore

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import org.checkerframework.checker.units.qual.Length

import gatlingDemostore.pagesObjects._

import scala.util.Random

class DemostoreSimulation extends Simulation {

  val domain = "demostore.gatling.io"

  private val httpProtocol = http
    .baseUrl("http://" + domain)

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  // method that creates a random number to use in session.set
  val rnd = new Random()

  //this method creates a random string to name the new cookie
  def randomString(length: Int): String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  before {
    println(s"Running test with ${userCount} users ")
    println(s"Ramping users over ${rampDuration} seconds ")
    println(s"Total tests duration ${testDuration} seconds ")
  }

  after {
    println("Stress testing complete")
  }

  //FlushCookieJar cleans the cookie jar for each virtual user
  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(session => session.set("cartTotal", 0.0))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
    //remember to remove println statements before executing load test
    //but it is useful when you are debugging, we can take it out later

  val scn = scenario("DemostoreSimulation")
    .exec(initSession)
    .exec(CmsPages.homepage)
    .pause(2)

    .exec(CmsPages.aboutUs)
    .pause(2)

    .exec(Catalog.Category.view)
    .pause(2)

    .exec(Catalog.Product.add)
    .pause(2)

    .exec(Checkout.viewCart)
    .pause(2)

    .exec(Checkout.completeCheckout)

  object UserJourneys {
    def minPause = 100.milliseconds
    def maxPause = 500.milliseconds

    def browseStore = {
      exec(initSession)
      .exec(CmsPages.homepage)
        .pause(maxPause)
        .exec(CmsPages.aboutUs)
        //passing 2 parameters Gatling generates a random number between them
        .pause(minPause, maxPause)
        .repeat(5) {
          exec(Catalog.Category.view)
            .pause(minPause, maxPause)
            .exec(Catalog.Product.view)
        }
    }
    def abandonCart = {
      exec(initSession)
        .exec(CmsPages.homepage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
    }

    def completePurchase = {
      exec(initSession)
        .exec(CmsPages.homepage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
        .pause(minPause, maxPause)
        .exec(Checkout.viewCart)
        .pause(minPause, maxPause)
        .exec(Checkout.completeCheckout)
    }
  }

  object Scenarios {
    def default = scenario("Default Load Test")
      .during(testDuration.seconds) {
        //this will randomly select a user journey, based on the probabilities that we define
        randomSwitch(
          // 75% of probabilities to execute the browseStore scenario
          //15% of probabilities to execute the abandonCart scenario
          //15% of probabilities to execute the completePurchase scenario
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
      }

    def highPurchase = scenario("High Purchase Load Test")
      .during(60.seconds) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
      }
  }

	setUp(
    Scenarios.default
      .inject(rampUsers(userCount) during(rampDuration.seconds))
      .protocols(httpProtocol),
    Scenarios.highPurchase
          .inject(rampUsers(5) during(5.seconds)).protocols(httpProtocol)
  )
}
