package com.offerservice.akka.http.service

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import com.offerservice.akka.http.service.OfferRegistryActor._
import akka.pattern.ask
import akka.util.Timeout

//#offer-routes-class
trait OfferRoutes extends JsonSupport {
  //#offer-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[OfferRoutes])

  // other dependencies that OfferRoutes use
  def offerRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  //#offers-get-post
  //#offers-get-delete   
  lazy val offerRoutes: Route =
    pathPrefix("offers") {
      concat(
        //#offers-get-delete
        pathEnd {
          concat(
            get {
              val offers: Future[Offers] =
                (offerRegistryActor ? GetOffers).mapTo[Offers]
              complete(offers)
            },
            post {
              entity(as[Offer]) { offer =>
                val offerCreated: Future[ActionPerformed] =
                  (offerRegistryActor ? CreateOffer(offer)).mapTo[ActionPerformed]
                onSuccess(offerCreated) { performed =>
                  log.info("Created offer [{}]: {}", offer.name, performed.description)
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        //#offers-get-post
        //#offers-get-delete
        path(Segments(2)) {
          case vendor :: name :: nil => {
            concat(
              get {
                //#retrieve-offer-info
                val maybeOffer: Future[Option[Offer]] =
                  (offerRegistryActor ? GetOffer(vendor, name)).mapTo[Option[Offer]]
                rejectEmptyResponse {
                  complete(maybeOffer)
                }
                //#retrieve-offer-info
              },
              delete {
                //#offers-delete-logic
                val offerDeleted: Future[ActionPerformed] =
                  (offerRegistryActor ? CancelOffer(vendor, name)).mapTo[ActionPerformed]
                onSuccess(offerDeleted) { performed =>
                  log.info("Cancelled offer [{}]: {}", name, performed.description)
                  complete((StatusCodes.OK, performed))
                }
                //#offers-delete-logic
              }
            )
          }
          case _ => reject
        }
      )
      //#offers-get-delete
    }
  //#all-routes
}
