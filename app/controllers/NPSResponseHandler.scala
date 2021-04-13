/*
 * Copyright 2021 HM Revenue & Customs
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

import model.{Error, HttpResponseDetails}
import play.api.{Logger, Logging}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.http.Status._
import uk.gov.hmrc.http._

trait NPSResponseHandler extends Logging {

  private[controllers] def handleNPSError(error : Throwable, errorContext: String): Result = {
    error match {
      case err @ Upstream5xxResponse(errorDetails, SERVICE_UNAVAILABLE, _, _) =>
        logger.error(s"$errorContext $errorDetails", err)
        ServiceUnavailable(errorDetails)
      case err @ Upstream5xxResponse(errorDetails, _, _, _) =>
        logger.error(s"$errorContext $errorDetails", err)
        InternalServerError(errorDetails)
      case err @ Upstream4xxResponse(errorDetails, UNAUTHORIZED, _, _) =>
        logger.error(s"$errorContext $errorDetails", err)
        Unauthorized(errorDetails)
      case err @ Upstream4xxResponse(errorDetails, _, _, _) =>
        logger.error(s"$errorContext $errorDetails", err)
        InternalServerError(errorDetails)
      case badRequest: BadRequestException =>
        logger.error(s"$errorContext ${badRequest.getMessage}", badRequest)
        BadRequest(badRequest.getMessage)
      case e => throw e
    }
  }

  private[controllers] def handleNPSSuccess(response: HttpResponseDetails): Result = {
    response.status match {
      case OK if response.body.isSuccess => Ok(response.body.get)
      case CONFLICT if response.body.isSuccess => Conflict(response.body.get)
      case _ =>
        val responseErrorDetails = if (!response.body.isSuccess) {
          ", but unable to parse the NPS response body"
        } else {
          ", body=" + Json.asciiStringify(response.body.get)
        }
        val error = Json.toJson(Error("NPS request resulted in a response with: HTTP status=" + response.status + responseErrorDetails))
        InternalServerError(error)
    }
  }
}
