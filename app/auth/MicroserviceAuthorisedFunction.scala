/**
  * Created by ste on 21/12/17.
  */

package auth

import uk.gov.hmrc.auth.core.AuthorisedFunctions

  object MicroserviceAuthorisedFunction extends MicroserviceAuthorisedFunctionTrait {
    def authConnector = AuthClientConnector
}

  trait MicroserviceAuthorisedFunctionTrait extends AuthorisedFunctions {
    override def authConnector : AuthClientConnectorTrait
  }
