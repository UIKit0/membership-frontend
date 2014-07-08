package actions

import scala.concurrent.Future

import play.api.mvc.{Request, ActionBuilder}
import play.api.mvc.Results.Forbidden
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.{MemberService, AuthenticationService}
import controllers.NoCache
import play.api.mvc.Result

trait MemberAction extends ActionBuilder[MemberRequest] {
  val authService: AuthenticationService

  def invokeBlock[A](request: Request[A], block: MemberRequest[A] => Future[Result]) = {
    val result = for {
      authRequest <- authService.authenticatedRequestFor(request)
      member <- MemberService.get(authRequest.user.id)
    } yield {
      block(MemberRequest[A](request, member, authRequest.user))
    }

    result.getOrElse(Future.successful(Forbidden)).map(NoCache(_))
  }
}

object MemberAction extends MemberAction {
  val authService = AuthenticationService
}

