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

import config.WSHttp
import util.ResponseHandler
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, _}
import model.HttpResponseDetails

import scala.concurrent.{ExecutionContext, Future}

object NpsConnector extends NpsConnector with ServicesConfig {

  override val serviceUrl = baseUrl("nps")
  override def http = WSHttp

  override val serviceAccessToken = getConfString("nps.accessToken", "")
  override val serviceEnvironment = getConfString("nps.environment", "")


}
trait NpsConnector {

  // add addtional headers
  implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Content-Type" -> "application/json")
  def http: HttpGet with HttpPost with HttpPut
  val serviceUrl: String
  def url(path: String): String = s"$serviceUrl$path"
  private def ninoWithoutSuffix(nino: String): String = nino.substring(0, 8)

  val serviceAccessToken: String
  val serviceEnvironment: String

  implicit val readApiResponse: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = NpsResponseHandler.handleNpsResponse(method, url, response)
  }

  def applyForProtection(nino: String, body: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = url(  s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix(nino)}/protection")
//    val requestJson: JsValue = Json.parse("""{"protectionType":1}""")

    val responseFut = post(requestUrl, body, hc)

    responseFut.map { response =>
      HttpResponseDetails(response.status, JsSuccess(response.json.as[JsObject]))
    }
  }

  def npsRequestHeaderCarrier(hc: HeaderCarrier): HeaderCarrier = {
    (HeaderCarrier())
      .withExtraHeaders("Authorization" -> s"Bearer $serviceAccessToken")
      .withExtraHeaders("Environment" -> serviceEnvironment)
  }

  def post(requestUrl: String, body: JsValue, hc: HeaderCarrier)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val npsHC = npsRequestHeaderCarrier(hc)
    http.POST[JsValue, HttpResponse](requestUrl, body)
  }

}

object NpsResponseHandler extends NpsResponseHandler{

}

trait NpsResponseHandler extends HttpErrorFunctions {
  def handleNpsResponse(method: String, url: String, response: HttpResponse): HttpResponse = {
    response.status match {
      case 409 => response // this is an expected response for this API, so don't throw an exception
      case _ => handleResponse(method, url)(response)
    }
  }
}