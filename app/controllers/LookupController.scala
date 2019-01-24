/*
 * Copyright 2019 HM Revenue & Customs
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
import javax.inject.Inject
import model.Error
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class DefaultLookupController @Inject()(val npsConnector: NpsConnector) extends LookupController

trait LookupController extends BaseController with NPSResponseHandler {
  val npsConnector: NpsConnector

  def psaLookup(psaRef: String, ltaRef: String): Action[AnyContent] = Action.async { implicit request =>
    npsConnector.getPSALookup(psaRef, ltaRef).map { response =>
      response.status match {
        case OK => Ok(response.json)
        case _ =>
          val error = Json.toJson(Error(s"NPS request resulted in a response with: HTTP status = ${response.status} body = ${response.json}"))
          Logger.error(error.toString)
          InternalServerError(error)
      }
    }.recover {
      case error => handleNPSError(error, "[LookupController.psaLookup]")
    }
  }
}