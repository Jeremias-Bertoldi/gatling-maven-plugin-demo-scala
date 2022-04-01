package gatlingDemostore.pagesObjects

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Checkout {
  def viewCart = {
    //doif is a DSL Gatling method
    //this checks if the customerLoggedIn is false and if it is then execute the method
    doIf(session => !session("customerLoggedIn").as[Boolean]) {
      exec(Customer.login)
    }
      .exec(
        http("Load Cart Page")
          .get("/cart/view")
          .check(status.is(200))
        //dollar symbol means get the next value, so, since we have a dollar symbol in the price.
        //we need to put 2 dollar symbols more, the first is getting the dollar symbol and the next the price.
        //.check(xpath("//td[@id='grandTotal']").is("${cartTotal}"))
      )
  }
  def completeCheckout = {
    exec(
      http("Checkout Cart")
        .get("/cart/checkout")
        .check(status.is(200))
        .check(substring("Thanks for your order! See you soon!"))
    )
  }
}
