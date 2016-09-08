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

package connectors

import util.NinoHelper
import config.WSHttp
import config.MicroserviceAuditConnector
import events.{NPSBaseLTAEvent,NPSCreateLTAEvent,NPSAmendLTAEvent}
import model.{Error,HttpResponseDetails}

import play.api.libs.json._
import play.api.{LoggerLike, Logger}

import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, _}
import uk.gov.hmrc.play.http.logging.Authorization

import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

object NpsConnector extends NpsConnector with ServicesConfig {

  override val serviceUrl = baseUrl("nps")
  override def http = WSHttp

  override val audit = MicroserviceAuditConnector

  override val serviceAccessToken = getConfString("nps.accessToken", "")
  override val serviceEnvironment = getConfString("nps.environment", "")

}
trait NpsConnector {

  def http: HttpGet with HttpPost with HttpPut
  val serviceUrl: String

  val serviceAccessToken: String
  val serviceEnvironment: String

  val audit: AuditConnector

  // add addtional headers for the NPS request
  def addExtraHeaders(implicit hc: HeaderCarrier): HeaderCarrier = hc.withExtraHeaders(
    "Accept" -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    //"Authorization" -> s"Bearer $serviceAccessToken",
    "Environment" -> serviceEnvironment).copy(authorization = Some(Authorization(s"Bearer $serviceAccessToken")))
  
  def getApplyUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protection"
  }

  def getAmendUrl(nino: String, id: Int): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protection/${id}"
  }

  def getReadUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protections"
  }

  implicit val readApiResponse: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = NpsResponseHandler.handleNpsResponse(method, url, response)
  }

  def applyForProtection(nino: String, body: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getApplyUrl(nino)
    val responseFut = post(requestUrl, body)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { response =>
      val responseBody =  response.json.as[JsObject]
      val auditEvent = new NPSCreateLTAEvent(nino=nino, npsRequestBodyJs=body, npsResponseBodyJs = responseBody, statusCode = response.status, path = requestUrl)
      handleAuditableResponse(nino, response, Some(auditEvent))
    }
  }

  def amendProtection(nino: String,id: Int, body: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getAmendUrl(nino, id)
    val responseFut = post(requestUrl, body)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { response =>
      val auditEvent = new NPSAmendLTAEvent(nino=nino, id = id, npsRequestBodyJs=body, npsResponseBodyJs = response.json.as[JsObject], statusCode = response.status, path = requestUrl)
      handleAuditableResponse(nino, response, Some(auditEvent))
    }
  }

  def handleAuditableResponse(nino: String, response: HttpResponse, auditEvent: Option[NPSBaseLTAEvent])(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponseDetails  =
  {
    val responseBody =  response.json.as[JsObject]
    val httpStatus = response.status

    Logger.debug(s"Created audit event: ${auditEvent.getOrElse("<None>")}")
    auditEvent.foreach { audit.sendEvent(_) }

    // assertion: nino returned in response must be the same as that sent in the request
    val responseNino = responseBody.value.get("nino").map { n => n.as[String] }.getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino == ninoWithoutSuffix) {
      HttpResponseDetails(httpStatus, JsSuccess(responseBody))
    }
    else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      Logger.error(report)
      HttpResponseDetails(400, JsSuccess(Json.toJson(Error(report)).as[JsObject]))
    }
  }

  def post(requestUrl: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.POST[JsValue, HttpResponse](requestUrl, body)
  }

  def readExistingProtections(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getReadUrl(nino)
    val requestTime = DateTimeUtils.now
    val responseFut = get(requestUrl)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { expectedResponse =>
      handleExpectedReadResponse(requestUrl,nino,requestTime,expectedResponse)
    }
  }

  def get(requestUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.GET[HttpResponse](requestUrl)
  }

  def handleExpectedReadResponse(
      requestUrl: String,
      nino: String,
      requestTime: org.joda.time.DateTime,
      response: HttpResponse)(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponseDetails = {

    val responseBody  = response.json.as[JsObject]
    val responseNino =  responseBody.value.get("nino").map { n => n.as[String]}.getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino==ninoWithoutSuffix) {
      HttpResponseDetails(response.status, JsSuccess(responseBody))
    }
    else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      Logger.error(report)
      HttpResponseDetails(400, JsSuccess(Json.toJson(Error(report)).as[JsObject]))
    }
  }

}

object NpsResponseHandler extends NpsResponseHandler

trait NpsResponseHandler extends HttpErrorFunctions {
  def handleNpsResponse(method: String, url: String, response: HttpResponse): HttpResponse = {
    response.status match {
      case 409 => response // this is an expected response for this API, so don't throw an exception
      case _ => handleResponse(method, url)(response)
    }
  }
}