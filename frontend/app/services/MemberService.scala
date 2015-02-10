package services

import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.gu.membership.util.Timing
import com.typesafe.scalalogging.slf4j.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._

import model.Eventbrite.EBCode
import model.RichEvent._
import model.Stripe.Customer
import model.{IdMinimalUser, IdUser, PaidTierPlan, ProductRatePlan}
import monitoring.MemberMetrics
import play.api.libs.json.{JsObject, Json}
import services.EventbriteService._
import tracking.{EventSubject, SingleEvent, EventTracking}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

class FrontendMemberRepository(salesforceConfig: SalesforceConfig) extends MemberRepository {
  val metrics = new MemberMetrics(salesforceConfig.envName)

  val salesforce = new Scalaforce {
    val consumerKey = salesforceConfig.consumerKey
    val consumerSecret = salesforceConfig.consumerSecret

    val apiURL = salesforceConfig.apiURL.toString
    val apiUsername = salesforceConfig.apiUsername
    val apiPassword = salesforceConfig.apiPassword
    val apiToken = salesforceConfig.apiToken

    val stage = Config.stage
    val application = "Frontend"

    val authTask = ScheduledTask("", Authentication("", ""), 0.seconds, 30.minutes)(getAuthentication)

    def authentication: Authentication = authTask.get()
  }
}

trait MemberService extends LazyLogging with EventTracking {
  def initialData(user: IdUser, formData: JoinForm) = {
    Seq(Json.obj(
      Keys.EMAIL -> user.primaryEmailAddress,
      Keys.FIRST_NAME -> formData.name.first,
      Keys.LAST_NAME -> formData.name.last,
      Keys.MAILING_STREET -> formData.deliveryAddress.line,
      Keys.MAILING_CITY -> formData.deliveryAddress.town,
      Keys.MAILING_STATE -> formData.deliveryAddress.countyOrState,
      Keys.MAILING_POSTCODE -> formData.deliveryAddress.postCode,
      Keys.MAILING_COUNTRY -> formData.deliveryAddress.country.alpha2,
      Keys.ALLOW_MEMBERSHIP_MAIL -> true
    )) ++ Map(
      Keys.ALLOW_THIRD_PARTY_EMAIL -> formData.marketingChoices.thirdParty,
      Keys.ALLOW_GU_RELATED_MAIL -> formData.marketingChoices.gnm
    ).collect { case (k, Some(v)) => Json.obj(k -> v) }
  }.reduce(_ ++ _)

  def memberData(plan: ProductRatePlan, customerOpt: Option[Customer]) = Json.obj(
    Keys.TIER -> plan.salesforceTier
  ) ++ customerOpt.map { customer =>
    Json.obj(
      Keys.CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Json.obj())

  def createMember(user: IdMinimalUser, formData: JoinForm, identityRequest: IdentityRequest): Future[MemberId] = {
    val touchpointBackend = TouchpointBackend.forUser(user)
    val identityService = IdentityService(IdentityApi)

    Timing.record(touchpointBackend.memberRepository.metrics, "createMember") {
      def futureCustomerOpt = formData match {
        case paid: PaidMemberJoinForm => touchpointBackend.stripeService.Customer.create(user.id, paid.payment.token).map(Some(_))
        case _ => Future.successful(None)
      }

      formData.password.map(identityService.updateUserPassword(_, identityRequest, user.id))

      for {
        fullUser <- identityService.getFullUserDetails(user, identityRequest)
        customerOpt <- futureCustomerOpt
        userData = initialData(fullUser, formData)
        memberId <- touchpointBackend.memberRepository.upsert(user.id, userData)
        subscription <- touchpointBackend.subscriptionService.createSubscription(memberId, formData, customerOpt)

        // Set some fields once subscription has been successful
        updatedMember <- touchpointBackend.memberRepository.upsert(user.id, memberData(formData.plan, customerOpt))
      } yield {
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        touchpointBackend.memberRepository.metrics.putSignUp(formData.plan)
        trackRegistration(formData, memberId, user)
        memberId
      }
    }.andThen {
      case Success(memberAccount) => logger.debug(s"createMember() success user=${user.id} memberAccount=$memberAccount")
      case Failure(error) => {
        logger.error(s"Error in createMember() user=${user.id}", error)
        touchpointBackend.memberRepository.metrics.putFailSignUp(formData.plan)
      }
    }
  }

  def createDiscountForMember(member: Member, event: RichEvent): Future[Option[EBCode]] = {
    member.tier match {
      case Tier.Partner | Tier.Patron if event.hasMemberTicket =>
          // Add a "salt" to make access codes different to discount codes
          val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
          event.service.createOrGetAccessCode(event, code, event.memberTickets).map(Some(_))
      case _ => Future.successful(None)
    }
  }

  // TODO: this currently only handles free -> paid
  def upgradeSubscription(member: FreeMember, user: IdMinimalUser, newTier: Tier, form: PaidMemberChangeForm, identityRequest: IdentityRequest): Future[MemberId] = {
    val touchpointBackend = TouchpointBackend.forUser(user)
    val newPaidPlan = PaidTierPlan(newTier, form.payment.annual)
    for {
      customer <- touchpointBackend.stripeService.Customer.create(user.id, form.payment.token)
      paymentResult <- touchpointBackend.subscriptionService.createPaymentMethod(member, customer)
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(member, newPaidPlan)
      memberId <- touchpointBackend.memberRepository.upsert(member.identityId, memberData(newPaidPlan, Some(customer)))
    } yield {
      IdentityService(IdentityApi).updateUserFieldsBasedOnUpgrade(user, form, identityRequest)
      touchpointBackend.memberRepository.metrics.putUpgrade(newTier)

      trackEvent(
        SingleEvent(
          eventSource = "membershipUpgrade",
          member = member,
          deliveryPostcode = Some(form.deliveryAddress.postCode),
          billingPostcode = form.billingAddress.map(_.postCode),
          subscriptionPaymentAnnual = Some(form.payment.annual),
          marketingChoices = None)
      )
      memberId
    }
  }

  private def trackRegistration(formData: JoinForm, member: MemberId, user: IdMinimalUser) {
    val subscriptionPaymentAnnual = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm => Some(paidMemberJoinForm.payment.annual)
      case _ => None
    }

    val billingPostcode = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm => paidMemberJoinForm.billingAddress.map(_.postCode).orElse(Some(formData.deliveryAddress.postCode))
      case _ => None
    }

    val eventSubject =
      EventSubject(
        member.salesforceContactId,
        user.id,
        formData.plan.salesforceTier,
        Some(formData.deliveryAddress.postCode),
        billingPostcode, subscriptionPaymentAnnual,
        Some(formData.marketingChoices)
    )

    trackEvent(SingleEvent("membershipRegistration", eventSubject))
  }
}

object MemberService extends MemberService
