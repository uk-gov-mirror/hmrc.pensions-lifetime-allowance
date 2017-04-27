/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.NpsConnector
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object LookupController extends LookupController {
  override val npsConnector = NpsConnector
}

trait LookupController extends BaseController {
  val npsConnector: NpsConnector

  def lookup(psaRef: String, ltaRef: String): Action[AnyContent] = Action.async { implicit request =>
    npsConnector.psaLookup(psaRef, ltaRef).map { response =>
      response.status match {
        case OK => Ok(response.json)
        case _ => handleErrorResponse(response)
      }
    }
  }

  private def handleErrorResponse(response: HttpResponse): Result = {
    val error = response.json
    response.status match {
      case BAD_REQUEST => BadRequest(error)
      case INTERNAL_SERVER_ERROR => InternalServerError(error)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(error)
      case UNAUTHORIZED => Unauthorized(error)
      case NOT_FOUND => NotFound(error)
      case _ => InternalServerError(error)
    }
  }
}
