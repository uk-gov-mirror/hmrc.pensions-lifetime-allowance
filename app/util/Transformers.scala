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

package util

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Transformers {
  private val protectionTypes = Vector(
    "Unknown", "FP2016", "IP2014", "IP2016", "Primary", "Enhanced", "Fixed", " FP2014"
  )

  private val protectionStatuses = Vector(
    "Unknown", "Open", "Dormant", "Withdrawn", "Expired", "Unsuccessful", "Rejected"
  )

  private def rename(origName: String, newName: String): Reads[JsObject] =
    (__ \ newName).json.copyFrom((__ \ origName).json.pick)

  private def renameIfExists(origName: String, newName: String): Reads[JsObject] =
    rename(origName, newName) orElse Reads.pure(Json.obj())

  private def copyIfExists(name: String): Reads[JsObject] = renameIfExists(name,name)

  private def string2Int(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    (__ \ fieldName).json.update(of[JsString].map(s => JsNumber(lookupTable.indexOf(s.value))))

  private def int2String(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    (__ \ fieldName).json.update(of[JsNumber].map(n => JsString(lookupTable(n.value.toInt))))

  private def int2StringIfExists(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    int2String(fieldName, lookupTable) orElse Reads.pure(Json.obj())

  /**
    * Transform an incoming MDTP API protection application request body Json to a request body for the corresponding
    * outbound NPS API request
    *
    * @param ninoWithoutSuffix the NINO with the suffix character dropped, as per DES API requirements
    * @param mdtpApplicationJson the incoming protecion application request body
    * @return
    */
  def transformApplyRequestBody(ninoWithoutSuffix: String, mdtpApplicationJson: JsObject): JsResult[JsObject] = {

    def mdtpToNpsPensionDebit =
      rename("amount", "pensionDebitEnteredAmount") and
      rename("startDate", "pensionDebitStartDate") reduce

    // arrays are tricky to manipulate in json transformers - reading them as a list and then
    // transforming tach item is easier . Luckily our lists won't be very large so no performance concerns with this.
    def readPensionDebitList = (__ \ "pensionDebits").readNullable[List[JsObject]]
    val mdtpPensionDebits = mdtpApplicationJson.validate(readPensionDebitList)

    val npsPensionDetails = mdtpPensionDebits.fold(
      errors => throw new Exception("Unable to parse pension debits. " + errors),
      pdListOpt => pdListOpt map { pdList => JsArray(pdList map { _.transform(mdtpToNpsPensionDebit).get }) }
    )

    val putPensionDebitsIfExist: Reads[JsObject] = npsPensionDetails.map { pdArray =>
      (__ \ "pensionDebits").json.put { pdArray }
    } getOrElse { Reads.pure(Json.obj()) }

    val npsProtectionFromApplication =
      ((rename("protectionType", "type") andThen string2Int("type", protectionTypes)) and
        renameIfExists("postADayBenefitCrystallisationEvents", "postADayBCE") and 
        copyIfExists("preADayPensionInPayment") and
        copyIfExists("uncrystallisedRights") and
        copyIfExists("nonUKRights") and
        copyIfExists("relevantAmount") reduce)

    // following builds NPS request with nino and protecion object, but wothout pension debits
    val insertNinoAndProtectionObject = __.json.pickBranch(
      (__ \ 'nino).json.put(JsString(ninoWithoutSuffix)) and
      (__ \ 'protection).json.copyFrom((__).json.pick) reduce
    )

    val npsRequestFromApplication =
      (putPensionDebitsIfExist and
      (npsProtectionFromApplication andThen insertNinoAndProtectionObject) reduce)

    mdtpApplicationJson.transform(npsRequestFromApplication)
  }

  /**
    * Transform a received NPS response for an Application into that to be returned to the client of this
    * service.
    *
    * @param ninoSuffix the last character of the NINO associated with the request - needs to be appended to the
    *                   NINO returned by the DES API
    * @param npsResponseJson the json body received from NPS in response to a Create Lifetime Allowance request.
    * @return a Json body for return to the MDTP service client.
    */

  def transformApplyResponseBody(ninoSuffix: Char, npsResponseJson: JsObject): JsResult[JsObject] = {

    def copyToTopLevel(fieldName: String): Reads[JsObject] =
      (__ \ fieldName).json.copyFrom((__ \ "protection" \ fieldName).json.pick)
    def copyToTopLevelIfExists(fieldName: String) = copyToTopLevel(fieldName) orElse Reads.pure(Json.obj())

    def copyProtectionDetailsToTopLevel: Reads[JsObject] =
      ( copyToTopLevelIfExists("id") andThen renameIfExists("id","protectionID") and
        copyToTopLevelIfExists("version") and
        (copyToTopLevel("type") andThen rename("type", "protectionType") andThen int2String("protectionType", protectionTypes)) and
        (copyToTopLevelIfExists("status") andThen int2StringIfExists("status", protectionStatuses)) and
        copyToTopLevelIfExists("relevantAmount") and
        (copyToTopLevelIfExists("postADayBCE") andThen renameIfExists("postADayBCE","postADayBenefitCrystallisationEvents")) and
        copyToTopLevelIfExists("preADayPensionInPayment") and
        copyToTopLevelIfExists("uncrystallisedRights") and
        copyToTopLevelIfExists("nonUKRights") and
        copyToTopLevelIfExists("pensionDebitAmount") and
        copyToTopLevelIfExists("pensionDebitEnteredAmount") and
        copyToTopLevelIfExists("pensionDebitStartDate") and
        copyToTopLevelIfExists("pensionDebitTotalAmount") and
        copyToTopLevelIfExists("protectedAmount") and
        (copyToTopLevelIfExists("notificationID") andThen renameIfExists("notificationID", "notificationId")) and
        copyToTopLevelIfExists("protectionReference")  reduce)

    def readCertificateDateOpt = (__ \ "protection" \ "certificateDate").readNullable[String]
    def readCertificateTimeOpt = (__ \ "protection" \ "certificateTime").readNullable[String]

    // NPS integration layer returns date and time in separate fields, but MDTP API requires a single ISOO601 date/time field.
    // so need to merge the returned data and time fields to create the full field for the MDTP API.
    def iso8601CertDateOpt: Option[String] = {
      val desCertDateJs = npsResponseJson.validate[Option[String]](readCertificateDateOpt)
      val desCertTimeJs = npsResponseJson.validate[Option[String]](readCertificateTimeOpt)
      (desCertDateJs, desCertTimeJs) match {
        case (d: JsSuccess[Option[String]], t: JsSuccess[Option[String]]) if d.value.isDefined && t.value.isDefined =>
          Some(d.value.get + "T" + t.value.get)
        case (d: JsSuccess[Option[String]], _) if d.value.isDefined => Some(d.value.get)
        case _ => None
      }
    }

    val certificateDateOpt=iso8601CertDateOpt

    val toMdtpProtection =
      ((__ \ 'nino).json.update(of[JsString].map { case JsString(s) => JsString(s + ninoSuffix) }) and
        renameIfExists("pensionSchemeAdministratorCheckReference", "psaCheckReference")  and
       copyProtectionDetailsToTopLevel and
      (
        // replace certificate date with full ISO8601 date/time
        copyToTopLevelIfExists("certificateDate")
          andThen (__ \ "certificateDate").json.update(of[JsString].map { s => JsString(iso8601CertDateOpt.get) })
          orElse Reads.pure(Json.obj())
      ) reduce) andThen (__ \ "protection").json.prune

    npsResponseJson.transform(toMdtpProtection)
  }

  /**
    * Transform received response to Read Protections request from NPS into service response
    *
    */
  def transformReadResponseBody(ninoSuffix: Char, npsResponseJson: JsObject): JsResult[JsObject] = {

    def readCertificateDateOpt = (__ \ "certificateDate").readNullable[String]
    def readCertificateTimeOpt = (__ \ "certificateTime").readNullable[String]

    // NPS request mediator DES returns date and time in separate fields, but MDTP API requires a single
    // ISOO601 date/time field.
    // so need to merge the returned data and time fields to create the full field for the MDTP API.
    def iso8601CertDateOpt(protectionJson: JsObject): Option[String] = {
      val desCertDateJs = protectionJson.validate[Option[String]](readCertificateDateOpt)
      val desCertTimeJs = protectionJson.validate[Option[String]](readCertificateTimeOpt)
      (desCertDateJs, desCertTimeJs) match {
        case (d: JsSuccess[Option[String]], t: JsSuccess[Option[String]]) if d.value.isDefined && t.value.isDefined =>
          Some(d.value.get + "T" + t.value.get)
        case (d: JsSuccess[Option[String]], _) if d.value.isDefined => Some(d.value.get)
        case _ => None
      }
    }

    def npsToMdtpProtectionWithoutCertDate =
      rename("id", "protectionID") and
      copyIfExists("version") and
      (renameIfExists("type", "protectionType") andThen int2String("protectionType", protectionTypes)) and
      (copyIfExists("status") andThen int2String("status", protectionStatuses)) and
      copyIfExists("protectedAmount") and
      copyIfExists("relevantAmount") and
      (copyIfExists("postADayBCE") andThen renameIfExists("postADayBCE","postADayBenefitCrystallisationEvents")) and
      copyIfExists("preADayPensionInPayment") and
      copyIfExists("uncrystallisedRights") and
      copyIfExists("nonUKRights") and
      copyIfExists("pensionDebitAmount") and
      copyIfExists("pensionDebitEnteredAmount") and
      copyIfExists("pensionDebitStartDate") and
      copyIfExists("pensionDebitTotalAmount") and
      (copyIfExists("notificationID") andThen renameIfExists("notificationID", "notificationId")) and
      copyIfExists("protectionReference") reduce

    def npsToMdtpProtection(npsProtection: JsObject) = {
      val withoutCertDate = npsProtection.transform(npsToMdtpProtectionWithoutCertDate)
      withoutCertDate.fold(
        errors => throw new Exception("Failed to parse protection received from NPS. " + errors),
        protection => {
          val certificateDateOpt = iso8601CertDateOpt(npsProtection)
          certificateDateOpt map { cdate => protection ++ Json.obj("certificateDate" -> cdate) } getOrElse protection
        }
      )
    }

    // arrays are tricky to manipulate in json transformers - reading them as a list and then
    // transforming tach item is easier . Luckily our lists won't be very large so no performance concerns with this.
    def readPProtectionList = (__ \ "protections").readNullable[List[JsObject]]
    val npsProtectionList = npsResponseJson.validate(readPProtectionList)

    val mdtpProtectionList = npsProtectionList.fold(
      errors => throw new Exception("Unable to parse protection list. " + errors),
      protListOpt => protListOpt map { protList =>
        JsArray(protList map { protJsObj =>
          npsToMdtpProtection(protJsObj)}
        )
      }
    )

    val putProtectionsIfExist: Reads[JsObject] = mdtpProtectionList.map { protArray =>
      (__ \ "lifetimeAllowanceProtections").json.put { protArray }
    } getOrElse { Reads.pure(Json.obj()) }

    val toMdtpResponse =
      ((__ \ 'nino).json.update(of[JsString].map { case JsString(s) => JsString(s + ninoSuffix) }) and
      renameIfExists("pensionSchemeAdministratorCheckReference", "psaCheckReference")  and
      putProtectionsIfExist reduce)

    npsResponseJson.transform(toMdtpResponse)
  }
}
