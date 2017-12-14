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

import model.ProtectionAmendment
import play.api.mvc._
import services.ProtectionService
import play.api.libs.json._
import model.Error
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object AmendProtectionsController extends AmendProtectionsController {
  override val protectionService = ProtectionService
  override def WithCitizenRecordCheck(nino: String)= ProtectionsActions.WithCitizenRecordCheckAction(nino)
}

trait AmendProtectionsController extends NPSResponseHandler {

  def protectionService: ProtectionService
  def WithCitizenRecordCheck(nino:String): ActionBuilder[Request]

  def amendProtection(nino: String, id: String): Action[JsValue] = WithCitizenRecordCheck(nino).async(BodyParsers.parse.json) { implicit request =>

    Try{id.toLong}.map { protectionId =>

        request.body.validate[ProtectionAmendment].fold(
        errors =>
          Future.successful(BadRequest(Json.toJson(Error(message = "body failed validation with errors: " + errors)))),
        amendment =>
          protectionService.amendProtection(nino, protectionId, Json.toJson(amendment).as[JsObject]).map { response =>
          handleNPSSuccess(response)
        }.recover {
          case downstreamError => handleNPSError(downstreamError, "[AmendProtectionsController.amendProtection]")
        }
      )
    }.getOrElse {
      Future.successful(BadRequest(Json.toJson(Error(message = "path parameter 'id' is not an integer: " + id))))
    }
  }
}
