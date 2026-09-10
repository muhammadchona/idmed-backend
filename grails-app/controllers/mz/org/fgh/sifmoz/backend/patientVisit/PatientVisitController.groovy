package mz.org.fgh.sifmoz.backend.patientVisit


import grails.converters.JSON
import grails.gorm.transactions.Transactional
import grails.rest.RestfulController
import grails.validation.ValidationException
import groovy.json.JsonSlurper
import mz.org.fgh.sifmoz.backend.clinic.Clinic
import mz.org.fgh.sifmoz.backend.convertDateUtils.ConvertDateUtils
import mz.org.fgh.sifmoz.backend.drug.Drug
import mz.org.fgh.sifmoz.backend.episode.Episode
import mz.org.fgh.sifmoz.backend.episode.EpisodeService
import mz.org.fgh.sifmoz.backend.episode.IEpisodeService
import mz.org.fgh.sifmoz.backend.healthInformationSystem.SystemConfigs
import mz.org.fgh.sifmoz.backend.interoperabilityTransation.InteroperabilityTransationService
import mz.org.fgh.sifmoz.backend.packagedDrug.PackagedDrug
import mz.org.fgh.sifmoz.backend.packagedDrug.PackagedDrugStock
import mz.org.fgh.sifmoz.backend.packaging.IPackService
import mz.org.fgh.sifmoz.backend.packaging.Pack
import mz.org.fgh.sifmoz.backend.packaging.PackService
import mz.org.fgh.sifmoz.backend.patient.Patient
import mz.org.fgh.sifmoz.backend.patientIdentifier.PatientServiceIdentifier
import mz.org.fgh.sifmoz.backend.patientVisitDetails.IPatientVisitDetailsService
import mz.org.fgh.sifmoz.backend.patientVisitDetails.PatientVisitDetails
import mz.org.fgh.sifmoz.backend.prescription.IPrescriptionService
import mz.org.fgh.sifmoz.backend.prescription.Prescription
import mz.org.fgh.sifmoz.backend.restUtils.RestOpenMRSClient
import mz.org.fgh.sifmoz.backend.screening.*
import mz.org.fgh.sifmoz.backend.startStopReason.StartStopReason
import mz.org.fgh.sifmoz.backend.stock.Stock
import mz.org.fgh.sifmoz.backend.stock.StockService
import mz.org.fgh.sifmoz.backend.utilities.JSONSerializer
import mz.org.fgh.sifmoz.backend.utilities.Utilities
import org.springframework.validation.BindingResult

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT

class PatientVisitController extends RestfulController {

    IPatientVisitService patientVisitService
    IPrescriptionService prescriptionService
    IPackService packService
    VitalSignsScreeningService vitalSignsScreeningService
    TBScreeningService tbScreeningService
    AdherenceScreeningService adherenceScreeningService
    RAMScreeningService ramScreeningService
    PregnancyScreeningService pregnancyScreeningService
    StockService stockService
    IPatientVisitDetailsService patientVisitDetailsService
    InteroperabilityTransationService interoperabilityTransationService
    EpisodeService episodeService
    RestOpenMRSClient restPost = new RestOpenMRSClient()

    static final String ACTIVEMQ_DISPENSE_QUEUE = "dispensation.queue"

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    PatientVisitController() {
        super(PatientVisit)
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        render JSONSerializer.setObjectListJsonResponse(patientVisitService.list(params)) as JSON
    }

    def show(String id) {
        render JSONSerializer.setJsonObjectResponse(patientVisitService.get(id)) as JSON
    }

    def saveRecord(PatientVisit visit) {
        render JSONSerializer.setJsonObjectResponse(visit) as JSON
    }

    @Transactional
    def save() {
        PatientVisit visit = new PatientVisit()
        def objectJSON = request.JSON
        def isTransitPatient = false
        def isNationalTransitPatient = false
        def lastEpisode = null
        def poc_pack = null
        def poc_service = null

        // Mobile synchronization can be retried after a timeout. The visit id
        // is the operation id: once it exists, returning it without reducing
        // stock again prevents duplicate PackagedDrugStock records and saldo.
        if (objectJSON.id && objectJSON.syncStatus == 'R') {
            PatientVisit synchronizedVisit = PatientVisit.get(objectJSON.id.toString())
            if (synchronizedVisit) {
                render JSONSerializer.setJsonObjectResponse(synchronizedVisit) as JSON
                return
            }
        }

        if (!objectJSON?.patientVisitDetails?.isEmpty()) {
            def amtPerTimePackaged = objectJSON?.patientVisitDetails[0]?.pack?.packagedDrugs[0]?.amtPerTime + ""
            objectJSON?.patientVisitDetails[0]?.pack?.packagedDrugs[0]?.amtPerTime = amtPerTimePackaged ? Double.parseDouble(amtPerTimePackaged) : 0
            def amtPerTimePrescribed = objectJSON?.patientVisitDetails[0]?.prescription?.prescribedDrugs[0]?.amtPerTime + ""
            objectJSON?.patientVisitDetails[0]?.prescription?.prescribedDrugs[0]?.amtPerTime = amtPerTimePrescribed ? Double.parseDouble(amtPerTimePrescribed) : 0

        }

        def syncStatus = objectJSON["syncStatus"]
        visit = objectJSON as PatientVisit

        visit.beforeInsert()
        visit.validate()

        configPatientVisitOrigin(visit)

        if (objectJSON.id) {
            visit.id = UUID.fromString(objectJSON.id)

            visit.patientVisitDetails.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.patientVisitDetails[index].id)
                item.origin = visit.origin
                item.prescription.id = UUID.fromString(objectJSON.patientVisitDetails[index].prescription.id)
                Prescription prescriptionCheck = Prescription.findById(item.prescription.id)
                if (prescriptionCheck)
                    item.prescription.origin = prescriptionCheck.origin
                item.prescription.prescribedDrugs.eachWithIndex { item2, index2 ->
                    item2.beforeInsert()
                    item2.id = UUID.fromString(objectJSON.patientVisitDetails[index].prescription.prescribedDrugs[index2].id)
                    if (prescriptionCheck) {
                        item2.origin = prescriptionCheck.origin
                        item2.clinic = prescriptionCheck.clinic
                    } else {
                        item2.origin = item.prescription.origin
                        item2.clinic = item.prescription.clinic
                    }

                }
                item.prescription.prescriptionDetails.eachWithIndex { item3, index3 ->
                    item3.beforeInsert()
                    item3.id = UUID.fromString(objectJSON.patientVisitDetails[index].prescription.prescriptionDetails[index3].id)
                    if (prescriptionCheck) {
                        item3.origin = prescriptionCheck.origin
                        item3.clinic = prescriptionCheck.clinic
                    } else {
                        item3.origin = item.prescription.origin
                        item3.clinic = item.prescription.clinic
                    }
                }
                item.pack.id = UUID.fromString(objectJSON.patientVisitDetails[index].pack.id)
                item.pack.origin = visit.origin
                item.pack.packagedDrugs.eachWithIndex { item4, index4 ->
                    item4.beforeInsert()
                    item4.id = UUID.fromString(objectJSON.patientVisitDetails[index].pack.packagedDrugs[index4].id)
                    item4.clinic = item.clinic
                    item4.origin = visit.origin
                    item4.packagedDrugStocks.eachWithIndex { item5, index5 ->
                        item5.id = UUID.fromString(objectJSON.patientVisitDetails[index].pack.packagedDrugs[index4].packagedDrugStocks[index5].id)
                    }
                }
                if (item.episode.startStopReason.code.equalsIgnoreCase("TRANSITO")) {
                    isTransitPatient = true
                    isNationalTransitPatient = item.episode.isResidentInCountry()
                    lastEpisode = item.episode
                }
            }

            visit.adherenceScreenings.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.adherenceScreenings[index].id)
                item.origin = visit.origin
                item.clinic = visit.clinic
            }
            visit.vitalSignsScreenings.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.vitalSignsScreenings[index].id)
                item.origin = visit.origin
                item.clinic = visit.clinic
            }
            visit.pregnancyScreenings.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.pregnancyScreenings[index].id)
                item.origin = visit.origin
                item.clinic = visit.clinic
            }
            visit.tbScreenings.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.tbScreenings[index].id)
                item.origin = visit.origin
                item.clinic = visit.clinic
            }
            visit.ramScreenings.eachWithIndex { item, index ->
                item.beforeInsert()
                item.id = UUID.fromString(objectJSON.ramScreenings[index].id)
                item.origin = visit.origin
                item.clinic = visit.clinic
            }
        }

        if (visit.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond visit.errors
            return
        }

        try {
            PatientVisit existingPatientVisit = PatientVisit.findByVisitDateAndPatient(visit.visitDate, visit.patient)

            if (existingPatientVisit != null) {
                visit.vitalSignsScreenings.each { item ->
                    item.visit = existingPatientVisit
                    item.origin = existingPatientVisit.origin
                    item.clinic = existingPatientVisit.clinic
                    item.save()
                }
                if (visit.patient.gender.startsWith('F')) {
                    visit.pregnancyScreenings.each { item ->
                        item.visit = existingPatientVisit
                        item.origin = existingPatientVisit.origin
                        item.clinic = existingPatientVisit.clinic
                        item.save()
                    }
                    existingPatientVisit.pregnancyScreenings = visit.pregnancyScreenings
                }
                visit.ramScreenings.each { item ->
                    item.visit = existingPatientVisit
                    item.origin = existingPatientVisit.origin
                    item.clinic = existingPatientVisit.clinic
                    item.save()
                }
                visit.adherenceScreenings.each { item ->
                    item.visit = existingPatientVisit
                    item.origin = existingPatientVisit.origin
                    item.clinic = existingPatientVisit.clinic
                    item.save()
                }
                visit.tbScreenings.each { item ->
                    item.visit = existingPatientVisit
                    item.origin = existingPatientVisit.origin
                    item.clinic = existingPatientVisit.clinic
                    item.save()
                }
                visit.patientVisitDetails.each { item ->
                    item.patientVisit = existingPatientVisit
                    item.origin = existingPatientVisit.origin
                    Prescription existingPrescription = Prescription.findById(item.prescription.id)
                    if (existingPrescription == null) {
                        //  item.prescription.origin = existingPatientVisit.origin
                        incrementPrescriptionSeq(item.prescription, item.episode)
                        prescriptionService.save(item.prescription)
                    }
                    if (!syncStatus) {
                        item.pack.isreferalsynced = true
                        item.pack.origin = existingPatientVisit.origin
                    }
                    packService.save(item.pack)
                    reduceStock(item.pack, syncStatus)
                }
                existingPatientVisit.vitalSignsScreenings = visit.vitalSignsScreenings
                existingPatientVisit.ramScreenings = visit.ramScreenings
                existingPatientVisit.adherenceScreenings = visit.adherenceScreenings
                existingPatientVisit.tbScreenings = visit.tbScreenings
                existingPatientVisit.patientVisitDetails = visit.patientVisitDetails

                visit = existingPatientVisit

            } else {
                visit.patientVisitDetails.each { item ->
                    item.pack.origin = visit.origin

                    //  item.episode.origin = visit.origin
                    Prescription existingPrescription = Prescription.findById(item.prescription.id)
                    if (existingPrescription != null) {
                        item.prescription = existingPrescription
                        // item.prescription.origin = existingPrescription.origin
                    }
                    item.pack.packagedDrugs.each { packagedDrugs ->
                        def clinicalService = item.episode.patientServiceIdentifier.service
                        if (!packagedDrugs.drug.clinical_service_id) {
                            packagedDrugs.origin = item.pack.origin
                            packagedDrugs.drug.clinical_service_id = clinicalService.id
                        }
                    }
                    if (!syncStatus)
                        item.pack.isreferalsynced = true

                    incrementPrescriptionSeq(item.prescription, item.episode)
                    if (item.prescription.photoContentType !== null) {
                        byte[] photoBytes = Base64.getDecoder().decode(item.prescription.photoContentType);
                        item.prescription.photoContentType = item.prescription.photoName.substring(item.prescription.photoName.lastIndexOf('.') + 1)
                        item.prescription.photo = photoBytes
                    }
                    prescriptionService.save(item.prescription)
                    packService.save(item.pack)
                    // if(!syncStatus)
                    reduceStock(item.pack, syncStatus)

                    if (item.episode.startStopReason.code.equalsIgnoreCase("TRANSITO")) {
                        isTransitPatient = true
                        isNationalTransitPatient = item.episode.isResidentInCountry()
                        lastEpisode = item.episode
                    }
                    if(verifyIfNeedToCreateMaintenanceEpisode(item.episode,item.patientVisit.patient)) {
                        def startStopReasonOther = StartStopReason.findByCode(StartStopReason.OUTRO)
                        episodeService.createClosureEpisode(item.episode,item.episode.patientServiceIdentifier, ConvertDateUtils.addMinutes(item.pack.pickupDate,2),startStopReasonOther)
                        def newEpisode = episodeService.createMaintenanceEpisode(item.episode, item.episode.patientServiceIdentifier,item.episode.clinicSector,ConvertDateUtils.addMinutes(item.pack.pickupDate,3))
                        item.episode = newEpisode
                    }
                }
            }
            if (visit.save(flush: true, failOnError: true)){
                if(isTransitPatient)
                    saveExternalPatientVisit(isNationalTransitPatient, visit, lastEpisode, objectJSON)
            }
        } catch (ValidationException e) {
            transactionStatus.setRollbackOnly()
            respond visit.errors
            return
        }

        def patientVisitDetailsJSON = JSONSerializer.setObjectListJsonResponse(visit.patientVisitDetails as List)
        def vitalSignsScreeningsJSON = JSONSerializer.setObjectListJsonResponse(visit.vitalSignsScreenings as List)
        def ramScreeningsJSON = JSONSerializer.setObjectListJsonResponse(visit.ramScreenings as List)
        def adherenceScreeningsJSON = JSONSerializer.setObjectListJsonResponse(visit.adherenceScreenings as List)
        def tbScreeningsJSON = JSONSerializer.setObjectListJsonResponse(visit.tbScreenings as List)
        def pregnancyScreeningsJSON = JSONSerializer.setObjectListJsonResponse(visit.pregnancyScreenings as List)

        def result = JSONSerializer.setJsonObjectResponse(visit)
        if (patientVisitDetailsJSON.length() > 0) {
            result.put('patientVisitDetails', patientVisitDetailsJSON)

        } else
            result.remove('patientVisitDetails')

        if (vitalSignsScreeningsJSON.length() > 0)
            result.put('vitalSignsScreenings', vitalSignsScreeningsJSON)
        else
            result.remove('vitalSignsScreenings')

        if (ramScreeningsJSON.length() > 0)
            result.put('ramScreenings', ramScreeningsJSON)
        else
            result.remove('ramScreenings')

        if (pregnancyScreeningsJSON.length() > 0)
            result.put('pregnancyScreenings', ramScreeningsJSON)
        else
            result.remove('pregnancyScreenings')

        if (adherenceScreeningsJSON.length() > 0)
            result.put('adherenceScreenings', adherenceScreeningsJSON)
        else
            result.remove('adherenceScreenings')

        if (tbScreeningsJSON.length() > 0)
            result.put('tbScreenings', tbScreeningsJSON)
        else
            result.remove('tbScreenings')

        render result as JSON

//        Thread.sleep(1000)
//        visit.refresh()

//        if (visit?.patient?.his !== null) {
//            PatientVisit.withTransaction {
//                PatientVisit patientVisit = PatientVisit.findById(visit.id)
//                String convertToJson = restPost.createPOCDispense(patientVisit)
//                interoperabilityTransationService.sendMessageToPOC(patientVisit?.patientVisitDetails?.first()?.prescription?.id, convertToJson.toString(), ACTIVEMQ_DISPENSE_QUEUE)
//            }
//        }

    }

    @Transactional
    def update() {
        def objectJSON = request.JSON
        PatientVisit patientVisitDB = PatientVisit.get(objectJSON.id)
        def syncStatus = objectJSON["syncStatus"]
        if (patientVisitDB == null) {
            render status: NOT_FOUND
            return
        }
        //updating db object
        patientVisitDB.properties = objectJSON as BindingResult
        List<AdherenceScreening> adherenceScreenings = new ArrayList<>()
        List<VitalSignsScreening> vitalSignsScreenings = new ArrayList<>()
        List<PregnancyScreening> pregnancyScreenings = new ArrayList<>()
        List<TBScreening> tbScreenings = new ArrayList<>()
        List<RAMScreening> ramScreenings = new ArrayList<>()

        patientVisitDB.adherenceScreenings = [].withDefault { new AdherenceScreening() }
        patientVisitDB.tbScreenings = [].withDefault { new TBScreening() }
        patientVisitDB.vitalSignsScreenings = [].withDefault { new VitalSignsScreening() }
        patientVisitDB.pregnancyScreenings = [].withDefault { new PregnancyScreening() }
        patientVisitDB.ramScreenings = [].withDefault { new RAMScreening() }

        configPatientVisitOrigin(patientVisitDB)
        (objectJSON.adherenceScreenings as List).collect { item ->
            if (item) {
                def adherenceScreeningObject = new AdherenceScreening(parseTo(item.toString()) as Map)
                adherenceScreeningObject.id = item.id
                def adherenceScreening = AdherenceScreening.get(adherenceScreeningObject.id)
                if (!adherenceScreening)
                    adherenceScreening = adherenceScreeningObject
                adherenceScreening.hasPatientCameCorrectDate = adherenceScreeningObject.hasPatientCameCorrectDate
                adherenceScreening.daysWithoutMedicine = adherenceScreeningObject.daysWithoutMedicine
                adherenceScreening.patientForgotMedicine = adherenceScreeningObject.patientForgotMedicine
                adherenceScreening.lateDays = adherenceScreeningObject.lateDays
                adherenceScreening.lateMotives = adherenceScreeningObject.lateMotives
                adherenceScreening.visit = patientVisitDB
                adherenceScreening.origin = patientVisitDB.origin
                adherenceScreening.clinic = patientVisitDB.clinic
                adherenceScreenings.add(adherenceScreening)
            }
        }

        (objectJSON.tbScreenings as List).collect { item ->
            if (item) {
                def tbScreeningObject = new TBScreening(parseTo(item.toString()) as Map)
                tbScreeningObject.id = item.id
                def tbScreening = TBScreening.get(tbScreeningObject.id)
                if (!tbScreening)
                    tbScreening = tbScreeningObject
                tbScreening.parentTBTreatment = tbScreeningObject.parentTBTreatment
                tbScreening.cough = tbScreeningObject.cough
                tbScreening.fever = tbScreeningObject.fever
                tbScreening.losingWeight = tbScreeningObject.losingWeight
                tbScreening.treatmentTB = tbScreeningObject.treatmentTB
                tbScreening.treatmentTPI = tbScreeningObject.treatmentTPI
                tbScreening.startTreatmentDate = tbScreeningObject.startTreatmentDate
                tbScreening.fatigueOrTirednessLastTwoWeeks = tbScreeningObject.fatigueOrTirednessLastTwoWeeks
                tbScreening.sweating = tbScreeningObject.sweating
                tbScreening.visit = patientVisitDB
                tbScreening.origin = patientVisitDB.origin
                tbScreening.clinic = patientVisitDB.clinic
                tbScreenings.add(tbScreening)
            }
        }


        (objectJSON.vitalSignsScreenings as List).collect { item ->
            if (item) {
                def vitalSignsScreeningObject = new VitalSignsScreening(parseTo(item.toString()) as Map)
                vitalSignsScreeningObject.id = item.id
                def vitalSignsScreening = VitalSignsScreening.get(vitalSignsScreeningObject.id)
                if (!vitalSignsScreening)
                    vitalSignsScreening = vitalSignsScreeningObject
                vitalSignsScreening.distort = vitalSignsScreeningObject.distort
                vitalSignsScreening.imc = vitalSignsScreeningObject.imc
                vitalSignsScreening.weight = vitalSignsScreeningObject.weight
                vitalSignsScreening.systole = vitalSignsScreeningObject.systole
                vitalSignsScreening.height = vitalSignsScreeningObject.height
                vitalSignsScreening.visit = patientVisitDB
                vitalSignsScreening.origin = patientVisitDB.origin
                vitalSignsScreening.clinic = patientVisitDB.clinic
                vitalSignsScreenings.add(vitalSignsScreening)
            }
        }

        (objectJSON.pregnancyScreenings as List).collect { item ->
            if (item) {
                def pregnancyScreeningObject = new PregnancyScreening(parseTo(item.toString()) as Map)
                pregnancyScreeningObject.id = item.id
                def pregnancyScreening = PregnancyScreening.get(pregnancyScreeningObject.id)
                if (!pregnancyScreening)
                    pregnancyScreening = pregnancyScreeningObject
                pregnancyScreening.pregnant = pregnancyScreeningObject.pregnant
                pregnancyScreening.menstruationLastTwoMonths = pregnancyScreeningObject.menstruationLastTwoMonths
                pregnancyScreening.lastMenstruation = pregnancyScreeningObject.lastMenstruation
                pregnancyScreening.visit = patientVisitDB
                pregnancyScreening.origin = patientVisitDB.origin
                pregnancyScreening.clinic = patientVisitDB.clinic
                pregnancyScreenings.add(pregnancyScreening)
            }
        }

        (objectJSON.ramScreenings as List).collect { item ->
            if (item) {
                def ramScreeningObject = new RAMScreening(parseTo(item.toString()) as Map)
                ramScreeningObject.id = item.id
                def ramScreening = RAMScreening.get(ramScreeningObject.id)
                if (!ramScreening)
                    ramScreening = ramScreeningObject
                ramScreening.adverseReaction = ramScreeningObject.adverseReaction
                ramScreening.adverseReactionMedicine = ramScreeningObject.adverseReactionMedicine
                ramScreening.visit = patientVisitDB
                ramScreening.origin = patientVisitDB.origin
                ramScreening.clinic = patientVisitDB.clinic
                ramScreenings.add(ramScreening)
            }
        }

        patientVisitDB.ramScreenings = ramScreenings
        patientVisitDB.vitalSignsScreenings = vitalSignsScreenings
        patientVisitDB.tbScreenings = tbScreenings
        patientVisitDB.adherenceScreenings = adherenceScreenings
        patientVisitDB.pregnancyScreenings = pregnancyScreenings

        //Only updated pack and packagedDrugs (refazer dispensa)
        patientVisitDB.patientVisitDetails.eachWithIndex { item, index ->
            item.origin = patientVisitDB.origin
            if (item.pack) {
                item.pack.origin = patientVisitDB.origin
                item.pack.packagedDrugs.eachWithIndex { item4, index4 ->
                    item4.id = UUID.fromString(objectJSON.patientVisitDetails[index].pack.packagedDrugs[index4].id)
                    item4.origin = patientVisitDB.origin
                    item4.packagedDrugStocks.eachWithIndex { item5, index5 ->
                        item5.id = UUID.fromString(objectJSON.patientVisitDetails[index].pack.packagedDrugs[index4].packagedDrugStocks[index5].id)
                    }
                }
            }
        }
        if (patientVisitDB.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond visit.errors
            return
        }

        render JSONSerializer.setJsonObjectResponse(patientVisitDB) as JSON
    }

//    @Transactional(rollbackFor = Throwable)
    @Transactional
    def delete(String id) {

        PatientVisit patientVisitDB = PatientVisit.get(id)

        try {
            patientVisitDB.patientVisitDetails.each { item ->
                def packToRemove = item.pack
                def prescriptionToRemove = null
                if (item.pack) {
                    restoreStock(item.pack, 'N')
                    if (PatientVisitDetails.countByPrescription(item.prescription) == 1)
                        prescriptionToRemove = item.prescription
                }
                item.delete()
                packService.delete(packToRemove.id)
                prescriptionToRemove ? prescriptionService.delete(prescriptionToRemove.id) : ''
            }
        } catch (ValidationException e) {
            respond patientVisitDB.errors
            return
        }
        if (patientVisitService.delete(patientVisitDB.id).hasErrors()) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }

    def getAllByClinicId(String clinicId, int offset, int max) {
        render JSONSerializer.setObjectListJsonResponse(patientVisitService.getAllByClinicId(clinicId, offset, max)) as JSON
    }

    def getByPatientId(String patientId) {

        def patient = Patient.get(patientId)
        def patientVisitList = new ArrayList<PatientVisit>()
        def patientVisitToAdd = PatientVisit.createCriteria().list {
            eq('patient', patient)
            isNotEmpty("vitalSignsScreenings")
            maxResults(3)
            order("visitDate", "desc")
        }

        patientVisitList.addAll(patientVisitToAdd as List<PatientVisit>)

        render JSONSerializer.setObjectListJsonResponse(patientVisitList) as JSON
    }

    def getLastVisitOfPatient(String patientId) {
        render JSONSerializer.setJsonObjectResponse(patientVisitService.getLastVisitOfPatient(patientId)) as JSON
    }

    def getAllLastVisitWithScreeningOfClinic(String clinicId, int offset, int max) {
        render JSONSerializer.setObjectListJsonResponse(patientVisitService.getAllLastWithScreening(clinicId, offset, max)) as JSON
    }

    void restoreStock(Pack pack, def syncStatus) {
        if ((pack.syncStatus == 'N' && syncStatus == '') || (pack.syncStatus == 'R' && syncStatus == 'R')) {
            List<PackagedDrug> packagedDrugsDb = PackagedDrug.findAllByPack(pack)
            List<PackagedDrugStock> packagedDrugStocks = PackagedDrugStock.findAllByPackagedDrug(packagedDrugsDb)
            for (PackagedDrugStock packagedDrugStock : packagedDrugStocks) {
                Stock stock = Stock.findById(packagedDrugStock.stock.id)
                stock.stockMoviment += packagedDrugStock.quantitySupplied
                stockService.save(stock)
                packagedDrugStock.delete()
            }
            packagedDrugsDb.each { item ->
                item.delete()
            }
        }
    }

    void reduceStock(Pack pack, def syncStatus) {
        SystemConfigs systemConfigs = SystemConfigs.findByKey("INSTALATION_TYPE")
        if (systemConfigs && systemConfigs.value.equalsIgnoreCase("LOCAL")) {
            if ((pack.syncStatus == 'N' && syncStatus == '') || (pack.syncStatus == 'R')) {
                pack.packagedDrugs.each { pcDrugs ->

                    def quantityControl = pcDrugs.quantitySupplied
                    pcDrugs.packagedDrugStocks = new HashSet<>()
                    while (quantityControl > 0) {
                        PackagedDrugStock packagedDrugStock = new PackagedDrugStock()
                        packagedDrugStock.beforeInsert()
                        Stock stock = Stock.get(getFirstExpiredBatchFromDrug(pcDrugs.drug, pack))

                        if (stock) {
                            packagedDrugStock.stock = stock
                            packagedDrugStock.packagedDrug = pcDrugs
                            packagedDrugStock.drug = pcDrugs.drug

                            if (stock.stockMoviment < 0) {
                                packagedDrugStock.quantitySupplied = 0
                                stock.stockMoviment = 0
                                quantityControl = 0
                            } else if (stock.stockMoviment <= quantityControl) {
                                quantityControl -= stock.stockMoviment
                                packagedDrugStock.quantitySupplied = stock.stockMoviment
                                stock.stockMoviment = 0
                            } else {
                                stock.stockMoviment -= quantityControl
                                packagedDrugStock.quantitySupplied = quantityControl
                                quantityControl = 0
                            }

                            stock.packagedDrugs = []
                            stock.save(flush: true)
                        } else {
                            break
                        }
                        pcDrugs.packagedDrugStocks.add(packagedDrugStock)
                    }
                }
            }
        }
    }

    void incrementPrescriptionSeq(Prescription newPrescription, Episode episode) {
        def random = new Random()
        def patientVisitDetails = patientVisitDetailsService.getLastByEpisodeId(episode.id)
        def sequence = 0
        def newSequence = 10000 + random.nextInt(900000)
        newPrescription.setPrescriptionSeq(episode.patientServiceIdentifier.value + "-" + newSequence)
    }

    private static def parseTo(String jsonString) {
        return new JsonSlurper().parseText(jsonString)
    }

    private static String getFirstExpiredBatchFromDrug(Drug drug, Pack pack) {

        String dispensingClinicId = pack.origin ?: pack.clinic?.id
        Clinic dispensingClinic = dispensingClinicId ? Clinic.findById(dispensingClinicId) : null

        if (!dispensingClinic)
            return null

        def stockList = Stock.
                findAllByDrugAndClinicAndExpireDateGreaterThanEqualsAndExpireDateGreaterThanAndStockMovimentGreaterThan(drug,
                        dispensingClinic,
                        Utilities.addDaysInDate(pack.pickupDate, ConvertDateUtils.getDaysBetween(pack.pickupDate,pack.nextPickUpDate).intValue()),
                        new Date(), 0,
                        [sort: "expireDate", order: "asc"])
        if (!stockList.isEmpty())
            return stockList.first().id

        return null
    }

    def getAllByVisitIds() {
        def objectJSON = request.JSON
        List<String> ids = objectJSON
        render JSONSerializer.setObjectListJsonResponse(PatientVisit.findAllByIdInList(ids)) as JSON
    }

    def getAllLastWithScreeningByPatientIds() {
        def objectJSON = request.JSON
        List<String> patientIds = objectJSON
        render JSONSerializer.setObjectListJsonResponse(patientVisitService.getAllLastWithScreeningByPatientIds(patientIds)) as JSON
    }

    def getAllLast3VisitsWithScreeningByPatientIds() {
        def objectJSON = request.JSON
        List<String> patientIds = objectJSON
        render JSONSerializer.setObjectListJsonResponse(patientVisitService.getAllLast3VisitsWithScreeningByPatientIds(patientIds)) as JSON
    }

    private static PatientVisit configPatientVisitOrigin(PatientVisit patientVisit) {
        SystemConfigs systemConfigs = SystemConfigs.findByKey("INSTALATION_TYPE")
        if (systemConfigs && systemConfigs.value.equalsIgnoreCase("LOCAL") && checkHasNotOrigin(patientVisit)) {
            patientVisit.origin = systemConfigs.description
        }

        return patientVisit
    }

    private static boolean checkHasNotOrigin(PatientVisit patientVisit) {
        return patientVisit.origin == null || patientVisit?.origin?.isEmpty()
    }

private static saveExternalPatientVisit(boolean isNational, PatientVisit patientVisit, Episode lastEpisode, def objectJSON) {
        ExternalPatientVisit externalPatientVisit = new ExternalPatientVisit()
        externalPatientVisit.beforeInsert()
        externalPatientVisit.patientId = patientVisit.patient.id
        externalPatientVisit.patientName = patientVisit.patient.firstNames.concat(" ").concat(patientVisit.patient.lastNames)
        externalPatientVisit.patientGender = patientVisit.patient.gender
        externalPatientVisit.patientDateOfBirth = patientVisit.patient.dateOfBirth
        externalPatientVisit.patientCellphone = patientVisit.patient.cellphone
        externalPatientVisit.nid = lastEpisode.patientServiceIdentifier.value
        externalPatientVisit.jsonObject = objectJSON as JSON

        if(isNational) {
            populateWithActualEpisodeDetails(externalPatientVisit, lastEpisode)
        } else {
            populateWithDefaultEpisodeDetails(externalPatientVisit)
        }
        externalPatientVisit.save(flush: true)
    }

   private static populateWithActualEpisodeDetails(ExternalPatientVisit externalPatientVisit, Episode lastEpisode){
        externalPatientVisit.sourceProvinceName = lastEpisode?.clinic?.province?.description
        externalPatientVisit.sourceProvinceId = lastEpisode?.clinic?.province?.id
        externalPatientVisit.sourceDistrictId = lastEpisode?.clinic?.district?.id
        externalPatientVisit.sourceDistrictName = lastEpisode?.clinic?.district?.description
        externalPatientVisit.sourceClinicId = lastEpisode?.clinic?.id
        externalPatientVisit.sourceClinicName = lastEpisode?.clinic?.clinicName
        externalPatientVisit.targetProvinceId = lastEpisode?.referralClinic?.province?.id
        externalPatientVisit.targetProvinceName = lastEpisode?.referralClinic?.province?.description
        externalPatientVisit.targetDistrictId = lastEpisode?.referralClinic?.district?.id
        externalPatientVisit.targetDistrictName = lastEpisode?.referralClinic?.district?.description
        externalPatientVisit.targetClinicId = lastEpisode?.referralClinic?.id
        externalPatientVisit.targetClinicName = lastEpisode?.referralClinic?.clinicName
        externalPatientVisit.syncStatus = 'R'
    }

  private static  populateWithDefaultEpisodeDetails(ExternalPatientVisit externalPatientVisit){
        externalPatientVisit.sourceProvinceName = "N/A"
        externalPatientVisit.sourceProvinceId = "N/A"
        externalPatientVisit.sourceDistrictId = "N/A"
        externalPatientVisit.sourceDistrictName = "N/A"
        externalPatientVisit.sourceClinicId = "N/A"
        externalPatientVisit.sourceClinicName = "N/A"
        externalPatientVisit.targetProvinceId = "N/A"
        externalPatientVisit.targetProvinceName = "N/A"
        externalPatientVisit.targetDistrictId = "N/A"
        externalPatientVisit.targetDistrictName = "N/A"
        externalPatientVisit.targetClinicId = "N/A"
        externalPatientVisit.targetClinicName = "N/A"
        externalPatientVisit.syncStatus = 'N'
    }

    private boolean  verifyIfNeedToCreateMaintenanceEpisode (Episode episode,Patient patient) {
       def packs = packService.getAllPacksByPatientAndEpisode(patient,episode)
        if (episode.startStopReason.code != StartStopReason.MANUNTENCAO && packs.size() >= 1) return true
    }

}
