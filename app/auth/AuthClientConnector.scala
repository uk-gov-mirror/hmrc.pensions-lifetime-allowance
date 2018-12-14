/*
 * Copyright 2018 HM Revenue & Customs
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

package auth

import config.{RunModeConfig, WSHttp}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.CorePost
import uk.gov.hmrc.play.config.ServicesConfig

// $COVERAGE-OFF$
  object AuthClientConnector extends AuthClientConnectorTrait with RunModeConfig {
    override val serviceUrl: String = baseUrl("auth")
    override def http: CorePost = WSHttp
  }
// $COVERAGE-ON$
  trait AuthClientConnectorTrait extends PlayAuthConnector with ServicesConfig