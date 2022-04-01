package gatlingDemostore.pagesObjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Customer{

  val csvFeederLoginDetails = csv("data/loginDetails.csv").circular

  def login = {
    feed(csvFeederLoginDetails)
      .exec(http("Load Login Page")
        .get("/login")
        .check(status.is(200))
        .check(substring("Username:")))
      //here we expected that the customerLoggedIn was false

      .exec(http("Customer Login Action")
        .post("/login")
        .formParam("_csrf", "${crsfValue}")
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .check(status.is(200))
      )
      .exec(session => session.set("customerLoggedIn", true))
    //And here we expected that the customerLoggedIn was true
  }
}
