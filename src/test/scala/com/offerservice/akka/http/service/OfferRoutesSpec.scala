package com.offerservice.akka.http.service

//#test-top
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

//#set-up
class OfferRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with OfferRoutes {
  //#test-top

  override val offerRegistryActor: ActorRef =
    system.actorOf(OfferRegistryActor.props, "offerRegistry")

  lazy val routes = offerRoutes

  def nowPlus10Mins = System.currentTimeMillis + 10 * 60 * 1000

  val anExpiredTime = 0
  val aValidVendor = "acme"

  //#set-up

  //#actual-test
  "OfferRoutes" should {
    "return no offer if no present (GET /offers)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/offers")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"offers":[]}""")
      }
    }
    //#actual-test

    //#testing-post
    "be able to add offers (POST /offers)" in {
      val offer = Offer(aValidVendor, "MyWidget", "The best widget ever", 9.99, "USD", nowPlus10Mins)
      val offerEntity = Marshal(offer).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/offers").withEntity(offerEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Offer MyWidget created."}""")
      }
    }
    //#testing-post

    "be able to cancel offers (DELETE /offers)" in {

      val request = Delete(uri = s"/offers/${aValidVendor}/MyWidget")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"Offer MyWidget cancelled."}""")
      }
    }

    //#testing-post
    "still be able to add expired offers (POST /offers)" in {
      val offer = Offer(aValidVendor, "MyExpiredWidget", "This widget is expired", 9.99, "USD", anExpiredTime)
      val offerEntity = Marshal(offer).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/offers").withEntity(offerEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Offer MyExpiredWidget created."}""")
      }
    }
    //#testing-post
    "but not get cancelled response for expired offers (DELETE /offers)" in {

      val request = Delete(uri = s"/offers/${aValidVendor}/MyExpiredWidget")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"Offer MyExpiredWidget not found or expired."}""")
      }
    }
    //#actual-test
  }
  //#actual-test

  //#set-up
}
//#set-up
