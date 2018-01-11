package com.offerservice.akka.http.service

import akka.actor.{ Actor, ActorLogging, Props }
import scala.concurrent.duration._

//#offer-case-classes
// Simplifications made:
// Using Double type for price.
// Vendors can choose to reuse offer names. At least a warning should be given in this case.
// Expires time is given as a Unix timestamp. ISO 8601 would be a better choice.
final case class Offer(vendor: String, name: String, description: String, price: Double, currency: String, expiresAt: Long)
final case class Offers(offers: Seq[Offer])
//#offer-case-classes

object OfferRegistryActor {

  // Details of registering offer vendors (e.g. by adding another "vendor" REST endpoint)
  // and verifying them (thier uniqueness etc) are not addressed.

  final case class ActionPerformed(description: String)
  final case object GetOffers // All vendors. TODO search filters.
  final case class CreateOffer(offer: Offer)
  final case class GetOffer(vendor: String, name: String)
  final case class CancelOffer(vendor: String, name: String)
  final case object ExpungeExpiredOffers

  def props: Props = Props[OfferRegistryActor]
}

class OfferRegistryActor extends Actor with ActorLogging {

  import OfferRegistryActor._

  implicit val executionContext = context.dispatcher

  val expungeTimer = context.system.scheduler.schedule(500 millis, 10 seconds, self, ExpungeExpiredOffers)

  def filter(vendor: String, name: String, currentTime: Long)(offer: Offer): Boolean =
    offer.vendor == vendor && offer.name == name && offer.expiresAt > currentTime

  def receiveProcess(offers: Set[Offer]): Receive = {
    case GetOffers =>
      val currentTime = System.currentTimeMillis
      sender() ! Offers(offers.toSeq.filter(_.expiresAt > currentTime))

    case CreateOffer(offer) =>
      context.become(receiveProcess(offers.filterNot(o => o.vendor == offer.vendor && o.name == offer.name) + offer))
      sender() ! ActionPerformed(s"Offer ${offer.name} created.")

    case GetOffer(vendor, name) =>
      sender() ! offers.find(filter(vendor, name, System.currentTimeMillis) _)

    case CancelOffer(vendor, name) =>
      val offersToRemove = offers.find(filter(vendor, name, System.currentTimeMillis) _)

      context.become(receiveProcess(offers -- offersToRemove))
      sender() ! ActionPerformed(
        if (offersToRemove.isEmpty)
          s"Offer ${name} not found or expired."
        else
          s"Offer ${name} cancelled."
      )
    case ExpungeExpiredOffers =>
      val currentTime = System.currentTimeMillis
      context.become(receiveProcess(offers -- offers.toSeq.filter(_.expiresAt < currentTime)))
  }

  def receive = receiveProcess(Set.empty[Offer])

  override def postStop(): Unit = {
    expungeTimer.cancel()
  }
}
