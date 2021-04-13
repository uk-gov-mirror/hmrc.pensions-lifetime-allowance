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

package connectors

import events.{NPSAmendLTAEvent, NPSBaseLTAEvent, NPSCreateLTAEvent}

import javax.inject.Inject
import model.{Error, HttpResponseDetails}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.{Configuration, Environment, Logging, Mode}
import util.NinoHelper
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class DefaultNpsConnector @Inject()(val http: DefaultHttpClient,
                                    environment: Environment,
                                    val runModeConfiguration: Configuration,
                                    servicesConfig: ServicesConfig,
                                    val audit: AuditConnector) extends NpsConnector {
  override lazy val serviceUrl: String = servicesConfig.baseUrl("nps")
  override lazy val serviceAccessToken: String = servicesConfig.getConfString("nps.accessToken", "")
  override lazy val serviceEnvironment: String = servicesConfig.getConfString("nps.environment", "")

  val mode: Mode = environment.mode
}

trait NpsConnector extends Logging {
  val http: DefaultHttpClient
  val serviceUrl: String
  val serviceAccessToken: String
  val serviceEnvironment: String
  val audit: AuditConnector

  def addExtraHeaders(implicit hc: HeaderCarrier): HeaderCarrier = hc.withExtraHeaders(
    "Accept" -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "Environment" -> serviceEnvironment).copy(authorization = Some(Authorization(s"Bearer $serviceAccessToken")))

  def getApplyUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protection"
  }

  def getAmendUrl(nino: String, id: Long): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protections/${id}"
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
      val responseBody = response.json.as[JsObject]
      val auditEvent = new NPSCreateLTAEvent(nino = nino, npsRequestBodyJs = body, npsResponseBodyJs = responseBody, statusCode = response.status, path = requestUrl)
      handleAuditableResponse(nino, response, Some(auditEvent))
    }
  }

  def amendProtection(nino: String, id: Long, body: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getAmendUrl(nino, id)
    val responseFut = put(requestUrl, body)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { response =>
      val auditEvent = new NPSAmendLTAEvent(nino = nino, id = id, npsRequestBodyJs = body, npsResponseBodyJs = response.json.as[JsObject], statusCode = response.status, path = requestUrl)
      handleAuditableResponse(nino, response, Some(auditEvent))
    }
  }

  def getPSALookup(psaRef: String, ltaRef: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val requestUrl = s"$serviceUrl/pensions-lifetime-allowance/scheme-administrator/certificate-lookup?pensionSchemeAdministratorCheckReference=$psaRef&lifetimeAllowanceReference=$ltaRef"
    get(requestUrl)(addExtraHeaders, ec).map(r => r)
  }

  def handleAuditableResponse(nino: String, response: HttpResponse, auditEvent: Option[NPSBaseLTAEvent])(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponseDetails = {
    val responseBody = response.json.as[JsObject]
    val httpStatus = response.status

    logger.debug(s"Created audit event: ${auditEvent.getOrElse("<None>")}")
    auditEvent.foreach {
      audit.sendEvent(_)
    }

    // assertion: nino returned in response must be the same as that sent in the request
    val responseNino = responseBody.value.get("nino").map { n => n.as[String] }.getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino == ninoWithoutSuffix) {
      HttpResponseDetails(httpStatus, JsSuccess(responseBody))
    }
    else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      logger.warn(report)
      HttpResponseDetails(400, JsSuccess(Json.toJson(Error(report)).as[JsObject]))
    }
  }

  def post(requestUrl: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.POST[JsValue, HttpResponse](requestUrl, body)
  }

  def put(requestUrl: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.PUT[JsValue, HttpResponse](requestUrl, body)
  }

  def readExistingProtections(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getReadUrl(nino)
    val responseFut = get(requestUrl)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { expectedResponse =>
      handleExpectedReadResponse(nino, expectedResponse)
    }
  }

  def get(requestUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.GET[HttpResponse](requestUrl)
  }

  def handleExpectedReadResponse(nino: String, response: HttpResponse): HttpResponseDetails = {

    val responseBody = response.json.as[JsObject]
    val responseNino = responseBody.value.get("nino").map { n => n.as[String] }.getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino == ninoWithoutSuffix) {
      HttpResponseDetails(response.status, JsSuccess(responseBody))
    }
    else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      logger.warn(report)
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
