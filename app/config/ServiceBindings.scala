/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services.{DefaultProtectionService, ProtectionService}

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    bindDeps() ++ bindControllers() ++ bindServices() ++ bindConnectors()

  private def bindDeps(): Seq[Binding[_]] = Seq(
    bind(classOf[WSHttp]).to(classOf[DefaultWSHttp]).eagerly()
  )

  private def bindControllers(): Seq[Binding[_]] = Seq(
    bind(classOf[AmendProtectionsController]).to(classOf[DefaultAmendProtectionsController]).eagerly(),
    bind(classOf[CreateProtectionsController]).to(classOf[DefaultCreateProtectionsController]).eagerly(),
    bind(classOf[LookupController]).to(classOf[DefaultLookupController]).eagerly(),
    bind(classOf[ReadProtectionsController]).to(classOf[DefaultReadProtectionsController]).eagerly()
  )

  private def bindServices(): Seq[Binding[_]] = Seq(
    bind(classOf[ProtectionService]).to(classOf[DefaultProtectionService]).eagerly()
  )

  private def bindConnectors(): Seq[Binding[_]] = Seq(
    bind(classOf[AuthClientConnector]).to(classOf[DefaultAuthClientConnector]).eagerly(),
    bind(classOf[CitizenDetailsConnector]).to(classOf[DefaultCitizenDetailsConnector]).eagerly(),
    bind(classOf[NpsConnector]).to(classOf[DefaultNpsConnector]).eagerly()
  )
}
