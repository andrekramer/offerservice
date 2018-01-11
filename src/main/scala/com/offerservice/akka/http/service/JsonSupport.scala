package com.offerservice.akka.http.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.offerservice.akka.http.service.OfferRegistryActor.ActionPerformed
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val offerJsonFormat = jsonFormat6(Offer)
  implicit val offersJsonFormat = jsonFormat1(Offers)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
