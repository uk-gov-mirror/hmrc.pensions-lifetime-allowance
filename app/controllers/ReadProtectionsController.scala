/*
 * Copyright 2016 HM Revenue & Customs
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

import model.Error
import play.api.mvc._
import play.api.libs.json.Json
import play.api.mvc.Action
import services.ProtectionService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object ReadProtectionsController extends ReadProtectionsController {
  override val protectionService = ProtectionService
  override def WithCitizenRecordCheck(nino: String) = ProtectionsActions.WithCitizenRecordCheckAction(nino)
}

trait ReadProtectionsController extends BaseController {

  def protectionService: ProtectionService
  def WithCitizenRecordCheck(nino: String): ActionBuilder[Request]

  def readExistingProtections(nino: String) = WithCitizenRecordCheck(nino).async { implicit request =>

      protectionService.readExistingProtections(nino) map { response =>
        response.status match {
          case OK if response.body.isSuccess => Ok(response.body.get)
          case _ => {
            //  error response handling
            val responseErrorDetails = if (!response.body.isSuccess) {
              ", but unable to parse the NPS response body"
            } else {
              ", body=" + Json.asciiStringify(response.body.get)
            }
            val error = Json.toJson(Error("NPS request resulted in a response with: HTTP status=" + response.status + responseErrorDetails))
            response.status match {
              case OK => InternalServerError(error)
              case BAD_REQUEST => BadRequest(error)
              case INTERNAL_SERVER_ERROR => InternalServerError(error)
              case SERVICE_UNAVAILABLE => ServiceUnavailable(error)
              case UNAUTHORIZED => Unauthorized(error)
              case _ => InternalServerError(error)
            }
          }
        }
      }
  }
}