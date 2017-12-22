/**
  * Created by ste on 21/12/17.
  */

package auth

import config.WSHttp
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.CorePost
import uk.gov.hmrc.play.config.ServicesConfig


  object AuthClientConnector extends AuthClientConnectorTrait {
    override val serviceUrl: String = baseUrl("auth")
    override def http: CorePost = WSHttp
  }

  trait AuthClientConnectorTrait extends PlayAuthConnector with ServicesConfig