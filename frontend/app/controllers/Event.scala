package controllers

import actions.AnyMemberTierRequest
import com.gu.membership.salesforce.Member
import com.gu.membership.util.Timing
import model.Eventbrite.EBEvent
import model.{TicketSaleDates, Eventbrite, EventPortfolio, PageInfo}
import monitoring.EventbriteMetrics
import org.joda.time.Instant

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.Functions._
import actions.Fallbacks.notYetAMemberOn
import services.{MemberService, EventbriteService}
import configuration.{Config, CopyConfig}

import com.netaporter.uri.dsl._
import views.support.Dates._

trait Event extends Controller {

  val eventService: EventbriteService
  val memberService: MemberService

  val BuyAction = NoCacheAction andThen metricRecord(EventbriteMetrics, "buy-action-invoked") andThen authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  def details(id: String) = CachedAction { implicit request =>
    eventService.getEvent(id).map { event =>
      // get the largest available image (for social media etc)
      // this mirrors the URL structure generated by Imager.js
      // so any changes there need to be reflected here
      def eventImageFullPath(id: String): String = {
        val largestWidth = Config.eventImageWidths.toArray.last
        val largestRatio = Config.eventImageRatios.toArray.last
        Config.eventImageUrlPath(id) + "/" + largestWidth + "-" + largestRatio + "x.jpg"
      }
      val pageInfo = PageInfo(
        event.name.text,
        request.path,
        Some(event.venue.name.getOrElse("") + ", " + event.eventAddressLine + " - " + prettyDateWithTime(event.start)),
        Some(eventImageFullPath(event.id))
      )
      Ok(views.html.event.page(event, pageInfo))
    }.getOrElse(NotFound)
  }

  def list = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.list(eventService.getEventPortfolio, pageInfo))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.list(EventPortfolio(Seq.empty, eventService.getEventsTagged(tag)), pageInfo))
  }

  def buy(id: String) = BuyAction.async { implicit request =>
    eventService.getEvent(id).map { event =>
      if(memberCanBuyTicket(event, request.member)) redirectToEventbrite(request, event)
      else Future.successful(Redirect(routes.TierController.change()))
    }.getOrElse(Future.successful(NotFound))
  }

  private def memberCanBuyTicket(event: Eventbrite.EBEvent, member: Member): Boolean = {
    event.ticketClassesHead.exists { ticket =>
      val ticketSaleDates = TicketSaleDates.datesFor(event, ticket)
      ticketSaleDates.datesByTier(member.tier).isBefore(Instant.now())
    }
  }

  private def redirectToEventbrite(request: AnyMemberTierRequest[AnyContent], event: EBEvent): Future[Result] = {
    Timing.record(EventbriteMetrics, "user-sent-to-eventbrite") {
      for {
        discountOpt <- memberService.createDiscountForMember(request.member, event)
      } yield Found(event.url ? ("discount" -> discountOpt.map(_.code)))
    }
  }

  def thankyou(id: String, orderIdOpt: Option[String]) = MemberAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- eventService.getEvent(id)
      } yield {
        eventService.getOrder(oid).map { order =>
          Ok(views.html.event.thankyou(event, order))
        }
      }

      resultOpt.getOrElse(Future.successful(NotFound))
    } { orderId =>
      Future.successful(Redirect(routes.Event.thankyou(id, None)).flashing("oid" -> orderId))
    }
  }
}

object Event extends Event {
  val eventService = EventbriteService
  val memberService = MemberService
}
