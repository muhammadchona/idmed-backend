package mz.org.fgh.sifmoz.backend.packaging


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import mz.org.fgh.sifmoz.backend.episode.Episode
import mz.org.fgh.sifmoz.backend.healthInformationSystem.HealthInformationSystem
import mz.org.fgh.sifmoz.backend.healthInformationSystem.ISystemConfigsService
import mz.org.fgh.sifmoz.backend.healthInformationSystem.SystemConfigs
import mz.org.fgh.sifmoz.backend.healthInformationSystem.SystemConfigsService
import mz.org.fgh.sifmoz.backend.interoperabilityTransation.InteroperabilityTransationService
import mz.org.fgh.sifmoz.backend.interoperabilityTransationLog.InteroperabilityTransationLogService
import mz.org.fgh.sifmoz.backend.openmrsErrorLog.OpenmrsErrorLog
import mz.org.fgh.sifmoz.backend.patient.Patient
import mz.org.fgh.sifmoz.backend.patientIdentifier.PatientServiceIdentifier
import mz.org.fgh.sifmoz.backend.patientUpdateOpenMrsErrorLog.PatientUpdateOpenMrsErrorLog
import mz.org.fgh.sifmoz.backend.patientVisit.PatientVisit
import mz.org.fgh.sifmoz.backend.patientVisitDetails.PatientVisitDetails
import mz.org.fgh.sifmoz.backend.patientVisitDetails.PatientVisitDetailsService
import mz.org.fgh.sifmoz.backend.restUtils.RestOpenMRSClient
import mz.org.fgh.sifmoz.backend.service.ClinicalService
import org.apache.commons.lang.StringUtils
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import java.text.MessageFormat

@Slf4j
@CompileStatic
@EnableScheduling
class RestPackService {

    RestOpenMRSClient restOpenMRSClient = new RestOpenMRSClient()
    PatientVisitDetailsService patientVisitDetailsService
    InteroperabilityTransationService interoperabilityTransationService
    @Autowired
    ISystemConfigsService configsService
    final String requestMethod_POST = "POST"
    final String requestMethod_PUT = "PUT"
    final String requestMethod_PATCH = "PATCH"
    final String requestMethod_GET = "GET"

    final String NO_MEDICATION_ERROR_MESSAGE = "- Dispensa sem Medicamentos\n"
    final String NO_DISPENSE_MODE_ERROR_MESSAGE = "- Dispensa sem Modos de Dispensa\n"
    final String NO_PRESCRIPTION_ERROR_MESSAGE = "- Dispensa sem Prescricao\n"
    final String NO_EPISODE_ERROR_MESSAGE = "- Dispensa sem Historico Clinico\n"
    final String PATIENT_OPENMRS_UUID_MISSING_ERROR_MESSAGE = "- Paciente sem UUID do OpenMRS \n"
    final String PATIENT_OPENMRS_LOCATION_MISSING_ERROR_MESSAGE = "- Paciente sem UUID LOCATION do OpenMRS \n"


    final String NID_DOESNT_EXIST_IN_OPENMRS =  "O NID [{0}] foi alterado no OpenMRS ou não possui UUID." + "Por favor actualize o NID na Administração do Paciente usando a opção Atualizar um Paciente Existente.";

    final String NID_DIFFERENT_IN_OPENMRS =  "O paciente [{0}] \" + \" Tem um UUID [{1}] diferente ou inactivo no OpenMRS \" + nidUuid + \"]. Por favor actualize o UUID correspondente .";

    final String NID_NOT_ACTIVE_IN_OPENMRS =  " O NID {0} com o uuid ({1})]  não se encontra no estado ACTIVO NO PROGRAMA/TRANSFERIDO DE. \" +\" ou contem o UUID inactivo/inexistente. Actualize primeiro o estado do paciente no OpenMRS..";

    //final String INVALID_PROVIDER =  "O UUID DO PROVEDOR NAO CONTEM O PADRAO RECOMENDADO OU NAO EXISTE NO OPENMRS.";

    final String INVALID_LOCATION =  " A UNIDADE SANITARIA (UUID) NAO EXISTE OU NAO CONTEM O PADRAO RECOMENDADO";

    static final String ACTIVEMQ_DISPENSE_QUEUE = "dispensation.queue"

    //final String GET_PROVIDER = "provider?q=";
    final String GET_PATIENT = "patient?q=";
    final String GET_REPORTING_REST = "?personUuid=";
    final String GET_LOCATION = "location/";
    static lazyInit = false

//    final String DISPENSA_IDMED_OPENMRS_ATIVO = true

    @Scheduled(fixedDelay = 120000L) // 2 minutos após terminar
    void schedulerRequestRunning() {
        if (configsService.getRotineStatus('DISPENSA_IDMED_OPENMRS_ATIVO')) {
            Pack.withTransaction {
                println " - REST DISPENSE FROM IDMED TO OPENMRS - " + new Date()
                List<Pack> packList = Pack.findAllWhere(syncStatus: 'R' as char)
                for (Pack pack : packList) {
                    try {
                        RestOpenMRSClient restPost = new RestOpenMRSClient()
                        PatientVisitDetails patientVisitDetails = patientVisitDetailsService.getByPack(pack)
                        PatientVisit patientVisit = PatientVisit.get(patientVisitDetails.patientVisit.id)
                        Patient patient = Patient.get(patientVisit.patient.id)
                        Episode episode = patientVisitDetails?.episode
                        PatientServiceIdentifier patientServiceIdentifier = episode?.patientServiceIdentifier
                        ClinicalService clinicalService = patientServiceIdentifier?.service

                        if (patient.his == null || clinicalService?.code?.equals('PREP') || episode?.startStopReason?.code?.equals("TRANSITO")) {
                            pack.setSyncStatus('N' as char)
                            pack.save(flush: true)
                            return
                        }

                        HealthInformationSystem his = HealthInformationSystem.get(patient.his.id)
                        if (pack.providerUuid == null) {
                            String providerUuid = his.interoperabilityAttributes.find { it.interoperabilityType.code == "OPENMRS_USER_PROVIDER_UUID" }.value
                            pack.providerUuid = providerUuid
                        }
                        String urlBase = his.interoperabilityAttributes.find { it.interoperabilityType.code == "URL_BASE" }.value
                        String userProviderUUid = his.interoperabilityAttributes.find { it.interoperabilityType.code == "OPENMRS_USER_PROVIDER_UUID" }.value
                        String urlBaseReportingRest = his.interoperabilityAttributes.find { it.interoperabilityType.code == "URL_BASE_REPORTING_REST" }.value
                        String openMRSUuuidLocation = his.interoperabilityAttributes.find { it.interoperabilityType.code == "OPENMRS_LOCATION_UUID" }.value
                        String patientNid = StringUtils.replace(patientServiceIdentifier.value, " ", "%20")
//                    String convertToJson = restPost.createOpenMRSDispense(pack, patient)
//                    interoperabilityTransationService.sendMessageToPOC(patientNid, convertToJson, ACTIVEMQ_DISPENSE_QUEUE)

                        String nidUuid = fetchNidUuid(patient, pack, patientVisitDetails, patientServiceIdentifier, patientNid, urlBase, userProviderUUid)
                        if (!isValidNidUuid(patient, pack, patientVisitDetails, nidUuid, patientServiceIdentifier)) return

                        if (!isPatientActiveInProgram(patient, pack, patientVisitDetails, patientServiceIdentifier, urlBaseReportingRest, userProviderUUid)) return

                        if (!isValidLocationUuid(pack, patientVisitDetails, urlBase, openMRSUuuidLocation)) return

                        String convertToJson = restPost.createOpenMRSDispense(pack, patient)
                        postDispenseData(pack, patient, convertToJson, patientVisitDetails, urlBase, userProviderUUid, restPost)
                    } catch (Exception e) {
                        e.printStackTrace()
                    } finally {
                        continue
                    }
                }
            }
        }
    }

    def saveErrorLog(Pack pack,  PatientVisitDetails patientVisitDetails, Patient patient, String errorResponse, String jSONRequest) {
        try {
            OpenmrsErrorLog errorLog = new OpenmrsErrorLog()
            errorLog.beforeInsert()
            Episode episode = Episode.get(patientVisitDetails.episode.id)
            PatientServiceIdentifier identifier = PatientServiceIdentifier.get(episode.patientServiceIdentifier.id)
            ClinicalService service = ClinicalService.get(identifier.service.id)
            OpenmrsErrorLog openmrsLogExists = OpenmrsErrorLog.findWhere(patientVisitDetails: patientVisitDetails.id)
            def messageError = findPossibleErrorMessage(pack, patientVisitDetails, patient)

            errorLog.patient = patient.id
            errorLog.nid = identifier.value
            errorLog.servicoClinico = service.description
            errorLog.patientVisitDetails = patientVisitDetails.id
            errorLog.pickupDate = pack.pickupDate
            errorLog.returnPickupDate = null
            errorLog.errorDescription = errorResponse.concat(messageError)
            errorLog.jsonRequest = jSONRequest

            if(pack.nextPickUpDate != null){
                errorLog.returnPickupDate = pack.nextPickUpDate
            }

            if (openmrsLogExists == null)   {
                errorLog.validate()
                    errorLog.save(flush:true)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    String findPossibleErrorMessage(Pack pack,  PatientVisitDetails patientVisitDetails, Patient patient){

        String error = " ERRO: \n"
        def isPackEmpty = pack?.packagedDrugs?.isEmpty()
        def isDispenseModeEmpty = pack?.dispenseMode?.openmrsUuid == null || pack?.dispenseMode?.openmrsUuid?.isEmpty()
        def isPatientOpenMRSUUIDEmpty = patient?.hisUuid == null || patient?.hisUuid?.isEmpty()
        def isPatientOpenMRLocationEmpty = patient?.hisLocation == null || patient?.hisLocation?.isEmpty()
        def isPrescriptionEmpty = patientVisitDetails.prescription == null
        def isEpisodeEmpty = patientVisitDetails.episode == null

        if (isPackEmpty) {
            error = error.concat(NO_MEDICATION_ERROR_MESSAGE)
        }

        if(isDispenseModeEmpty){
            error = error.concat(NO_DISPENSE_MODE_ERROR_MESSAGE)
        }

        if(isPatientOpenMRSUUIDEmpty){
            error = error.concat(PATIENT_OPENMRS_UUID_MISSING_ERROR_MESSAGE)
        }

        if(isPatientOpenMRLocationEmpty){
            error = error.concat(PATIENT_OPENMRS_LOCATION_MISSING_ERROR_MESSAGE)
        }

        if(isPrescriptionEmpty){
            error = error.concat(NO_PRESCRIPTION_ERROR_MESSAGE)
        }

        if(isEpisodeEmpty){
            error = error.concat(NO_EPISODE_ERROR_MESSAGE)
        }

        return error
    }

    def deleteErrorLog(PatientVisitDetails patientVisitDetails) {
        List<OpenmrsErrorLog> patientVisitDetailsinLog = OpenmrsErrorLog.findAllWhere(patientVisitDetails:patientVisitDetails.id)
        patientVisitDetailsinLog.each { it ->
            if (it) {
                it.delete(flush: true)
            }
        }
    }

    String fetchNidUuid(Patient patient, Pack pack, PatientVisitDetails patientVisitDetails,PatientServiceIdentifier patientServiceIdentifier,String patientNid, String urlBase, String userProviderUuid) {
        String urlPath = 'patient?q=' + patientNid
        JSONArray results = new JSONArray()

        String nidRest = new RestOpenMRSClient().getResponseOpenMRSClient(userProviderUuid, null, urlBase, urlPath, requestMethod_GET)
        if (nidRest == null) {
            // saveErrorLog(pack, patientVisitDetails, patient, MessageFormat.format(NID_DOESNT_EXIST_IN_OPENMRS, patientServiceIdentifier.value), null)
            createErrorLog(patient.id,MessageFormat.format(NID_DOESNT_EXIST_IN_OPENMRS, patientServiceIdentifier.value), patientServiceIdentifier.value, patientServiceIdentifier.service.id)
             // return
        }

        if (nidRest != null) {
            JSONObject resultsObject = new JSONObject(nidRest)
            if(resultsObject.containsKey("results"))
                results = resultsObject.getJSONArray("results")
        }
        return results.length() > 0 ? results.getJSONObject(0).getString("uuid") : null
    }

    boolean isValidNidUuid(Patient patient, Pack pack, PatientVisitDetails patientVisitDetails, String nidUuid, PatientServiceIdentifier patientServiceIdentifier) {
        if (nidUuid == null) {
            createErrorLog(patient.id,MessageFormat.format(NID_DOESNT_EXIST_IN_OPENMRS, patientServiceIdentifier.value), patientServiceIdentifier.value, patientServiceIdentifier.service.id)
            //   return false
        }

        if (nidUuid !== null && !patient.getHisUuid().equals(nidUuid)) {
            saveErrorLog(pack, patientVisitDetails, patient, MessageFormat.format(NID_DIFFERENT_IN_OPENMRS, patientServiceIdentifier.value,patient.hisUuid), null)
            return false
        }

        return true
    }

    boolean isPatientActiveInProgram(Patient patient, Pack pack, PatientVisitDetails patientVisitDetails,PatientServiceIdentifier patientServiceIdentifier, String urlBaseReportingRest, String userProviderUuid) {
//        String urlPath = '&provider?q=' + patient.getHisUuid()
        String urlPath = '?personUuid=' + patient.getHisUuid()
        String openMrsReportingRest = new RestOpenMRSClient().getResponseOpenMRSClient(userProviderUuid, null, urlBaseReportingRest, urlPath, requestMethod_GET)

        if(openMrsReportingRest == null)
            return

        JSONObject resultsReportingRest = new JSONObject(openMrsReportingRest)
        JSONArray members = new JSONArray()
        if(resultsReportingRest.containsKey("members"))
            members = resultsReportingRest.getJSONArray("members")

        if (members.length() < 1) {
            saveErrorLog(pack, patientVisitDetails, patient, MessageFormat.format(NID_NOT_ACTIVE_IN_OPENMRS, patientServiceIdentifier.value,patient.hisUuid), null)
            return false
        }

        return true
    }

    /*
    boolean isValidProviderUuid(Pack pack,PatientVisitDetails patientVisitDetails, HealthInformationSystem his, String urlBase, String universalProviderUuid) {
        String urlPath = GET_PROVIDER  + universalProviderUuid
        String response = new RestOpenMRSClient().getResponseOpenMRSClient(universalProviderUuid, null, urlBase, urlPath, requestMethod_GET)
        if (response.length() < 50) {
            saveErrorLog(pack, patientVisitDetails, patientVisitDetails.patientVisit.patient, INVALID_PROVIDER, null)
            return false
        }
        return true
    }
     */

    boolean isValidLocationUuid(Pack pack,PatientVisitDetails patientVisitDetails, String urlBase, String openMRSUuuidLocation) {
        String urlPath = GET_LOCATION  + openMRSUuuidLocation
        String response = new RestOpenMRSClient().getResponseOpenMRSClient(pack.providerUuid, null, urlBase, urlPath, requestMethod_GET)
        if (response?.length() < 50) {
            saveErrorLog(pack, patientVisitDetails, patientVisitDetails.patientVisit.patient, INVALID_LOCATION, null)
            return false
        }
        return true
    }

    void postDispenseData(Pack pack, Patient patient, String convertToJson, PatientVisitDetails patientVisitDetails, String urlBase, String universalProviderUuid, RestOpenMRSClient restPost) {
        String responsePost = restPost.requestOpenMRSClient(pack.providerUuid, convertToJson, urlBase, "encounter", requestMethod_POST)

        if (responsePost.startsWith('-> Green')) {
            pack.setSyncStatus('S' as char)
            pack.save()
            deleteErrorLog(patientVisitDetails)
        } else {
            saveErrorLog(pack, patientVisitDetails, patient, responsePost, convertToJson)
        }
    }

    void createErrorLog(String patientId, String errorDescription, String nid,String clinicalService) {
        def errorLog = new PatientUpdateOpenMrsErrorLog(
                patient: patientId,
                nid: nid,
                errorDescription: errorDescription,
                servicoClinico: clinicalService
        )
        PatientUpdateOpenMrsErrorLog errorLogExists = PatientUpdateOpenMrsErrorLog.findWhere(patient:patientId,errorDescription: errorLog.errorDescription)
        if (errorLogExists == null)   {
            errorLog.beforeInsert()
            errorLog.save(flush: true)
        }
    }
}
