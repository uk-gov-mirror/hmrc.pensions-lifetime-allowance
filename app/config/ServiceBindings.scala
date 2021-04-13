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

package config

import auth.{AuthClientConnector, DefaultAuthClientConnector}
import connectors.{CitizenDetailsConnector, DefaultCitizenDetailsConnector, DefaultNpsConnector, NpsConnector}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import services.{DefaultProtectionService, ProtectionService}

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    bindServices() ++ bindConnectors()

  private def bindServices(): Seq[Binding[_]] = Seq(
    play.api.inject.bind(classOf[ProtectionService]).to(classOf[DefaultProtectionService]).eagerly()
  )

  private def bindConnectors(): Seq[Binding[_]] = Seq(
    play.api.inject.bind(classOf[AuthClientConnector]).to(classOf[DefaultAuthClientConnector]).eagerly(),
    play.api.inject.bind(classOf[CitizenDetailsConnector]).to(classOf[DefaultCitizenDetailsConnector]).eagerly(),
    play.api.inject.bind(classOf[NpsConnector]).to(classOf[DefaultNpsConnector]).eagerly()
  )
}
