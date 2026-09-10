package mz.org.fgh.sifmoz.backend

import grails.core.GrailsApplication
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql
import mz.org.fgh.sifmoz.backend.clinic.Clinic
import mz.org.fgh.sifmoz.backend.clinicSector.ClinicSector
import mz.org.fgh.sifmoz.backend.clinicSectorType.ClinicSectorType
import mz.org.fgh.sifmoz.backend.convertDateUtils.ConvertDateUtils
import mz.org.fgh.sifmoz.backend.dispenseMode.DispenseMode
import mz.org.fgh.sifmoz.backend.dispenseType.DispenseType
import mz.org.fgh.sifmoz.backend.distribuicaoAdministrativa.District
import mz.org.fgh.sifmoz.backend.distribuicaoAdministrativa.Province
import mz.org.fgh.sifmoz.backend.doctor.Doctor
import mz.org.fgh.sifmoz.backend.drug.Drug
import mz.org.fgh.sifmoz.backend.duration.Duration
import mz.org.fgh.sifmoz.backend.episode.Episode
import mz.org.fgh.sifmoz.backend.episodeType.EpisodeType
import mz.org.fgh.sifmoz.backend.facilityType.FacilityType
import mz.org.fgh.sifmoz.backend.form.Form
import mz.org.fgh.sifmoz.backend.groupType.GroupType
import mz.org.fgh.sifmoz.backend.healthInformationSystem.HealthInformationSystem
import mz.org.fgh.sifmoz.backend.healthInformationSystem.SystemConfigs
import mz.org.fgh.sifmoz.backend.identifierType.IdentifierType
import mz.org.fgh.sifmoz.backend.interoperabilityAttribute.InteroperabilityAttribute
import mz.org.fgh.sifmoz.backend.interoperabilityType.InteroperabilityType
import mz.org.fgh.sifmoz.backend.migration.fromDataSource.DataSourceMigrationService
import mz.org.fgh.sifmoz.backend.migration.stage.MigrationService
import mz.org.fgh.sifmoz.backend.migration.stage.MigrationStage
import mz.org.fgh.sifmoz.backend.multithread.ExecutorThreadProvider
import mz.org.fgh.sifmoz.backend.packagedDrug.PackagedDrug
import mz.org.fgh.sifmoz.backend.packaging.Pack
import mz.org.fgh.sifmoz.backend.patient.Patient
import mz.org.fgh.sifmoz.backend.patientIdentifier.PatientServiceIdentifier
import mz.org.fgh.sifmoz.backend.patientVisit.PatientVisit
import mz.org.fgh.sifmoz.backend.patientVisitDetails.PatientVisitDetails
import mz.org.fgh.sifmoz.backend.prescription.Prescription
import mz.org.fgh.sifmoz.backend.prescription.SpetialPrescriptionMotive
import mz.org.fgh.sifmoz.backend.prescriptionDetail.PrescriptionDetail
import mz.org.fgh.sifmoz.backend.prescriptionDrug.PrescribedDrug
import mz.org.fgh.sifmoz.backend.protection.*
import mz.org.fgh.sifmoz.backend.provincialServer.ProvincialServer
import mz.org.fgh.sifmoz.backend.service.ClinicalService
import mz.org.fgh.sifmoz.backend.serviceattribute.ClinicalServiceAttribute
import mz.org.fgh.sifmoz.backend.serviceattributetype.ClinicalServiceAttributeType
import mz.org.fgh.sifmoz.backend.startStopReason.StartStopReason
import mz.org.fgh.sifmoz.backend.stockoperation.StockOperationType
import mz.org.fgh.sifmoz.backend.tansreference.PatientTransReferenceType
import mz.org.fgh.sifmoz.backend.therapeuticLine.TherapeuticLine
import mz.org.fgh.sifmoz.backend.therapeuticRegimen.TherapeuticRegimen
import mz.org.fgh.sifmoz.backend.utilities.Utilities
import org.grails.core.artefact.ControllerArtefactHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod

import javax.sql.DataSource

class BootStrap {

    GrailsApplication grailsApplication
    SpringSecurityService springSecurityService
    DataSource dataSource

    // DataSourceMigrationService dataSourceMigrationService

    def init = { servletContext ->
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        FacilityType.withTransaction { initFacilityType() }

        IdentifierType.withTransaction { initIdentifierType() }

        ClinicSectorType.withTransaction { initClinicSectorType() }

        ClinicalServiceAttributeType.withTransaction { initClinicalServiceAttributeType() }

        DispenseMode.withTransaction { initDispenseMode() }

        DispenseType.withTransaction { initDispenseType() }

        Form.withTransaction { initForm() }

        Duration.withTransaction { initDuration() }

        Doctor.withTransaction { initDoctor() }

        StartStopReason.withTransaction { initStartStopReason() }

        GroupType.withTransaction { initGroupType() }

        TherapeuticLine.withTransaction { initTherapeuticLine() }

        EpisodeType.withTransaction { initEpisodeType() }

        SpetialPrescriptionMotive.withTransaction { initSpetialPrescriptionMotive() }

        ClinicalService.withTransaction { initClinicalService() }

        HealthInformationSystem.withTransaction { initHealthInformationSystem() }

        InteroperabilityType.withTransaction { initInteroperabilityType() }

        InteroperabilityAttribute.withTransaction { initInteroperabilityAttribute() }

        Province.withTransaction { initProvince() }

        District.withTransaction { initDistrict() }

        TherapeuticRegimen.withTransaction {
            initTherapeuticRegimen()
        }

        Drug.withNewTransaction {
            initDrug()
            initRegimenDrugAssossiation()
        }

        Clinic.withTransaction {
//            initDefaultClinic()
            initClinic()
            updateClinicName()
            updateClinicDistrict()
        }

//        ClinicSector.withTransaction { initClinicSector()  }

        SystemConfigs.withTransaction { initSystemConfigs() }

        MigrationStage.withTransaction { initMigrationStage() }

        StockOperationType.withTransaction { initStockOperationType() }

        PatientTransReferenceType.withTransaction { initPatientTransReferenceType() }

        ProvincialServer.withTransaction { initProvincialServer() }

        MigrationStage.withTransaction { initMigration() }

        Menu.withTransaction {
            initMenus()

        }

        UiSection.withTransaction {
            initUiSections()
        }

        SpringSecurityUtils.clientRegisterFilter("corsFilterTest", SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order - 1)

        Requestmap.withTransaction {
            initRequestMaps()
        }

        ClinicalServiceAttribute.withTransaction {
            initClinicalServiceAttribute()
        }


        SecUser.withTransaction {
            initUserManagement()
            springSecurityService.clearCachedRequestmaps()
        }
        Episode.withTransaction {
         //   getEpisodesWithSameStartStopReason()
        }

        ClinicSector.withTransaction {
            //   insertClinicSectorsOnClinics()
            //       compareClinics()
            //   dropClinicSector()
        }
//        Pack.withTransaction {
//           resolvePackWithoutPackagedDrugs()
//        }

//        Prescription.withTransaction {
//            resolvePrescriptionsWithoutPrescribedDrugs()
//        }


//        PatientServiceIdentifier.withTransaction {
        // adjustServiceDate()
//        }

//        Patient.withTransaction {
        // resolvePatientsAddMatchId()
        //     unitePatient()
//        }

//        dataSourceMigrationService.loadAndSaveClinicSectorMigrationService()
//        dataSourceMigrationService.loadAndSaveDoctorsMigrationService()
//        dataSourceMigrationService.loadAndSavePatientVisitDetailsMigrationService()
        //      dataSourceMigrationService.compareClinics()

    }

    def destroy = {
    }


    void initMenus() {
        new Menu(code: '01', description: 'Pacientes').save()
        new Menu(code: '02', description: 'Grupos').save()
        new Menu(code: '03', description: 'Stock').save()
        new Menu(code: '04', description: 'Dashboard').save()
        new Menu(code: '05', description: 'Relatorios').save()
        new Menu(code: '06', description: 'Administração').save()
        new Menu(code: '07', description: 'Migração').save()
        new Menu(code: '08', description: 'Tela Inicial').save()
        new Menu(code: '09', description: 'DCProvedor').save()
    }

    void initRequestMaps() {
        final String ANONYMOUS_ROLE = 'IS_AUTHENTICATED_ANONYMOUSLY'
        final String ADMIN_ROLE = 'ROLE_ADMIN,IS_AUTHENTICATED_REMEMBERED'

        HashMap<String, Object> maps = grailsApplication.getArtefactInfo(ControllerArtefactHandler.TYPE).logicalPropertyNameToClassMap
        for (String key : maps.keySet()) {
            def url = '/api' + '/' + key + '/**'

            if (key.equalsIgnoreCase("systemConfigs") || key.equalsIgnoreCase("drug") ||
                    key.equalsIgnoreCase("therapeuticRegimen") || key.equalsIgnoreCase("identifierType") ||
                    key.equalsIgnoreCase("clinic") || key.equalsIgnoreCase("province") ||
                    key.equalsIgnoreCase("district") || key.equalsIgnoreCase("menu")
            ) {
                def requastmapObjectList = Requestmap.findAllByUrl(url)

                if (requastmapObjectList) {

                    for (requastmapObject in requastmapObjectList) {

                        switch (requastmapObject.httpMethod) {
                            case HttpMethod.GET:
                                requastmapObject.configAttribute = ANONYMOUS_ROLE
                                break
                            case HttpMethod.POST:
                                if (key.equalsIgnoreCase("systemConfigs")) {
                                    requastmapObject.configAttribute = ANONYMOUS_ROLE
                                } else {
                                    requastmapObject.configAttribute = ADMIN_ROLE
                                }
                                break
                            case HttpMethod.PUT:
                                requastmapObject.configAttribute = ADMIN_ROLE
                                break
                            case HttpMethod.PATCH:
                                requastmapObject.configAttribute = ADMIN_ROLE
                                break
                            case HttpMethod.DELETE:
                                requastmapObject.configAttribute = ADMIN_ROLE
                                break
                            default:
                                break
                        }
                        requastmapObject.url = url
                        requastmapObject.save(flush: true, failOnError: false)
                    }
                } else {
                    if (key.equalsIgnoreCase("systemConfigs"))
                        new Requestmap(url: url, configAttribute: ANONYMOUS_ROLE, httpMethod: HttpMethod.POST).save(flush: true, failOnError: false)
                    else
                        new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.POST).save(flush: true, failOnError: false)
                    new Requestmap(url: url, configAttribute: ANONYMOUS_ROLE, httpMethod: HttpMethod.GET).save(flush: true, failOnError: false)
                    new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.PUT).save(flush: true, failOnError: false)
                    new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.PATCH).save(flush: true, failOnError: false)
                    new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.DELETE).save(flush: true, failOnError: false)
                }
            } else {
                new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.GET).save(flush: true, failOnError: false)
                new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.POST).save(flush: true, failOnError: false)
                new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.PUT).save(flush: true, failOnError: false)
                new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.PATCH).save(flush: true, failOnError: false)
                new Requestmap(url: url, configAttribute: ADMIN_ROLE, httpMethod: HttpMethod.DELETE).save(flush: true, failOnError: false)
            }
        }

        for (UiSection uiSection: UiSection.findAll()) {
            new Requestmap(url: uiSection.requestMapUrl , configAttribute: ADMIN_ROLE).save(flush: true)
        }

        for (
                String url in [
                        '/', '/index', '/index.gsp', '/**/favicon.ico',
                        '/**/js/**', '/**/css/**', '/**/images/**',
                        '/login', '/login.*', '/login/*',
                        '/logout', '/logout.*', '/logout/*']) {
            new Requestmap(url: url, configAttribute: 'permitAll').save(flush: true, failOnError: false);

        }
        initAddReportsToRolesFix()
    }

    void initUserManagement() {

        def adminRole = Role.findByAuthorityIlike("%ROLE_ADMIN%")
//        Role adminRole = Role.findByAuthority('ROLE_ADMIN')

        if (!adminRole) {
            adminRole = new Role('ROLE_ADMIN', 'Admin', 'ROLE_ADMIN', true)
            for (Menu menu : Menu.findAll()) {
                adminRole.menus.add(menu)
            }

            adminRole.save(flush: true, failOnError: true)
        }

        for (userObject in listUsers()) {
            if (!SecUser.findByUsername(userObject.username)) {
                SecUser secUser = new SecUser()
                secUser.username = userObject.username
                secUser.password = userObject.password
                secUser.fullName = userObject.fullName
                secUser.contact = userObject.contact
                secUser.email = userObject.email
                secUser.openmrsPassword = userObject.openmrsPassword
                secUser.save(flush: true, failOnError: true)
                SecUserRole.create(secUser, adminRole, true)

            }
        }
    }
// Methods
    void initMigration() {
        if (MigrationStage.findByValue(MigrationStage.STAGE_IN_PROGRESS) != null) ExecutorThreadProvider.getInstance().getExecutorService().execute(new MigrationService())
    }

    void initFacilityType() {

        for (facilityTypeObject in listFacilityType()) {
            if (!FacilityType.findByCode(facilityTypeObject.code)) {
                FacilityType facilityType = new FacilityType()
                facilityType.id = facilityTypeObject.id
                facilityType.code = facilityTypeObject.code
                facilityType.description = facilityTypeObject.description
                facilityType.type = facilityTypeObject.type
                facilityType.save(flush: true, failOnError: true)
            }
        }
    }

    void initIdentifierType() {
        for (identifierTypeObject in listIdentifierType()) {
            if (!IdentifierType.findByCode(identifierTypeObject.code)) {
                IdentifierType identifierType = new IdentifierType()
                identifierType.id = identifierTypeObject.id
                identifierType.code = identifierTypeObject.code
                identifierType.description = identifierTypeObject.description
                identifierType.setPattern(identifierTypeObject.pattern)
                identifierType.save(flush: true, failOnError: true)
            }
        }
    }

    void initClinicSectorType() {
        for (clinicSectorTypeObject in listClinicSectorType()) {
            if (!ClinicSectorType.findByCode(clinicSectorTypeObject.code)) {
                ClinicSectorType clinicSectorType = new ClinicSectorType()
                clinicSectorType.id = clinicSectorTypeObject.id
                clinicSectorType.code = clinicSectorTypeObject.code
                clinicSectorType.description = clinicSectorTypeObject.description
                clinicSectorType.save(flush: true, failOnError: true)
            }
        }
    }

    void initClinicalServiceAttributeType() {
        for (clinicalServiceAttributeTypeObject in listClinicalServiceAttributeType()) {
            if (!ClinicalServiceAttributeType.findByCode(clinicalServiceAttributeTypeObject.code)) {
                ClinicalServiceAttributeType clinicalServiceAttributeType = new ClinicalServiceAttributeType()
                clinicalServiceAttributeType.id = clinicalServiceAttributeTypeObject.id
                clinicalServiceAttributeType.code = clinicalServiceAttributeTypeObject.code
                clinicalServiceAttributeType.description = clinicalServiceAttributeTypeObject.description
                clinicalServiceAttributeType.save(flush: true, failOnError: true)
            }
        }
    }

    void initClinicalServiceAttribute() {
        for (clinicalServiceAttributeObject in listClinicalServiceAttribute()) {
            try {
                ClinicalService clinicalService = ClinicalService.get(clinicalServiceAttributeObject.clinicalService)
                clinicalService.addToClinicalServiceAttributes(ClinicalServiceAttributeType.get(clinicalServiceAttributeObject.clinicalServiceAttributeType))
                clinicalService.save(flush: true, failOnError: true)
            } catch (Exception e) {
                e.printStackTrace()
            } finally {
                continue
            }
        }
    }


    void initDispenseMode() {
        for (dispenseModeObject in listDispenseMode()) {
            if (!DispenseMode.findByCode(dispenseModeObject.code)) {
                DispenseMode dispenseMode = new DispenseMode()
                dispenseMode.id = dispenseModeObject.id
                dispenseMode.code = dispenseModeObject.code
                dispenseMode.description = dispenseModeObject.description
                dispenseMode.openmrsUuid = dispenseModeObject.openmrs_uuid
                dispenseMode.save(flush: true, failOnError: true)
            }
        }
    }

    void initDispenseType() {
        for (dispenseTypeObject in listDispenseType()) {
            DispenseType dispenseType = DispenseType.get(dispenseTypeObject.id)
            if (!dispenseType) {
                dispenseType = new DispenseType()
            }
            dispenseType.id = dispenseTypeObject.id
            dispenseType.code = dispenseTypeObject.code
            dispenseType.description = dispenseTypeObject.description
            dispenseType.save(flush: true, failOnError: true)
        }
    }

    void initForm() {
        for (formObject in listForm()) {
            if (!Form.findByCode(formObject.code)) {
                Form form = new Form()
                form.id = formObject.id
                form.code = formObject.code
                form.description = formObject.description
                form.unit = formObject.unit
                form.howToUse = formObject.how_to_use
                form.save(flush: true, failOnError: true)
            }
        }
    }

    void initDuration() {
        for (durationObject in listDuration()) {
            if (!Duration.findByWeeks(durationObject.weeks)) {
                Duration duration = new Duration()
                duration.id = durationObject.id
                duration.weeks = durationObject.weeks
                duration.description = durationObject.description
                duration.save(flush: true, failOnError: true)
            }
        }
    }

    void initDoctor(){
        def clinic  = Clinic.findAllWhere(mainClinic: true)
        if(!clinic.isEmpty())
        for(doctorObject in listDoctor()){
            if(!Doctor.findWhere(id: doctorObject.id) && clinic){
                Doctor doctor = new Doctor()
                doctor.id = doctorObject.id
                doctor.firstnames = doctorObject.firstnames
                doctor.lastname = doctorObject.lastname
                doctor.gender = doctorObject.gender
                doctor.telephone = doctorObject.telephone
                doctor.email = doctorObject.telephone
                doctor.active = true
                doctor.clinic =  clinic.first()
                doctor.save(flush: true, failOnError: true)
            }
        }
    }

    void initStartStopReason() {
        for (startStopReasonObject in listStartStopReason()) {
            if (!StartStopReason.findById(startStopReasonObject.id)) {
                StartStopReason startStopReason = new StartStopReason()
                startStopReason.id = startStopReasonObject.id
                startStopReason.isStartReason = startStopReasonObject.isStartReason
                startStopReason.code = startStopReasonObject.code
                startStopReason.reason = startStopReasonObject.reason
                startStopReason.save(flush: true, failOnError: true)
            }
        }
    }

    void initGroupType() {
        for (groupTypeObject in listGroupType()) {
            if (!GroupType.findByCode(groupTypeObject.code)) {
                GroupType groupType = new GroupType()
                groupType.id = groupTypeObject.id
                groupType.code = groupTypeObject.code
                groupType.description = groupTypeObject.description
                groupType.save(flush: true, failOnError: true)
            }
        }
    }

    void initTherapeuticLine() {
        for (therapeuticLineObject in listTherapeuticLine()) {
            if (!TherapeuticLine.findByCode(therapeuticLineObject.code)) {
                TherapeuticLine therapeuticLine = new TherapeuticLine()
                therapeuticLine.id = therapeuticLineObject.id
                therapeuticLine.code = therapeuticLineObject.code
                therapeuticLine.description = therapeuticLineObject.description
                therapeuticLine.uuid = therapeuticLineObject.uuid
                therapeuticLine.save(flush: true, failOnError: true)
            }
        }
    }

    void initEpisodeType() {
        for (episodeTypeObject in listEpisodeType()) {
            if (!EpisodeType.findByCode(episodeTypeObject.code)) {
                EpisodeType episodeType = new EpisodeType()
                episodeType.id = episodeTypeObject.id
                episodeType.code = episodeTypeObject.code
                episodeType.description = episodeTypeObject.description
                episodeType.save(flush: true, failOnError: true)
            }
        }
    }

    void initClinicalService() {
        for (clinicalServiceObject in listClinicalService()) {
            if (!ClinicalService.findByCode(clinicalServiceObject.code)) {
                ClinicalService clinicalService = new ClinicalService()
                clinicalService.id = clinicalServiceObject.id
                clinicalService.code = clinicalServiceObject.code
                clinicalService.description = clinicalServiceObject.description
                clinicalService.identifierType = clinicalServiceObject.identifierType
                clinicalService.active = clinicalServiceObject.active
                clinicalService.save(flush: true, failOnError: true)
            }
        }
    }

    void initSpetialPrescriptionMotive() {
        for (spetialPrescriptionMotiveObject in listSpetialPrescriptionMotive()) {
            if (!SpetialPrescriptionMotive.findByCode(spetialPrescriptionMotiveObject.code)) {
                SpetialPrescriptionMotive spetialPrescriptionMotive = new SpetialPrescriptionMotive()
                spetialPrescriptionMotive.id = spetialPrescriptionMotiveObject.id
                spetialPrescriptionMotive.code = spetialPrescriptionMotiveObject.code
                spetialPrescriptionMotive.description = spetialPrescriptionMotiveObject.description
                spetialPrescriptionMotive.save(flush: true, failOnError: true)
            }
        }
    }

    void initHealthInformationSystem() {
        for (healthInformationSystemObject in listHealthInformationSystem()) {
            if (!HealthInformationSystem.findById(healthInformationSystemObject.id)) {
                HealthInformationSystem healthInformationSystem = new HealthInformationSystem()
                healthInformationSystem.id = healthInformationSystemObject.id
                healthInformationSystem.abbreviation = healthInformationSystemObject.abbreviation
                healthInformationSystem.description = healthInformationSystemObject.description
                healthInformationSystem.active = healthInformationSystemObject.active
                healthInformationSystem.save(flush: true, failOnError: true)
            }
        }
    }

    void initInteroperabilityType() {
        for (interoperabilityTypeObject in listInteroperabilityType()) {
            if (!InteroperabilityType.findByCode(interoperabilityTypeObject.code)) {
                InteroperabilityType interoperabilityType = new InteroperabilityType()
                interoperabilityType.id = interoperabilityTypeObject.id
                interoperabilityType.code = interoperabilityTypeObject.code
                interoperabilityType.description = interoperabilityTypeObject.description
                interoperabilityType.save(flush: true, failOnError: true)
            }
        }
    }


    void initInteroperabilityAttribute() {
        for (interoperabilityAttributeObject in listInteroperabilityAttribute()) {
            if (!InteroperabilityAttribute.findById(interoperabilityAttributeObject.id)) {
                InteroperabilityAttribute interoperabilityAttribute = new InteroperabilityAttribute()
                interoperabilityAttribute.id = interoperabilityAttributeObject.id
                interoperabilityAttribute.interoperabilityType = interoperabilityAttributeObject.interoperabilityType
                interoperabilityAttribute.value = interoperabilityAttributeObject.value
                interoperabilityAttribute.healthInformationSystem = interoperabilityAttributeObject.healthInformationSystem
                interoperabilityAttribute.save(flush: true, failOnError: true)
            }
        }
    }

    void initProvince() {
        for (provinceObject in listProvince()) {
            if (!Province.findByCode(provinceObject.code)) {
                Province province = new Province()
                province.id = provinceObject.id
                province.code = provinceObject.code
                province.description = provinceObject.description
                province.save(flush: true, failOnError: true)
            }
        }
    }

    void initDistrict() {
        for (districtObject in listDistrict()) {
            if (!District.findById(districtObject.id)) {
                District district = new District()
                district.id = districtObject.id
                district.code = districtObject.code
                district.description = districtObject.description
                district.province = districtObject.province
                district.save(flush: true, failOnError: true)
            }
        }
    }

    void initTherapeuticRegimen() {
        for (therapeuticRegimenObject in listTherapeuticRegimen()) {

            ClinicalService clinicalService =  ClinicalService.findById(therapeuticRegimenObject.clinical_service_id)
            TherapeuticRegimen therapeuticRegimen1 = TherapeuticRegimen.findById(therapeuticRegimenObject.id.toString().trim())
            TherapeuticRegimen therapeuticRegimen2 = TherapeuticRegimen.findByCode(therapeuticRegimenObject.code)

            if (!therapeuticRegimen1) {
                if (!therapeuticRegimen2) {
                    TherapeuticRegimen therapeuticRegimen = new TherapeuticRegimen()
                    therapeuticRegimen.id = therapeuticRegimenObject.id
                    therapeuticRegimen.code = therapeuticRegimenObject.code
                    therapeuticRegimen.regimenScheme = therapeuticRegimenObject.regimen_scheme
                    therapeuticRegimen.active = therapeuticRegimenObject.active
                    therapeuticRegimen.description = therapeuticRegimenObject.description
                    therapeuticRegimen.openmrsUuid = therapeuticRegimenObject.openmrs_uuid
//                    therapeuticRegimen.clinicalService = ClinicalService.findById(therapeuticRegimenObject.clinical_service_id)
//                    therapeuticRegimen.save(flush: true, failOnError: true)
                    clinicalService.addToTherapeuticRegimens(therapeuticRegimen)
                } else {
                    if (therapeuticRegimen2.code.equalsIgnoreCase("X6APed")) {
                        therapeuticRegimen2.regimenScheme = "ABC+3TC+DTG (2DFC+DTG50)"
                        therapeuticRegimen2.description = "ABC+3TC+DTG (2DFC+DTG50)"
                    }
//                    therapeuticRegimen2.active = therapeuticRegimenObject.active
                    therapeuticRegimen2.openmrsUuid = therapeuticRegimenObject.openmrs_uuid
//                    therapeuticRegimen2.clinicalService = ClinicalService.findById(therapeuticRegimenObject.clinical_service_id)
                    therapeuticRegimen2.save(flush: true, failOnError: true)
                    clinicalService.addToTherapeuticRegimens(therapeuticRegimen2)
                }
            } else {
                if (therapeuticRegimen1.code.equalsIgnoreCase("X6APed")) {
                    therapeuticRegimen1.regimenScheme = "ABC+3TC+DTG (2DFC+DTG50)"
                    therapeuticRegimen1.description = "ABC+3TC+DTG (2DFC+DTG50)"
                }
//                therapeuticRegimen1.active = therapeuticRegimenObject.active
                therapeuticRegimen1.openmrsUuid = therapeuticRegimenObject.openmrs_uuid
//                therapeuticRegimen1.clinicalService = ClinicalService.findById(therapeuticRegimenObject.clinical_service_id)
                therapeuticRegimen1.save(flush: true, failOnError: true)
                clinicalService.addToTherapeuticRegimens(therapeuticRegimen1)
            }
            clinicalService.save()
        }
    }

    void initDrug() {
        for (drugObject in listDrug()) {
            Drug drug1 = Drug.findById(drugObject.id)
            if (!drug1) {
                Drug drug = new Drug()
                drug.id = drugObject.id
                drug.packSize = drugObject.pack_size
                drug.name = drugObject.name
                drug.active = drugObject.active
                drug.defaultTreatment = drugObject.default_treatment
                drug.defaultTimes = drugObject.default_times
                drug.defaultPeriodTreatment = drugObject.default_period_treatment
                drug.fnmCode = drugObject.fnm_code
                drug.uuidOpenmrs = drugObject.uuid_openmrs
                drug.active = drugObject.active
                drug.clinical_service_id = drugObject.clinical_service_id
//                drug.clinicalService = ClinicalService.findById(drugObject.clinical_service_id)
                drug.form = Form.findById(drugObject.form_id)
                drug.save(flush: true, failOnError: true)

            } else {
                if (drug1.getClinical_service_id() == null) {
                    drug1.clinical_service_id = drugObject.clinical_service_id
//                    drug1.clinicalService = ClinicalService.findById(drugObject.clinical_service_id)
                    drug1.save(flush: true, failOnError: true)
                } else {
                    if (drug1.getUuidOpenmrs() == null || drug1.getUuidOpenmrs().trim().empty) {
                        drug1.uuidOpenmrs = drugObject.name
                        drug1.uuidOpenmrs = drugObject.uuid_openmrs
                        drug1.active = drugObject.active
                        drug1.save(flush: true, failOnError: true)
                    }
                }
            }
        }
    }

    void initRegimenDrugAssossiation() {
        for (drugRegimen in listRegimenDrugs()) {
            Drug drug = Drug.findByFnmCode(drugRegimen.drug_id)
            TherapeuticRegimen therapeuticRegimen = TherapeuticRegimen.findByCode(drugRegimen.regimen_id)

            Set<TherapeuticRegimen> therapeuticRegimenList = new ArrayList<>() as Set<TherapeuticRegimen>
            therapeuticRegimenList.add(therapeuticRegimen)

            if (drug && therapeuticRegimen) {
                therapeuticRegimen.addToDrugs(drug)
                therapeuticRegimen.save(flush: true, failOnError: true)
            }
        }
    }

//    void initDefaultClinic(){
//
//        if(Clinic.list().isEmpty()){
//            Clinic defaultClinic = new Clinic()
//            defaultClinic.setId('56F128E8-A85E-45B4-AE5C-E91D14ACA906')
//            defaultClinic.setCode('DC')
//            defaultClinic.setNotes('Default Clinic')
//            defaultClinic.setTelephone('0000000000')
//            defaultClinic.setClinicName('Clínica Padrão')
//            defaultClinic.setProvince(Province.findByCode('01'))
//            defaultClinic.setDistrict(District.findByCodeAndProvince('01', Province.findByCode('01')))
//            defaultClinic.setFacilityType(FacilityType.findByCode('US'))
//            defaultClinic.setMainClinic(true)
//            defaultClinic.setActive(true)
//            defaultClinic.setUuid('56F128E8-A85E-45B4-AE5C-E91D14ACA906')
//            defaultClinic.save(flush: true, failOnError: true)
//        }
//    }

    void initClinic() {
        for (clinicObject in listClinic1()) {
            if (!Clinic.findById(clinicObject.uuid)) {
                Clinic clinic = new Clinic()
                clinic.id = clinicObject.uuid
                clinic.code = clinicObject.site_nid
                clinic.notes = "SISMA CODE: ".concat(clinicObject.sisma_id)
                clinic.clinicName = clinicObject.sitename
                clinic.province = Province.findByCode(clinicObject.provinceCode)
                clinic.district = District.findByCodeAndProvince(clinicObject.districtCode, Province.findByCode(clinicObject.provinceCode))
                clinic.facilityType = FacilityType.findByCode("US")
                clinic.mainClinic = false
                clinic.active = true
                clinic.uuid = clinicObject.uuid
                clinic.syncStatus = 'S'
                clinic.save(flush: true, failOnError: true)
            }
        }

        for (clinicObject in listClinic2()) {
            if (!Clinic.findById(clinicObject.uuid)) {
                Clinic clinic = new Clinic()
                clinic.id = clinicObject.uuid
                clinic.code = clinicObject.site_nid
                clinic.notes = "SISMA CODE: ".concat(clinicObject.sisma_id)
                clinic.clinicName = clinicObject.sitename
                clinic.province = Province.findByCode(clinicObject.provinceCode)
                clinic.district = District.findByCodeAndProvince(clinicObject.districtCode, Province.findByCode(clinicObject.provinceCode))
                clinic.facilityType = FacilityType.findByCode("US")
                clinic.mainClinic = false
                clinic.active = true
                clinic.uuid = clinicObject.uuid
                clinic.syncStatus = 'S'
                clinic.save(flush: true, failOnError: true)
            }
        }

        for (clinicObject in listClinic3()) {
            if (!Clinic.findById(clinicObject.uuid)) {
                Clinic clinic = new Clinic()
                clinic.id = clinicObject.uuid
                clinic.code = clinicObject.site_nid
                clinic.notes = "SISMA CODE: ".concat(clinicObject.sisma_id)
                clinic.clinicName = clinicObject.sitename
                clinic.province = Province.findByCode(clinicObject.provinceCode)
                clinic.district = District.findByCodeAndProvince(clinicObject.districtCode, Province.findByCode(clinicObject.provinceCode))
                clinic.facilityType = FacilityType.findByCode("US")
                clinic.mainClinic = false
                clinic.active = true
                clinic.uuid = clinicObject.uuid
                clinic.syncStatus = 'S'
                clinic.save(flush: true, failOnError: true)
            }
        }
/*
        for (clinicObject in listClinic4()) {
            if (!Clinic.findById(clinicObject.uuid)) {
                Clinic clinic = new Clinic()
                clinic.id = clinicObject.uui
                clinic.code = clinicObject.site_nid
                clinic.notes = "SISMA CODE: ".concat(clinicObject.sisma_id)
                clinic.clinicName = clinicObject.sitename
                clinic.province = Province.findByCode(clinicObject.provinceCode)
                clinic.district = District.findByCodeAndProvince(clinicObject.districtCode, Province.findByCode(clinicObject.provinceCode))
                clinic.facilityType = FacilityType.findByCode("FP")
                clinic.mainClinic = false
                clinic.active = true
                clinic.uuid = clinicObject.uuid
                clinic.save(flush: true, failOnError: true)
            }
        }
 */
    }

    void initSystemConfigs() {
        for (systemConfigsObject in listSystemConfigs()) {
            SystemConfigs systemConfigs = SystemConfigs.findById(systemConfigsObject.id)
            if (!systemConfigs) {
                systemConfigs = new SystemConfigs()
                systemConfigs.id = systemConfigsObject.id
                systemConfigs.value = systemConfigsObject.value
                systemConfigs.description = systemConfigsObject.description
                systemConfigs.key = systemConfigsObject.key
            }else{
                if(systemConfigs.key.equalsIgnoreCase("APP_VERSION")){
                    systemConfigs.value = systemConfigsObject.value
                }
            }
            systemConfigs.save(flush: true, failOnError: true)
        }
    }

    void initMigrationStage() {
        for (migrationStageObject in listMigrationStage()) {
            if (!MigrationStage.findByCode(migrationStageObject.code)) {
                MigrationStage migrationStage = new MigrationStage()
                migrationStage.id = migrationStageObject.id
                migrationStage.value = migrationStageObject.value
                migrationStage.code = migrationStageObject.code
                migrationStage.save(flush: true, failOnError: true)
            }
        }
    }

    void initStockOperationType() {
        for (stockOperationTypeObject in listStockOperationType()) {
            if (!StockOperationType.findByCode(stockOperationTypeObject.code)) {
                StockOperationType stockOperationType = new StockOperationType()
                stockOperationType.id = stockOperationTypeObject.id
                stockOperationType.description = stockOperationTypeObject.description
                stockOperationType.code = stockOperationTypeObject.code
                stockOperationType.save(flush: true, failOnError: true)
            }
        }
    }

    void initPatientTransReferenceType() {
        for (patientTransReferenceTypeObject in listPatientTransReferenceType()) {
            if (!PatientTransReferenceType.findByCode(patientTransReferenceTypeObject.code)) {
                PatientTransReferenceType patientTransReferenceType = new PatientTransReferenceType()
                patientTransReferenceType.id = patientTransReferenceTypeObject.id
                patientTransReferenceType.description = patientTransReferenceTypeObject.description
                patientTransReferenceType.code = patientTransReferenceTypeObject.code
                patientTransReferenceType.save(flush: true, failOnError: true)
            }
        }
    }

    void initProvincialServer() {
        for (provincialServerObject in listProvincialServer()) {
            ProvincialServer provincialServer = ProvincialServer.findByCodeAndDestination(provincialServerObject.code, provincialServerObject.destination)
            if (!provincialServer) {
                provincialServer = new ProvincialServer()
                provincialServer.id = provincialServerObject.id
                provincialServer.urlPath = provincialServerObject.urlPath
                provincialServer.port = provincialServerObject.port
                provincialServer.destination = provincialServerObject.destination
                provincialServer.code = provincialServerObject.code
                provincialServer.username = provincialServerObject.username
                provincialServer.password = provincialServerObject.password
                provincialServer.save(flush: true, failOnError: true)
            } else {
                if (provincialServer.destination.equalsIgnoreCase("IDMED")) {
                    provincialServer.username = "N/A"
                    provincialServer.password = "N/A"
                }
            }
        }
    }

    void adjustServiceDate() {

        def listPatientService = PatientServiceIdentifier.list()
        for (PatientServiceIdentifier patientServiceIdentifier : listPatientService) {

            if (!patientServiceIdentifier.episodes.isEmpty()) {
                Episode episode = Episode.findAllByIdInList(patientServiceIdentifier.episodes.id, [sort: "episodeDate", order: "desc"]).first()
                if (episode.episodeDate < patientServiceIdentifier.startDate) {
                    patientServiceIdentifier.startDate = episode.episodeDate
                    patientServiceIdentifier.save(flush: true, failOnError: true)
                }
            }
        }
    }

    List<Object> listUsers() {
        List<Object> usersList = new ArrayList<>()
        usersList.add(new LinkedHashMap(username: 'admin', password: 'admin', fullName: 'admin', contact: 'admin', email: 'admin@gmail.com', openmrsPassword: Utilities.getMd5('admin')))
        usersList.add(new LinkedHashMap(username: 'iDMED', password: 'iDMED123', fullName: 'iDMED', contact: 'iDMED', email: 'iDMED@gmail.com', openmrsPassword: Utilities.getMd5('iDMED123')))

        return usersList
    }

    List<Object> listDoctor() {
        List<Object> usersList = new ArrayList<>()
        usersList.add(new LinkedHashMap(id: '3F2D1A4B-9C6E-4F89-B5D3-8A2E7F1D0CBA', firstnames: 'Provedor', lastname: 'Externo', gender: 'TBD', email: 'provedor.externo@idmed.co.mz', telephone: ''))
        return usersList
    }

    List<Object> listProvincialServer() {
        List<Object> provincialServerList = new ArrayList<>()
        provincialServerList.add(new LinkedHashMap(id: '59BA4DAD-A32F-4B60-84D9-4A7F7E8C84FC', code: '01', urlPath: 'http://idartniassa.fgh.org.mz:', port: '3001', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '9BDBBAEB-F14E-4E9F-9C89-09F4D2A469FE', code: '02', urlPath: 'http://idartcabodelegado.fgh.org.mz:', port: '3002', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'C4F70C54-32BC-48F2-8BC5-A4CDD8B6571D', code: '03', urlPath: 'http://idartnampula.fgh.org.mz:', port: '3003', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '9E0F91B0-14F3-4FEC-9097-64E3F2B65B59', code: '04', urlPath: 'http://idartzambezia.fgh.org.mz:', port: '3004', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '168FAD26-1A41-4F46-9238-69528F35D3ED', code: '05', urlPath: 'http://idarttete.fgh.org.mz:', port: '3005', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '32DAA4E4-D949-4534-8B86-AFC54262421C', code: '06', urlPath: 'http://idartmanica.fgh.org.mz:', port: '3006', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '220212B7-4744-4DCD-A8CB-27DDEEA91140', code: '07', urlPath: 'http://idartsofala.fgh.org.mz:', port: '3007', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'E1DAE032-1C95-4872-9EF3-6ED9DE9D9DE5', code: '08', urlPath: 'http://idartinhambane.fgh.org.mz:', port: '3008', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '4489B1B6-A485-439A-B966-1C752873BF79', code: '09', urlPath: 'http://idartgaza.fgh.org.mz:', port: '3009', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'A5336E44-A9DC-4019-8B33-9F0738DB2D55', code: '10', urlPath: 'http://idartmaputo-prov.fgh.org.mz:', port: '3010', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'F21B5D3F-9C70-40A0-BE2F-66F4A458655F', code: '11', urlPath: 'http://idartmaputo-cid.fgh.org.mz:', port: '3011', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'F86C6280-0D36-4E5E-9C2C-D68002DB7A51', code: '12', urlPath: 'http://dev.fgh.org.mz:', port: '3910', destination: 'MOBILE', username: 'postgres', password: 'N/A', 'dbname':'N/A'))

        provincialServerList.add(new LinkedHashMap(id: '73864BCB-5FEA-4067-81D2-E5A50C71CD8F', code: '01', urlPath: 'idmed-ns.csaude.org.mz', port: '50411', destination: 'DB', username: 'idmedns', password: 'N/A', 'dbname':'idmedns'))
        provincialServerList.add(new LinkedHashMap(id: '4018D01F-B452-46C1-A97A-B1FE90A05A79', code: '02', urlPath: 'idmed-ca.csaude.org.mz', port: '50410', destination: 'DB', username: 'idmedca', password: 'N/A', 'dbname':'idmedca'))
        provincialServerList.add(new LinkedHashMap(id: '782A6251-673A-4180-9C8D-DE14F804B275', code: '03', urlPath: 'idmed-np.csaude.org.mz', port: '50409', destination: 'DB', username: 'idmednp', password: 'N/A', 'dbname':'idmednp'))
        provincialServerList.add(new LinkedHashMap(id: '5822E612-420F-45EE-855C-CEB051F2E901', code: '04', urlPath: 'idmed-zb.csaude.org.mz', port: '50408', destination: 'DB', username: 'idmedzb', password: 'N/A', 'dbname':'idmedzb'))
        provincialServerList.add(new LinkedHashMap(id: '59139F11-43BE-4E63-91D1-494C4E4637BE', code: '05', urlPath: 'idmed-tt.csaude.org.mz', port: '50407', destination: 'DB', username: 'idmedtt', password: 'N/A', 'dbname':'idmedtt'))
        provincialServerList.add(new LinkedHashMap(id: '36BDF50C-7851-46C8-BC29-7974ABF68382', code: '06', urlPath: 'idmed-mn.csaude.org.mz', port: '50406', destination: 'DB', username: 'idmedmn', password: 'N/A', 'dbname':'idmedmn'))
        provincialServerList.add(new LinkedHashMap(id: 'CAFC1BD3-FE43-49A8-A121-A2FBA56A552E', code: '07', urlPath: 'idmed-sf.csaude.org.mz', port: '50405', destination: 'DB', username: 'idmedsf', password: 'N/A', 'dbname':'idmedsf'))
        provincialServerList.add(new LinkedHashMap(id: 'EF428861-BAC9-4156-B489-8B73148D81EB', code: '08', urlPath: 'idmed-ib.csaude.org.mz', port: '50404', destination: 'DB', username: 'idmedib', password: 'N/A', 'dbname':'idmedib'))
        provincialServerList.add(new LinkedHashMap(id: '6FD5A6CD-755D-4FFF-A2B3-F2FA75C3BF64', code: '09', urlPath: 'idmed-gz.csaude.org.mz', port: '50403', destination: 'DB', username: 'idmedgz', password: 'N/A', 'dbname':'idmedgz'))
        provincialServerList.add(new LinkedHashMap(id: '48FCD6A2-80C3-46E8-AD23-3F5BC468F752', code: '10', urlPath: 'idmed-mp.csaude.org.mz', port: '50402', destination: 'DB', username: 'idmedmp', password: 'N/A', 'dbname':'idmedmp'))
        provincialServerList.add(new LinkedHashMap(id: '84383A39-0535-49EA-956C-FDAEA191D1F0', code: '11', urlPath: 'idmed-mc.csaude.org.mz', port: '50401', destination: 'DB', username: 'idmedmc', password: 'N/A', 'dbname':'idmedmc'))
        provincialServerList.add(new LinkedHashMap(id: '49896B8E-631D-4A4D-BC39-45B747688ECE', code: '12', urlPath: '172.16.60.20', port: '50420', destination: 'DB', username: 'postgres', password: 'N/A', 'dbname':'idmed_maputo'))

        provincialServerList.add(new LinkedHashMap(id: '0231B69C-A7AC-4024-8DF7-E75E2828E578', code: '01', urlPath: 'https://idmed-ns.csaude.org.mz', port: '5001', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '2C3B00F3-C8CB-4071-A070-54819C2F0962', code: '02', urlPath: 'https://idmed-ca.csaude.org.mz', port: '5002', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'C44CD2E8-DCB3-464A-9F0F-F6E6421E73C8', code: '03', urlPath: 'https://idmed-np.csaude.org.mz', port: '5003', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '3C0BF87C-87F3-4AF6-B95B-2D93F6B274AE', code: '04', urlPath: 'https://idmed-zb.csaude.org.mz', port: '5004', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '2408211C-7ACD-42C5-AC52-A3ACBCA747CF', code: '05', urlPath: 'https://idmed-tt.csaude.org.mz', port: '5005', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '29467F85-6CE4-4757-8AC5-E18FCFE0784C', code: '06', urlPath: 'https://idmed-mn.csaude.org.mz', port: '5006', destination: 'IDMED', username: 'iDMED', password:'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '668BD439-B176-4FF5-9525-81D9DFB84F6D', code: '07', urlPath: 'https://idmed-sf.csaude.org.mz', port: '5007', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'EDE2C07E-EE4B-4DED-8A7D-16D58BFB3751', code: '08', urlPath: 'https://idmed-ib.csaude.org.mz', port: '5008', destination: 'IDMED', username: 'iDMED', password:'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '6C50D9EB-9165-49AE-8A33-C9C837F58084', code: '09', urlPath: 'https://idmed-gz.csaude.org.mz', port: '5009', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: 'DFF5C2ED-FB41-4574-9C26-BB164605BC00', code: '10', urlPath: 'https://idmed-mp.csaude.org.mz', port: '3302', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        provincialServerList.add(new LinkedHashMap(id: '9E0AD237-9C7E-4656-9E58-D311F5E47F28', code: '11', urlPath: 'https://idmed-mc.csaude.org.mz', port: '3300', destination: 'IDMED', username: 'iDMED', password: 'N/A', 'dbname':'N/A'))
        return provincialServerList
    }


    List<Object> listSystemConfigs() {
        List<Object> systemConfigsList = new ArrayList<>()
        systemConfigsList.add(new LinkedHashMap(id: 'E55F958E-89B3-4BFD-90AF-B1DFFBAE62F8', value: 'ON', key: 'PATIENT_MIGRATION_ENGINE', description: 'Patient migration engine'))
        systemConfigsList.add(new LinkedHashMap(id: 'C35CC29F-CA73-4B26-9AC1-BC440750F854', value: 'ON', key: 'STOCK_MIGRATION_ENGINE', description: 'Stock migration engine'))
        systemConfigsList.add(new LinkedHashMap(id: '570911CB-6D46-4A3A-8A56-906A73DF1062', value: 'ON', key: 'PARAMS_MIGRATION_ENGINE', description: 'Params migration engine'))
        // systemConfigsList.add(new LinkedHashMap(id: 'E6CDDA47-DC11-4DAA-8672-04B37DEE9703', value: 'LOCAL', key: 'INSTALATION_TYPE',  description: 'Local/Provincial Instalation'))
        systemConfigsList.add(new LinkedHashMap(id: 'B5C44B42-3328-40D9-90D7-6DFF90A672D4', value: 'true', key: 'ACTIVATE_DATA_MIGRATION', description: 'Migração de dados'))
        systemConfigsList.add(new LinkedHashMap(id: '550e8400-e29b-41d4-a716-446655440000', value: '3', key: 'MAX_LOGIN_TRIES', description: 'Número máximo de tentativas para Login'))
        systemConfigsList.add(new LinkedHashMap(id: '3b241101-e2bb-4255-8caf-4136c566a964', value: '90', key: 'MAX_ACTIVE_DAYS_WITHOUT_LOGIN', description: 'Número máximo de dias em activo sem login'))
        systemConfigsList.add(new LinkedHashMap(id: 'ff98828d-0e91-468c-809f-d391edf8cd24', value: '2.0.0', key: 'APP_VERSION', description: 'Versão do iDMED'))

        // Configuação padrao das rotinas
        systemConfigsList.add(new LinkedHashMap(id: '8F2E4B1A-9C3D-4E5F-A1B2-C3D4E5F67890', value: 'true', key: 'DISPENSA_IDMED_OPENMRS_ATIVO', description: 'Envio de dispensas do IDMED para OpenMRS'))
        systemConfigsList.add(new LinkedHashMap(id: '2A5B8C9D-1E4F-7A0B-3C6D-9E2F5A8B1C4D', value: 'true', key: 'PACIENTE_IDMED_OPENMRS_ATIVO', description: 'Envio de pacientes do IDMED para OpenMRS'))
        systemConfigsList.add(new LinkedHashMap(id: '7F1E4D8A-2B5C-9E6F-0A3B-6C9D2E5F8A1B', value: 'true', key: 'PACIENTE_OPENMRS_IDMED_ATIVO', description: 'Atualização de dados do pacientes do OpenMRS para IDMED'))
        systemConfigsList.add(new LinkedHashMap(id: '9E2B5E8A-1D4A-7C0D-3F6C-9C2F5A8D1E4A', value: 'true', key: 'LOGIN_STATUS_USUARIOS_ATIVO', description: 'Validação do estado de acesso de usuários'))

        systemConfigsList.add(new LinkedHashMap(id: '3C6F9B2E-5D8A-1F4A-7B0C-3D6E9F2A5B8C', value: 'true', key: 'SP_DISPENSA_TRANSITO_IDMED_ATIVO', description: 'Carregamento de dispensas de Pacientes em Trânsito do Servidor Provincial para IDMED'))
        systemConfigsList.add(new LinkedHashMap(id: '4A7D0B3C-6E9F-2B5E-8A1D-4E7A0D3C6F9B', value: 'true', key: 'SP_DISPENSA_REFERIDOS_IDMED_ATIVO', description: 'Carregamento de dispensas de Pacientes Referidos do Servidor Provincial para IDMED'))

        systemConfigsList.add(new LinkedHashMap(id: '1D4A7D0B-3C6E-9F2B-5E8A-1D4E7A0D3C6F', value: 'true', key: 'EPISODIO_IDART_PROVINCIAL_IDMED_ATIVO', description: 'Carregamento de episódios de pacientes do Idart Provincial para IDMED'))
        systemConfigsList.add(new LinkedHashMap(id: '8E1A4D7A-0C3D-6F9B-2C5E-8A1D4E7A0C3D', value: 'true', key: 'EPISODIO2_IDART_PROVINCIAL_IDMED_ATIVO', description: 'Atualização de episódios de pacientes do Idart Provincial para IDMED'))
        systemConfigsList.add(new LinkedHashMap(id: '5B8E1A4D-7A0C-3D6F-9B2C-5E8A1D4E7A0C', value: 'true', key: 'PACIENTE_IDART_PROVINCIAL_IDMED_ATIVO', description: 'Carregamento de pacientes do Idart Provincial para IDMED'))

        systemConfigsList.add(new LinkedHashMap(id: '2E5B8E1A-4D7A-0C3D-6F9B-2C5E8A1D4E7A', value: 'true', key: 'EPISODIO_IDMED_IDART_PROVINCIAL_ATIVO', description: 'Envio de episódios de pacientes do IDMED para Idart Provincial'))
        systemConfigsList.add(new LinkedHashMap(id: '6C9B2E5B-8E1A-4D7A-0C3D-6F9B2C5E8A1D', value: 'true', key: 'REFERENCIA_IDMED_IDART_PROVINCIAL_ATIVO', description: 'Envio de referências de pacientes do IDMED para Idart Provincial'))

        return systemConfigsList
    }

    List<Object> listMigrationStage() {
        List<Object> migrationStageList = new ArrayList<>()
        migrationStageList.add(new LinkedHashMap(id: 'B15A0866-4A82-4826-9C3B-AD0532DCB077', code: 'STOCK_MIGRATION_STAGE', value: 'NOT_STARTED'))
        migrationStageList.add(new LinkedHashMap(id: '23B894EB-E9BB-4EFE-A2E4-C0776095AC35', code: 'PATIENT_MIGRATION_STAGE', value: 'NOT_STARTED'))
        migrationStageList.add(new LinkedHashMap(id: 'DCDCA69E-3FCF-4815-B0F2-8CEAF610AAB0', code: 'PARAMS_MIGRATION_STAGE', value: 'NOT_STARTED'))

        return migrationStageList
    }

    List<Object> listStockOperationType() {
        List<Object> stockOperationTypeList = new ArrayList<>()
        stockOperationTypeList.add(new LinkedHashMap(id: '919327EC-CA8B-4529-9B92-769CECB96785', code: 'AJUSTE_POSETIVO', description: 'Ajuste Posetivo'))
        stockOperationTypeList.add(new LinkedHashMap(id: 'B54FDBC8-5DD2-4CFA-9B22-627B3CC58D36', code: 'AJUSTE_NEGATIVO', description: 'Ajuste Negativo'))
        stockOperationTypeList.add(new LinkedHashMap(id: 'C000A387-8076-4002-95B1-5EB4B993C848', code: 'SEM_AJUSTE', description: 'Sem Ajuste'))

        return stockOperationTypeList
    }

    List<Object> listPatientTransReferenceType() {
        List<Object> stockOperationTypeList = new ArrayList<>()
        stockOperationTypeList.add(new LinkedHashMap(id: '98309d25-dbba-4f77-a69b-f2b87126a738', code: 'TRANSFERENCIA', description: 'Transfêrencia'))
        stockOperationTypeList.add(new LinkedHashMap(id: '7992ccf1-e9f6-417e-8b59-ba0a66feeb0e', code: 'TRANSITO', description: 'Trânsito'))
        stockOperationTypeList.add(new LinkedHashMap(id: 'fac618a2-a98d-42a1-892e-71d56a850380', code: 'REFERENCIA_FP', description: 'Refêrencia para Farmácia Privada'))
        stockOperationTypeList.add(new LinkedHashMap(id: '36cfa45f-6277-4108-bf34-5030002540c7', code: 'REFERENCIA_DC', description: 'Refêrencia para Dispensa Comunitária'))
        stockOperationTypeList.add(new LinkedHashMap(id: '5e4111c1-556a-4e25-8045-0f520dff79de', code: 'VOLTOU_DA_REFERENCIA', description: 'Voltou da Refêrencia'))

        return stockOperationTypeList
    }


    List<Object> listFacilityType() {
        List<Object> facilityTypeList = new ArrayList<>()
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81900fee0181901608890000', code: 'FP', description: 'Farmácia Privada', type: "clinic"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81900fee018190163e0c0001', code: 'FC', description: 'Farmácia Comunitária', type: "clinic"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81900fee0181901674b20002', code: 'US', description: 'Unidade Sanitária', type: "clinic"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c801ab120000', code: 'PARAGEM_UNICA', description: 'Paragem Única', type: "clinic_sector"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c801fcac0001', code: 'PROVEDOR', description: 'Dispensa Comunitária pelo Provedor', type: "clinic_sector"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c8025ea10002', code: 'APE', description: 'Agente Polivalente Elementar', type: "clinic_sector"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c8029c890003', code: 'CLINICA_MOVEL', description: 'Clinica Móvel', type: "clinic_sector"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c802d7ec0004', code: 'BRIGADA_MOVEL', description: 'Brigada Móvel', type: "clinic_sector"))
        facilityTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c802d7ec0006', code: 'NORMAL', description: 'Atendimento Normal', type: "clinic_sector"))


        return facilityTypeList
    }


    List<Object> listIdentifierType() {
        List<Object> identifierTypeList = new ArrayList<>()
        identifierTypeList.add(new LinkedHashMap(id: '8BC2D0A9-9AC4-487B-B71F-F8088B1CB532', code: 'NID', description: 'NID', pattern: '##########/####/#####'))
        identifierTypeList.add(new LinkedHashMap(id: '50D0185F-A115-40D9-BF70-75E3F1F6DD91', code: 'NID_CCR', description: 'NID CCR', pattern: '##########/####/#####'))
        identifierTypeList.add(new LinkedHashMap(id: '9A502C09-5F57-4262-A3D5-CA6B62E0D58F', code: 'NID_PREP', description: 'NID PREP', pattern: '##########/####/#####'))
        identifierTypeList.add(new LinkedHashMap(id: '6C2D9E83-6B6B-4F78-9EE8-30CB19CDDF93', code: 'NID_TB', description: 'NIT', pattern: '##########/####/#####'))

        return identifierTypeList
    }


    List<Object> listClinicSectorType() {
        List<Object> clinicSectorTypeList = new ArrayList<>()
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c801ab120000', code: 'PARAGEM_UNICA', description: 'Paragem Única'))
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c801fcac0001', code: 'PROVEDOR', description: 'Dispensa Comunitária pelo Provedor'))
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c8025ea10002', code: 'APE', description: 'Agente Polivalente Elementar'))
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c8029c890003', code: 'CLINICA_MOVEL', description: 'Clinica Móvel'))
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c802d7ec0004', code: 'BRIGADA_MOVEL', description: 'Brigada Móvel'))
        clinicSectorTypeList.add(new LinkedHashMap(id: '8a8a823b81c7fa9d0181c802d7ec0006', code: 'NORMAL', description: 'Atendimento Normal'))

        return clinicSectorTypeList
    }

    List<Object> listDispenseMode() {
        List<Object> dispenseModeList = new ArrayList<>()
        dispenseModeList.add(new LinkedHashMap(id: '091737af-d6bf-4830-8e87-82572ffac9ea', code: 'DC_CM-FHN', openmrs_uuid: '091737af-d6bf-4830-8e87-82572ffac9ea', description: 'Clínicas Móveis - Distribuição durante o final do dia'))
        dispenseModeList.add(new LinkedHashMap(id: '1309d08a-5c73-4429-8f4b-43a551952858', code: 'US_FP_FHN', openmrs_uuid: '1309d08a-5c73-4429-8f4b-43a551952858', description: 'Farmácia Pública - Fora Hora Normal'))
        dispenseModeList.add(new LinkedHashMap(id: '3ab58d0e-f831-4966-97bd-209738f5e4df', code: 'DC_BM_HN', openmrs_uuid: '3ab58d0e-f831-4966-97bd-209738f5e4df', description: 'Brigadas Móveis - Distribuição durante o dia'))
        dispenseModeList.add(new LinkedHashMap(id: '467718bc-1756-4b3f-b1ee-98d01910153a', code: 'DC_CM_HN', openmrs_uuid: '467718bc-1756-4b3f-b1ee-98d01910153a', description: 'Clínicas Móveis - Distribuição durante o dia'))
        dispenseModeList.add(new LinkedHashMap(id: '4b51ace2-f778-4f54-bdaa-be2b350b7499', code: 'US_FP_HN', openmrs_uuid: '4b51ace2-f778-4f54-bdaa-be2b350b7499', description: 'Farmácia Pública - Hora Normal'))
        dispenseModeList.add(new LinkedHashMap(id: '870e2d25-c5ef-4e36-89db-0a4a37af214e', code: 'DC_PS', openmrs_uuid: '870e2d25-c5ef-4e36-89db-0a4a37af214e', description: 'Distribuição Comunitária pelo Provedor de Saúde'))
        dispenseModeList.add(new LinkedHashMap(id: 'd6ad74a1-ff67-4b81-afa1-a0d906462623', code: 'DC_BM_FHN', openmrs_uuid: 'd6ad74a1-ff67-4b81-afa1-a0d906462623', description: 'Brigadas Móveis - Distribuição durante o final do dia'))
        dispenseModeList.add(new LinkedHashMap(id: '0843c71b-be47-4de2-ba16-a08db52c1136', code: 'DC_APE', openmrs_uuid: '0843c71b-be47-4de2-ba16-a08db52c1136', description: 'Distribuição Comunitária pelos APEs'))
        dispenseModeList.add(new LinkedHashMap(id: 'd2eaec39-9c48-443b-a8d5-b2b163d42c53', code: 'DD_FP', openmrs_uuid: 'd2eaec39-9c48-443b-a8d5-b2b163d42c53', description: 'FARMAC/Farmácia Privada'))

        return dispenseModeList
    }


    List<Object> listDispenseType() {
        List<Object> dispenseTypeList = new ArrayList<>()
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf823190010', code: 'DD', description: 'Dispensa Diária'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf823190004', code: 'DN', description: 'Dispensa Semanal'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf823190005', code: 'FRM', description: 'Fluxo Rapido Mensal'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf78a8c0006', code: 'DM', description: 'Dispensa Mensal'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf78a8c0066', code: 'DB', description: 'Dispensa Bimensal'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf7ca4e0007', code: 'DT', description: 'Dispensa Trimestral'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf8044f0008', code: 'DS', description: 'Dispensa Semestral'))
        dispenseTypeList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbf823190009', code: 'DA', description: 'Dispensa Anual'))

        return dispenseTypeList

    }


    List<Object> listForm() {
        List<Object> formList = new ArrayList<>()
        formList.add(new LinkedHashMap(id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', code: 'Comp', description: 'Comprimido', 'unit': 'Comp(s)', 'how_to_use': 'Tomar'))
        formList.add(new LinkedHashMap(id: 'B61168FC-0178-4DC3-A89E-46A169A7457D', code: 'Xarope', description: 'Xarope', 'unit':  'mL(s)', 'how_to_use': 'Tomar'))
        formList.add(new LinkedHashMap(id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', code: 'Capsula', description: 'Capsula', 'unit': 'Capsula(s)', 'how_to_use': 'Tomar'))
        formList.add(new LinkedHashMap(id: '4EA7EF2B-7F86-4DA7-9B74-AF9B614A8DA6', code: 'Sabonete', description: 'Sabonete', 'unit': 'Sabonete(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '024D260C-F3A3-4149-ACDF-97A68855D85A', code: 'Supositório', description: 'Supositório', 'unit': 'Comp(s)', 'how_to_use': 'Inserir'))
        formList.add(new LinkedHashMap(id: '451906BE-792D-4506-98DB-408F69B97AB1', code: 'Descongestionante_nasal', description: 'Descongestionante Nasal', 'unit': 'Gota(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: 'D213A686-782C-4AF3-BC31-BA04EEF0EDB0', code: 'Solução_Oftámilca', description: 'Solução Oftámilca', 'unit': 'Gota(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '70E72CB3-F66D-41D7-B2C9-8F545E880FC6', code: 'Gotas_Ouvidos', description: 'Gotas para os ouvidos', 'unit': 'Gota(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', code: 'Suspensão', description: 'Suspensão', 'unit': 'mL(s)', 'how_to_use': 'Tomar'))
        formList.add(new LinkedHashMap(id: '17828974-026F-41CF-A906-E97ABFB2A0AC', code: 'Creme', description: 'Creme', 'unit': 'Creme(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '700FBB4F-C67C-4085-B05C-33259C29F90E', code: 'Pomada', description: 'Pomada', 'unit': 'Pomada(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '935436AC-8CB2-4691-8FC6-99209CDB9906', code: 'Gel', description: 'Gel', 'unit': 'Gel', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: '3B627885-00C6-4F49-97D0-11CECD6CABB4', code: 'Gel_Oral', description: 'Gel Oral', 'unit': 'Gel', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: 'DB4162F8-A5B3-4656-ABEF-AC977E37A9EF', code: 'Loção', description: 'Loção', 'unit': 'Gota(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: 'BA8E6254-0F88-43D7-8C39-500756AA7B2F', code: 'Pomada_olhos', description: 'Pomada para os olhos', 'unit': 'Pomada(s)', 'how_to_use': 'Aplicar'))
        formList.add(new LinkedHashMap(id: 'A80409ED-89FC-40AF-BA34-5CD0BA886570', code: 'Creme_Vaginal', description: 'Creme Vaginal', 'unit': 'Creme(s)', 'how_to_use': 'Aplicar'))

        formList.add(new LinkedHashMap(id: 'E8427B78-B4B2-4570-8721-03A60425909D', code: 'Suspensão_Injectável', description: 'Suspensão Injectável', 'unit': 'mL(s)', 'how_to_use': 'Administrar'))
        formList.add(new LinkedHashMap(id: 'A19B6D0E-4F11-42C9-B720-9C8B617E29F8', code: 'Anel_Vaginal', description: 'Anel_Vaginal', 'unit': 'Anel', 'how_to_use': 'Inserir'))
        return formList

    }

    List<Object> listDuration() {
        List<Object> durationList = new ArrayList<>()
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbd136bf0006', description: 'Um dia', weeks: 0))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbcecfe30000', description: 'Uma Semana', weeks: 1))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbcf41280001', description: 'Duas Semanas', weeks: 2))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbcf9a550002', description: 'Um mês', weeks: 4))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbd02e620003', description: 'Dois meses', weeks: 8))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbd079e20004', description: 'Três meses', weeks: 12))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbd136bf0005', description: 'Seis meses', weeks: 24))
        durationList.add(new LinkedHashMap(id: 'ff8081817cbbce66017cbbd136bf0007', description: ' Um ano', weeks: 48))

        return durationList
    }


    List<Object> listStartStopReason() {
        List<Object> startStopReasonList = new ArrayList<>()
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bc4cdd0003', isStartReason: false, reason: 'Termino do Tratamento', code: 'TERMINO_DO_TRATAMENTO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bc5cdd0004', isStartReason: true, reason: 'Transferido DE', code: 'TRANSFERIDO_DE'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bc6cdd0005', isStartReason: true, reason: 'Reinicio ao Tratamento', code: 'REINICIO_TRATAMETO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bc9cdd0008', isStartReason: false, reason: 'Abandono', code: 'ABANDONO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bf1cdd0009', isStartReason: false, reason: 'Transferido PARA', code: 'TRANSFERIDO_PARA'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bf1ldc0012', isStartReason: true, reason: 'Inicio na Maternidade', code: 'INICIO_MATERNIDADE'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bc7cdd0006', isStartReason: true, reason: 'Voltou da Referência', code: 'VOLTOU_REFERENCIA'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bf2cdd0010', isStartReason: false, reason: 'Referido para outra Farmácia', code: 'REFERIDO_PARA'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bf3cdd0011', isStartReason: true, reason: 'Trânsito', code: 'TRANSITO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017cj9bh1ldc0013', isStartReason: false, reason: 'Óbito', code: 'OBITO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017cj9bh1ldc0014', isStartReason: true, reason: 'Em Manutenção', code: 'MANUNTENCAO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0002', isStartReason: true, reason: 'Novo Paciente', code: 'NOVO_PACIENTE'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0003', isStartReason: true, reason: 'Inicio CCR', code: 'INICIO_CCR'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0004', isStartReason: false, reason: 'Fim PPE', code: 'FIM_PPE'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0005', isStartReason: false, reason: 'Tratamento Suspenso Pelo Clinico', code: 'TSPC'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0015', isStartReason: false, reason: 'Voltou a ser referido para outra Farmacia', code: 'VOLTOU_A_SER_REFERIDO_PARA'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0016', isStartReason: false, reason: 'Outro', code: 'OUTRO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0017', isStartReason: false, reason: 'Referido para dispensa comunitária', code: 'REFERIDO_DC'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0018', isStartReason: false, reason: 'Referido para Sector Clinico', code: 'REFERIDO_SECTOR_CLINICO'))
        startStopReasonList.add(new LinkedHashMap(id: 'ff8081817c9791ee017c99bbb2aa0019', isStartReason: false, reason: 'Voltou Para Farmacia US', code: 'VOLTOU_FARMACIA_US'))
        return startStopReasonList

    }


    List<Object> listGroupType() {
        List<Object> groupTypeList = new ArrayList<>()
        groupTypeList.add(new LinkedHashMap(id: 'c586e280-8f23-11ec-b909-0242ac120002', code: '001', description: 'GAAC'))
        groupTypeList.add(new LinkedHashMap(id: 'c586e50a-8f23-11ec-b909-0242ac120002', code: '002', description: 'Abordagem Familiar'))
        groupTypeList.add(new LinkedHashMap(id: 'c586e8b6-8f23-11ec-b909-0242ac120002', code: '003', description: 'Clube de Adesão '))

        return groupTypeList
    }


    List<Object> listTherapeuticLine() {
        List<Object> therapeuticLineList = new ArrayList<>()
        therapeuticLineList.add(new LinkedHashMap(id: 'ff8081817cb69063017cbbaea6f30009', code: '1', uuid: '7323b36e-fedf-45bc-b866-083854c09f7b', description: '1ª Linha'))
        therapeuticLineList.add(new LinkedHashMap(id: 'ff8081817cb69063017cbbagb014av0c', code: '1_ALT', uuid: '6E117555-BB10-43C9-83B4-9171A1734BB7', description: '1ª Linha Alternativa'))
        therapeuticLineList.add(new LinkedHashMap(id: 'ff8081817cb69063017cbbaeef36000a', code: '2', uuid: '8112b34d-6695-48b2-975a-7fd7abb06a6e', description: '2ª Linha'))
        therapeuticLineList.add(new LinkedHashMap(id: 'ff8081817cb69063017cbbaf1701000b', code: '3', uuid: '843c7cff-f2ba-4134-a015-43370c614de6', description: '3ª Linha'))

        return therapeuticLineList

    }

    List<Object> listEpisodeType() {
        List<Object> episodeTypeList = new ArrayList<>()
        episodeTypeList.add(new LinkedHashMap(id: 'ff8081817c7ace37017c7ae8f0050000', code: 'INICIO', description: 'Episódio de Início'))
        episodeTypeList.add(new LinkedHashMap(id: 'ff8081817c7ace37017c7ae917be0001', code: 'FIM', description: 'Episódio de Fim'))

        return episodeTypeList

    }


    List<Object> listSpetialPrescriptionMotive() {
        List<Object> spetialPrescriptionMotiveList = new ArrayList<>()
        spetialPrescriptionMotiveList.add(new LinkedHashMap(id: '8a8a823b81dd5c600181dd5db9820000', code: 'OUTRO', description: 'Outro'))
        spetialPrescriptionMotiveList.add(new LinkedHashMap(id: '8a8a823b8200e213018210d5a2ee0027', code: 'PERDA_MEDICAMENTO', description: 'Perda de Medicamento'))
        spetialPrescriptionMotiveList.add(new LinkedHashMap(id: '8a8a823b8200e213018210d5e4900028', code: 'AUSENCIA_CLINICO', description: 'Ausência do Clínico'))
        spetialPrescriptionMotiveList.add(new LinkedHashMap(id: '8a8a823b8200e213018210d619ba0029', code: 'LABORATORIO', description: 'Laboratório'))
        spetialPrescriptionMotiveList.add(new LinkedHashMap(id: '8a8a823b8200e213018210d6715b002a', code: 'ROTURA_STOCK', description: 'Rotura de Stock'))

        return spetialPrescriptionMotiveList
    }


    List<Object> listClinicalService() {
        List<Object> clinicalServiceList = new ArrayList<>()
        clinicalServiceList.add(new LinkedHashMap(id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F', code: 'TARV', description: 'Tratamento Anti-RetroViral', identifierType: IdentifierType.findById('8BC2D0A9-9AC4-487B-B71F-F8088B1CB532'), active: true))
        clinicalServiceList.add(new LinkedHashMap(id: '6D12193B-7D5D-4665-8FC6-A03855986FBD', code: 'TPT', description: 'Tratamento Preventivo da Tuberculose', identifierType: IdentifierType.findById('50D0185F-A115-40D9-BF70-75E3F1F6DD91'), active: true))
        clinicalServiceList.add(new LinkedHashMap(id: '165C876C-F850-436F-B0BB-80D519056BC3', code: 'PREP', description: 'Tratamento Preventivo à Infecção Pelo HIV', identifierType: IdentifierType.findById('9A502C09-5F57-4262-A3D5-CA6B62E0D58F'), active: true))
        clinicalServiceList.add(new LinkedHashMap(id: 'C2AE49AE-FD70-4E6C-8C96-9131B62ECEDF', code: 'PPE', description: 'Tratamento Após Exposição ao HIV', identifierType: IdentifierType.findById('8BC2D0A9-9AC4-487B-B71F-F8088B1CB532'), active: true))
        clinicalServiceList.add(new LinkedHashMap(id: 'F5FEAD76-3038-4D3D-AC28-D63B9952F022', code: 'TB', description: 'Tratamento da Tuberculose', identifierType: IdentifierType.findById('6C2D9E83-6B6B-4F78-9EE8-30CB19CDDF93'), active: true))
        clinicalServiceList.add(new LinkedHashMap(id: 'C4A3FFFA-BA52-4BEF-948D-1C8C90C3F38E', code: 'CCR', description: 'Consulta de Crianca em Risco', identifierType: IdentifierType.findById('50D0185F-A115-40D9-BF70-75E3F1F6DD91'), active: true))

        return clinicalServiceList
    }

    List<Object> listClinicalServiceAttribute() {
        List<Object> clinicalServiceAttributeList = new ArrayList<>()
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44f', clinicalService: '80A7852B-57DF-4E40-90EC-ABDE8403E01F', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef6f59d0000'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44a', clinicalService: '80A7852B-57DF-4E40-90EC-ABDE8403E01F', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef853610003'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44b', clinicalService: '80A7852B-57DF-4E40-90EC-ABDE8403E01F', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef7ab3f0002'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44c', clinicalService: '80A7852B-57DF-4E40-90EC-ABDE8403E01F', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef760800001'))

        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44e', clinicalService: '6D12193B-7D5D-4665-8FC6-A03855986FBD', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef853610003'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44g', clinicalService: '6D12193B-7D5D-4665-8FC6-A03855986FBD', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef7ab3f0002'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44r', clinicalService: '6D12193B-7D5D-4665-8FC6-A03855986FBD', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef6f59d0000'))

        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44i', clinicalService: '165C876C-F850-436F-B0BB-80D519056BC3', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef6f59d0000'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44j', clinicalService: '165C876C-F850-436F-B0BB-80D519056BC3', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef853610003'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44k', clinicalService: '165C876C-F850-436F-B0BB-80D519056BC3', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef7ab3f0002'))

        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44m', clinicalService: 'F5FEAD76-3038-4D3D-AC28-D63B9952F022', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef6f59d0000'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44n', clinicalService: 'F5FEAD76-3038-4D3D-AC28-D63B9952F022', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef853610003'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44o', clinicalService: 'F5FEAD76-3038-4D3D-AC28-D63B9952F022', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef7ab3f0002'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: '02a8d740-362f-4df8-b5ba-061594d8a44p', clinicalService: 'F5FEAD76-3038-4D3D-AC28-D63B9952F022', clinicalServiceAttributeType: 'ff8081817c99e33a017c9ef760800001'))
        return clinicalServiceAttributeList
    }

    List<Object> listClinicalServiceAttributeType() {
        List<Object> clinicalServiceAttributeList = new ArrayList<>()
        clinicalServiceAttributeList.add(new LinkedHashMap(id: 'ff8081817c99e33a017c9ef6f59d0000', code: 'THERAPEUTICAL_REGIMEN', description: 'Regime terapeutico'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: 'ff8081817c99e33a017c9ef853610003', code: 'PRESCRIPTION_CHANGE_MOTIVE', description: 'Motivo de alteracao de prescricao'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: 'ff8081817c99e33a017c9ef7ab3f0002', code: 'PATIENT_TYPE', description: 'Tipo Paciente'))
        clinicalServiceAttributeList.add(new LinkedHashMap(id: 'ff8081817c99e33a017c9ef760800001', code: 'THERAPEUTICAL_LINE', description: 'Linha terapeutica'))

        return clinicalServiceAttributeList
    }

    List<Object> listHealthInformationSystem() {
        List<Object> healthInformationSystemList = new ArrayList<>()
        healthInformationSystemList.add(new LinkedHashMap(id: 'ff8080817d9aa854017d9e2809b50008', abbreviation: 'OpenMRS', description: 'OpenMRS', active: true))
        healthInformationSystemList.add(new LinkedHashMap(id: '06e15e5c-d771-477a-9db5-130a038af290', abbreviation: 'Província', description: 'Servidor Provincial', active: true))

        return healthInformationSystemList
    }


    List<Object> listInteroperabilityType() {
        List<Object> interoperabilityTypeList = new ArrayList<>()
        interoperabilityTypeList.add(new LinkedHashMap(id: 'D3B4942C-514B-47A5-BAED-F3A47A466E09', code: 'URL_BASE', description: 'URL BASE'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'CFCA8326-0AB9-46D0-B5CE-1E277E564823', code: 'URL_BASE_REPORTING_REST', description: 'URL BASE REPORTING REST'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '0718F9D9-39A8-405A-8092-5BDDE4204B25', code: 'OPENMRS_VERSION', description: 'OPENMRS VERSION'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'c5531ee8-2d73-4006-829a-e46645391c60', code: 'OPENMRS_LOCATION_UUID', description: 'OPENMRS LOCATION UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'CEBEF157-57B9-4442-A1E3-59FA92E818D6', code: 'FORM_FILA_UUID', description: 'FILA FORM UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '73715EBA-8BDD-4C55-8895-C01FB0D5AF57', code: 'FILA_REGIMEN_CONCEPT_UUID', description: 'FILA REGIMEN CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'EE650283-C62F-43E7-8835-A7B7A9706119', code: 'FILA_VISIT_CONCEPT_UUID', description: 'FILA VISIT CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '416001E8-2F6E-46D4-A448-88E3188160E5', code: 'FILA_DISPENSED_TYPE_CONCEPT_UUID', description: 'FILA DISPENSED TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'BD2B4C58-4CEE-49D9-9D70-ED2B3B777064', code: 'FILA_PATIENT_TYPE_UUID', description: 'FILA PATIENT TYPE UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '7C7C893F-D22F-46A7-8CE4-953A9853B6A8', code: 'FILA_NEXT_VISIT_CONCEPT_UUID', description: 'FILA NEXT VISIT CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'FA1E03AA-EEB0-446E-89F0-BB966C9FC65C', code: 'FORM_FILT_UUID', description: 'FILT FORM UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '44A81300-7B59-4196-8BF6-B15CBFD11667', code: 'FILT_REGIMEN_CONCEPT_UUID', description: 'FILT REGIMEN CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'BCD2F582-6737-48B6-B56E-FF80EE37DD22', code: 'FILT_DISPENSED_TYPE_CONCEPT_UUID', description: 'FILT DISPENSED TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'DE6B0CA7-5481-407C-B542-D0E56EF43342', code: 'FILT_TPT_PATIENT_TYPE_UUID', description: 'FILT TPT PATIENT TYPE UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '8B0D0D86-C6C1-4327-8206-78DC4DFC6EDA', code: 'FILT_VISIT_CONCEPT_UUID', description: 'FILT VISIT CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '22AD8E9F-1A09-4794-A393-4D4C7F4676F8', code: 'FILT_NEXT_VISIT_CONCEPT_UUID', description: 'FILT NEXT VISIT CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'A7D2F52E-8409-492B-8ECD-A7093E83224E', code: 'DISPENSED_AMOUNT_CONCEPT', description: 'DISPENSED AMOUNT CONCEPT'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '585F5AAC-A014-4F6A-84C3-428C9C68EFD8', code: 'DOSAGE_CONCEPT_UUID', description: 'DOSAGE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'DD0FCFE6-94DB-4A8B-9760-C8EEB512044A', code: 'DISPENSE_MODE_CONCEPT_UUID', description: 'DISPENSE MODE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '3D7530B4-C5DE-4073-8A0D-4137CBA6297D', code: 'MONTHLY_DISPENSED_TYPE_CONCEPT_UUID', description: 'MONTHLY DISPENSED TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '517C1996-CE00-4159-87F2-3F9DA2D24962', code: 'QUARTERLY_DISPENSED_TYPE_CONCEPT_UUID', description: 'QUARTERLY DISPENSED TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '4794A14A-EF6E-4AD7-A7FD-D3E291AC2BDD', code: 'SEMESTRAL_DISPENSED_TYPE_CONCEPT_UUID', description: 'SEMESTRAL DISPENSED TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'B361B735-26D0-4B58-9BB1-35C317778698', code: 'PATIENT_TYPE_INITIAL_UUID', description: 'PATIENT TYPE INITIAL UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '05C533A6-0BC1-495C-95EF-80C0B3C063A7', code: 'PATIENT_TYPE_CONTINUE_UUID', description: 'PATIENT TYPE CONTINUE UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '30C65546-FACA-489F-809B-60EFCDFB5BF2', code: 'PATIENT_TYPE_RESTART_UUID', description: 'PATIENT TYPE RESTART UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '80B52D48-0946-4267-9535-EC01484914E8', code: 'PATIENT_TYPE_END_UUID', description: 'PATIENT TYPE END UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '329B13FD-82BE-429F-ABD3-A0B72051332C', code: 'FILA_ENCOUNTER_TYPE_CONCEPT_UUID', description: 'FILA ENCOUNTER TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'B2AAF0F5-D23B-4DD4-95BA-0ACCC0656A9C', code: 'FILT_ENCOUNTER_TYPE_CONCEPT_UUID', description: 'FILT ENCOUNTER TYPE CONCEPT UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '94094FC8-99F1-4A23-8E2A-1E823E30B6D7', code: 'UNIVERSAL_PROVIDER_UUID', description: 'UNIVERSAL PROVIDER UUID'))

        interoperabilityTypeList.add(new LinkedHashMap(id: '302bca64-9bfd-4fa8-8e29-be08cdfd0e92', code: 'PATIENT_IDENTIFIER_TYPE_NID_UUID', description: 'PATIENT IDENTIFIER TYPE NID UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'b5907b94-8924-4ad3-8028-3722d787e79e', code: 'PATIENT_IDENTIFIER_TYPE_PREP_UUID', description: 'PATIENT IDENTIFIER TYPE PREP UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '1b221e2b-69b4-4bd7-a0a0-56ffeae9137e', code: 'PATIENT_IDENTIFIER_TYPE_CCR_UUID', description: 'PATIENT IDENTIFIER TYPE CCR UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '07e09518-00a7-4e8e-9a55-ce405660c118', code: 'PATIENT_IDENTIFIER_TYPE_CRAM_UUID', description: 'PATIENT IDENTIFIER TYPE CRAM UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'e4d42149-d962-4a15-bcd2-7cecad5ce73c', code: 'PATIENT_IDENTIFIER_TYPE_TB_UUID', description: 'PATIENT IDENTIFIER TYPE TB UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '7908d8f2-f158-44cc-97b5-f289545791e8', code: 'PATIENT_IDENTIFIER_TYPE_NUIC_UUID', description: 'PATIENT IDENTIFIER TYPE NUIC UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: 'c5531ee8-2d73-4006-829a-e46645391c60', code: 'OPENMRS_LOCATION_UUID', description: 'OPENMRS LOCATION UUID'))
        interoperabilityTypeList.add(new LinkedHashMap(id: '2aa97747-dbeb-4cbe-b838-d5174a5fc9ec', code: 'OPENMRS_USER_PROVIDER_UUID', description: 'OPENMRS USER PROVIDER UUID'))

        return interoperabilityTypeList
    }


    List<Object> listInteroperabilityAttribute() {
        List<Object> interoperabilityAttributeList = new ArrayList<>()
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'CA5B9D8A-F0B4-4957-B249-63A75214CD0C', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'http://localhost/openmrs/ws/rest/v1/', interoperabilityType: InteroperabilityType.findById('D3B4942C-514B-47A5-BAED-F3A47A466E09')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '34C5A7BB-E7DE-4CFB-B349-2BC36FD4FFCA', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'http://localhost/openmrs/ws/rest/v1/reportingrest/cohort/ba36b483-c17c-454d-9a3a-f060a933c6da', interoperabilityType: InteroperabilityType.findById('CFCA8326-0AB9-46D0-B5CE-1E277E564823')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'CE34FBAD-D1F1-4C8E-A823-CBCF3137A0BA', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '2.x', interoperabilityType: InteroperabilityType.findById('0718F9D9-39A8-405A-8092-5BDDE4204B25')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'b9437cf5-0b50-482f-8e92-20ff4d6f5e6d', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '22a0d433-b9fd-46c1-9387-97e225f8014f', interoperabilityType: InteroperabilityType.findById('c5531ee8-2d73-4006-829a-e46645391c60')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '876FA63C-8F5F-42FB-A2D5-24F1BFEDD3ED', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '49857ace-1a92-4980-8313-1067714df151', interoperabilityType: InteroperabilityType.findById('CEBEF157-57B9-4442-A1E3-59FA92E818D6')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'AF6051CD-7AB6-4841-A47D-2D6608095822', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1d83e4e-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('73715EBA-8BDD-4C55-8895-C01FB0D5AF57')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'FFB2BE0D-2191-41A1-88A4-78ACADE251D4', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1e2efd8-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('EE650283-C62F-43E7-8835-A7B7A9706119')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '9CC90263-5A21-47D9-AAC0-33FE9FB60C09', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'd5c15047-58f3-4eb2-9f98-af82e3531cb5', interoperabilityType: InteroperabilityType.findById('416001E8-2F6E-46D4-A448-88E3188160E5')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '7439C298-97EE-486D-9711-D9D17BA161F9', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '93603742-1cae-4970-9077-e2b27e46bd7e', interoperabilityType: InteroperabilityType.findById('BD2B4C58-4CEE-49D9-9D70-ED2B3B777064')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'F9AE9B98-CE83-4010-85E5-B9F569555054', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1e2efd8-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('7C7C893F-D22F-46A7-8CE4-953A9853B6A8')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'A542DCF1-99AB-4DC2-AAE2-80279D34B0A9', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '4ce83895-5c0e-4170-b0cc-d3974b54131f', interoperabilityType: InteroperabilityType.findById('FA1E03AA-EEB0-446E-89F0-BB966C9FC65C')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '4F3C8B49-20F1-4BBB-83C8-7F217A35D505', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '9db4ce3b-4c1c-45dd-905f-c984a052f26e', interoperabilityType: InteroperabilityType.findById('44A81300-7B59-4196-8BF6-B15CBFD11667')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '6D5E1BBE-CC76-439C-A11A-8CD804B044AF', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'd5c15047-58f3-4eb2-9f98-af82e3531cb5', interoperabilityType: InteroperabilityType.findById('BCD2F582-6737-48B6-B56E-FF80EE37DD22')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '531A21E7-2123-4B72-803B-4C6FADE0D0BD', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '93603742-1cae-4970-9077-e2b27e46bd7e', interoperabilityType: InteroperabilityType.findById('DE6B0CA7-5481-407C-B542-D0E56EF43342')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'CAFC31D5-8617-47A0-A286-C63971772337', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'd9911494-b231-4b3c-9246-1fe5f269476c', interoperabilityType: InteroperabilityType.findById('8B0D0D86-C6C1-4327-8206-78DC4DFC6EDA')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'AB2F5969-E6C9-4F37-94F1-992DF8788DF2', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'b7c246bc-f2b6-49e5-9325-911cdca7a8b3', interoperabilityType: InteroperabilityType.findById('22AD8E9F-1A09-4794-A393-4D4C7F4676F8')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'A77D1618-1321-44CF-85FA-9BF1D6099C90', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1de2ca0-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('A7D2F52E-8409-492B-8ECD-A7093E83224E')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '4E165FE8-7A33-4349-B8A7-10FF204A7DBD', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1de28ae-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('585F5AAC-A014-4F6A-84C3-428C9C68EFD8')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '73E51E2B-4DEB-450B-8F0E-E355BC988D34', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '40a9a12b-1205-4a55-bb93-caf15452bf61', interoperabilityType: InteroperabilityType.findById('DD0FCFE6-94DB-4A8B-9760-C8EEB512044A')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'A34A1593-2E3A-4371-8B35-BAAFF3FA058B', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '1098AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', interoperabilityType: InteroperabilityType.findById('3D7530B4-C5DE-4073-8A0D-4137CBA6297D')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '82BDF33C-42FB-4226-B0CF-52D4B793049D', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'f53848d5-3521-4cc8-ac72-d63adef281a1', interoperabilityType: InteroperabilityType.findById('517C1996-CE00-4159-87F2-3F9DA2D24962')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'A4B2E452-1565-48BD-B866-2EB7DE823BA0', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '3069be5c-cd02-4ddb-aa1f-bdd71d3dd6be', interoperabilityType: InteroperabilityType.findById('4794A14A-EF6E-4AD7-A7FD-D3E291AC2BDD')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'F6EFA41C-4308-4782-835C-8B2A9E2B3B61', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1d9ef28-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('B361B735-26D0-4B58-9BB1-35C317778698')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '22E9B370-EA14-4B8D-98BC-1E3ADC84F72E', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1d9f036-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('05C533A6-0BC1-495C-95EF-80C0B3C063A7')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '4FE4D425-5514-4CDA-9467-0AD33A5CA9CF', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1de1bfc-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('30C65546-FACA-489F-809B-60EFCDFB5BF2')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '4107FCCF-4B40-4B58-BFDD-0B9BF83D25BC', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e1d9facc-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('80B52D48-0946-4267-9535-EC01484914E8')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '9881585A-79F3-4756-AD05-7968775BF98C', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e279133c-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('329B13FD-82BE-429F-ABD3-A0B72051332C')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '5143CEA0-EAD7-4A1C-8D0D-DC1697B23AA3', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '24bd60e2-a1c9-4159-a24f-12af15b77510', interoperabilityType: InteroperabilityType.findById('B2AAF0F5-D23B-4DD4-95BA-0ACCC0656A9C')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'C60F7C25-7198-4605-BDD4-FF6DF3309AB2', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '7013d271-1bc2-4a50-bed6-8932044bc18f', interoperabilityType: InteroperabilityType.findById('94094FC8-99F1-4A23-8E2A-1E823E30B6D7')))

        interoperabilityAttributeList.add(new LinkedHashMap(id: 'eeaf1b98-5b87-489f-876d-8da6c49b3da4', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e2b966d0-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('302bca64-9bfd-4fa8-8e29-be08cdfd0e92')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'b89bfeef-cc93-43b9-879b-2b67e967c9c5', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'bce7c891-27e9-42ec-abb0-aec3a641175e', interoperabilityType: InteroperabilityType.findById('b5907b94-8924-4ad3-8028-3722d787e79e')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '09a67454-5089-4e05-bddd-251159c29f3f', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e2b97b70-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('1b221e2b-69b4-4bd7-a0a0-56ffeae9137e')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'ee1d6717-d944-4b93-a232-a4170c06cffb', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '1c72703d-fb55-439e-af4f-ef39a1049e19', interoperabilityType: InteroperabilityType.findById('07e09518-00a7-4e8e-9a55-ce405660c118')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '049a1f1d-efa3-4082-b09c-5614a12d312e', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e2b97e40-1d5f-11e0-b929-000c29ad1d07', interoperabilityType: InteroperabilityType.findById('e4d42149-d962-4a15-bcd2-7cecad5ce73c')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: '13def87c-89df-4dc8-a257-596e615befff', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: 'e89c8925-35cc-4a29-9002-6b36bf3fd47f', interoperabilityType: InteroperabilityType.findById('7908d8f2-f158-44cc-97b5-f289545791e8')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'b9437cf5-0b50-482f-8e92-20ff4d6f5e6d', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '22a0d433-b9fd-46c1-9387-97e225f8014f', interoperabilityType: InteroperabilityType.findById('c5531ee8-2d73-4006-829a-e46645391c60')))
        interoperabilityAttributeList.add(new LinkedHashMap(id: 'f8ed6fe6-b256-4bef-b0ac-a570c7a10bd4', healthInformationSystem: HealthInformationSystem.findById('ff8080817d9aa854017d9e2809b50008'), value: '8aee10da-ebc3-425b-8af7-1a1679fea49d', interoperabilityType: InteroperabilityType.findById('2aa97747-dbeb-4cbe-b838-d5174a5fc9ec')))

        return interoperabilityAttributeList
    }

    List<Object> listProvince() {
        List<Object> provinceList = new ArrayList<>()

        provinceList.add(new LinkedHashMap(id: '4A4A8DA4-9985-49AC-87DE-DCC076809BDA', code: '11', description: 'Maputo Cidade'))
        provinceList.add(new LinkedHashMap(id: '7D89070B-5F18-4C5D-9C12-639EC471999B', code: '10', description: 'Maputo Província'))
        provinceList.add(new LinkedHashMap(id: 'EB26B875-7649-47DB-B500-594AF91E676E', code: '08', description: 'Inhambane'))
        provinceList.add(new LinkedHashMap(id: '5209FFE9-4D41-41B7-A36E-DBF8E718FD74', code: '09', description: 'Gaza'))
        provinceList.add(new LinkedHashMap(id: '471989A9-C5A4-4289-84B8-34A2738A1AB3', code: '07', description: 'Sofala'))
        provinceList.add(new LinkedHashMap(id: 'E6BC4B22-3E0A-4292-98D9-8F6D590D5A05', code: '06', description: 'Manica'))
        provinceList.add(new LinkedHashMap(id: '3E97A1E4-56CC-4903-8DDC-F0B256E56BC1', code: '05', description: 'Tete'))
        provinceList.add(new LinkedHashMap(id: 'D0F5AFC9-AA22-41C6-A2B1-53E6C4765DDF', code: '04', description: 'Zambézia'))
        provinceList.add(new LinkedHashMap(id: 'B120B693-BA3F-4B40-820E-6D13AAE07BB1', code: '03', description: 'Nampula'))
        provinceList.add(new LinkedHashMap(id: 'A5679E05-6853-410C-A2BF-CDC7B721DA01', code: '02', description: 'Cabo Delgado'))
        provinceList.add(new LinkedHashMap(id: '81808B08-CC4E-42D5-B8D5-F43AFC055FDD', code: '01', description: 'Niassa'))

        return provinceList
    }


    List<Object> listDistrict() {
        List<Object> districtList = new ArrayList<>()
        districtList.add(new LinkedHashMap(id: 'F27A05FB-0D32-43BD-ABAC-7B75ECF47C75', code: '01', description: 'Distrito Urbano de KaMpfumo', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: 'C4CFDDA6-5330-4FC6-AE47-26E6DD1C5CEA', code: '02', description: 'Distrito Urbano de Nlhamankulu', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: '9C310822-97F5-49AE-ABFB-D367DC4358C6', code: '03', description: 'Distrito Urbano de KaMaxaquene', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: '0C4F2D4C-47B3-449B-A977-F2E7F67D6CA6', code: '04', description: 'Distrito Urbano de KaMavota', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: 'FA1B526D-4DC8-4371-A18E-CF1E087CC018', code: '05', description: 'Distrito Urbano de KaMubukwana', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: '61AA3652-D63A-4EE2-B3B5-34A54476B65C', code: '06', description: 'Distrito Municipal de KaTembe', province: Province.findByCode('11')))
        districtList.add(new LinkedHashMap(id: 'BACF1638-6E1C-423A-B298-384523CC2268', code: '07', description: 'Distrito Municipal de KaNyaka', province: Province.findByCode('11')))

        districtList.add(new LinkedHashMap(id: '44EFAD60-D00D-4DF5-AE1C-CB9DAB25EB0E', code: '01', description: 'Boane', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: 'EC988413-8C1A-4192-AA77-9D2A85A62239', code: '02', description: 'Magude', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: 'B9188B68-5222-4FDC-AA45-93949EBD706E', code: '03', description: 'Manhiça', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: 'B7A67D5D-7F96-47CF-8BB4-E8EEB66850CF', code: '04', description: 'Marracuene', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: '48192177-F5D9-43BC-A675-63CDCEF90883', code: '05', description: 'Matola', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: 'F649B099-5376-4A57-A480-73B75C8A103E', code: '06', description: 'Matutuine', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: '67E54803-0E43-40DB-869E-92ADBD03299C', code: '07', description: 'Moamba', province: Province.findByCode('10')))
        districtList.add(new LinkedHashMap(id: '9332C8CA-ABEB-4ADC-9C85-EB15D2EED69F', code: '08', description: 'Namaacha', province: Province.findByCode('10')))

        districtList.add(new LinkedHashMap(id: '8AD3230C-4FAA-410C-8457-E2286BA8BC8B', code: '01', description: 'Bilene', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '4C3BE6CE-E0FC-4F3F-B22A-273D6234840A', code: '02', description: 'Chibuto', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '06D559FA-AF23-4518-BEED-0746A71BF57D', code: '03', description: 'Chicualacuala', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '7CAE8FEB-58C7-41D7-9269-7A63FA22DFF8', code: '04', description: 'Chigubo', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '2BE867EA-B518-4747-84A7-4A12840B9D58', code: '05', description: 'Chókwè', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: 'FD58DBCA-2014-444F-8EFC-1B9B82F1CF0C', code: '06', description: 'Chongoene', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '9EFF0997-F938-41AA-8C17-6D3FB2256CB7', code: '07', description: 'Guijá', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '1AD71364-8871-4A65-937A-50D1FB4C8EF1', code: '08', description: 'Limpopo', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: 'E8B17360-3630-4DFE-B5F9-5EA538018A81', code: '09', description: 'Mabalane', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: 'BFBA9E9E-0EC2-4251-AE7E-D07379E22D65', code: '10', description: 'Manjacaze', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: 'A1C8E515-F988-444B-AD34-369827955964', code: '11', description: 'Mapai', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '3BEEFC01-A920-4475-9719-D119783D2AC7', code: '12', description: 'Massangena', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '629D983C-B917-40EC-86CA-4227A75FC0DA', code: '13', description: 'Massingir', province: Province.findByCode('09')))
        districtList.add(new LinkedHashMap(id: '67E405F3-3AAB-4145-9F0C-0223177EDAD1', code: '14', description: 'Xai-Xai', province: Province.findByCode('09')))

        districtList.add(new LinkedHashMap(id: '5E19363B-A942-4781-AEE7-F6A43FC8412F', code: '01', description: 'Funhalouro', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '82FFDCCA-D2E3-46F4-AB19-060E51A80951', code: '02', description: 'Govuro', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '872A8930-77C5-4650-AC38-EC9779B998FF', code: '03', description: 'Homoíne', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '99D77119-8D2C-4C77-95E4-24C1CA7ACC1B', code: '04', description: 'Cidade de Inhambane', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: 'DEAB4A98-22F3-474F-91A0-7E7FAFACA2EF', code: '05', description: 'Inharrime', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: 'F03BA714-0F59-4782-B824-B8FEC060937E', code: '06', description: 'Inhassoro', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '5ABDCD4E-8909-40EE-93F5-CC1E38308894', code: '07', description: 'Jangamo', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '3C02C8AA-1AA9-43E9-AC45-14EBB994C7A1', code: '08', description: 'Mabote', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: 'F7371071-73FA-43E9-A56A-19AB6150D567', code: '09', description: 'Massinga', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '99DD2959-C737-4279-82F2-1A1065D75009', code: '10', description: 'Maxixe', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '12825CF6-6BE5-49B4-AC01-9DD0554A729C', code: '11', description: 'Morrumbene', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '9EE1E655-3AFB-459B-A424-EAF2490123F6', code: '12', description: 'Panda', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: 'B7934A06-34ED-49FC-AC14-12BF36E186C8', code: '13', description: 'Vilanculo', province: Province.findByCode('08')))
        districtList.add(new LinkedHashMap(id: '34D9143E-F1CB-4A7E-9840-D8ADD105458F', code: '14', description: 'Zavala', province: Province.findByCode('08')))


        districtList.add(new LinkedHashMap(id: 'DA5DDE0F-6A53-471B-95AD-210F266B5059', code: '01', description: 'Beira', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '3DAACADD-BA24-41E9-9536-B8F6283C3330', code: '02', description: 'Búzi', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: 'DAF8EF28-8F0E-4070-B898-A1C54DF309BE', code: '03', description: 'Caia', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: 'D82F161F-95ED-4E2B-9775-5B796A3CEC8E', code: '04', description: 'Chemba', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '3700F059-6BDC-4492-A31B-092771D60B75', code: '05', description: 'Cheringoma', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: 'D6FBEA62-88D7-4555-ABD6-83CE632C6209', code: '06', description: 'Chibabava', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: 'A3071861-A577-422D-81DF-7FC4E6C1965C', code: '08', description: 'Dondo', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '7429F24A-6ADB-4F5D-8108-D68882CBE834', code: '09', description: 'Gorongosa', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '87AFED5C-228E-4AEA-ABB6-C346743C539D', code: '10', description: 'Machanga', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: 'B783DFA8-D402-4487-A6D2-2F237D85D4CC', code: '11', description: 'Maringué', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '5A4FA212-445E-46F3-BFA2-A757D3A9E696', code: '12', description: 'Marromeu', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '7E25D960-4E2C-4BA0-93F2-D614BE9812B9', code: '13', description: 'Muanza', province: Province.findByCode('07')))
        districtList.add(new LinkedHashMap(id: '9849837B-410A-4F31-B906-8CF697CE90B5', code: '14', description: 'Nhamatanda', province: Province.findByCode('07')))

        districtList.add(new LinkedHashMap(id: '8A58BA9C-A7FA-4D7E-8358-1D90EE9ED534', code: '01', description: 'Bárue', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '6E776FFE-9157-4C79-99DE-659D6C09DCD2', code: '02', description: 'Chimoio', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '0F97B760-C6F2-41E5-A53B-D3BBD83378E6', code: '03', description: 'Gondola', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '0ADC481B-1CD9-414F-9EDA-995B9C08F75E', code: '04', description: 'Guro', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: 'F3E6AE45-CC3D-44F0-85F0-C359EBB8DCA5', code: '05', description: 'Macate', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '878EDB8B-DB6E-4CD0-8D23-CF1D3AE3D890', code: '06', description: 'Machaze', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '1A36A770-C744-4093-8422-DC0DE3220606', code: '07', description: 'Macossa', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: 'B928B3B3-778C-4291-A998-74752B582E0E', code: '08', description: 'Gorongosa', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '71817140-5050-41CA-93DC-B714CFE96834', code: '09', description: 'Manica', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '1D567CBB-00B2-48B4-A9E8-282E88ACEF1B', code: '10', description: 'Mossurize', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: 'D2BD6130-34B8-44F2-86A5-310FBBC03D19', code: '11', description: 'Sussundenga', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: 'E0097B22-830C-4334-A087-7FD7C100E4F8', code: '12', description: 'Tambara', province: Province.findByCode('06')))
        districtList.add(new LinkedHashMap(id: '27A10EC6-B368-4F67-80D4-03847DCDE3F4', code: '13', description: 'Vanduzi', province: Province.findByCode('06')))

        districtList.add(new LinkedHashMap(id: 'EF759149-553D-47C0-A7B1-FAA2A527C1F0', code: '01', description: 'Angónia', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '05E5944A-4896-402F-BA80-EE74EAEF29C7', code: '02', description: 'Cahora-Bassa', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '81A60580-3D64-491E-AFD0-C877BC376EC0', code: '03', description: 'Changara', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '58994801-885E-4F45-A469-029BA4710AA5', code: '04', description: 'Chifunde', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'A1049E32-2078-4071-9FC6-C3EC6F0F6FE0', code: '05', description: 'Chiuta', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'D5692FB8-E46B-4213-B253-58F8C6E169E7', code: '06', description: 'Dôa', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '7970B01A-85DF-43EC-B552-B7ADAB3528C5', code: '08', description: 'Macanga', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '7E8AABD1-2520-4454-B300-4BB8C3205D88', code: '09', description: 'Magoé', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '3FA9BFBD-956C-4EF2-A30C-87300B5397CD', code: '10', description: 'Marara', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '61AA946C-2FEB-4095-AE87-A3998D2B27BD', code: '11', description: 'Marávia', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'A467C575-28E8-46CD-95CC-B92EDA764BB2', code: '12', description: 'Moatize', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: '6E02D15A-17C3-4FC8-BBB6-8AD84AE3EFC5', code: '13', description: 'Mutarara', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'ED232E24-3ABE-4FA1-993E-7A736929A75C', code: '14', description: 'Tete', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'FA92B99A-7D50-43C5-99D3-2C12FB8A4FBE', code: '15', description: 'Tsangano', province: Province.findByCode('05')))
        districtList.add(new LinkedHashMap(id: 'F4A1EC10-929D-4C53-8264-C239EEC6646A', code: '16', description: 'Zumbo', province: Province.findByCode('05')))

        districtList.add(new LinkedHashMap(id: '263AE10E-6B3C-4683-B7CD-1132ECA6EB18', code: '01', description: 'Alto Molócue', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: 'F0A7DC31-DA32-4926-9177-1D9FF6AFF780', code: '02', description: 'Chinde', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '172B498C-3AFD-4942-AF43-F4CC18AA76E7', code: '03', description: 'Derre', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: 'B24481C7-1ECA-4A5B-84EA-55F063788504', code: '04', description: 'Gilé', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '9CCC9F86-914A-493D-8E8C-CB3126F7FD54', code: '05', description: 'Gurué', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '970769BC-B374-4740-BDD2-185723AB4814', code: '06', description: 'Ile', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '1236E974-679A-4EB8-823B-48B222EBEFC7', code: '07', description: 'Inhassunge', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '0747E091-F4FA-4629-B9D5-30842ED77AED', code: '08', description: 'Luabo', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '9F695C84-4B12-4D16-BAD2-E02B5246A38D', code: '09', description: 'Lugela', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '3416D3B7-7CEE-4696-AE3C-6B9B67F63167', code: '10', description: 'Maganja da Costa', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '2ABB1BF2-57A6-49C4-8E66-5A2ED3B892C5', code: '11', description: 'Milange', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '42553F11-F80A-4E3E-BE02-EFFC18C85A7D', code: '12', description: 'Mocuba', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '8592A3F3-D406-4C82-8C0C-6F09E8820667', code: '13', description: 'Mocubela', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '6B766999-2550-499B-AA53-4ED8574830E2', code: '14', description: 'Molumbo', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: 'EC4487FD-D851-4BA0-8D0F-85290559F44A', code: '15', description: 'Mopeia', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: 'FAD587E8-D834-4862-8BB2-29F2B1812083', code: '16', description: 'Morrumbala', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '084EAAB6-0200-4C6E-924C-50B1D08B75AF', code: '17', description: 'Mulevala', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '882EEDED-3BF2-4CA2-BC3E-6A08666AC23D', code: '18', description: 'Namacurra', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '687109F8-EB77-43FC-915A-AB5034376187', code: '19', description: 'Namarroi', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '31870979-A39C-4F87-AF1C-285A3F2B72FB', code: '20', description: 'Nicoadala', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '53923876-2CDF-4EB1-96AF-A11F794C481B', code: '21', description: 'Pebane', province: Province.findByCode('04')))
        districtList.add(new LinkedHashMap(id: '61986E16-BD21-47A2-A101-B296C7FCAB5E', code: '22', description: 'Quelimane', province: Province.findByCode('04')))

        districtList.add(new LinkedHashMap(id: 'FC203647-3210-4457-9682-F5B6251E47E8', code: '01', description: 'Angoche', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'CBF9625A-231B-439F-B4B1-C39C94AB6FDD', code: '02', description: 'Eráti', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '0D9F070C-A368-4EEE-AA11-91A36D6ACEB9', code: '03', description: 'Ilha de Moçambique', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '467E0DC2-EAD4-4FF9-A958-95D1BE238758', code: '04', description: 'Lalaua', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '299ADC47-F523-4180-B4D8-05F5DDAEC3A7', code: '05', description: 'Larde', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '09617379-E7A0-47BB-8B54-7AAC2DB54D6A', code: '06', description: 'Liúpo', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '7A0951BF-7FD5-4472-A661-0FC14DF75C0E', code: '07', description: 'Malema', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '0A30E69C-D522-41D2-8D8F-5F8868BE28CA', code: '08', description: 'Meconta', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '8FFED27F-377C-41AF-8DDB-0F4059C12C2E', code: '09', description: 'Mecubúri', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'D9199556-BAA3-43D2-B155-272F7260C171', code: '10', description: 'Memba', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '7187B364-EFAE-44DF-8DB9-97766BC4FF6D', code: '11', description: 'Mogincual', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '97323234-C56F-4BC3-BA31-7B1AD89CA10B', code: '12', description: 'Mogovolas', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '9D527BEF-10D3-4569-84BF-838FC95D99CA', code: '13', description: 'Moma', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '71B6D5F9-FDA2-4F9C-A3BA-5769DF657D48', code: '14', description: 'Monapo', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '43FDC475-29E6-4217-925F-E51BD451CE47', code: '15', description: 'Mossuril', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'D8F7B630-E192-4753-ADFB-86CE813F5C27', code: '16', description: 'Muecate', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'D7ACB655-9A27-4852-811A-A1F873C37150', code: '17', description: 'Murrupula', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '06AAE98C-6C54-4CC5-8AD8-6E2BC9FEBAC8', code: '18', description: 'Nacala-a-Velha', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '1121410A-37D1-47E8-8B27-701E39C9501C', code: '19', description: 'Nacala Porto', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '8E08B0DB-301E-4716-AC64-0D1F73CCEA3C', code: '20', description: 'Nacarôa', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'F369DD0F-88DC-4433-8C9E-5038471296C4', code: '21', description: 'Nampula', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: 'D3CD3D1C-1159-48BC-9430-EC14437404FB', code: '22', description: 'Rapale', province: Province.findByCode('03')))
        districtList.add(new LinkedHashMap(id: '176A9C07-B40A-4073-BE8C-4FFC2708FD0F', code: '23', description: 'Ribaué', province: Province.findByCode('03')))

        districtList.add(new LinkedHashMap(id: 'A49BDD42-20FB-4EB3-90D6-0D787FFDB532', code: '01', description: 'Ancuabe', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '34A31F6E-4955-4A2E-83C6-93B8A239745C', code: '02', description: 'Balama', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '5D2A1183-7C03-48A6-B736-70D08B697B5E', code: '03', description: 'Chiúre', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: 'FB6276F8-9366-4330-A580-2A59A7264D22', code: '04', description: 'Ibo', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '3F472154-5A88-4F71-B81D-9F1401ECAC6F', code: '05', description: 'Macomia', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '41161745-44A2-4196-B756-F7B039578D5A', code: '06', description: 'Mecúfi', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: 'B81C1F81-BFA8-4374-9A55-91CEAB9E1E30', code: '07', description: 'Meluco', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: 'D57443AB-E315-4F95-8EE7-69288FC85264', code: '08', description: 'Metuge', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '578E2815-F3C8-476D-B38C-084F0F3D5702', code: '09', description: 'Mocímboa da Praia', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '09167338-40F0-4B04-BF7A-65C90932FCA9', code: '10', description: 'Montepuez', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '807DB077-6944-4C14-9262-DDD5B58DC268', code: '11', description: 'Mueda', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '732870EE-CBF7-4FCB-8754-2B52BF7E5A9D', code: '12', description: 'Muidumbe', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: 'D3CA9983-19E9-4C40-BD2E-98AE5988EF00', code: '13', description: 'Namuno', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '01373605-49B5-45A2-A1C7-8C8D410E8EAA', code: '14', description: 'Nangade', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '9AC2FC42-C2DF-4899-85F2-275F96AB10D0', code: '15', description: 'Palma', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '98D53582-726A-4E3E-863D-33EC6DFECCE0', code: '16', description: 'Pemba', province: Province.findByCode('02')))
        districtList.add(new LinkedHashMap(id: '9BBE7D7E-D15E-441B-972B-42B69D089EFB', code: '17', description: 'Quissanga', province: Province.findByCode('02')))

        districtList.add(new LinkedHashMap(id: 'EB31F967-2129-4EBD-9F53-2DE17DFCBE77', code: '01', description: 'Chimbonila', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '03D67DAA-2A0A-4BD6-8FAD-3874D5A8C97D', code: '02', description: 'Cuamba', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'E0B1D8F1-7B66-4E1D-BBBF-1DCA0C87CB19', code: '03', description: 'Lago', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '37F31525-C269-4DDC-AB25-AC66087C0260', code: '04', description: 'Lichinga', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '31500FB8-5BEF-4233-99A2-942BE425E6AF', code: '05', description: 'Majune', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '097D9707-F88D-465B-A523-487E1FA01B72', code: '06', description: 'Mandimba', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '7B965708-30DB-4083-A946-7D23974F785E', code: '07', description: 'Marrupa', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'F21A8B83-3095-4E78-8E52-CB20C6C4F4D6', code: '08', description: 'Maúa', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'A5B280AA-9E45-4C38-849A-FE5A5D5E565C', code: '09', description: 'Mavago', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '03F5B47E-2817-430D-8371-1B525E4DC885', code: '10', description: 'Mecanhelas', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'FE46BFCB-E641-4385-9880-149A6F055591', code: '11', description: 'Mecula', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'C0AB18B6-3EBC-49D8-95DE-226619187A2D', code: '12', description: 'Metarica', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '359C944B-4C9C-4300-B254-6BC7A608BA08', code: '13', description: 'Muembe', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '17262FF6-D6A7-4D29-9EA6-3157FF27C972', code: '14', description: 'Ngauma', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: 'F8FD7B86-4F15-40BD-82F6-F038B0293F0A', code: '15', description: 'Nipepe', province: Province.findByCode('01')))
        districtList.add(new LinkedHashMap(id: '9D45427F-4546-4386-8DC3-5E2ABCC5B426', code: '16', description: 'Sanga', province: Province.findByCode('01')))

        return districtList
    }

    List<Object> listTherapeuticRegimen() {

        List<Object> therapeuticRegimenList = new ArrayList<>()

        therapeuticRegimenList.add(new LinkedHashMap(id: '90118ad6-999e-4f4d-8aee-8bde6e3d4f6d', regimen_scheme: 'TDF+3TC+ATV/r', code: '2alt1', openmrs_uuid: '7bf5a88d-6db6-4899-a01a-bfd14ce77b53', active: false, description: 'TDF+3TC+ATV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'c2b8e086-6f82-4a7e-9615-3c95fea396b1', regimen_scheme: 'AZT+3TC+LPV/r(2DFC+LPV/r80/20)', code: 'A2Fped Xarope', openmrs_uuid: '28b28521-b6cd-454e-9ec5-f2c6c9c58468', active: true, description: 'AZT+3TC+LPV/r(2DFC+LPV/r80/20)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '8552a6d1-9e75-4c33-84fb-8a35ac0a1f2c', regimen_scheme: 'ABC+3TC+LPV/r(2DFC+LPV/r100/25', code: 'ABCPedCpts', openmrs_uuid: 'cf05347e-063c-4896-91a4-097741cf6be6', active: true, description: 'ABC+3TC+LPV/r(2DFC+LPV/r100/25', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '66359dea-94c3-4de7-b7ef-d858b2f33eb8', regimen_scheme: 'AZT+3TC+ATV/r', code: '2alt3', openmrs_uuid: 'ba25f2b5-4216-4605-9e6b-1f591033dc3e', active: true, description: 'AZT+3TC+ATV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '3c6e19e6-ba06-4c33-9970-62e89b62582d', regimen_scheme: 'ABC+3TC+LPV/r(2DFC+LPV/r80/20)', code: 'ABCPedXarope', openmrs_uuid: 'cf05347e-063c-4896-91a4-097741cf6be6', active: true, description: 'ABC+3TC+LPV/r(2DFC+LPV/r80/20)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'a3f0aa60-c296-48bc-aee9-bca43f3d00bb', regimen_scheme: 'ABC+3TC+EFV', code: 'X5A', openmrs_uuid: '78419317-cdda-42e9-92a3-13cb0cbf0020', active: true, description: 'ABC+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'de4a3f40-d20c-42c9-b8ee-3c646bc3e85b', regimen_scheme: 'AZT+3TC+NVP', code: 'A2A', openmrs_uuid: 'e1dd2f44-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'AZT+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '172a3ed8-5738-4e39-bd63-62467c2068f8', regimen_scheme: 'TDF+AZT+3TC+LPV/r', code: 'C1A', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'TDF+AZT+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '84581535-1c71-4e7d-9da3-852fdb8df744', regimen_scheme: 'ABC+3TC+LPV/r(2DFC+LPV/r40/10)', code: 'ABCPedGranulos', openmrs_uuid: 'cf05347e-063c-4896-91a4-097741cf6be6', active: false, description: 'ABC+3TC+LPV/r(2DFC+LPV/r40/10)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'c9bddf65-1400-4d76-813f-908efc0350c4', regimen_scheme: 'ABC+3TC+EFV Ped(2DFC+EFV200)', code: 'X5APed', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'ABC+3TC+EFV Ped(2DFC+EFV200)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'b3a3b2c4-6832-4b9e-bad0-79b9e967ec74', regimen_scheme: 'TDF+3TC+NVP', code: 'TDF12', openmrs_uuid: '2e44e77e-eac4-4f64-84d2-73d32abf94d5', active: true, description: 'TDF+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'd513cd89-7b92-433a-82f0-16b9f1b74ee7', regimen_scheme: 'TDF+ABC+LPV/r', code: 'C6A', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TDF+ABC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '81670de2-6dae-4da7-84f6-f6e2c42025cd', regimen_scheme: 'AZT+3TC+LPV/r(2DFC+LPV/r40/10)', code: 'A2Fped Granulos', openmrs_uuid: '28b28521-b6cd-454e-9ec5-f2c6c9c58468', active: true, description: 'AZT+3TC+LPV/r(2DFC+LPV/r40/10)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '0f347ba1-cba7-4637-978b-1b920e7f0437', regimen_scheme: 'ABC+3TC+RAL', code: '1TB2', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'ABC+3TC+RAL', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '5c3ca182-b55c-4409-ba6e-9a2586f0e7d1', regimen_scheme: '3TC+RAL+DRV/r', code: '3L_3TC', openmrs_uuid: '', active: false, description: '3TC+RAL+DRV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '0104ded8-054e-41ed-a669-6c11e1d5fec2', regimen_scheme: 'TDF+3TC+DTG', code: '1aLTLD', openmrs_uuid: 'e3f6bb60-e2cf-46cb-a9da-27d634ba8607', active: true, description: 'TDF+3TC+DTG', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '1e4fbb43-ecd9-4d45-9ab7-2232baf47110', regimen_scheme: 'ABC+3TC+DTG', code: '1alt1', openmrs_uuid: 'af15246d-30b8-4aff-8391-ca2b58e2c88b', active: true, description: 'ABC+3TC+DTG', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'ca77f024-b568-4ee5-8fa9-946e5e38a9c5', regimen_scheme: 'd4T30+3TC+dDI400+LPV/r', code: 'dDI123', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'd4T30+3TC+dDI400+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '5a96f4e2-6ad5-417a-b891-d32998db6ffc', regimen_scheme: 'ABC+3TC+NVP (2FDC+NVP50)', code: 'X5CPed', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'ABC+3TC+NVP (2FDC+NVP50)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '238d6085-89bd-4f0d-8055-1f98602b04e5', regimen_scheme: 'AZT+3TC+ABC', code: 'A2C', openmrs_uuid: '3e7f46c7-a971-4c0c-82aa-a65589fd518e', active: true, description: 'AZT+3TC+ABC', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'ea929911-25f9-4687-a867-e8ed126a1755', regimen_scheme: 'AZT+3TC+DTG', code: '1alt2', openmrs_uuid: '9cb63f72-4c08-4543-878a-537dcabe5670', active: false, description: 'AZT+3TC+DTG', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '6f01220d-adbf-43a8-a446-8304816db868', regimen_scheme: 'ABC+AZT+LPV/r', code: 'X3N', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'ABC+AZT+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '7d010064-2a47-4d62-bf5c-b2de9fea2188', regimen_scheme: 'TDF+AZT+LPV/r', code: 'TDF13', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'TDF+AZT+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'df826005-f486-40ac-be77-5fd8760b15a3', regimen_scheme: 'TDF+ABC+3TC+LPV/r', code: 'C4A', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'TDF+ABC+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'cc844796-789c-4418-a12c-48f14ef4b879', regimen_scheme: 'ABC+3TC+ATV/r', code: '2alt2', openmrs_uuid: 'e8b741b3-463c-46b1-8423-a16f736af8d4', active: true, description: 'ABC+3TC+ATV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'f03621ab-358a-4143-943b-f0df34e70ffa', regimen_scheme: 'TDF+3TC+LPV/r', code: 'C7A', openmrs_uuid: 'f8c5d365-7636-4449-9acd-c83c4fd2ea01', active: true, description: 'TDF+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '83f720f6-c11b-447f-8149-faaead605104', regimen_scheme: 'AZT+3TC+EFV', code: 'A2B', openmrs_uuid: 'e1de19fe-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'AZT+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'c8bfba60-97ce-4a38-8f53-71b1b758c347', regimen_scheme: 'AZT+3TC+ABC (2FDC+ABC Baby)', code: 'A2Cped', openmrs_uuid: '3e7f46c7-a971-4c0c-82aa-a65589fd518e', active: true, description: 'AZT+3TC+ABC (2FDC+ABC Baby)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '74937b5a-440a-4844-921c-62609ea78725', regimen_scheme: 'AZT+3TC+NVP (3FDC Baby)', code: 'A2Aped', openmrs_uuid: 'e1dd2f44-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'AZT+3TC+NVP (3FDC Baby)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '93b15d9b-ea38-4438-80f8-dad2162c9c65', regimen_scheme: 'TDF+3TC+EFV', code: 'A4A', openmrs_uuid: '9dc17c1b-7b6d-488e-a38d-505a7b65ec82', active: true, description: 'TDF+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'e11e5b38-c18b-4546-bd80-98df9906957a', regimen_scheme: 'AZT+3TC+RAL+DRV/r', code: '3Lb', openmrs_uuid: '', active: false, description: 'AZT+3TC+RAL+DRV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'f415d3be-67f3-4801-863f-b5a245e918fe', regimen_scheme: 'TDF+3TC+RAL+DRV/r', code: '3La', openmrs_uuid: '', active: false, description: 'TDF+3TC+RAL+DRV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '3d080342-990e-47aa-8195-81e472cc5fbc', regimen_scheme: 'ABC+3TC+RAL+DRV/r', code: '3Lbb', openmrs_uuid: '', active: true, description: 'ABC+3TC+RAL+DRV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '0cba491b-afd9-4f15-bad1-1a78f94ab40d', regimen_scheme: 'd4T+3TC+EFV (2DFC Baby + EFV)', code: 'D4T123X', openmrs_uuid: '', active: false, description: 'd4T+3TC+EFV (2DFC Baby + EFV)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'dbd4d793-5d1e-40d8-b242-1153e7977438', regimen_scheme: 'TDF+3TC+RAL', code: '1TB1', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'TDF+3TC+RAL', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '85cf6677-186b-40e8-a3a5-52bc493a82b0', regimen_scheme: 'AZT+3TC+RAL', code: '1TB3', openmrs_uuid: 'c4a56680-ac6e-4538-8126-e3097b7b4789', active: true, description: 'AZT+3TC+RAL', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'b0da927c-c1e4-4df7-8e5c-634d3a43bcc1', regimen_scheme: 'PTV Pediatrico', code: 'PTV Pediatri', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'PTV Pediatrico', clinical_service_id: 'C4A3FFFA-BA52-4BEF-948D-1C8C90C3F38E'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '6c87963f-d603-4a62-a0c1-df6645f74f3c', regimen_scheme: 'TDF+FTC PreEP', code: 'PreEP', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'TDF+FTC PreEP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'f4b4703e-49aa-4e86-8aee-5a22f3c4bde4', regimen_scheme: 'ABC+3TC+DTG (2DFC+DTG50)', code: 'X6APed', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'ABC+3TC+DTG (2DFC+DTG50)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '80f03d2f-b7b6-4922-a79d-344959517911', regimen_scheme: 'AZT+3TC+LPV/r', code: 'A2F', openmrs_uuid: 'daf60844-9002-403f-bd93-3838149a9a5e', active: false, description: 'AZT+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '1d2b832f-8ef9-4a32-8fdd-8774aa579533', regimen_scheme: 'ABC+3TC+NVP', code: 'X5C', openmrs_uuid: 'e11be52e-0da1-4d32-ab5c-e0feb9b6abd6', active: false, description: 'ABC+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'ef22088a-30d4-4981-8628-3ae6b7026ea5', regimen_scheme: 'ABC+AZT+3TC+LPV/r', code: 'X3N1', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: false, description: 'ABC+AZT+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '4f630e5d-f35c-4bc4-acb6-3d19b1b420f6', regimen_scheme: 'AZT+3TC+EFV (2FDC+EFV 200)', code: 'A2Bped', openmrs_uuid: 'e1de19fe-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'AZT+3TC+EFV (2FDC+EFV 200)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'a54eed82-3789-4863-ae37-ff43e19bab90', regimen_scheme: 'AZT+3TC+LPV/r(2DFC+LPV/r100/25', code: 'A2Fped Cpts', openmrs_uuid: '28b28521-b6cd-454e-9ec5-f2c6c9c58468', active: true, description: 'AZT+3TC+LPV/r(2DFC+LPV/r100/25', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '3f892be3-f5d7-404b-b2ca-dfcea9edcc80', regimen_scheme: 'ABC+3TC120/60+LPV/R100/25', code: '2ALT3', openmrs_uuid: 'ABC+3TC120/60+LPV/R100/25', active: false, description: 'ABC+3TC120/60+LPV/R100/25', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'd68c8585-1d4b-411c-94f9-45bedca6894c', regimen_scheme: 'ABC+3TC+LPV/r', code: 'ABC12', openmrs_uuid: 'cf05347e-063c-4896-91a4-097741cf6be6', active: true, description: 'ABC+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'fb27fd40-c739-407d-b9e0-37bac8561bff', regimen_scheme: 'd4T+3TC+LPV/r(2DFC Baby+LPV/r)', code: 'D4T124', openmrs_uuid: '', active: false, description: 'd4T+3TC+LPV/r(2DFC Baby+LPV/r)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'a647afcb-4c90-48de-a647-39ec87aca13d', regimen_scheme: 'd4T+3TC+NVP (3DFC Baby )', code: 'A1Aped', openmrs_uuid: '', active: false, description: 'd4T+3TC+NVP (3DFC Baby )', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'cb5b73dd-deda-4363-a8da-6859b34f79e4', regimen_scheme: 'd4T+3TC+ABC (2DFC Baby + ABC )', code: 'A1Eped', openmrs_uuid: '', active: false, description: 'd4T+3TC+ABC (2DFC Baby + ABC )', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '8A049D25-8804-439D-84D3-A730E0F0699F', regimen_scheme: '2as Optimizadas ATV/r', code: '2Op1', openmrs_uuid: '', active: true, description: '2as Optimizadas ATV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '9FC3ABE1-925E-43F2-A2F4-D01C9B67BA89', regimen_scheme: '2as Optimizadas ATV/r+RAL', code: '2Op2', openmrs_uuid: '', active: true, description: '2as Optimizadas ATV/r+RAL', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '1F7B72CB-DF9D-4FFB-BA00-7D6B847223AA', regimen_scheme: '2as Optimizadas DRV+RTV', code: '2Op3', openmrs_uuid: '', active: true, description: '2as Optimizadas DRV+RTV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'B34FEE8D-52B8-40B2-819C-66CA29FC5ADC', regimen_scheme: '3a Linha adaptada DRV+RAL+RTV', code: '3op1', openmrs_uuid: '', active: true, description: '3a Linha adaptada DRV+RAL+RTV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '05D2C1BF-A824-410B-B627-211302400213', regimen_scheme: 'ABC+3TC+DTG (2DFC ped + DTG10)', code: 'X7APed', openmrs_uuid: 'af15246d-30b8-4aff-8391-ca2b58e2c88b', active: true, description: 'ABC+3TC+DTG (2DFC ped + DTG10)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '7D18A417-7CCE-47C2-A1EF-6212356FA7FE', regimen_scheme: 'AZT+3TC+LPV/r (2FDC+LPV/r Baby)', code: 'A2Fped', openmrs_uuid: '', active: true, description: 'AZT+3TC+LPV/r (2FDC+LPV/r Baby)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '678A48E0-6B7B-438D-B5CD-EFFA4557F5E9', regimen_scheme: 'ABC+3TC+LPV/r (2FC+LPV/r)', code: 'ABCPedNew', openmrs_uuid: '', active: false, description: 'ABC+3TC+LPV/r (2FC+LPV/r)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'E10520F0-E38C-4FD6-A1D8-F4D9DE637F7C', regimen_scheme: 'ABC+dDI250+LPV/r', code: 'C2A2', openmrs_uuid: '', active: false, description: 'ABC+dDI250+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'B657EB26-005E-4F95-A069-DE08D346820A', regimen_scheme: 'ABC+dDI400+LPV/r', code: 'C2A1', openmrs_uuid: '', active: false, description: 'ABC+dDI400+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'F8120855-2653-4C74-8D1D-E00517477012', regimen_scheme: 'AZT+3TC+ddI 250+LPV/r', code: 'B5C', openmrs_uuid: '', active: false, description: 'AZT+3TC+ddI 250+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '91C6F86A-DC93-488F-B490-0359C18AFD76', regimen_scheme: 'AZT+3TC+ddI 400+LPV/r', code: 'B5D', openmrs_uuid: '', active: false, description: 'AZT+3TC+ddI 400+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '4BA93343-3129-404A-B82C-0FFBFB3F4006', regimen_scheme: 'AZT+ddI 250+LPV/r', code: 'B1C', openmrs_uuid: '', active: false, description: 'AZT+ddI 250+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'D04B75BE-2B57-48FE-9044-976C1D01D4E6', regimen_scheme: 'AZT+ddI 400+EFV', code: 'A3M', openmrs_uuid: '', active: false, description: 'AZT+ddI 400+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '63E0973A-E072-430A-AC28-0696F907F21E', regimen_scheme: 'AZT+ddI 400+LPV/r', code: 'B1D', openmrs_uuid: '', active: false, description: 'AZT+ddI 400+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'A343AD70-87E0-49E5-9408-A4E7148AFEC8', regimen_scheme: 'd4T 30+3TC+ABC', code: 'A1E', openmrs_uuid: '', active: false, description: 'd4T 30+3TC+ABC', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '16A80420-067A-4E6F-8881-44779B89BAAD', regimen_scheme: 'd4T 30+3TC+ddI 250+LPV/r', code: 'B2F', openmrs_uuid: '', active: false, description: 'd4T 30+3TC+ddI 250+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'F0AC82D1-3AFF-4A5B-8E20-6102C011660E', regimen_scheme: 'd4T 30+3TC+EFV', code: 'A1C', openmrs_uuid: '', active: false, description: 'd4T 30+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'BDE352BF-7E1E-4AFE-B85C-B8DE54381D5F', regimen_scheme: 'd4T 30+3TC+NVP', code: 'A1A', openmrs_uuid: '', active: false, description: 'd4T 30+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '352FEF64-1ACB-45E2-806A-9026B132F785', regimen_scheme: 'd4T 30+ddI 250+EFV', code: 'A3L', openmrs_uuid: '', active: false, description: 'd4T 30+ddI 250+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '6EC60A57-B191-4C38-B82A-FF0820DCBA8E', regimen_scheme: 'd4T 30+ddI 250+NVP', code: 'A1K', openmrs_uuid: '', active: false, description: 'd4T 30+ddI 250+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '7DF4F49A-DA4C-498A-9879-0C63276ADF8E', regimen_scheme: 'd4T+3TC+EFZ200 (Ped)', code: 'D4T123', openmrs_uuid: '', active: false, description: 'd4T+3TC+EFZ200 (Ped)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'F7901F59-C9B0-46BC-AA8D-2B906ACDFF8B', regimen_scheme: 'd4T30+3TC+ABC+LPV/r', code: 'A1O', openmrs_uuid: '', active: false, description: 'd4T30+3TC+ABC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'DE077C18-BFDC-453E-BA70-544B59517EB2', regimen_scheme: 'd4T30+3TC+LPV/r', code: 'A1M', openmrs_uuid: '', active: false, description: 'd4T30+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'D67DBB95-46EF-4074-9D87-284FD7A7F5CF', regimen_scheme: 'd4T30+3TC+SQV+RTV', code: 'A1P', openmrs_uuid: '', active: false, description: 'd4T30+3TC+SQV+RTV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'B2A9928E-C66B-47AE-AC9A-037066B6A8A1', regimen_scheme: 'd4T30+ABC+LPV/r', code: 'A1N', openmrs_uuid: '', active: false, description: 'd4T30+ABC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '6245CDD5-000F-4EBF-AF53-4B0B36591661', regimen_scheme: 'd4T30+dDI250+LPV/r', code: 'C2X2', openmrs_uuid: '', active: false, description: 'd4T30+dDI250+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'E3BA4694-3315-4B52-803F-1189C2A873BD', regimen_scheme: 'd4T30+dDI400+LPV/r', code: 'C2X1', openmrs_uuid: '', active: false, description: 'd4T30+dDI400+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'F69C1484-5872-47D5-8240-024F3557A433', regimen_scheme: 'ddI 250+3TC+EFV', code: 'A3C', openmrs_uuid: '', active: false, description: 'ddI 250+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'B207065A-CFE7-4986-BE91-D8EE6913887F', regimen_scheme: 'ddI 250+3TC+LPV/r', code: 'A3N', openmrs_uuid: '', active: false, description: 'ddI 250+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '1A2D5659-CB3C-46C5-AF7F-21581A168999', regimen_scheme: 'ddI 250+3TC+NVP', code: 'A3A', openmrs_uuid: '', active: false, description: 'ddI 250+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'F7803CDE-3005-43B8-AF50-B6827A2D074C', regimen_scheme: 'ddI 400+3TC+EFV', code: 'A3D', openmrs_uuid: '', active: false, description: 'ddI 400+3TC+EFV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'DFF036F8-CE2A-4901-9ECD-39EFAC3D4E18', regimen_scheme: 'ddI 400+3TC+LPV/r', code: 'A3O', openmrs_uuid: '', active: false, description: 'ddI 400+3TC+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '4841FD1F-A01C-4EB1-8565-BDD3D9AAA1AC', regimen_scheme: 'ddI 400+3TC+NVP', code: 'A3B', openmrs_uuid: '', active: false, description: 'ddI 400+3TC+NVP', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '18BD5C82-EDD7-4497-9132-7064E0BFCB12', regimen_scheme: 'Regime Genérico TARV', code: 'RG TARV', openmrs_uuid: '', active: false, description: 'Regime Genérico TARV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'FAA2F4FC-40B0-4A1A-B1D4-E8D49616B64B', regimen_scheme: 'SQV+RTV+ddI 250+ABC', code: 'C8A', openmrs_uuid: '', active: false, description: 'SQV+RTV+ddI 250+ABC', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '9C970473-2C41-4411-A4B0-8FBCCA062919', regimen_scheme: 'SQV+RTV+ddi 400+ABC', code: 'C8B', openmrs_uuid: '', active: false, description: 'SQV+RTV+ddi 400+ABC', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '0765552C-7369-4281-BC8A-A8DD4E297A23', regimen_scheme: 'TDF+3TC+d4T+LPV/r', code: 'TDF14', openmrs_uuid: '', active: false, description: 'TDF+3TC+d4T+LPV/r', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'B9509EF0-D01D-47AF-961C-CED32A841E8D', regimen_scheme: 'TDF+AZT+3TC+SQV+RTV', code: 'C3A1', openmrs_uuid: '', active: false, description: 'TDF+AZT+3TC+SQV+RTV', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        therapeuticRegimenList.add(new LinkedHashMap(id: 'cfccf8f9-9756-4ebc-9436-fa66460de797', regimen_scheme: 'ABC+3TC+DTG (3DFC DTG5mg)', code: 'X7BPed', openmrs_uuid: 'af15246d-30b8-4aff-8391-ca2b58e2c88b', active: true, description: 'ABC+3TC+DTG (3DFC DTG5mg)', clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        therapeuticRegimenList.add(new LinkedHashMap(id: '346054c4-592d-41e9-a2f4-6cfd04f1be7a', regimen_scheme: 'TPT - 3HP Adultos (>30kg)', code: 'TPT - 3HP Adultos (>30kg)', openmrs_uuid: 'bce7d965-3396-4667-8294-aa5260c26334', active: true, description: 'TPT - 3HP Adultos (>30kg)', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'bb8641fb-cba0-4d0f-afba-8e854df9176b', regimen_scheme: 'TPT - 3HP Crianças (< 30kg)', code: 'TPT - 3HP Crianças (< 30kg)', openmrs_uuid: 'bce7d965-3396-4667-8294-aa5260c26334', active: true, description: 'TPT - 3HP Crianças (< 30kg)', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '36efd422-455c-47aa-8939-6890e3f35b01', regimen_scheme: 'TPT - MR- LFX100', code: 'TPT - MR- LFX100', openmrs_uuid: 'e1d4b30a-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TPT - MR- LFX100', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '968f1790-3fe5-487d-8f35-033a0297c359', regimen_scheme: 'TPT - 1HP Adultos (>30kg)', code: 'TPT - 1HP Adultos (>30kg)', openmrs_uuid: 'bce7d965-3396-4667-8294-aa5260c26334', active: true, description: 'TPT - 1HP Adultos (>30kg)', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'b5daad5f-35b8-4fc6-ab0d-11a2dc4304bf', regimen_scheme: 'TPT - INH100', code: 'TPT - INH100', openmrs_uuid: 'e1d43e52-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TPT - INH100', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '3754b576-a8d1-4c4e-86de-8f9b3d475d21', regimen_scheme: 'TPT - MR- LFX250', code: 'TPT - MR- LFX250', openmrs_uuid: 'e1d4b30a-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TPT - MR- LFX250', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        therapeuticRegimenList.add(new LinkedHashMap(id: '3116136c-675c-4936-af9b-c8976a624c14', regimen_scheme: 'TTPT - INH300', code: 'TPT - INH300', openmrs_uuid: 'e1d43e52-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TPT - INH300', clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))

        therapeuticRegimenList.add(new LinkedHashMap(id: '017b6045-2c7c-437c-96c1-8e0867c6b579', regimen_scheme: 'TDF+3TC PrEP', code: 'TDF+3TC PrEP', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'TDF+3TC PrEP', clinical_service_id: '165C876C-F850-436F-B0BB-80D519056BC3'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'bbcf6efc-be7f-4011-821d-04b4683c327e', regimen_scheme: 'Cabotegravir PrEP', code: 'Cabotegravir PrEP', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'Cabotegravir PrEP', clinical_service_id: '165C876C-F850-436F-B0BB-80D519056BC3'))
        therapeuticRegimenList.add(new LinkedHashMap(id: 'bb77f680-de8e-4741-85be-db262b10f10d', regimen_scheme: 'LEN (PreP)', code: 'PreP_LEN', openmrs_uuid: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', active: true, description: 'LEN (PreP)', clinical_service_id: '165C876C-F850-436F-B0BB-80D519056BC3'))

        return therapeuticRegimenList

    }

    List<Object> listDrug() {

        List<Object> listDrug = new ArrayList<>()

        // TPT MEDICATION
        listDrug.add(new LinkedHashMap(id: 'd985d045-c98f-4cec-83c7-6f461e5d36a2', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[INH 100 cp] Isoniazida 100mg', uuid_openmrs: '', fnm_code: '08L04', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: 'f8f1d700-5556-4baf-be6d-ba293b38cee4', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[INH 300mg cp] Isoniazida 300mg', uuid_openmrs: '', fnm_code: '08L03', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: '238ea424-9f4e-4b6e-8cec-b39c3e4e3345', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[Vit B6 50mg cp] Piridoxina (Vitamina B6) 50mg', uuid_openmrs: '', fnm_code: '12D14', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: '8fa7539a-4452-44b2-9b0f-c1d5229746b4', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF/3TC/EFV] Tenofovir 300mg/Lamivudina 300mg/Efavirenze 400mg TLE30', uuid_openmrs: '08S18X-b0-de4a-4233-86fa-c91f9e606be5', fnm_code: '08S18X', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'f0196993-bca0-4284-9474-c7fc5f0f5000', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[Vit B6 25mg cp] Piridoxina (Vit B6) 25mg', uuid_openmrs: '', fnm_code: '12D14Z', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: '06fd53a0-5201-4bf8-8e46-f4e0b0809963', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[RPT/INH 300/300mg cp] Rifapentina 300mg/ Isoniazida 300mg', uuid_openmrs: '', fnm_code: '08L06X', default_treatment: 3, default_period_treatment: 'Semana', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: '0de13637-deb3-42d1-8870-b5f4102dc641', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[RPT 150mg cp] Rifapentina 150mg', uuid_openmrs: '', fnm_code: '08L06XZ', default_treatment: 1, default_period_treatment: 'Semana', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: '6cba5522-48f6-4f3e-9ece-e4f2495f79ab', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[LFX 250mg cp] Levofloxacina 250mg ', uuid_openmrs: '', fnm_code: '08H07', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        listDrug.add(new LinkedHashMap(id: 'ebaec112-1b91-4aaa-a865-d5b770b5bebd', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 1, name: '[LFX 100mg cp] Levofloxacina 100 mg Disp', uuid_openmrs: '', fnm_code: '08H07Y', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '6D12193B-7D5D-4665-8FC6-A03855986FBD'))
        // ARV MEDICATION
        listDrug.add(new LinkedHashMap(id: 'b40c9f35-16c1-48da-af00-c45096fbf88c', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 2, pack_size: 120, name: '[LPV/RTV] Lopinavir/Ritornavir 40mg/10mg Pellets/Granulos', uuid_openmrs: '08S38Y-0c-0932-4b37-ab53-4aae60820544', fnm_code: '08S38Y', default_treatment: 2, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'ef000f5c-2e4c-4843-b2fb-0a265dc60a6a', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[RAL] Raltegravir 400mg', uuid_openmrs: '08S30ZZ-b2-edb5-4815-a4bf-8f0618f029be', fnm_code: '08S30ZZ', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'd95d079e-a83c-4ca5-86c6-23b99e6fa6a7', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 80, name: '[LPV/RTV] Lopinavir/Ritonavir 400mg/100mg 5ml 80ml', uuid_openmrs: '08S39-054-ad23-4d26-8ca1-88308070d08e', fnm_code: '08S39', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '53b9d0ca-ce34-42d7-b9b1-0e9de4f47222', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 120, name: '[LPV/RTV] Lopinavir/Ritonavir 200mg/50mg', uuid_openmrs: '08S39Z-7a-93f6-4e05-baa3-8c4684c8013e', fnm_code: '08S39Z', default_treatment: 2, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'bc7fe62f-a72f-4935-84a8-1d47a60db24f', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[NVP] Nevirapine 200mg', uuid_openmrs: '08S22-d18-2655-4426-868a-b291a5adff38', fnm_code: '08S22', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '8aa8e4c1-3d9c-4d67-9d28-a27e018e0676', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[NVP]  Nevirapina 50mg', uuid_openmrs: '08S23Z-cd-22ff-4993-9378-335cf43bd5c8', fnm_code: '08S23Z', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '6ede4a9f-8da4-4581-b8d8-52a88e01f8b2', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 2, pack_size: 240, name: '[AZT] Zidovudine 50mg/5ml', uuid_openmrs: '08S17Y-b6-6e76-451d-8892-a6d407e0e0d6', fnm_code: '08S17Y', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'dcd3ce1c-5b97-4248-ba51-c1b98ebbc376', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[AZT] Zidovudine 300mg', uuid_openmrs: '08S15-f3b-d54c-420d-ad96-698f71c003c5', fnm_code: '08S15', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '6d0462de-c438-4099-af6a-c1ea8ff2e224', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[ABC] Abacavir 300mg', uuid_openmrs: '08S01-ab-d6e9-4de6-beb8-441daf8d00a3', fnm_code: '08S01', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '59c796e2-89e2-4473-b827-45aae3cac43d', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[ABC] Abacavir 60mg', uuid_openmrs: '08S01Z-02-843e-4dd0-af6f-2eff173d4b30', fnm_code: '08S01Z', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'a037bad8-1e21-4f37-be3c-120c6751d3ac', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[EFV] Efavirenz 600mg', uuid_openmrs: '08S21-918-6273-4f5c-be72-755da371e731', fnm_code: '08S21', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '69600a88-15c0-436c-a6ed-9d14b97c7586', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 90, name: '[EFV] Efavirenz 200mg', uuid_openmrs: '08S20-9db-3bef-4de8-af61-4610d4ead1ba', fnm_code: '08S20', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'd012e46e-5aec-4993-bed4-5f062f3b63a6', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF] Tenofovir 300mg', uuid_openmrs: '08S18-6cb-4b62-4cc0-98af-8053e1396419', fnm_code: '08S18', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'a46c022d-0d74-46ec-9932-7b02788afea5', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 90, name: '[TDF/3TC/EFV] Tenofovir 300mg/Lamivudina 300mg/Efavirenze 400mg TLE90', uuid_openmrs: '08S18XI-6-0715-4389-a4b2-d20de330e26c', fnm_code: '08S18XI', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '03e69b01-f5d7-49e9-9fd4-bc582c24af2b', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[3TC/AZT/NVP] Lamivudina 150mg/Zidovudina 300mg/Nevirapina 200mg', uuid_openmrs: '08S42-d98-6b3e-485f-b969-376fca8b7789', fnm_code: '08S42', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '45a61698-cc56-4f7f-8081-8b8c17df3622', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[3TC/AZT/NVP] Lamivudina 30mg/Zidovudina 60mg/Nevirapina 50mg', uuid_openmrs: '08S42B-d89-7e03-4f25-975e-b66a5e28e063', fnm_code: '08S42B', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '4d2c441c-4393-489e-bd4c-bab5c7456a78', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[3TC/AZT/ABC] Lamivudina 150mg/Zidovudina 300mg/Abacavir 300mg', uuid_openmrs: '08S41-ec0-c1cf-42e1-9a37-9f23b4f07bba', fnm_code: '08S41', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'b8620a31-e203-4b1d-9f22-7e13d5c44746', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF/FTC] Tenofovir 300mg/Emtricitabina 200mg', uuid_openmrs: '08S31-9e9-49f9-4cd6-8268-8df7315e3d09', fnm_code: '08S31', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'cfe9ea7f-7833-47e9-9049-f3309446e984', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[ABC/3TC] Abacavir 120mg/Lamivudina 60mg 60 Comp', uuid_openmrs: '08S01ZW-ec-ec31-45aa-a74e-7238872483e8', fnm_code: '08S01ZW', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '3fc60321-4488-4b7a-bbf6-05c89ac04101', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[ABC/3TC] Abacavir 120mg/Lamivudina 60mg 30 Comp', uuid_openmrs: '08S01ZWii-527b-4c8c-9282-21826775e25b', fnm_code: '08S01ZWii', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '135b1a6f-0791-47d4-8e8e-6f8b75bab054', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[3TC/AZT] Lamivudina 30mg/ Zidovudina 60mg', uuid_openmrs: '08S40Z-fc-6563-49e4-bf81-a456bf79ec88', fnm_code: '08S40Z', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'f8a6a5be-9737-474b-ade2-b2789610d7ee', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 120, name: '[LPV/RTV] Lopinavir/Ritonavir -Aluvia 200mg/50mg', uuid_openmrs: '08S38Z-99-3fe6-48b7-9b25-3052660f3d8b', fnm_code: '08S38Z', default_treatment: 2, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '909a8ee5-30cd-45ef-8540-0a44f26a1a09', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[ABC/3TC] Abacavir 60 and Lamivudina 30mg', uuid_openmrs: '08S01ZZ-2e-29dd-40aa-94b4-0d4fe65e081c', fnm_code: '08S01ZZ', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'f5d786b4-9d65-472d-8720-b59f0a4db033', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[ABC/3TC] Abacavir 60mg/ Lamivudina 30mg 30 Comp.', uuid_openmrs: '08S01ZWi-527b-4c8c-9282-21826775e25b', fnm_code: '08S01ZWi.', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '3c6518d4-ad5b-445e-b3d4-8812363e056c', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[3TC/D4T/NVP] Lamivudina 150mg/Stavudina 30mg/Nevirapina 200mg', uuid_openmrs: '08S4X-833-b26a-4996-8066-48847431404a', fnm_code: '08S4X', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '10a8ead6-f561-4440-b483-4261e27be295', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[ABC/3TC] Abacavir 600mg/Lamivudina 300mg', uuid_openmrs: '08S01ZY-d7-4218-4032-aa8c-615aec71a218', fnm_code: '08S01ZY', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '8ba0bfde-b1b1-408a-806f-4683f2bb17c1', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[3TC/AZT] Lamivudina 150mg/ Zidovudina 300mg', uuid_openmrs: '08S40-833-b26a-4996-8066-48847431404a', fnm_code: '08S40', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'df7f5db0-cd09-44fd-a1cd-d9398482dffc', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[LPV/RTV] Lopinavir/Ritonavir -Aluvia 100mg/25mg', uuid_openmrs: '08S39B-b2-e392-425a-b824-f33f745733e6', fnm_code: '08S39B', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '0596bddc-63cb-4805-80ae-2cc693adf66f', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 90, name: '[TDF/3TC/DTG] Tenofovir 300mg/Lamivudina 300mg/Dolutegravir 50mg TLD90', uuid_openmrs: '08S18WI-4-70b1-4732-af8b-be24cb04aaa6', fnm_code: '08S18WI', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '67034ec5-f1b7-4832-b187-d7e621b84262', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 2, pack_size: 240, name: '[NVP] Nevirapine 50mg/5ml', uuid_openmrs: '08S23-316-d7e9-4757-9466-d0e6c1d8b1db', fnm_code: '08S23', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '1892f3ff-d77d-4d46-b8df-496695eac891', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF/3TC] Tenofovir 300mg/Lamivudina 300mg', uuid_openmrs: '08S18Z-da-b787-4fa1-a2d6-2fda22da6564', fnm_code: '08S18Z', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '3cbfb612-cc39-4001-82f0-451568d43a8c', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[ATV/RTV] Atazanavir 300mg/Ritonavir 100mg', uuid_openmrs: '08S30WZ-48-cf6b-47e2-a101-0cc9faa8fbce', fnm_code: '08S30WZ', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'd5d428c0-6219-459c-8837-b4069694b45a', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 2, pack_size: 60, name: '[3TC] Lamivudine 150mg', uuid_openmrs: '08S13-ddb-e5bd-4b96-ae5d-11ada78c8a35', fnm_code: '08S13', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'c8e78317-ee4d-415f-9674-d3e0fd543d12', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 180, name: '[TDF/3TC/EFV] Tenofovir 300mg/Lamivudina 300mg/Efavirenze 400mg TLE180', uuid_openmrs: '08S18XII-3a07-4e5f-9d00-0016dc840a84', fnm_code: '08S18XII', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'da4377a3-f9ab-4bb3-aa33-7aa1aee56db5', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 2, pack_size: 60, name: '[LPV/RTV]  Lopinavir/Ritonavir-Kaletra 80/20 mg/ml', uuid_openmrs: '08S39Y-3a-20c8-4a16-aaea-f2d4537202e4', fnm_code: '08S39Y', default_treatment: 0, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '215B0404-E745-40C8-9F53-4C1BB510D4DF', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[EFV] Efavirenz 50mg', uuid_openmrs: '08S20-9db-3bef-4de8-af61-4610d4ead1ba', fnm_code: 'TSW80', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '1c607f0f-311a-43a4-ac8c-2e205d53ad0e', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 180, name: '[TDF/3TC/DTG] Tenofovir 300mg/Lamivudina 300mg/Dolutegravir 50mg TLD180', uuid_openmrs: '08S18WII-5863-4423-9e97-bc1b480df134', fnm_code: '08S18WII', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '228bfc9b-2260-407b-9ef5-db5c6c4f0523', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF/3TC/DTG] Tenofovir 300mg/Lamivudina 300mg/Dolutegravir 50mg TLD30', uuid_openmrs: '08S18W-7f-c2a7-4d27-95dc-564791951b5f', fnm_code: '08S18W', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '30daa641-eefa-4dd7-a924-2f49e9f127b1', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DTG] Dolutegravir 50mg', uuid_openmrs: '08S30ZY-ae-3c79-46bd-9970-2d02b8788fdf', fnm_code: '08S30ZY', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '8AAA4C79-A149-459C-BE95-2934DE80F93F', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DTG] Dolutegravir 10mg 30 Comp', uuid_openmrs: '08S30ZXi-d6ed-484b-b980-4a68176f5860', fnm_code: '08S30ZXi', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '797715FF-3272-4E30-87D7-A45DB9DE05FE', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 90, name: '[DTG] Dolutegravir 10mg 90 Comp', uuid_openmrs: '08S30ZX-fa05-4812-8354-ae21b58e366f', fnm_code: '08S30ZX', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'DE655468-9D3F-45A0-92D3-42E44685A576', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[DDI] Didanosina libertacao lenta 250mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S08', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'CE60B194-AC32-481D-8965-937E63964584', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[DDI] Didanosina libertacao lenta 400mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S09', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '1F0BCF33-07BC-4CAF-B01E-9BFFC558A25F', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[EFV] Efavirenz 50mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S19', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '9FE7AE23-0ABD-4423-AEF0-B7900BCF6AD3', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[RTV] Ritonavir 100mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S29Z', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '71C04DAA-CA94-4B6D-B828-30E197402B63', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[D4T/3TC] Estavudina 30mg/Lamivudina150mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S32', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'EE02A3FE-F6BE-4078-9E9C-94CB36DB0ADA', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[D4T/3TC] Estavudina 200mg/Lamivudina 150mg/Nevirapina 30mg ', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S36', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'F1048116-51BC-4FB4-81F4-0CE611C2F44A', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 60, name: '[D4T/3TC] Estavudina 6mg/Lamivudina 30mg/Nevirapina 50mg ', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S34B', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'F7988AA1-ADBC-4BCF-BBF1-0CFF93F8CFCD', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 270, name: '[SQV] Saquinavir 200mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S30', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '5BE410A2-F63C-4FA5-A31E-5CD623569604', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[DDI] Didanosina 25mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S03Z', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '10296AA7-B361-4D11-B61F-C84B1C9EB460', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 240, name: '[3TC] Lamivudina 50mg/5mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S14', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'CF46123E-6BE9-4D7E-85DC-75DF88472E82', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 240, name: '[AZT] Zidovudina 50mg/5mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S17', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'F0E0B162-64F7-466A-8B21-C3B2A9E8838B', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[TDF/3TC/EFV] Tenofovir 300mg/Lamivudina 300mg/Efavirenz 600mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S18Y', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '44A0216C-48BD-406F-9C41-60DA9DCB05D9', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DRV] Darunavir 300mg ', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S30Z', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'D7C3CDFE-A012-4B96-9595-8170FACDCBBD', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 300, name: '[ABC] Abacavir sulfato 10mg/mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S02', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '98D80A5B-2C0A-4EB0-B7AD-01B3B03FB1AD', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 600, name: '[ABC] Abacavir sulfato 20mg/mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S02Z', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '9C60B7A7-17F0-45F7-B410-BAFB5B580FDA', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DDI] Didanosina 100mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S04', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '10BDC114-7DB7-477F-AC4A-812AC9C74ACB', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DDI] Didanosina 150mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S05', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '09DD20BB-ACF0-42C4-8456-AA3E20D5244F', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[DDI] Didanosina 200mg;', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S06', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '14F63C61-5334-4B2D-B3E5-FA7F752B46F8', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[DDI] Didanosina libertacao lenta 125mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S07', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '3A8336DB-0B51-4718-9BCF-5C1FB6F5892C', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 60, name: '[D4T] Estavudina 30mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S10', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '0B2B431C-D5E5-4CC7-89BE-F50D03470E05', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[D4T] Estavudina 20mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S10Y', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '3D535485-3F5D-4FF9-B18E-285842F11328', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[D4T] Estavudina 15mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S10Z', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '1F49A249-32E4-49B0-B89F-92ADA0823B60', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[D4T] Estavudina 40mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S11', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '82C08307-8878-459B-8267-A3BE76364D99', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 30, name: '[D4T] Estavudine solução; 1mg/mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S12', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'F49C62A3-D676-49E6-A2ED-87439D433D7B', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 30, name: '[APV] Amprenavir 150mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S24', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '9AB55DB5-22FF-44A4-A39D-39343A169A01', form_id: '742F4BC0-E0CC-4602-829B-BEC4EAFB0D2C', default_times: 1, pack_size: 30, name: '[APV] Amprenavir 15mg/mL', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S25', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '6641A2EE-8AAD-4E35-AEDE-A42E3DCBFC7A', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 180, name: '[IDV] Indinavir 400mg,', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S26', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'A487066D-31F1-43C2-AA40-7720071F8310', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 60, name: '[D4T/3TC] Estavudina 40mg/Lamivudina 150mg ', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S33', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '0064E589-AB56-4D06-B52C-AFF6D8BDE4D5', form_id: '74C8F060-1EA4-45E9-94DB-2DE6775E6481', default_times: 1, pack_size: 30, name: '[LPV/RTV] Lopinavir 133,3mg/Ritonavir 33,3mg Gelatinosas', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S38', default_treatment: 1, default_period_treatment: 'Dia', active: false, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        listDrug.add(new LinkedHashMap(id: '04BCD7CD-5770-4140-BAA2-931DC70EA2E9', form_id: 'E8427B78-B4B2-4570-8721-03A60425909D', default_times: 1, pack_size: 3, name: '[CAB-LA] Cabotegravir 600mg/3ml Inj', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S18ZW', default_treatment: 1, default_period_treatment: 'Mês', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: 'E7C7B604-0C34-48B3-A941-6DF3ADBEA3A8', form_id: 'A19B6D0E-4F11-42C9-B720-9C8B617E29F8', default_times: 1, pack_size: 1, name: '[DPV-VR] Dapivirina anel vaginal 25mg', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S18ZY', default_treatment: 1, default_period_treatment: 'Mês', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        listDrug.add(new LinkedHashMap(id: '640452ab-3978-48e0-94e6-16c9a426ce5a', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 90, name: 'Abacavir+Lamivudina+Dolutegravir; 60mg+30mg+5mg 90 Comp', uuid_openmrs: '08S01ZV-dce62-4881-9798-0274a32a5fab', fnm_code: '08S01ZV', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '0060cdd3-44ee-4514-9ea4-5b283b7ea433', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 180, name: 'Abacavir+Lamivudine+Dolutegravir; 60mg+30mg+5mg 180 Comp', uuid_openmrs: '08S01ZVI-731c-44c9-b1da-8507fecd55b8', fnm_code: '08S01ZVI', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        listDrug.add(new LinkedHashMap(id: 'b18e45eb-8215-4d40-aea3-50697e25cdc5', form_id: 'E8427B78-B4B2-4570-8721-03A60425909D', default_times: 1, pack_size: 3, name: 'Lenacapavir; 463,5mg/1,5mL; 2 Frascos; Inj', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S18XZ', default_treatment: 1, default_period_treatment: 'Mês', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))
        listDrug.add(new LinkedHashMap(id: '1c789f83-cc5b-420d-b6f9-e06a87cba296', form_id: 'AB6442FF-6DA0-46F2-81E1-F28B1A44A31C', default_times: 1, pack_size: 4, name: 'Lenacapavir; 300mg, 4Comp; Comp', uuid_openmrs: 'e1e59e0e-1d5f-11e0-b929-000c29ad1d07', fnm_code: '08S18ZX', default_treatment: 1, default_period_treatment: 'Dia', active: true, clinical_service_id: '80A7852B-57DF-4E40-90EC-ABDE8403E01F'))

        return listDrug


    }

    List<Object> listRegimenDrugs() {

        List<Object> listDrugRegimen = new ArrayList<>()

        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op1', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op2', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op2', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op3', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op3', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op3', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op3', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2Op3', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3op1', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3L_3TC', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt2', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt2', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1alt1', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1alt1', drug_id: '08S30ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X7APed', drug_id: '08S01ZWi'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X7APed', drug_id: '08S30ZXi'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X7APed', drug_id: '08S30ZX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X6APed', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X6APed', drug_id: '08S30ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X6APed', drug_id: '08S01ZW'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5A', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5A', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5A', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5A', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5APed', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5APed', drug_id: '08S20'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABC12', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABC12', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABC12', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABC12', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABC12', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedCpts', drug_id: '08S01ZW'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedCpts', drug_id: '08S01ZWi'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedCpts', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedCpts', drug_id: '08S39B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedGranulos', drug_id: '08S01ZW'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedGranulos', drug_id: '08S01ZWi'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedGranulos', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedGranulos', drug_id: '08S38Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedXarope', drug_id: '08S01ZW'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedXarope', drug_id: '08S01ZWi'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedXarope', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedXarope', drug_id: '08S39Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedNew', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedNew', drug_id: '08S38Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedNew', drug_id: '08S39'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedNew', drug_id: '08S39B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'ABCPedNew', drug_id: '08S39Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5C', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5C', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5C', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5C', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5CPed', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5CPed', drug_id: '08S23Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5CPed', drug_id: '08S01ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X5CPed', drug_id: '08S23Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB2', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB2', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB2', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB2', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S01ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lbb', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N1', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N1', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N1', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N1', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N', drug_id: '08S15'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X3N', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A2', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A2', drug_id: '08S08'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A2', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A1', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A1', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C2A1', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2C', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2C', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2C', drug_id: '08S41'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Cped', drug_id: '08S01Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Cped', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt3', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt3', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1alt2', drug_id: '08S30ZY'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1alt2', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2B', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2B', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Bped', drug_id: '08S20'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Bped', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Bped', drug_id: '08S19'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Bped', drug_id: '08S20'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Bped', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2F', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2F', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2F', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Cpts', drug_id: '08S39B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Cpts', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Granulos', drug_id: '08S38Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Granulos', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Xarope', drug_id: '08S39Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped Xarope', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped', drug_id: '08S38Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped', drug_id: '08S39'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped', drug_id: '08S39B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped', drug_id: '08S39Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Fped', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2A', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2A', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2A', drug_id: '08S42'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Aped', drug_id: '08S23Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Aped', drug_id: '08S40Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A2Aped', drug_id: '08S42B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB3', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB3', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S29Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3Lb', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1C', drug_id: '08S08'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1C', drug_id: '08S15'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1C', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3M', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3M', drug_id: '08S15'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3M', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1D', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1D', drug_id: '08S15'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'B1D', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1E', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1E', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'dDI123', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'dDI123', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'dDI123', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1C', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1C', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1A', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1A', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1A', drug_id: '08S36'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1Eped', drug_id: '08S01Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1Eped', drug_id: '08S32Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'D4T124', drug_id: '08S32Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'D4T124', drug_id: '08S39'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'D4T124', drug_id: '08S39B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'D4T124', drug_id: '08S39Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1Aped', drug_id: '08S23Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1Aped', drug_id: '08S32Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1Aped', drug_id: '08S34B'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1O', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1O', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1O', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1M', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1M', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1P', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1P', drug_id: '08S30'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A1P', drug_id: '08S32'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3C', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3C', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3C', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3A', drug_id: '08S08'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3A', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3A', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3D', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3D', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3D', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3B', drug_id: '08S09'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3B', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A3B', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S02'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S02Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S03Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S04'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S05'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S06'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S07'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S10'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S10Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S10Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S11'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S12'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S14'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S24'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S25'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S26'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S31'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S33'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'DUMMY TARV', drug_id: '08S38'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'PTV Pediatri', drug_id: '08S17Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'PTV Pediatri', drug_id: '08S23'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'RG TARV', drug_id: '08S17'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'RG TARV', drug_id: '08S23'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C8A', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C8A', drug_id: '08S08'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C8A', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C8A', drug_id: '08S30'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt1', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '2alt1', drug_id: '08S30WZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1aLTLD', drug_id: '08S18W'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1aLTLD', drug_id: '08S18WI'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1aLTLD', drug_id: '08S18WII'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S18X'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S18XI'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S18XII'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S18Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'A4A', drug_id: '08S21'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C7A', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C7A', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C7A', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C7A', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C7A', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF12', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF12', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF12', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF12', drug_id: '08S22'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB1', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '1TB1', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S30YX'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S29Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: '3La.', drug_id: '08S30ZZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C4A', drug_id: '08S01'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C4A', drug_id: '08S13'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C4A', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C4A', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C4A', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C6A', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C1A', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C1A', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C1A', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C3A1', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C3A1', drug_id: '08S29'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C3A1', drug_id: '08S29Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C3A1', drug_id: '08S30'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'C3A1', drug_id: '08S40'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF13', drug_id: '08S15'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF13', drug_id: '08S18'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF13', drug_id: '08S38Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF13', drug_id: '08S39Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TDF+3TC PrEP', drug_id: '08S18Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'PreEP', drug_id: '08S31'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'Cabotegravir PrEP', drug_id: '08S18ZW'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X7BPed', drug_id: '08S01ZV'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'X7BPed', drug_id: '08S01ZVI'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - INH300', drug_id: '12D14'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - INH300', drug_id: '08L03'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - INH100', drug_id: '08L04'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - INH100', drug_id: '12D14Z'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - INH100', drug_id: '12D14'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - 1HP Adultos (>30kg)', drug_id: '12D14'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - 1HP Adultos (>30kg)', drug_id: '08L06X'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - 1HP Adultos (>30kg)', drug_id: '08L06XZ'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - MR- LFX250', drug_id: '08H07Y'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'TPT - MR- LFX250', drug_id: '08H07'))

        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'PreP_LEN', drug_id: '08S18XZ'))
        listDrugRegimen.add(new LinkedHashMap(regimen_id: 'PreP_LEN', drug_id: '08S18ZX'))

        return listDrugRegimen

    }

    List<Object> listClinic1() {

        List<Object> clinicList = new ArrayList<>()
        clinicList.add(new LinkedHashMap(uuid: "0D749139-46F3-4C23-9D65-4FB0CF1D58A0", sisma_id: "s8e95DectKU", provinceCode: "02", province: "Cabo Delgado", districtCode: "01", district: "Ancuabe", sitename: "Ancuabe CS", site_nid: "1020206"))
        clinicList.add(new LinkedHashMap(uuid: "7DE4D02C-C1F8-4E18-947B-983BEFA1A141", sisma_id: "gjRLgvy4fCZ", provinceCode: "02", province: "Cabo Delgado", districtCode: "01", district: "Ancuabe", sitename: "Mariri CS", site_nid: "1020207"))
        clinicList.add(new LinkedHashMap(uuid: "72A1F2A9-4C64-45CB-93B3-ED013E9DC32A", sisma_id: "M0IU4je44O0", provinceCode: "02", province: "Cabo Delgado", districtCode: "01", district: "Ancuabe", sitename: "Mesa CS", site_nid: "1020210"))
        clinicList.add(new LinkedHashMap(uuid: "075AE5E6-D83B-4326-A68F-9F3539409D43", sisma_id: "DskcGzJ9uzE", provinceCode: "02", province: "Cabo Delgado", districtCode: "01", district: "Ancuabe", sitename: "Metoro CS", site_nid: "1020209"))
        clinicList.add(new LinkedHashMap(uuid: "B1010896-46F3-4DB2-90BE-B7EB9526DAB3", sisma_id: "fOMensxYJQ4", provinceCode: "02", province: "Cabo Delgado", districtCode: "01", district: "Ancuabe", sitename: "Ntutupue CS", site_nid: "1020208"))
        clinicList.add(new LinkedHashMap(uuid: "9E8E9ADB-3344-4216-AEE5-1E2432200786", sisma_id: "O2LjfShX5pB", provinceCode: "02", province: "Cabo Delgado", districtCode: "02", district: "Balama", sitename: "Balama CS", site_nid: "1020306"))
        clinicList.add(new LinkedHashMap(uuid: "281764C1-D02C-4462-8CFA-A8D0632E8400", sisma_id: "ioF6POi6sJJ", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Chiure HD", site_nid: "1020406"))
        clinicList.add(new LinkedHashMap(uuid: "3807768E-8B3D-4C9C-8B88-33E1187C2BCC", sisma_id: "J8Q88mdeJBy", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Chiure-Velho CS", site_nid: "1020407"))
        clinicList.add(new LinkedHashMap(uuid: "D9A8B337-22C1-4541-89C7-8754F7E7E578", sisma_id: "yV6xAoZZOhH", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Nakoto CS", site_nid: "1020415"))
        clinicList.add(new LinkedHashMap(uuid: "B2941BC2-B6EC-4E81-9DA1-6327A75D2C74", sisma_id: "ILEiklE5E81", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Namogelia CS", site_nid: "1020413"))
        clinicList.add(new LinkedHashMap(uuid: "7757DAC0-C748-4DF5-B55E-7883003CF71E", sisma_id: "x6RfIlS2KZ6", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Ocua CS", site_nid: "1020411"))
        clinicList.add(new LinkedHashMap(uuid: "934C0E8E-56E8-4615-8163-6CDA0C932424", sisma_id: "JrAHL3Yq9WT", provinceCode: "02", province: "Cabo Delgado", districtCode: "03", district: "Chiure", sitename: "Samora Machel CS", site_nid: "1020404"))
        clinicList.add(new LinkedHashMap(uuid: "5F7E1064-6272-42ED-A2F0-FBA2FB31B121", sisma_id: "wFyhgR5yQFU", provinceCode: "02", province: "Cabo Delgado", districtCode: "05", district: "Macomia", sitename: "Chai CS", site_nid: "1020614"))
        clinicList.add(new LinkedHashMap(uuid: "20B01C31-FF64-4983-9977-FBD6E440AC16", sisma_id: "vjsJ5EjtsJG", provinceCode: "02", province: "Cabo Delgado", districtCode: "05", district: "Macomia", sitename: "Macomia CS", site_nid: "1020612"))
        clinicList.add(new LinkedHashMap(uuid: "E6E3AC32-03EA-4060-AFBC-4A3BFD888ACE", sisma_id: "w1WQ6AirZfw", provinceCode: "02", province: "Cabo Delgado", districtCode: "06", district: "Mecufi", sitename: "Mecufi CS", site_nid: "1020706"))
        clinicList.add(new LinkedHashMap(uuid: "FEC3141C-5C98-4767-94EE-B63614DD87FE", sisma_id: "ISKh6C7Tcff", provinceCode: "02", province: "Cabo Delgado", districtCode: "07", district: "Meluco", sitename: "Meluco CS", site_nid: "1020809"))
        clinicList.add(new LinkedHashMap(uuid: "415DF511-A74C-44FC-8EFD-78E56BEC1AA6", sisma_id: "q6yzjdauWyx", provinceCode: "02", province: "Cabo Delgado", districtCode: "08", district: "Metuge", sitename: "Metuge CS", site_nid: "1021601"))
        clinicList.add(new LinkedHashMap(uuid: "E00C7BDE-63E7-4B1B-8C17-C162485F891C", sisma_id: "Py91BmizNnB", provinceCode: "02", province: "Cabo Delgado", districtCode: "08", district: "Metuge", sitename: "Mieze CS", site_nid: "1021609"))
        clinicList.add(new LinkedHashMap(uuid: "545B8F43-FB6D-42F2-B99C-340CF8160873", sisma_id: "cm9td6rQUyf", provinceCode: "02", province: "Cabo Delgado", districtCode: "09", district: "Mocimboa Da Praia", sitename: "Diaca CS", site_nid: "1020908"))
        clinicList.add(new LinkedHashMap(uuid: "11B6E7C2-B348-48EB-B6E4-50FA694595F9", sisma_id: "tesGFmndLvn", provinceCode: "02", province: "Cabo Delgado", districtCode: "09", district: "Mocimboa Da Praia", sitename: "Mocimboa Praia HR", site_nid: "1020901"))
        clinicList.add(new LinkedHashMap(uuid: "8450EA09-5AC0-4F45-B240-B78BEBEA5ECB", sisma_id: "ETBP8sv6mDb", provinceCode: "02", province: "Cabo Delgado", districtCode: "10", district: "Montepuez", sitename: "Montepuez CS", site_nid: "1021015"))
        clinicList.add(new LinkedHashMap(uuid: "E5A4F6C6-BE4B-4D8E-9AE6-1EEB5EC22D5A", sisma_id: "kqWtMbIHnnK", provinceCode: "02", province: "Cabo Delgado", districtCode: "10", district: "Montepuez", sitename: "Montepuez HR", site_nid: "1021001"))
        clinicList.add(new LinkedHashMap(uuid: "28AA544E-3C0E-41EF-831D-24CA3B29B618", sisma_id: "MeCMhzu8MpG", provinceCode: "02", province: "Cabo Delgado", districtCode: "10", district: "Montepuez", sitename: "Namanhumbiri CS", site_nid: "1021011"))
        clinicList.add(new LinkedHashMap(uuid: "F1AC0EA9-2CA4-4520-AFE1-2F250493E7F4", sisma_id: "DAoIpbeXYjM", provinceCode: "02", province: "Cabo Delgado", districtCode: "10", district: "Montepuez", sitename: "Niuhula CS", site_nid: "1021016"))
        clinicList.add(new LinkedHashMap(uuid: "5E9E8C4C-84BE-45AA-A94C-EE058A6DD9D2", sisma_id: "N/A", provinceCode: "02", province: "Cabo Delgado", districtCode: "10", district: "Pemba", sitename: "Centro de Saúde Militar de Montepuez", site_nid: "1021003"))
        clinicList.add(new LinkedHashMap(uuid: "CB8F7667-EA98-4C9D-94ED-40EF4865B3A7", sisma_id: "BCs1k1Cyvcj", provinceCode: "02", province: "Cabo Delgado", districtCode: "11", district: "Mueda", sitename: "M'Peme CS", site_nid: "1021108"))
        clinicList.add(new LinkedHashMap(uuid: "63CF260D-000D-4BF2-A978-227974FE9950", sisma_id: "TTXY00DjcGH", provinceCode: "02", province: "Cabo Delgado", districtCode: "11", district: "Mueda", sitename: "Mbuo CS", site_nid: "1021113"))
        clinicList.add(new LinkedHashMap(uuid: "85834D2B-EC1D-438B-A0E5-13EBEF6E57DA", sisma_id: "RB9BAv2h56c", provinceCode: "02", province: "Cabo Delgado", districtCode: "11", district: "Mueda", sitename: "Mueda HR", site_nid: "1021106"))
        clinicList.add(new LinkedHashMap(uuid: "1E9F0518-56BB-458B-8E31-815B68CB38B3", sisma_id: "cUZ6Q6UFJri", provinceCode: "02", province: "Cabo Delgado", districtCode: "12", district: "Muidumbe", sitename: "Chitunda CS", site_nid: "1021207"))
        clinicList.add(new LinkedHashMap(uuid: "C268AB50-A55A-414E-A14B-C1B2ED1D8AE6", sisma_id: "ob9KMQ4KddX", provinceCode: "02", province: "Cabo Delgado", districtCode: "12", district: "Muidumbe", sitename: "Miangalewa CS", site_nid: "1021212"))
        clinicList.add(new LinkedHashMap(uuid: "4820ACAF-3DD1-45C9-A9AB-9EABA7827A12", sisma_id: "Xei2ltD3AaE", provinceCode: "02", province: "Cabo Delgado", districtCode: "12", district: "Muidumbe", sitename: "Miteda CS", site_nid: "1021209"))
        clinicList.add(new LinkedHashMap(uuid: "0E9E8CAC-ECB4-4903-AF06-001C49F90C58", sisma_id: "tEBB7RrT7mF", provinceCode: "02", province: "Cabo Delgado", districtCode: "12", district: "Muidumbe", sitename: "Muambula CS", site_nid: "1021206"))
        clinicList.add(new LinkedHashMap(uuid: "B3C7ACCA-9701-43CC-AA0A-DA5AEC162911", sisma_id: "prRLukxgfJK", provinceCode: "02", province: "Cabo Delgado", districtCode: "12", district: "Muidumbe", sitename: "Muatide CS", site_nid: "1021208"))
        clinicList.add(new LinkedHashMap(uuid: "69279D61-28B4-4DD2-AE55-000E141DE3F8", sisma_id: "D3KkMNOQ62J", provinceCode: "02", province: "Cabo Delgado", districtCode: "13", district: "Namuno", sitename: "Namuno CS", site_nid: "1021306"))
        clinicList.add(new LinkedHashMap(uuid: "9296A411-331D-4DC1-A8E2-D75C3C58352E", sisma_id: "nwbcw0cHA92", provinceCode: "02", province: "Cabo Delgado", districtCode: "14", district: "Nangade", sitename: "N'Tamba CS", site_nid: "1021407"))
        clinicList.add(new LinkedHashMap(uuid: "E524D3F7-197B-4882-9CAF-1A8CAFC8D0C2", sisma_id: "R0Zm6RRbNyo", provinceCode: "02", province: "Cabo Delgado", districtCode: "14", district: "Nangade", sitename: "Nangade CS", site_nid: "1021406"))
        clinicList.add(new LinkedHashMap(uuid: "B420FC2E-B4EE-4415-950B-48DEC1BF76E9", sisma_id: "sJjLHqH0yC0", provinceCode: "02", province: "Cabo Delgado", districtCode: "15", district: "Palma", sitename: "Palma CS", site_nid: "1021506"))
        clinicList.add(new LinkedHashMap(uuid: "71AD43A6-BB76-48E3-9293-D8BF02F4B046", sisma_id: "u7Y7rqlYB3g", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "B. Cariacó CS", site_nid: "1020106"))
        clinicList.add(new LinkedHashMap(uuid: "2C0BD11D-33E3-47B7-94BB-32C588736327", sisma_id: "P7phKJR1IG9", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "B. Cimento CS", site_nid: "1020109"))
        clinicList.add(new LinkedHashMap(uuid: "DB7E1BDF-68BD-4CE4-A51C-74D7AE117057", sisma_id: "Vk3omNX0SBp", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "B. Ingonane CS", site_nid: "1020107"))
        clinicList.add(new LinkedHashMap(uuid: "34DFB3F2-F801-43C4-AFCD-272EEA96C0EE", sisma_id: "mBau1p2mgTw", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "B. Muxara CS", site_nid: "1020112"))
        clinicList.add(new LinkedHashMap(uuid: "A56A26FB-0B5C-456A-BF6B-8A3676831249", sisma_id: "e94u5ioNGno", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "Eduardo Mondlane CS", site_nid: "1020110"))
        clinicList.add(new LinkedHashMap(uuid: "8320B6F9-31C6-4D37-AF97-4B609A67A251", sisma_id: "CpLr4Y8vAR7", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "Mahate CS", site_nid: "1020111"))
        clinicList.add(new LinkedHashMap(uuid: "D04CB53B-A4C6-4E67-BCA9-11EECD1D7B2D", sisma_id: "pVWc5C0huL0", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "Natite PS", site_nid: "1020108"))
        clinicList.add(new LinkedHashMap(uuid: "677830FA-5740-4BC0-BC54-09B168015049", sisma_id: "Yjo3rCofbvU", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "Pemba HP", site_nid: "1020100"))
        clinicList.add(new LinkedHashMap(uuid: "0AE32A5C-CED1-415A-A102-F0B4016D0600", sisma_id: "N/A", provinceCode: "02", province: "Cabo Delgado", districtCode: "16", district: "Pemba", sitename: "Centro de Saúde Militar de Pemba", site_nid: "1020150"))
        clinicList.add(new LinkedHashMap(uuid: "513E4EFD-FD6D-4549-87B0-0EE70139BBBC", sisma_id: "N/A", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Centro de Saúde Militar da ESFA de Boane", site_nid: "1100217"))
        clinicList.add(new LinkedHashMap(uuid: "8D5554C5-1528-4781-9C2C-B6BBB0306E95", sisma_id: "nABun6gqxx6", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Beleluane CS", site_nid: "1100210"))
        clinicList.add(new LinkedHashMap(uuid: "62B12327-5FD7-4C6D-A935-9DC402EC9916", sisma_id: "wObdxZxpBIh", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Boane CS", site_nid: "1100206"))
        clinicList.add(new LinkedHashMap(uuid: "E1D5E0C3-B1B1-4F25-ADF1-0E90596A962A", sisma_id: "PewAH00QAYd", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Campoane CS", site_nid: "1100207"))
        clinicList.add(new LinkedHashMap(uuid: "F049B1A5-50BC-4D0C-8F70-6F967AC4FE1C", sisma_id: "yTEdMelD6Gw", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Casa Gaiato PS", site_nid: "1100214"))
        clinicList.add(new LinkedHashMap(uuid: "6F553F1E-5805-44BB-8E4F-D216AB2BA672", sisma_id: "KjugYzLr9Ka", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Mahubo CS", site_nid: "1100208"))
        clinicList.add(new LinkedHashMap(uuid: "954B9128-B2F1-437C-B1D9-4C4D08861CD5", sisma_id: "r58y5fPkQti", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Matola-Rio CS", site_nid: "1100212"))
        clinicList.add(new LinkedHashMap(uuid: "818F269F-74C3-4B87-89A3-08E87EFF642C", sisma_id: "jPJuMz93v3q", provinceCode: "10", province: "Maputo", districtCode: "01", district: "Boane", sitename: "Mulotana CS", site_nid: "1100218"))
        clinicList.add(new LinkedHashMap(uuid: "4ED8443A-84C4-4347-830E-481037DCE30B", sisma_id: "uIeXRsqK5OK", provinceCode: "10", province: "Maputo", districtCode: "02", district: "Magude", sitename: "Magude CS", site_nid: "1100306"))
        clinicList.add(new LinkedHashMap(uuid: "0D2ED12D-DA44-48BA-88DC-0244113C0742", sisma_id: "kChMTC6H1Ff", provinceCode: "10", province: "Maputo", districtCode: "02", district: "Magude", sitename: "Motaze CS", site_nid: "1100308"))
        clinicList.add(new LinkedHashMap(uuid: "F78005C4-296D-43E3-A31D-200E9DC7B742", sisma_id: "N/A", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Centro de Saúde Munguine", site_nid: "1100420"))
        clinicList.add(new LinkedHashMap(uuid: "8F1C6EBF-F206-451B-92C1-7084B9D29541", sisma_id: "YT9Jt2Vnqg7", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "3 de Fevereiro CS", site_nid: "1100415"))
        clinicList.add(new LinkedHashMap(uuid: "029B1352-A500-49EC-88CE-6C5CD764218B", sisma_id: "mwjzR7pRXkF", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Calanga CS", site_nid: "1100410"))
        clinicList.add(new LinkedHashMap(uuid: "69E30A87-5840-408F-82D8-34AF59BCEFAF", sisma_id: "Js4URsUyy4S", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Chibucutso CS", site_nid: "1100417"))
        clinicList.add(new LinkedHashMap(uuid: "E748BF01-0B87-4772-94D9-50B7A7C3BC96", sisma_id: "mTffLowx3Uv", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Ilha Josina CS", site_nid: "1100414"))
        clinicList.add(new LinkedHashMap(uuid: "CEFF5226-DB22-4109-A057-D8AEFA777604", sisma_id: "OYpzWUQD268", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Malavela PS", site_nid: "1100408"))
        clinicList.add(new LinkedHashMap(uuid: "EA7CB31D-D82F-46F7-9A96-BFF7DC24A08F", sisma_id: "Hpt4jrSWMBQ", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Maluana CS", site_nid: "1100409"))
        clinicList.add(new LinkedHashMap(uuid: "267CE92D-98AE-4B8F-8B13-42F46D4F8F59", sisma_id: "CgRnqdGpFKD", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Manhiça CS", site_nid: "1100402"))
        clinicList.add(new LinkedHashMap(uuid: "09112BB5-ED76-4309-BA84-132CC9D36001", sisma_id: "gnP6rzezWlY", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Maragra CS", site_nid: "1100406"))
        clinicList.add(new LinkedHashMap(uuid: "BA496CAE-4D3D-476E-96CB-DC909C6E9A0F", sisma_id: "nXJzFg0sOJa", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Munguine CS", site_nid: "1100411"))
        clinicList.add(new LinkedHashMap(uuid: "6892B222-2507-4C35-A34E-461F8DBC1E82", sisma_id: "EPzAPDoYsR4", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Nwamatibjana CS", site_nid: "1100413"))
        clinicList.add(new LinkedHashMap(uuid: "FEE83450-4B7A-4633-BAEF-FC00C8C32906", sisma_id: "G9HZMNgDkMp", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Taninga CS", site_nid: "1100412"))
        clinicList.add(new LinkedHashMap(uuid: "0921ED63-7EB2-4640-8E33-06D794841C41", sisma_id: "lkihQRg1KaE", provinceCode: "10", province: "Maputo", districtCode: "03", district: "Manhiça", sitename: "Xinavane HR", site_nid: "1100419"))
        clinicList.add(new LinkedHashMap(uuid: "F6F0C09B-B288-403F-A247-BD8481B642F8", sisma_id: "mGO1HpjsrOA", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Habel Jafar CS", site_nid: "1100555"))
        clinicList.add(new LinkedHashMap(uuid: "898CD46C-5CE0-426C-BC61-AE097EDCA7C7", sisma_id: "RlNcqWf2rSe", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Machubo PS", site_nid: "1100508"))
        clinicList.add(new LinkedHashMap(uuid: "7EB63C08-26B5-4B41-85C8-D05BCA766223", sisma_id: "DPzL6hulncw", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Mali CS", site_nid: "1100514"))
        clinicList.add(new LinkedHashMap(uuid: "68505B1F-DEFD-4BB8-9967-12488D9B8C65", sisma_id: "fE9opJzMG7i", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Marracuene CS", site_nid: "1100506"))
        clinicList.add(new LinkedHashMap(uuid: "7879B78D-4F61-43C3-B838-5FEAB36392E6", sisma_id: "WdTMDbs05ro", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Michafutene CS", site_nid: "1100509"))
        clinicList.add(new LinkedHashMap(uuid: "08789096-F929-46D1-A0AD-085C5CE43DE0", sisma_id: "b0ckAUaTVtZ", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Mumemo CS", site_nid: "1100513"))
        clinicList.add(new LinkedHashMap(uuid: "BCB30CB5-69B0-4BA7-856E-69C2FA34769E", sisma_id: "bAqv9y7xlsX", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Nhongonhane CS", site_nid: "1100510"))
        clinicList.add(new LinkedHashMap(uuid: "593B30D0-2420-49D8-B34F-86819030507C", sisma_id: "tGtIwAxzZdS", provinceCode: "10", province: "Maputo", districtCode: "04", district: "Marracuene", sitename: "Ricatla CS", site_nid: "1100511"))
        clinicList.add(new LinkedHashMap(uuid: "2B584D9E-AEFB-4166-BE36-5005A749837B", sisma_id: "T6DBrrGLLDi", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Bedene CS", site_nid: "1100122"))
        clinicList.add(new LinkedHashMap(uuid: "ACE387EA-F9F3-4312-9F1D-F51B532D8B8C", sisma_id: "urxAgpFh4i6", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "BO-Machava PS", site_nid: "1100166"))
        clinicList.add(new LinkedHashMap(uuid: "66077406-DDF9-47FC-A7E9-8C99CB0865F5", sisma_id: "nxeFEojZSJ4", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Boquisso CS", site_nid: "1100114"))
        clinicList.add(new LinkedHashMap(uuid: "CE7CC620-E413-4E19-A057-8DA2A7A885BC", sisma_id: "P7fsvBDExal", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Cadeia Central PS", site_nid: "1100119"))
        clinicList.add(new LinkedHashMap(uuid: "7E120F5E-ACAE-4797-A39A-C2DC0A52E777", sisma_id: "Un5HZnXQVRh", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Komgolote CS", site_nid: "1100117"))
        clinicList.add(new LinkedHashMap(uuid: "F26A42DC-D5DF-4E41-A267-891002F3CE26", sisma_id: "zSmG6MW87yP", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Liberdade CS", site_nid: "1100113"))
        clinicList.add(new LinkedHashMap(uuid: "C3767FE7-6C1D-4256-AA1B-2B95C8A4426F", sisma_id: "BEIhKkXlEWf", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Machava HG", site_nid: "1100100"))
        clinicList.add(new LinkedHashMap(uuid: "575DF00F-A531-4E8C-B730-386E5700D40A", sisma_id: "mUU9a5tMwRx", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Machava II CS", site_nid: "1100107"))
        clinicList.add(new LinkedHashMap(uuid: "71D8EB98-BAA0-4188-BB2D-586FAD6A5505", sisma_id: "oiZ3zfUsFME", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Matola Gare CS", site_nid: "1100108"))
        clinicList.add(new LinkedHashMap(uuid: "83A5A31E-683B-4F52-8EC6-90150D955D62", sisma_id: "PNBeb2UG9CQ", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Matola I CS", site_nid: "1100110"))
        clinicList.add(new LinkedHashMap(uuid: "2A362480-BAE0-4D3B-8769-608A8FD16E84", sisma_id: "Kn1NyG26nrg", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Matola II CS", site_nid: "1100106"))
        clinicList.add(new LinkedHashMap(uuid: "687C700A-E255-42B8-BC9E-D5EF3B74F7EB", sisma_id: "Jw1yNLTSIRv", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Muhalaze CS", site_nid: "1100125"))
        clinicList.add(new LinkedHashMap(uuid: "3A4E7DD6-AE1A-4C62-8D8D-D4A32692C515", sisma_id: "Z0xWugm1j2V", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Ndlavela CS", site_nid: "1100109"))
        clinicList.add(new LinkedHashMap(uuid: "331CD3CD-9DAC-4181-A76D-238439ABB8A6", sisma_id: "Kj2KzMrJ5im", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "S. Damanse CS", site_nid: "1100111"))
        clinicList.add(new LinkedHashMap(uuid: "0ED5E9D1-3B48-428D-91B3-50CF2BCAF945", sisma_id: "pfcze48mxHk", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Tsalala CS", site_nid: "1100167"))
        clinicList.add(new LinkedHashMap(uuid: "1193844A-291D-460A-BFCF-568EE3681F4A", sisma_id: "xVpltNq8VVM", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Unidade A CS", site_nid: "1100118"))
        clinicList.add(new LinkedHashMap(uuid: "C90FECCE-757F-4CA8-9EFA-C729DA363BED", sisma_id: "N/A", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Centro de Saúde Militar de Boquisso", site_nid: "1110102"))
        clinicList.add(new LinkedHashMap(uuid: "D46E1738-D43E-47D4-A9BB-F9626571396B", sisma_id: "N/A", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Centro de Saúde Militar de Serviços Cívicos", site_nid: "1100102"))
        clinicList.add(new LinkedHashMap(uuid: "108F9300-E116-4974-A408-760B09A8C82A", sisma_id: "JRd8UuUMTA8", provinceCode: "10", province: "Maputo", districtCode: "06", district: "Matutuine", sitename: "Matutuine CS", site_nid: "1100607"))
        clinicList.add(new LinkedHashMap(uuid: "DFB1F69D-FF3D-4E57-9BB1-D355FB9386D4", sisma_id: "kYkwV3ipiK3", provinceCode: "10", province: "Maputo", districtCode: "06", district: "Matutuine", sitename: "Ponta de Ouro CS", site_nid: "1100611"))
        clinicList.add(new LinkedHashMap(uuid: "6A0BCF6B-FA2B-43AB-B70B-B36D3E2E5A48", sisma_id: "v2frjywxH8D", provinceCode: "10", province: "Maputo", districtCode: "06", district: "Matutuine", sitename: "Salamanga CS", site_nid: "1100612"))
        clinicList.add(new LinkedHashMap(uuid: "1C534F33-AEDA-47DB-B21D-D49E8C39D71D", sisma_id: "N/A", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Centro de Saúde Militar de Moamba", site_nid: "1100606"))
        clinicList.add(new LinkedHashMap(uuid: "4A10B6DC-850C-449E-B011-5B3ABC677659", sisma_id: "PGiOXt59vMY", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Corrumane CS", site_nid: "1100708"))
        clinicList.add(new LinkedHashMap(uuid: "94757DF0-483D-4828-990B-F8123F669457", sisma_id: "ctdJ1ZG7EnB", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Mahulane CS", site_nid: "1100712"))
        clinicList.add(new LinkedHashMap(uuid: "087B9202-722D-47C2-A3D2-1145DB64A860", sisma_id: "CvIIlQOydAY", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Moamba CS", site_nid: "1100706"))
        clinicList.add(new LinkedHashMap(uuid: "0D91ED06-DAA6-427E-9557-3D6FED4D8895", sisma_id: "PC1tfxubwdr", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Ressano Garcia CS", site_nid: "1100707"))
        clinicList.add(new LinkedHashMap(uuid: "29C0849E-FF79-480A-8D3A-0929E686FC7A", sisma_id: "mHrCtNthqFj", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Sabie CS", site_nid: "1100709"))
        clinicList.add(new LinkedHashMap(uuid: "E28ED374-FA2B-4560-955B-C9962A872869", sisma_id: "w5kNfznTu7m", provinceCode: "10", province: "Maputo", districtCode: "07", district: "Moamba", sitename: "Tenga CS", site_nid: "1100710"))
        clinicList.add(new LinkedHashMap(uuid: "D9D6845C-1EAF-4847-A134-FF9F27A33EDC", sisma_id: "Uh3jRSZ9gPR", provinceCode: "10", province: "Maputo", districtCode: "08", district: "Namaacha", sitename: "Goba CS", site_nid: "1100808"))
        clinicList.add(new LinkedHashMap(uuid: "ACC306B9-C018-4893-8D34-05B4B1FC5E1F", sisma_id: "T1NVqw3MkAU", provinceCode: "10", province: "Maputo", districtCode: "08", district: "Namaacha", sitename: "Mafuiane CS", site_nid: "1100811"))
        clinicList.add(new LinkedHashMap(uuid: "6DC2CA7E-83FD-43BD-B6FC-092BCE0E46D9", sisma_id: "daBciskoByr", provinceCode: "10", province: "Maputo", districtCode: "08", district: "Namaacha", sitename: "Mahelane CS", site_nid: "1100810"))
        clinicList.add(new LinkedHashMap(uuid: "81621D66-F03D-4C03-A42F-54D735910EFF", sisma_id: "RpSDQufWzb0", provinceCode: "10", province: "Maputo", districtCode: "08", district: "Namaacha", sitename: "Namaacha CS", site_nid: "1100806"))


        return clinicList

    }


    List<Object> listClinic2() {
        List<Object> clinicList = new ArrayList<>()

        clinicList.add(new LinkedHashMap(uuid: "CBAA1F5B-B2ED-4ECE-9BAD-E7F84B4215DC", sisma_id: "vrgtQ4mpifC", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "1º de Junho CS", site_nid: "1110406"))
        clinicList.add(new LinkedHashMap(uuid: "7539DD5B-CEF8-47E5-A17A-D9ED97700BEB", sisma_id: "bRGUobgKVW6", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Albasine CS", site_nid: "1110411"))
        clinicList.add(new LinkedHashMap(uuid: "269F1926-18B4-4FCD-9CDE-D744D442C5D6", sisma_id: "mjIi6C9yxfg", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Hulene CS", site_nid: "1110414"))
        clinicList.add(new LinkedHashMap(uuid: "0349C826-E6A2-4FCF-A4DE-A203317B7D77", sisma_id: "bXCwZkdj6sZ", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Mavalane CS", site_nid: "1110422"))
        clinicList.add(new LinkedHashMap(uuid: "63F5F89D-B6AB-43C3-853A-EC5F9D892514", sisma_id: "N/A", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Centro de Saúde da Base Aérea de Mavalane", site_nid: "1070131"))
        clinicList.add(new LinkedHashMap(uuid: "61281D3B-1823-4E4D-9B57-148CC84E1951", sisma_id: "xxGqiSW0LFw", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Mavalane HG", site_nid: "1110401"))
        clinicList.add(new LinkedHashMap(uuid: "F88C163D-7A4A-4363-8843-7C28E48D8A84", sisma_id: "TAaOnwejKBJ", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Pescadores CS", site_nid: "1110415"))
        clinicList.add(new LinkedHashMap(uuid: "1EBBF65B-200B-4A2A-961C-4C86E4103332", sisma_id: "wAXr6kStDoM", provinceCode: "11", province: "Cidade De Maputo", districtCode: "04", district: "Kamavota", sitename: "Romão CS", site_nid: "1110412"))
        clinicList.add(new LinkedHashMap(uuid: "0AE2D455-2DF2-4655-B878-8BA5D3118D8D", sisma_id: "kkLOXDYdkwU", provinceCode: "11", province: "Cidade De Maputo", districtCode: "03", district: "Kamaxakeni", sitename: "1º de Maio CS", site_nid: "1110307"))
        clinicList.add(new LinkedHashMap(uuid: "E2771C0D-89D9-4818-9480-5BC04C48067A", sisma_id: "aqwp0KZqqLX", provinceCode: "11", province: "Cidade De Maputo", districtCode: "03", district: "Kamaxakeni", sitename: "Polana Caniço CS", site_nid: "1110317"))
        clinicList.add(new LinkedHashMap(uuid: "DBA59C40-2A5C-4404-9D65-5FE0B32059EE", sisma_id: "o42MfFGOunh", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Alto Maé CS", site_nid: "1110106"))
        clinicList.add(new LinkedHashMap(uuid: "06D45B95-0FEE-4678-95E0-92B26C8B7054", sisma_id: "wCzSUwrRNrI", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Cadeia Civil PS", site_nid: "1110125"))
        clinicList.add(new LinkedHashMap(uuid: "AE173D65-B445-4199-9A8D-A1D417DA2D2A", sisma_id: "N/A", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Hospital Militar de Maputo (CICTRA)", site_nid: "1110102"))
        clinicList.add(new LinkedHashMap(uuid: "D3509268-03FB-4F3D-91CA-CFDDD5676A7F", sisma_id: "XNSs3kUHboV", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Malhangalene CS", site_nid: "1110109"))
        clinicList.add(new LinkedHashMap(uuid: "150ECCC3-846D-411D-B77D-2554B8FAEDFF", sisma_id: "rRMXWDygZG5", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Maputo HC", site_nid: "1110101"))
        clinicList.add(new LinkedHashMap(uuid: "53B7CDFB-774F-4073-86CB-7A2ED7754E8A", sisma_id: "MufctJ31zoc", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Maxaquene CS", site_nid: "1110107"))
        clinicList.add(new LinkedHashMap(uuid: "8F9CF69C-5837-4D00-97D8-ACDF536C2944", sisma_id: "UDhYkkI8gVd", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Polana Cimento CS", site_nid: "1110110"))
        clinicList.add(new LinkedHashMap(uuid: "0F9025B1-C595-4FB3-B429-20136B951CC1", sisma_id: "djNVcxfh98A", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Porto CSURB", site_nid: "1110111"))
        clinicList.add(new LinkedHashMap(uuid: "E7FD7813-94B4-4EF8-B2AD-E0F1EFF3A829", sisma_id: "N/A", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Centro de Saúde Militar da Marinha", site_nid: "1100420"))
        clinicList.add(new LinkedHashMap(uuid: "2AE03C09-A97D-4915-B257-7716E82EE71A", sisma_id: "N/A", provinceCode: "11", province: "Cidade De Maputo", districtCode: "01", district: "Kampfumu", sitename: "Centro de Saúde Militar Base Naval de Marinha de Guerra", site_nid: "1110105"))
        clinicList.add(new LinkedHashMap(uuid: "E58325A1-87AF-40BE-984B-ECE48EB36E5F", sisma_id: "SkaWmL79FN8", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Bagamoio CS", site_nid: "1110507"))
        clinicList.add(new LinkedHashMap(uuid: "496519E3-8C6D-4866-8412-549089508F24", sisma_id: "CsibyX7eEp5", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Infulene HPsi", site_nid: "1110508"))
        clinicList.add(new LinkedHashMap(uuid: "BF2FA5DD-629E-4D5A-AD4E-98AAFC5CCCB4", sisma_id: "sXffTDJmvCD", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Inhagoia CS", site_nid: "1110512"))
        clinicList.add(new LinkedHashMap(uuid: "13010B27-A8D5-4859-8163-53E6161260D3", sisma_id: "H9mOUBrLH8X", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Magoanine CS", site_nid: "1110514"))
        clinicList.add(new LinkedHashMap(uuid: "6DC1E4A0-94C4-4716-8463-B2D27F5B7EB9", sisma_id: "LQDMtj4FMXc", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Magoanine Tenda CS", site_nid: "1110511"))
        clinicList.add(new LinkedHashMap(uuid: "2D6152B2-9F42-4441-ADF5-6D301A0D1DC3", sisma_id: "tgXuH2BLLib", provinceCode: "11", province: "Cidade De Maputo", districtCode: "05", district: "Kamubukwana", sitename: "Zimpeto CS", site_nid: "1110515"))
        clinicList.add(new LinkedHashMap(uuid: "D45F38EA-05F1-473E-B092-074E0E5D9541", sisma_id: "nFdC62tVjmp", provinceCode: "11", province: "Cidade De Maputo", districtCode: "07", district: "Kanyaka", sitename: "Inhaca CS", site_nid: "1110708"))
        clinicList.add(new LinkedHashMap(uuid: "67EC68D7-A6F4-406F-A364-695D880F49D2", sisma_id: "IR3h2DL4rA6", provinceCode: "11", province: "Cidade De Maputo", districtCode: "07", district: "Katembe", sitename: "Catembe CS", site_nid: "1110609"))
        clinicList.add(new LinkedHashMap(uuid: "DF85CC3C-2B41-4931-B574-5B4A6A1591F1", sisma_id: "qPCXHQuPU09", provinceCode: "11", province: "Cidade De Maputo", districtCode: "07", district: "Katembe", sitename: "Incassane CS", site_nid: "1110613"))
        clinicList.add(new LinkedHashMap(uuid: "748CDC63-D3B1-4B86-A317-48781D132C69", sisma_id: "i5CfM8n4EXK", provinceCode: "11", province: "Cidade De Maputo", districtCode: "02", district: "Nlhamankulu", sitename: "Chamanculo HG", site_nid: "1110214"))
        clinicList.add(new LinkedHashMap(uuid: "53911C7C-5C47-4F14-B3A8-D61495F5901C", sisma_id: "eNuZ05xfP3k", provinceCode: "11", province: "Cidade De Maputo", districtCode: "02", district: "Nlhamankulu", sitename: "José Macamo CS", site_nid: "1110206"))
        clinicList.add(new LinkedHashMap(uuid: "27CA5DFB-4EB1-4061-8648-203CACC9231C", sisma_id: "E15hMrnuTpP", provinceCode: "11", province: "Cidade De Maputo", districtCode: "02", district: "Nlhamankulu", sitename: "José Macamo HG", site_nid: "1110202"))
        clinicList.add(new LinkedHashMap(uuid: "C33B0730-7145-49E4-AC31-000D8BB03E45", sisma_id: "jwVLYEoX4J7", provinceCode: "11", province: "Cidade De Maputo", districtCode: "02", district: "Nlhamankulu", sitename: "Xipamanine CS", site_nid: "1110212"))
        clinicList.add(new LinkedHashMap(uuid: "11743D97-75F4-4A04-8FED-63FF4AB58C25", sisma_id: "C5sZYUmuWE2", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Catandica HD", site_nid: "1060200"))
        clinicList.add(new LinkedHashMap(uuid: "B7AFC71E-5D6E-48FE-8C7F-19D0F8A193BF", sisma_id: "N/A", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: " Centro de Saude Militar de Catandica", site_nid: "1060201"))
        clinicList.add(new LinkedHashMap(uuid: "AEF6B98D-FE63-4247-9388-D8DD0FF098D7", sisma_id: "yZ2DkwwDjTE", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Chuala CS", site_nid: "1060216"))
        clinicList.add(new LinkedHashMap(uuid: "1EA3D1D7-CF8E-4937-92A0-B66E94589633", sisma_id: "sojvZgmkmY5", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Cruz de Macossa CS", site_nid: "1060206"))
        clinicList.add(new LinkedHashMap(uuid: "7416D320-4978-4B15-8EC1-029587AEEEF3", sisma_id: "CQuZCqsENQC", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Honde CS", site_nid: "1060208"))
        clinicList.add(new LinkedHashMap(uuid: "25885FDC-91FC-4C5A-B8D4-3B73B95950AB", sisma_id: "zEFdD2qLo6T", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Nhampassa CS", site_nid: "1060210"))
        clinicList.add(new LinkedHashMap(uuid: "F91D02CE-2D7F-4B3D-A0D1-F0FC477663B2", sisma_id: "hmlVoLLXgYE", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Nhassacara CS", site_nid: "1060211"))
        clinicList.add(new LinkedHashMap(uuid: "4F781DEE-BD98-44E4-8813-4F455FD62797", sisma_id: "hGlnx3eprkv", provinceCode: "06", province: "Manica", districtCode: "01", district: "Barue", sitename: "Nhazonia CS", site_nid: "1060209"))
        clinicList.add(new LinkedHashMap(uuid: "F157CDF1-3B04-464F-A612-DA161D61FEE7", sisma_id: "L9HfQupTHsG", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "1º Maio CS", site_nid: "1060107"))
        clinicList.add(new LinkedHashMap(uuid: "56FE0F6F-C8F4-4340-82E1-D1F4B69E4CFB", sisma_id: "N/A", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Centro de Saúde Militar de Chimoio", site_nid: "1060113"))
        clinicList.add(new LinkedHashMap(uuid: "A69DD9A7-39A8-4C20-96F3-C7ADBDE29C0E", sisma_id: "lw4UAQWs1mX", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "7 de Abril CS", site_nid: "1060110"))
        clinicList.add(new LinkedHashMap(uuid: "4F440FF3-E088-41E7-B3D5-EE001960CF2D", sisma_id: "Ri7Zqp64Zx4", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Chimoio HP", site_nid: "1060100"))
        clinicList.add(new LinkedHashMap(uuid: "5E810E08-49B8-4561-9FA1-F1E65032A01F", sisma_id: "mtASqgMsqet", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Chimoio_PCdVC", site_nid: "1060111"))
        clinicList.add(new LinkedHashMap(uuid: "9DBE0AA6-120D-49FA-BB88-678012227E08", sisma_id: "gn5PsxTj7fc", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Chissui CS", site_nid: "1060109"))
        clinicList.add(new LinkedHashMap(uuid: "ECFD7EFD-249D-455F-9AED-F0C27AE139B9", sisma_id: "KMnM7UdzVQC", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Eduardo Mondlane CS", site_nid: "1060106"))
        clinicList.add(new LinkedHashMap(uuid: "CD7C0190-D3CE-423A-939C-0450F316324C", sisma_id: "sm7nB6raArY", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Nhamaonha CS", site_nid: "1060108"))
        clinicList.add(new LinkedHashMap(uuid: "8B4FFBCB-4BEF-4F70-B569-BFF0B78A4B09", sisma_id: "zSdoz02l19B", provinceCode: "06", province: "Manica", districtCode: "02", district: "Chimoio", sitename: "Vila Nova CS", site_nid: "1060112"))
        clinicList.add(new LinkedHashMap(uuid: "3FDD1FB9-B81B-4C1D-98FC-D0DE8B7E8E08", sisma_id: "MlUQ4Qum7BT", provinceCode: "06", province: "Manica", districtCode: "03", district: "Gondola", sitename: "Amatongas CS", site_nid: "1060307"))
        clinicList.add(new LinkedHashMap(uuid: "8BC3EE47-AC58-4E0B-AA2E-7EFBE00989BF", sisma_id: "w9cvF5me0xI", provinceCode: "06", province: "Manica", districtCode: "03", district: "Gondola", sitename: "Gondola Sede CS", site_nid: "1060300"))
        clinicList.add(new LinkedHashMap(uuid: "2F6881BA-1C74-4D44-855A-E491C26F0AB4", sisma_id: "hq7nY2Ptaal", provinceCode: "06", province: "Manica", districtCode: "03", district: "Gondola", sitename: "Inchope CS", site_nid: "1060309"))
        clinicList.add(new LinkedHashMap(uuid: "FB0F6E93-3FD2-4C17-99AB-AC2F1B593D12", sisma_id: "dKjZT0tM430", provinceCode: "06", province: "Manica", districtCode: "03", district: "Gondola", sitename: "Muda Serração CS", site_nid: "1060313"))
        clinicList.add(new LinkedHashMap(uuid: "D2786C18-EE04-4CA4-8F21-7D600B203B12", sisma_id: "uhbknO6KTt5", provinceCode: "06", province: "Manica", districtCode: "04", district: "Guro", sitename: "Guro - Sede CS", site_nid: "1060408"))
        clinicList.add(new LinkedHashMap(uuid: "23D26C2B-DAE8-429C-9E43-D54B2D830FBE", sisma_id: "krRQllFTwDp", provinceCode: "06", province: "Manica", districtCode: "04", district: "Guro", sitename: "Mandie CS", site_nid: "1060409"))
        clinicList.add(new LinkedHashMap(uuid: "77179524-3CBB-478D-BD58-297020302684", sisma_id: "D6AHz1vuzAr", provinceCode: "06", province: "Manica", districtCode: "05", district: "Macate", sitename: "Macate CS", site_nid: "1061203"))
        clinicList.add(new LinkedHashMap(uuid: "56CFEAF6-A4DC-425F-A152-24898EAE94CA", sisma_id: "O1ZwBbpIbPJ", provinceCode: "06", province: "Manica", districtCode: "05", district: "Macate", sitename: "Marera CS", site_nid: "1061204"))
        clinicList.add(new LinkedHashMap(uuid: "3E56F467-DB27-4865-8139-B11FDE4B632A", sisma_id: "ng99R0nL1my", provinceCode: "06", province: "Manica", districtCode: "05", district: "Macate", sitename: "Zembe Centro CS", site_nid: "1061205"))
        clinicList.add(new LinkedHashMap(uuid: "50C0A8EC-A6FF-4C33-891D-94FDAFC75E06", sisma_id: "nc2FqN0Kwq6", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Bassane CS", site_nid: "1060506"))
        clinicList.add(new LinkedHashMap(uuid: "9EEADE99-E4A2-4F63-9C67-89E2F29DD45E", sisma_id: "K3YAvqRUptX", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Chipopopo CS", site_nid: "1060507"))
        clinicList.add(new LinkedHashMap(uuid: "832B2C17-1E84-4D4E-ABB8-FE0A72FB76C5", sisma_id: "lubefcyFZXj", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Chipudji CS", site_nid: "1060508"))
        clinicList.add(new LinkedHashMap(uuid: "4EBF59A9-147E-4084-8A62-AF5DF99249B3", sisma_id: "g3cRVQxCVM7", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Chitobe CS", site_nid: "1060509"))
        clinicList.add(new LinkedHashMap(uuid: "ACFBCD92-05B9-4485-B291-087D0E9C62C3", sisma_id: "O5WZiQ26Sjg", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Mavende CS", site_nid: "1060596"))
        clinicList.add(new LinkedHashMap(uuid: "87DA5930-4FD8-4103-B251-8342F9910C14", sisma_id: "a33cvtuk1NA", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Mazwissanga CS", site_nid: "1060510"))
        clinicList.add(new LinkedHashMap(uuid: "127E52F6-4ED4-4E3A-9E3F-BBC65A658A5F", sisma_id: "VKwoZ3xeVEe", provinceCode: "06", province: "Manica", districtCode: "06", district: "Machaze", sitename: "Save CS", site_nid: "1060511"))
        clinicList.add(new LinkedHashMap(uuid: "BECACBBB-1F41-4F91-89D8-02EC1EF1F587", sisma_id: "fdg8pYjJO5B", provinceCode: "06", province: "Manica", districtCode: "09", district: "Manica", sitename: "4º Congresso CS", site_nid: "1060798"))
        clinicList.add(new LinkedHashMap(uuid: "3230BF7A-3132-4DBD-A9D0-751FC1D3F2C6", sisma_id: "vgiquow2PBe", provinceCode: "06", province: "Manica", districtCode: "09", district: "Manica", sitename: "Machipanda CS", site_nid: "1060712"))
        clinicList.add(new LinkedHashMap(uuid: "8D8FB079-C7CC-4BE3-88CB-6B23093D8B02", sisma_id: "OYQDwvgshMj", provinceCode: "06", province: "Manica", districtCode: "09", district: "Manica", sitename: "Manica HD", site_nid: "1060706"))
        clinicList.add(new LinkedHashMap(uuid: "90CD6206-5B86-4650-8C27-D9004DA97A65", sisma_id: "F9DSeVeJqtA", provinceCode: "06", province: "Manica", districtCode: "09", district: "Manica", sitename: "Messica CS", site_nid: "1060714"))
        clinicList.add(new LinkedHashMap(uuid: "ACB52BB7-84B4-4669-ACD1-DCC145C54648", sisma_id: "J53NbqCZfEq", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Chaiva CS", site_nid: "1060813"))
        clinicList.add(new LinkedHashMap(uuid: "AD2291CC-A16C-4F53-A7CD-6191FB6603A6", sisma_id: "sg6XWA0xcTP", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Dacata CS", site_nid: "1060808"))
        clinicList.add(new LinkedHashMap(uuid: "05AE0E65-2AA3-4046-9BEC-B11D86BD9C48", sisma_id: "Aj4uieGHo99", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Espungabera CS", site_nid: "1060806"))
        clinicList.add(new LinkedHashMap(uuid: "9BC8912E-F01D-4E55-BF34-A8F7793264BE", sisma_id: "IgqJTmFje9F", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Garagua CS", site_nid: "1060814"))
        clinicList.add(new LinkedHashMap(uuid: "601A6A3B-5D62-4F30-B6D8-658B40D7EB93", sisma_id: "fXwGJyHowkx", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Gunhe CS", site_nid: "1060812"))
        clinicList.add(new LinkedHashMap(uuid: "7C94E476-C83C-4A62-94E4-373D15D7F978", sisma_id: "gXMvLNYQehu", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Mude CS", site_nid: "1060809"))
        clinicList.add(new LinkedHashMap(uuid: "54643248-DD78-44FE-85C8-E63A91DDCD36", sisma_id: "hfrL5sAEveC", provinceCode: "06", province: "Manica", districtCode: "10", district: "Mossurize", sitename: "Mupengo CS", site_nid: "1060810"))
        clinicList.add(new LinkedHashMap(uuid: "AEAE9EDC-B534-4A29-B46F-3B89AE652177", sisma_id: "j66mq4NRD6U", provinceCode: "06", province: "Manica", districtCode: "11", district: "Sussundenga", sitename: "Darue CS", site_nid: "1060995"))
        clinicList.add(new LinkedHashMap(uuid: "0A26CA62-BD5F-440F-9BBC-BDD33A469B1D", sisma_id: "tkJKrz6KXtL", provinceCode: "06", province: "Manica", districtCode: "11", district: "Sussundenga", sitename: "Dombe CS", site_nid: "1060911"))
        clinicList.add(new LinkedHashMap(uuid: "CCB18614-8BDD-4FB5-9610-AF7E678D7C9C", sisma_id: "WyU05Mfu3EH", provinceCode: "06", province: "Manica", districtCode: "11", district: "Sussundenga", sitename: "Munhinga CS", site_nid: "1060908"))
        clinicList.add(new LinkedHashMap(uuid: "0D7B9AC2-27D1-4502-8130-AC8DDD583A0F", sisma_id: "rPXTcXWEz5R", provinceCode: "06", province: "Manica", districtCode: "11", district: "Sussundenga", sitename: "Sembezea CS", site_nid: "1060913"))
        clinicList.add(new LinkedHashMap(uuid: "8139BE03-14E9-459F-A72D-48AE9428C957", sisma_id: "XxTByH8V9Im", provinceCode: "06", province: "Manica", districtCode: "11", district: "Sussundenga", sitename: "Sussundenga CS", site_nid: "1060906"))
        clinicList.add(new LinkedHashMap(uuid: "7ECA2D1C-48F4-4C9B-AA39-E6BD005E7AC1", sisma_id: "cXSEInNMnZI", provinceCode: "06", province: "Manica", districtCode: "13", district: "Vanduzi", sitename: "Chigodole CS", site_nid: "1061103"))
        clinicList.add(new LinkedHashMap(uuid: "42820C6A-1714-4E73-B833-F1B9BB235A3A", sisma_id: "BbBu5wpPMoD", provinceCode: "06", province: "Manica", districtCode: "13", district: "Vanduzi", sitename: "IAC CS", site_nid: "1061108"))
        clinicList.add(new LinkedHashMap(uuid: "1D8AD509-465C-4CFE-B0E6-D101BA375411", sisma_id: "uCKWnMNhvUW", provinceCode: "06", province: "Manica", districtCode: "13", district: "Vanduzi", sitename: "Matsinho CS", site_nid: "1061104"))
        clinicList.add(new LinkedHashMap(uuid: "436BA2E4-AAAF-4028-A0E1-B13D7E685DAE", sisma_id: "Ms7kdnoZKta", provinceCode: "06", province: "Manica", districtCode: "13", district: "Vanduzi", sitename: "Pungue Sul CS", site_nid: "1061106"))
        clinicList.add(new LinkedHashMap(uuid: "4F6BD589-1290-427F-BEE6-5F6E28AD37C9", sisma_id: "Oqmpqq6IMp8", provinceCode: "06", province: "Manica", districtCode: "13", district: "Vanduzi", sitename: "Vanduzi CS", site_nid: "1061102"))
        clinicList.add(new LinkedHashMap(uuid: "66C33D97-464C-458C-A950-EF85A20E18E4", sisma_id: "N/A", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Centro de Saúde Militar de Cuamba", site_nid: "1010204"))
        clinicList.add(new LinkedHashMap(uuid: "AFD5650F-9326-4362-9FDB-AC5DAC357E52", sisma_id: "ZDD1uu0NtI8", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Adine 3 CS", site_nid: "1010202"))
        clinicList.add(new LinkedHashMap(uuid: "BB5EF1BB-5966-4A9E-BD9E-CEA15CAF7D5E", sisma_id: "kTpsjuvHxOR", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Cuamba CS", site_nid: "1010216"))
        clinicList.add(new LinkedHashMap(uuid: "4CF68491-8266-450F-8C9D-F3D6CAB8FB61", sisma_id: "J4yTQoY5PxH", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Cuamba HR", site_nid: "1010201"))
        clinicList.add(new LinkedHashMap(uuid: "2B5F8D0D-0B99-434B-AFFC-61B586F1E336", sisma_id: "NiGXo1nvR9C", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Meripo CS", site_nid: "1010225"))
        clinicList.add(new LinkedHashMap(uuid: "66B81A7F-B6CE-4E18-A835-3D08698FD7BD", sisma_id: "xenLlWm2Oez", provinceCode: "01", province: "Niassa", districtCode: "02", district: "Cuamba", sitename: "Mitucué CS", site_nid: "1010209"))
        clinicList.add(new LinkedHashMap(uuid: "43FC74FB-5D06-405E-B48A-D322CFF3B9DE", sisma_id: "OgAM2b54zkg", provinceCode: "01", province: "Niassa", districtCode: "03", district: "Lago", sitename: "Metangula CS", site_nid: "1010308"))
        clinicList.add(new LinkedHashMap(uuid: "B8D2BC52-4DA3-4E91-9B8F-70886C470700", sisma_id: "N/A", provinceCode: "01", province: "Niassa", districtCode: "03", district: "Lago", sitename: "Centro de Saúde Militar de Metangula", site_nid: "1030801"))
        clinicList.add(new LinkedHashMap(uuid: "E4DC1097-ECD3-4F71-B550-09307CEEFF06", sisma_id: "kmhlHtkTLd4", provinceCode: "01", province: "Niassa", districtCode: "04", district: "Lichinga", sitename: "Chiuaula PS", site_nid: "1010108"))
        clinicList.add(new LinkedHashMap(uuid: "A07F973F-7165-41BD-9A95-2CAA31E668C6", sisma_id: "s74LEiLUn2T", provinceCode: "01", province: "Niassa", districtCode: "04", district: "Lichinga", sitename: "CS Lichinga", site_nid: "1010106"))
        clinicList.add(new LinkedHashMap(uuid: "BD27AD11-650B-475C-B62B-73571CA95751", sisma_id: "hcHDACTXCum", provinceCode: "01", province: "Niassa", districtCode: "04", district: "Lichinga", sitename: "Lichinga HP", site_nid: "1010100"))
        clinicList.add(new LinkedHashMap(uuid: "B23123A4-2481-4466-8AC0-8472B4166063", sisma_id: "QMDxghiDP5r", provinceCode: "01", province: "Niassa", districtCode: "04", district: "Lichinga", sitename: "Lulimire CS", site_nid: "1010111"))
        clinicList.add(new LinkedHashMap(uuid: "20663BC6-0CBA-477C-B577-030A80F7FFB8", sisma_id: "V5W4eJi1ILK", provinceCode: "01", province: "Niassa", districtCode: "04", district: "Lichinga", sitename: "Namacula CS", site_nid: "1010115"))
        clinicList.add(new LinkedHashMap(uuid: "E06D430E-D947-40FB-A1CE-EC8CC5D5CDC8", sisma_id: "I4haq7zb83f", provinceCode: "01", province: "Niassa", districtCode: "06", district: "Mandimba", sitename: "Mandimba CS", site_nid: "1010606"))
        clinicList.add(new LinkedHashMap(uuid: "385BF0F1-A6B6-4771-9E61-BAE4FBC612D2", sisma_id: "tCrS8lG7qDj", provinceCode: "01", province: "Niassa", districtCode: "06", district: "Mandimba", sitename: "Mitande CS", site_nid: "1010612"))
        clinicList.add(new LinkedHashMap(uuid: "B963F1DC-D800-4B89-A33D-DC9666F71F57", sisma_id: "D0W5gMc3ZeD", provinceCode: "01", province: "Niassa", districtCode: "10", district: "Mecanhelas", sitename: "Chissaua CS", site_nid: "1011007"))
        clinicList.add(new LinkedHashMap(uuid: "61038420-092D-4295-A64A-CC662C4DB10E", sisma_id: "yACDIfxjbTe", provinceCode: "01", province: "Niassa", districtCode: "10", district: "Mecanhelas", sitename: "Entre-Lagos CS", site_nid: "1011009"))
        clinicList.add(new LinkedHashMap(uuid: "7EF4858C-087D-40B4-A5AE-435484A4DCFC", sisma_id: "K6GW1zIFs2B", provinceCode: "01", province: "Niassa", districtCode: "10", district: "Mecanhelas", sitename: "Mecanhelas CS", site_nid: "1011011"))
        clinicList.add(new LinkedHashMap(uuid: "4E5B1E54-91F5-4471-B1E7-D37817F2CF9B", sisma_id: "h9UrrRY4cSo", provinceCode: "01", province: "Niassa", districtCode: "12", district: "Metarica", sitename: "Metarica CS", site_nid: "1011209"))
        clinicList.add(new LinkedHashMap(uuid: "7C501628-7500-46B5-82EB-15B19D0D05DC", sisma_id: "KlXIRRzXMUR", provinceCode: "01", province: "Niassa", districtCode: "14", district: "Ngauma", sitename: "Massangulo CS", site_nid: "1011409"))
        clinicList.add(new LinkedHashMap(uuid: "6B3D8796-0207-451B-9E30-7686DC702DA1", sisma_id: "N/A", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Cadeia Provincial", site_nid: "TBD001"))
        clinicList.add(new LinkedHashMap(uuid: "D6784069-5F23-4031-8F82-2AB80B46C82E", sisma_id: "N/A", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Centro de Saúde da Base Naval da Beira", site_nid: "1070124"))
        clinicList.add(new LinkedHashMap(uuid: "780946C8-18F1-45E1-B1E9-E3D59AA2E5C1", sisma_id: "N/A", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Centro de Saúde da Base Aérea da Beira", site_nid: "1070142"))
        clinicList.add(new LinkedHashMap(uuid: "EBF6E4C5-5812-4FB2-9483-2FD046AB7B47", sisma_id: "N/A", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Centro de Saúde Militar de Matacuane", site_nid: "1070131"))
        clinicList.add(new LinkedHashMap(uuid: "4CAE72C2-5217-4630-AF83-E36AF7C51253", sisma_id: "KcR06HHOClT", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Cerâmica CS", site_nid: "1070110"))
        clinicList.add(new LinkedHashMap(uuid: "1BECC9C8-D811-43EC-B6DE-9BB91D3C28E4", sisma_id: "qR4WslJnFtW", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Chamba PS", site_nid: "1070111"))
        clinicList.add(new LinkedHashMap(uuid: "54757C40-3857-4D3D-924E-DB8F42C86CA1", sisma_id: "CxkeqQZFIun", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Chota PS", site_nid: "1070112"))
        clinicList.add(new LinkedHashMap(uuid: "DD74B35C-F40D-40EB-833D-74D348B114F7", sisma_id: "HkllDLMAm2N", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Hospital Central da Beira HC", site_nid: "1070100"))
        clinicList.add(new LinkedHashMap(uuid: "391E3589-BC47-4CC2-B707-B70C1B2AC0D0", sisma_id: "MJCmDb21vyA", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "M. Mascarenha CS", site_nid: "1070114"))
        clinicList.add(new LinkedHashMap(uuid: "7517086D-40C9-4E9E-82A7-454688FB64B3", sisma_id: "xKLw7u2sxKF", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Macurungo PSA", site_nid: "1070113"))
        clinicList.add(new LinkedHashMap(uuid: "F14798C4-AE74-47AD-A2E0-296B926D1DEF", sisma_id: "omnFp6jwIeQ", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Manga Loforte", site_nid: "1070104"))
        clinicList.add(new LinkedHashMap(uuid: "802668B7-935D-4205-BB6B-D1C52A87DC82", sisma_id: "OSTvRJK0eAF", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Manga Nhaconjo PS", site_nid: "1070107"))
        clinicList.add(new LinkedHashMap(uuid: "00D25971-0072-4B51-801A-05F3804EE736", sisma_id: "FZyn3d87ZiC", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Marrocanhe", site_nid: "1070105"))
        clinicList.add(new LinkedHashMap(uuid: "04EDB934-E6A4-4B54-A646-A7023086B07B", sisma_id: "OGNvxKxzy0l", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Matadouro PS", site_nid: "1070115"))
        clinicList.add(new LinkedHashMap(uuid: "86823A7B-5047-4890-A3E1-DFF69BED9419", sisma_id: "C7Eveum7lMY", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Munhava CS", site_nid: "1070106"))
        clinicList.add(new LinkedHashMap(uuid: "C3CEE955-6BC7-4D8B-85A7-F7F46B1B09BC", sisma_id: "oPwI1fBSSVd", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Nhangau CS", site_nid: "1070116"))
        clinicList.add(new LinkedHashMap(uuid: "89E4E513-C9B8-40BA-B92D-D3675E23FEB0", sisma_id: "FjDywngVp2a", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "Ponta Gêa PS", site_nid: "1070109"))
        clinicList.add(new LinkedHashMap(uuid: "8A03F7D0-8539-11EE-8501-0800200C9A66", sisma_id: "rPXTcXWEz5R", provinceCode: "07", province: "Sofala", districtCode: "01", district: "Beira", sitename: "CS Inhamizua", site_nid: "1070103"))
        clinicList.add(new LinkedHashMap(uuid: "9A85DEBD-CAAB-48F7-99EA-CAD2BD40BCE1", sisma_id: "PGiHivPCaUt", provinceCode: "07", province: "Sofala", districtCode: "02", district: "Buzi", sitename: "Ampara PS", site_nid: "1070207"))
        clinicList.add(new LinkedHashMap(uuid: "CABC9768-2FA7-43E5-817B-38A9FFFEEAD2", sisma_id: "BPIWiDzlFEs", provinceCode: "07", province: "Sofala", districtCode: "02", district: "Buzi", sitename: "Bandua CS", site_nid: "1070206"))
        clinicList.add(new LinkedHashMap(uuid: "2745F9D4-1693-414F-ACD3-59CBE6B7CB3F", sisma_id: "th9obNeoxrO", provinceCode: "07", province: "Sofala", districtCode: "02", district: "Buzi", sitename: "Bura CS", site_nid: "1070210"))
        clinicList.add(new LinkedHashMap(uuid: "48988764-69FE-4145-9711-6516A86B0470", sisma_id: "apno1AmmrGK", provinceCode: "07", province: "Sofala", districtCode: "02", district: "Buzi", sitename: "Buzi Sede HR", site_nid: "1070200"))
        clinicList.add(new LinkedHashMap(uuid: "B023ED88-6E4D-4F73-9914-D3C72950811B", sisma_id: "EJoi6NGZtPU", provinceCode: "07", province: "Sofala", districtCode: "02", district: "Buzi", sitename: "Guara-Guara PS", site_nid: "1070217"))
        clinicList.add(new LinkedHashMap(uuid: "DF2610E9-0BA6-4BA9-891A-65C5AA622D1A", sisma_id: "siVFdAcTTK6", provinceCode: "07", province: "Sofala", districtCode: "03", district: "Caia", sitename: "Caia HD", site_nid: "1070306"))
        clinicList.add(new LinkedHashMap(uuid: "F0BF52A0-EED9-4166-AA36-3746D3DA6565", sisma_id: "msAZrrY1CK9", provinceCode: "07", province: "Sofala", districtCode: "03", district: "Caia", sitename: "Murraça CS", site_nid: "1070308"))
        clinicList.add(new LinkedHashMap(uuid: "35B4935C-CBE7-4B5B-BFF3-C04041BD2558", sisma_id: "rQZWmDKmnxT", provinceCode: "07", province: "Sofala", districtCode: "03", district: "Caia", sitename: "Nhambalo CS", site_nid: "1070315"))
        clinicList.add(new LinkedHashMap(uuid: "DA6E4F33-C8CE-4AA0-81B8-CF5E5FF2F1FC", sisma_id: "KIgN6eaDY54", provinceCode: "07", province: "Sofala", districtCode: "03", district: "Caia", sitename: "Sena CS", site_nid: "1070312"))
        clinicList.add(new LinkedHashMap(uuid: "49930D76-C303-4581-9C1F-A03404518349", sisma_id: "PBYuexaSh6x", provinceCode: "07", province: "Sofala", districtCode: "05", district: "Cheringoma", sitename: "Inhaminga CS", site_nid: "1070500"))
        clinicList.add(new LinkedHashMap(uuid: "731B53FA-D687-4B7D-BBA7-445CA17574A9", sisma_id: "GTM6xb6qVCx", provinceCode: "07", province: "Sofala", districtCode: "06", district: "Chibabava", sitename: "Chibabava-Sede CS", site_nid: "1070606"))
        clinicList.add(new LinkedHashMap(uuid: "EA5F928F-F992-4DE6-BC20-B742938B810F", sisma_id: "OcRC1eQyy4H", provinceCode: "07", province: "Sofala", districtCode: "06", district: "Chibabava", sitename: "Hoode PS", site_nid: "1070609"))
        clinicList.add(new LinkedHashMap(uuid: "9944B377-4786-4B5A-B0C9-617002A6476C", sisma_id: "mlN8M41CXmF", provinceCode: "07", province: "Sofala", districtCode: "06", district: "Chibabava", sitename: "Mutindire CS", site_nid: "1070604"))
        clinicList.add(new LinkedHashMap(uuid: "936875C9-95B3-4348-BD31-A87B75D2D481", sisma_id: "tPfx6PPlyiR", provinceCode: "07", province: "Sofala", districtCode: "06", district: "Chibabava", sitename: "Muxungue CS III", site_nid: "1070600"))
        clinicList.add(new LinkedHashMap(uuid: "1DD2E5B8-67D9-4FC3-96A5-4D64A2CFEA9B", sisma_id: "u0rDOAZhDEY", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Canhandula PS", site_nid: "1070716"))
        clinicList.add(new LinkedHashMap(uuid: "9E510989-190A-448E-BE75-59110D84DC95", sisma_id: "ap27tLsJRfs", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Dondo Sede CS I", site_nid: "1070706"))
        clinicList.add(new LinkedHashMap(uuid: "E2043A2A-DAC1-4B48-93F5-1613E22AEB73", sisma_id: "ACSn11FXitD", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Igreja Baptista CS", site_nid: "1070719"))
        clinicList.add(new LinkedHashMap(uuid: "CEEFD1AF-A3F9-4B42-B409-2DA8BF9E24CF", sisma_id: "LQDwTv4guwS", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Mafambisse CS", site_nid: "1070707"))
        clinicList.add(new LinkedHashMap(uuid: "BED76C3B-9382-4B9D-89B8-29F3D2A395D5", sisma_id: "dfCtfKhN8Ls", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Maxarote PS", site_nid: "1070714"))
        clinicList.add(new LinkedHashMap(uuid: "98C662AD-15EA-4A16-A7DA-167DD627B5D4", sisma_id: "SpDSDcuQSlO", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Mutua PS", site_nid: "1070713"))
        clinicList.add(new LinkedHashMap(uuid: "3A3274A0-B2EA-4469-8D8B-B4CBCB9681C6", sisma_id: "tbmvB89kzwX", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Samora Machel PS (Sofala, Dondo)", site_nid: "1070777"))
        clinicList.add(new LinkedHashMap(uuid: "D9F2FA92-D0B4-495D-8B9B-4DABB27A949C", sisma_id: "gINfgRdH1bc", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Savane PS", site_nid: "1070711"))
        clinicList.add(new LinkedHashMap(uuid: "04FBBE53-5A65-4866-B138-9EF4CF695CC8", sisma_id: "N/A", provinceCode: "07", province: "Sofala", districtCode: "08", district: "Dondo", sitename: "Centro de Saúde Militar do Dondo", site_nid: "1070721"))
        clinicList.add(new LinkedHashMap(uuid: "83B64197-7CA0-4A68-BACD-BFE8358F112E", sisma_id: "GpknMJUqXaP", provinceCode: "07", province: "Sofala", districtCode: "09", district: "Gorongosa", sitename: "Gorongoza-Sede CS", site_nid: "1070806"))
        clinicList.add(new LinkedHashMap(uuid: "16D35185-94A9-4C6B-B601-AA1DDDE9241A", sisma_id: "ZtrHpuMITev", provinceCode: "07", province: "Sofala", districtCode: "10", district: "Machanga", sitename: "Machang CS", site_nid: "1070908"))
        clinicList.add(new LinkedHashMap(uuid: "89D40139-8DD3-412B-8673-10197CD49011", sisma_id: "mXOBZijjNGw", provinceCode: "07", province: "Sofala", districtCode: "12", district: "Marromeu", sitename: "Marromeu HR", site_nid: "1071100"))
        clinicList.add(new LinkedHashMap(uuid: "FB8A58B0-91F4-4243-B082-4595E7A9448B", sisma_id: "SBZrCG3IdkO", provinceCode: "07", province: "Sofala", districtCode: "12", district: "Marromeu", sitename: "Nensa CS", site_nid: "1071106"))
        clinicList.add(new LinkedHashMap(uuid: "151B4705-15ED-4BA0-AD57-71CD2C64E5D0", sisma_id: "Mc8RAHbrGbq", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Chirassicua CS", site_nid: "1071321"))
        clinicList.add(new LinkedHashMap(uuid: "5ACAED5B-E82F-4EC0-85FF-CB04DCD87BDA", sisma_id: "lbp9AAxIbfk", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Lamego CS", site_nid: "1071308"))
        clinicList.add(new LinkedHashMap(uuid: "790A6E59-A42B-4447-AC5C-DD50C7CD84C8", sisma_id: "QdlMeJOmGZd", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Metuchira Lomaco CS", site_nid: "1071307"))
        clinicList.add(new LinkedHashMap(uuid: "6A240E60-C00C-4462-AF20-8ACDB61CA1C6", sisma_id: "vH7k60413Bv", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Nhamatanda HR", site_nid: "1071301"))
        clinicList.add(new LinkedHashMap(uuid: "7F441840-203A-4165-AE6C-78AA91A64D37", sisma_id: "WC2073QYTdY", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Nharuchonga PS", site_nid: "1071312"))
        clinicList.add(new LinkedHashMap(uuid: "033B3043-59F0-475A-94B1-5DFA54E58DE7", sisma_id: "qoPsXd5S2f4", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Siluvo PS", site_nid: "1071313"))
        clinicList.add(new LinkedHashMap(uuid: "BB81B78F-4FD9-41BF-B07F-3153B9023873", sisma_id: "q0ZxACWV34U", provinceCode: "07", province: "Sofala", districtCode: "14", district: "Nhamatanda", sitename: "Tica PS", site_nid: "1071318"))
        clinicList.add(new LinkedHashMap(uuid: "555428E6-4B1E-4A8E-8BE6-2DEA38C96590", sisma_id: "gjaJB4Kwi5N", provinceCode: "05", province: "Tete", districtCode: "01", district: "Angonia", sitename: "Dómue CS", site_nid: "1050207"))
        clinicList.add(new LinkedHashMap(uuid: "C9326B4F-9E14-482F-88EF-0CB88C1A5258", sisma_id: "X4j9nrbXIFD", provinceCode: "05", province: "Tete", districtCode: "01", district: "Angonia", sitename: "Lifidzi CS", site_nid: "1050210"))
        clinicList.add(new LinkedHashMap(uuid: "AD8AD593-81B9-412E-9209-32158F7EEF96", sisma_id: "B4nZa45pFS6", provinceCode: "05", province: "Tete", districtCode: "01", district: "Angonia", sitename: "Ulongue CS", site_nid: "1050215"))
        clinicList.add(new LinkedHashMap(uuid: "738C7733-0616-4F31-90F0-E2DACB37592B", sisma_id: "JJK6y7XUXwg", provinceCode: "05", province: "Tete", districtCode: "01", district: "Angonia", sitename: "Ulongue HR", site_nid: "1050200"))
        clinicList.add(new LinkedHashMap(uuid: "C87A16CE-BCC3-461C-BB98-F465C7DB17DD", sisma_id: "iHMPfPAgejg", provinceCode: "05", province: "Tete", districtCode: "02", district: "Cahora Bassa", sitename: "Chitima CS", site_nid: "1050308"))
        clinicList.add(new LinkedHashMap(uuid: "A42D767B-A235-442C-ACBF-DDFA1B67BF89", sisma_id: "NHToo8qOJtB", provinceCode: "05", province: "Tete", districtCode: "02", district: "Cahora Bassa", sitename: "Songo HR", site_nid: "1050301"))
        clinicList.add(new LinkedHashMap(uuid: "41719367-2AE2-4B16-A7C5-257AA69B2693", sisma_id: "N/A", provinceCode: "05", province: "Tete", districtCode: "02", district: "Cahora Bassa", sitename: "Centro de Saúde Militar do Songo", site_nid: "1050312"))
        clinicList.add(new LinkedHashMap(uuid: "9125104B-779B-4B6D-A703-933C58AC579E", sisma_id: "hCtIJsUJf1z", provinceCode: "05", province: "Tete", districtCode: "03", district: "Changara", sitename: "Changara CS", site_nid: "1050406"))
        clinicList.add(new LinkedHashMap(uuid: "FD223963-07F4-4A40-9587-DCD42B111AB2", sisma_id: "OBQrUtJIzm4", provinceCode: "05", province: "Tete", districtCode: "03", district: "Changara", sitename: "Dzunga CS", site_nid: "1050409"))
        clinicList.add(new LinkedHashMap(uuid: "8DD3BE7D-8F08-49E4-9725-40460F730CB6", sisma_id: "DUmysYmLkmN", provinceCode: "05", province: "Tete", districtCode: "03", district: "Changara", sitename: "M'Saua CS", site_nid: "1050415"))
        clinicList.add(new LinkedHashMap(uuid: "465CD27A-2074-46B1-B4EE-04F235FF4010", sisma_id: "wzOtGpFWedz", provinceCode: "05", province: "Tete", districtCode: "03", district: "Changara", sitename: "Mazoe Ponte CS", site_nid: "1050411"))
        clinicList.add(new LinkedHashMap(uuid: "713C9914-64C3-4BC2-BB97-69D609727DBF", sisma_id: "P2Q61EK1Hae", provinceCode: "05", province: "Tete", districtCode: "05", district: "Chiuta", sitename: "Manje CS", site_nid: "1050608"))
        clinicList.add(new LinkedHashMap(uuid: "9563B5F8-6FEC-4E14-82E9-D709A01D8E63", sisma_id: "IxREZ1Bstdj", provinceCode: "05", province: "Tete", districtCode: "05", district: "Macanga", sitename: "Furancungo CS", site_nid: "1050701"))
        clinicList.add(new LinkedHashMap(uuid: "8AEEDE8F-DA74-4931-8D14-1E37D3ADCB1B", sisma_id: "H436FeCHje7", provinceCode: "05", province: "Tete", districtCode: "09", district: "Magoe", sitename: "Daque CS", site_nid: "1050809"))
        clinicList.add(new LinkedHashMap(uuid: "F52B7D0B-BD74-4282-8C40-FCA5479217B6", sisma_id: "fySHBDzfcfs", provinceCode: "05", province: "Tete", districtCode: "09", district: "Magoe", sitename: "Magoe CS", site_nid: "1050810"))
        clinicList.add(new LinkedHashMap(uuid: "DD514E61-BEFF-4827-80E3-0F9DBE47FF86", sisma_id: "FV6MG0l7WKb", provinceCode: "05", province: "Tete", districtCode: "09", district: "Magoe", sitename: "Mucumbura CS", site_nid: "1050811"))
        clinicList.add(new LinkedHashMap(uuid: "DE0C314A-65A4-43AC-9962-0C065F12992F", sisma_id: "ZFQolEiHHYC", provinceCode: "05", province: "Tete", districtCode: "10", district: "Marara", sitename: "Boroma CS", site_nid: "1051505"))
        clinicList.add(new LinkedHashMap(uuid: "51CDDCA3-54FF-410F-AE19-EF72AF45A05A", sisma_id: "rRlDT6E5nh2", provinceCode: "05", province: "Tete", districtCode: "10", district: "Marara", sitename: "Cachembe CS", site_nid: "1051507"))
        clinicList.add(new LinkedHashMap(uuid: "8589591D-6001-42F5-8A39-46B0EC240F87", sisma_id: "pialHm2D58U", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "25 de Setembro CS", site_nid: "1051018"))
        clinicList.add(new LinkedHashMap(uuid: "9C0172D0-B1AE-43E8-A824-8D4E0B20DC84", sisma_id: "q607TuZee6U", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Caphirizange PS", site_nid: "1051011"))
        clinicList.add(new LinkedHashMap(uuid: "3AAEE296-BE4B-4EE5-945C-2D5B5D43C4D6", sisma_id: "CWCQ4ITSuX7", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Cateme CS", site_nid: "1051017"))
        clinicList.add(new LinkedHashMap(uuid: "3F9A9DC4-A0D2-48D2-83A4-F864FEEFF7C2", sisma_id: "XRnDLkFpgrx", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "CFM CS", site_nid: "1051013"))
        clinicList.add(new LinkedHashMap(uuid: "8C01E2AC-D7BC-4E63-BDA6-EA7997EED877", sisma_id: "hMebVt7gOmV", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Mameme II CS", site_nid: "1051007"))
        clinicList.add(new LinkedHashMap(uuid: "57D89245-84AC-48AD-BF9A-00A291F21E57", sisma_id: "HutmsTbIiVr", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Moatize CS", site_nid: "1051008"))
        clinicList.add(new LinkedHashMap(uuid: "35659CF6-EE29-4E47-9DC1-35983B6BE26A", sisma_id: "XapF957qGgl", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Ncondezi CS", site_nid: "1051003"))
        clinicList.add(new LinkedHashMap(uuid: "715BD0EC-8A66-45B4-9731-E11E3FFD1614", sisma_id: "fXj9fD2iRbM", provinceCode: "05", province: "Tete", districtCode: "12", district: "Moatize", sitename: "Zobue CS", site_nid: "1051006"))
        clinicList.add(new LinkedHashMap(uuid: "CD3A14A6-921B-4791-A641-DA1A697F0DD1", sisma_id: "Wkec36bPAdt", provinceCode: "05", province: "Tete", districtCode: "13", district: "Mutarara", sitename: "Inhangoma CS", site_nid: "1051111"))
        clinicList.add(new LinkedHashMap(uuid: "01BC812D-5CA5-47B0-BA4B-2EE9988DF256", sisma_id: "RQ9BadPSFjO", provinceCode: "05", province: "Tete", districtCode: "13", district: "Mutarara", sitename: "Mutarara HR", site_nid: "1051101"))
        clinicList.add(new LinkedHashMap(uuid: "308A24F7-6262-4C7B-8FD4-6F210F6371BD", sisma_id: "FkUYIegS10Y", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Mpadue CS", site_nid: "1050120"))
        clinicList.add(new LinkedHashMap(uuid: "2AB4107E-DD17-40B0-8284-98EDFB840A8E", sisma_id: "zTeHQ5rGdnX", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Nº 1 - Bairro Magaia CS", site_nid: "1050109"))
        clinicList.add(new LinkedHashMap(uuid: "EAF111E8-E4DB-484D-BC28-8C46D0D177E0", sisma_id: "orS0TfuqXwk", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Nº 2 - Bairro Matundo CS", site_nid: "1050110"))
        clinicList.add(new LinkedHashMap(uuid: "862A8BFA-078D-4B87-A0A4-2EBF8535F00D", sisma_id: "b7tzsqi5Dgl", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Nº 3 - Bairro Manyanga CS", site_nid: "1050111"))
        clinicList.add(new LinkedHashMap(uuid: "FB28B53E-AE41-4821-B71F-A155014A070D", sisma_id: "R3I0hJ85IPq", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Nº 4 - Bairro Muthemba CS", site_nid: "1050106"))
        clinicList.add(new LinkedHashMap(uuid: "D6A0B4E3-0C5C-4704-9727-1F7223ACEEF3", sisma_id: "CgH5aTlqbTe", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Tete HP", site_nid: "1050100"))
        clinicList.add(new LinkedHashMap(uuid: "A2B1DC05-5196-4AF2-BBA3-83D6B1F35DC2", sisma_id: "N/A", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Centro de Saúde do Batalhão de Infantaria de Tete", site_nid: "105011401"))
        clinicList.add(new LinkedHashMap(uuid: "C269D3AD-DACC-4238-8F7C-DE61035DC0D7", sisma_id: "N/A", provinceCode: "05", province: "Tete", districtCode: "14", district: "Tete", sitename: "Base Aérea de Chingodzi M. Centro de Saúde", site_nid: "105012201"))

        return clinicList

    }


    List<Object> listClinic3() {
        List<Object> clinicList = new ArrayList<>()

        clinicList.add(new LinkedHashMap(uuid: "1EB4079A-EE82-4E83-8627-1BF6DFA88A6E", sisma_id: "N/A", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Chimondzo CS", site_nid: "0"))
        clinicList.add(new LinkedHashMap(uuid: "190AD8AC-3BDA-40F7-BD89-680F1CF6C129", sisma_id: "e9IyOwC3IxO", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Incaia CS", site_nid: "1090210"))
        clinicList.add(new LinkedHashMap(uuid: "12C79E2C-67BF-40FB-819A-582D34BC1E7A", sisma_id: "Q8UfR47Rv8X", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Macia CS", site_nid: "1090206"))
        clinicList.add(new LinkedHashMap(uuid: "D2CDA137-29A2-4F7D-B53C-60B324CF39E4", sisma_id: "TQ8oHK3IrIx", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Mamonho CS", site_nid: "1090208"))
        clinicList.add(new LinkedHashMap(uuid: "4CDFCAF1-6413-4ACE-A774-068D4D8ACFF1", sisma_id: "NGlwqwnSu3U", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Mangol CS", site_nid: "1090216"))
        clinicList.add(new LinkedHashMap(uuid: "3CAC89E7-DBAD-47DC-A1D7-BFADC6575781", sisma_id: "SomSfWXwyew", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Mazivila CS", site_nid: "1090212"))
        clinicList.add(new LinkedHashMap(uuid: "AC6AF35A-9FCC-4A76-9744-9A50038EEE4E", sisma_id: "JTODh5FD3SR", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Messano CS", site_nid: "1090213"))
        clinicList.add(new LinkedHashMap(uuid: "F27C33E1-F331-48E9-9541-608037DD154B", sisma_id: "wXmZ9hYQfjy", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Olombe CS", site_nid: "1090214"))
        clinicList.add(new LinkedHashMap(uuid: "5796B2C8-E79D-4EDA-9D49-98FA312F06E5", sisma_id: "FYvH5A8Zrcy", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Praia de Bilene CS", site_nid: "1090207"))
        clinicList.add(new LinkedHashMap(uuid: "2F06ED78-2EBD-4DFD-869F-57344FE17DF2", sisma_id: "N9QMrMiRmsG", provinceCode: "09", province: "Gaza", districtCode: "01", district: "Bilene", sitename: "Tuane CS", site_nid: "1090215"))
        clinicList.add(new LinkedHashMap(uuid: "6A021660-EB58-4A37-850F-967E12D60376", sisma_id: "wMk2W2lIIWH", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Alto Changane CS", site_nid: "1090307"))
        clinicList.add(new LinkedHashMap(uuid: "97A0CFB4-2489-4F67-A62A-4B4C6AA08AAE", sisma_id: "AK0UNNu6uk9", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Celula Mussavene CS", site_nid: "1090304"))
        clinicList.add(new LinkedHashMap(uuid: "510620F0-A576-4B8C-A040-DF1B1A6F3701", sisma_id: "ICeXgnZtZJ1", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Chaimite CS", site_nid: "1090312"))
        clinicList.add(new LinkedHashMap(uuid: "E675A3DE-8017-4CA3-9A80-28FA9797D393", sisma_id: "sGrpr9ItUC4", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Changanine CS", site_nid: "1090317"))
        clinicList.add(new LinkedHashMap(uuid: "63AB9B86-F644-4CF6-B90C-58604F70D12D", sisma_id: "GIo6pjLsIus", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Chibuto HR", site_nid: "1090306"))
        clinicList.add(new LinkedHashMap(uuid: "9C74F574-D110-4629-80A1-3AD9E34A41A4", sisma_id: "RdWF0C8zhEG", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Chimundo CS", site_nid: "1090313"))
        clinicList.add(new LinkedHashMap(uuid: "DCC38A59-EC19-461A-A83B-2286724463BC", sisma_id: "a1eoyaVa0ET", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Chipadja PS", site_nid: "1090308"))
        clinicList.add(new LinkedHashMap(uuid: "A4C88254-AA82-45C1-A7F9-DAF883A6B297", sisma_id: "BeX9u3N9wOJ", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Coca Mussava CS", site_nid: "1090305"))
        clinicList.add(new LinkedHashMap(uuid: "A75562E1-724C-4168-BC7E-70FBCC5E02CE", sisma_id: "WwETfYyJDcQ", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Maivene CS", site_nid: "1090311"))
        clinicList.add(new LinkedHashMap(uuid: "D7E555BE-E0B8-4EBC-B02A-1878A5BFDB2E", sisma_id: "fR1EnEkW7zv", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Malehice CS", site_nid: "1090309"))
        clinicList.add(new LinkedHashMap(uuid: "913C3B06-B813-40AC-A1D2-737DEE1CF416", sisma_id: "JATuy9bo4Vq", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Maqueze CS", site_nid: "1090302"))
        clinicList.add(new LinkedHashMap(uuid: "B7D64CB2-0507-42DF-81F9-3CA2D573EF69", sisma_id: "UvYcE4ajEfB", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Meboi CS", site_nid: "1090318"))
        clinicList.add(new LinkedHashMap(uuid: "C8868328-4FD2-467E-B324-5544BD82C562", sisma_id: "rCHA87l5pXk", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Mukhotwene CS", site_nid: "1090315"))
        clinicList.add(new LinkedHashMap(uuid: "A5CA8114-2697-4A31-8E7F-8E27703F6631", sisma_id: "KKQaEPfxnRW", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Muxaxane CS", site_nid: "1090310"))
        clinicList.add(new LinkedHashMap(uuid: "1EC04A87-DE39-4EDF-AABB-D8926F3335AF", sisma_id: "OLQb0ZDQlbs", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Nwavaquene CS", site_nid: "1090314"))
        clinicList.add(new LinkedHashMap(uuid: "5640E196-C65E-4C3A-803A-6B6E0BF5CA91", sisma_id: "lpO47pIJ16f", provinceCode: "09", province: "Gaza", districtCode: "02", district: "Chibuto", sitename: "Vila Milénio CS", site_nid: "1090303"))
        clinicList.add(new LinkedHashMap(uuid: "85323123-0350-4C56-A3D0-ADD7E8FAE78B", sisma_id: "OYg36chiKSp", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "25 de Setembro CS", site_nid: "1090605"))
        clinicList.add(new LinkedHashMap(uuid: "105D38CF-3957-4497-8622-AB9DF785C725", sisma_id: "paqvE6C8WW0", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Carmelo CS", site_nid: "1090603"))
        clinicList.add(new LinkedHashMap(uuid: "9B20F9F5-C463-4E48-88C0-7A7CE9E3391B", sisma_id: "lKI8bnvIJxj", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Chalocuane CS", site_nid: "1090615"))
        clinicList.add(new LinkedHashMap(uuid: "EF6D38E9-3801-461E-A3E6-5465E9D77118", sisma_id: "eIbjovFRQig", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Chiaquelane PS", site_nid: "1090626"))
        clinicList.add(new LinkedHashMap(uuid: "11773F40-6331-4E8D-B251-883EAD8BE84F", sisma_id: "fDvPwgu9pGH", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Chokwé CS", site_nid: "1090604"))
        clinicList.add(new LinkedHashMap(uuid: "D79E0673-B0EE-4E05-928E-E7E989BD3E6F", sisma_id: "fDvPwgu9pGH", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Chokwé HR", site_nid: "1090606"))
        clinicList.add(new LinkedHashMap(uuid: "D01070DD-4558-48C7-BD2D-B6E266D5A8DB", sisma_id: "kLRSNTYhGTc", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Conhane CS", site_nid: "1090617"))
        clinicList.add(new LinkedHashMap(uuid: "491D071D-73B0-4D29-AF9B-AB0CBC066CC3", sisma_id: "PHTug1tZ579", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Cumba PS", site_nid: "1090614"))
        clinicList.add(new LinkedHashMap(uuid: "166F17EB-46CC-4FDC-81BD-64853AA0059B", sisma_id: "E195Kngukyd", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Hokwe CS", site_nid: "1090616"))
        clinicList.add(new LinkedHashMap(uuid: "EFFC1302-BB7E-47A1-93BF-AF7C6CA4E91F", sisma_id: "LVRaYH8tw0E", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Lionde CS", site_nid: "1090620"))
        clinicList.add(new LinkedHashMap(uuid: "B23B6C21-A988-4297-938F-F2316DC6C283", sisma_id: "fUE0wYuBCdN", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Machua PS", site_nid: "1090613"))
        clinicList.add(new LinkedHashMap(uuid: "6EAEC1A0-6F12-4D12-83C5-8FE54087F260", sisma_id: "XbRKHHE9RY3", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Macunene CS", site_nid: "1090625"))
        clinicList.add(new LinkedHashMap(uuid: "1BEF9BBD-0342-43BB-BCAB-302C33AC2107", sisma_id: "xpsFhg5R8W3", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Malhazine CS", site_nid: "1090622"))
        clinicList.add(new LinkedHashMap(uuid: "0F98A97A-A088-4824-AF3F-8E7D133523DE", sisma_id: "csqhDvMNumL", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Manjangue CS", site_nid: "1090601"))
        clinicList.add(new LinkedHashMap(uuid: "A7F70189-FB31-4D4E-9180-E9322D4ECAD0", sisma_id: "IfkONUSeYHs", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Mapapa PS", site_nid: "1090610"))
        clinicList.add(new LinkedHashMap(uuid: "478CD2C9-5DFC-4DBB-AE1D-52E80D60836A", sisma_id: "Ni5OGFMWAY3", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Massavasse PS", site_nid: "1090621"))
        clinicList.add(new LinkedHashMap(uuid: "0B8D868B-CFC9-44AE-88FD-B3C79951B6E5", sisma_id: "sns0NVdAtfC", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Muianga CS", site_nid: "1090609"))
        clinicList.add(new LinkedHashMap(uuid: "12058BAE-E7BB-4DBA-AF1D-E2FF4C436AD5", sisma_id: "SABBZ4g8mG7", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Wachicoloane PS", site_nid: "1090611"))
        clinicList.add(new LinkedHashMap(uuid: "AFFE7D75-22E1-4747-A244-9E07E8368AB6", sisma_id: "V49I1WSVMzf", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Xilembene CS", site_nid: "1090619"))
        clinicList.add(new LinkedHashMap(uuid: "553B5C44-6E89-4F29-A731-19F8FC8F84D5", sisma_id: "IK4d5WLu2zd", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Zuza CS", site_nid: "1090607"))
        clinicList.add(new LinkedHashMap(uuid: "4AF573D9-505A-4178-A209-160F751E0EA2", sisma_id: "N/A", provinceCode: "09", province: "Gaza", districtCode: "05", district: "Chokwe", sitename: "Centro de Saúde Militar de Chokwe", site_nid: "1090623"))
        clinicList.add(new LinkedHashMap(uuid: "249E9AE1-DC87-4BB5-8E03-E5D4340F1C47", sisma_id: "qLGlLqcnNlv", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Banhine CS", site_nid: "1091206"))
        clinicList.add(new LinkedHashMap(uuid: "0823EE3D-972C-4A85-8C09-4DA3BA88F190", sisma_id: "iwWxXTk99T5", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Bungane CS", site_nid: "1091217"))
        clinicList.add(new LinkedHashMap(uuid: "99B26826-8C5E-4F8F-8778-72057FFE6C08", sisma_id: "U5UHektHHZB", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Chongoene CS", site_nid: "1091209"))
        clinicList.add(new LinkedHashMap(uuid: "F0AA7951-55AA-4E95-9705-FAC978FE69A9", sisma_id: "E801hVG6H4r", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Cucuine CS", site_nid: "1090917"))
        clinicList.add(new LinkedHashMap(uuid: "0C56A467-6A46-4C61-8898-3B919B368DB9", sisma_id: "AwLXLxO8HsA", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Maciene CS", site_nid: "1091212"))
        clinicList.add(new LinkedHashMap(uuid: "FAFB95A4-3A9F-4E27-AAFE-CEBBB384C098", sisma_id: "KL9t3bMlada", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Macupulane CS", site_nid: "1090913"))
        clinicList.add(new LinkedHashMap(uuid: "8BA16F46-9069-4B99-B18A-88ADE0411992", sisma_id: "xs3ALMCaSeQ", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Mangunze CS", site_nid: "1090914"))
        clinicList.add(new LinkedHashMap(uuid: "1A1C18AD-62E6-4910-BB31-A50996C28780", sisma_id: "ycdckvvkMIA", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Matsinhane CS", site_nid: "1090904"))
        clinicList.add(new LinkedHashMap(uuid: "EBECB021-E4AE-4726-897C-9ABCA0440E66", sisma_id: "rJEdkX7LBq5", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Ndambine 2000 PS", site_nid: "1091222"))
        clinicList.add(new LinkedHashMap(uuid: "5E07D6A8-8C61-4A2B-8503-26F9D83E0C15", sisma_id: "gcEI5N71UuE", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Nhacutse CS", site_nid: "1091210"))
        clinicList.add(new LinkedHashMap(uuid: "438BE042-E792-44E5-89C7-5883772D938A", sisma_id: "ZClAeRuJbRg", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Nhamavila PS", site_nid: "1091216"))
        clinicList.add(new LinkedHashMap(uuid: "AB562232-025E-4796-975E-B69D18E2F203", sisma_id: "reW3VekW8z3", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "Siaia CS", site_nid: "1091213"))
        clinicList.add(new LinkedHashMap(uuid: "5D0DE4F2-8859-4DE3-BF59-3F2C09B626C1", sisma_id: "lDDPyg8oEWn", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Chibabel CS", site_nid: "1090715"))
        clinicList.add(new LinkedHashMap(uuid: "7E1C659A-750D-4BC7-A797-BF58A8276154", sisma_id: "rWnN5AlEY5d", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Chimbembe CS", site_nid: "1090709"))
        clinicList.add(new LinkedHashMap(uuid: "782F63EB-2092-43F0-84AC-520D87D484BC", sisma_id: "pkmVnIU3x3Y", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Chinhacanine CS", site_nid: "1090710"))
        clinicList.add(new LinkedHashMap(uuid: "8FD95E4F-5F3A-4A34-9BD1-098BAF3059EE", sisma_id: "D3vdYqHkLPT", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Chivonguene CS", site_nid: "1090711"))
        clinicList.add(new LinkedHashMap(uuid: "34C7F603-3387-4045-BCCA-B1F6B81B7E1D", sisma_id: "LqTNZiipqh3", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Guija CS", site_nid: "1090706"))
        clinicList.add(new LinkedHashMap(uuid: "3D966D83-741E-4AAF-97F7-4FFE231DD608", sisma_id: "A90TdZN4Wqt", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Javanhane CS", site_nid: "1090708"))
        clinicList.add(new LinkedHashMap(uuid: "340ACF93-6181-4460-BE85-9F339992258C", sisma_id: "G9lkKjFz58F", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Mbalavala PS", site_nid: "1090701"))
        clinicList.add(new LinkedHashMap(uuid: "0D88053E-91FC-465B-B77E-0E347B7CB499", sisma_id: "SNqIvbMXo35", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Mpelane CS", site_nid: "1090712"))
        clinicList.add(new LinkedHashMap(uuid: "63D382DE-932C-4EB4-8589-E8C4C50348D9", sisma_id: "yusrrlxfJP0", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Mubanguene CS", site_nid: "1090713"))
        clinicList.add(new LinkedHashMap(uuid: "6DDEA5CA-80A3-4B54-A3C4-8C949A41C89B", sisma_id: "lRie85DjIfr", provinceCode: "09", province: "Gaza", districtCode: "07", district: "Guija", sitename: "Nalazi CS", site_nid: "1090707"))
        clinicList.add(new LinkedHashMap(uuid: "43C3C4BE-6DCA-4AA1-B08C-56346EA4AECF", sisma_id: "agVSQo1X7he", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Chicumbane HR", site_nid: "1091201"))
        clinicList.add(new LinkedHashMap(uuid: "87DE0AA2-405F-4E4C-BC55-F918C75F29BD", sisma_id: "Wv91ax5fXcy", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Chipenhe CS", site_nid: "1091208"))
        clinicList.add(new LinkedHashMap(uuid: "1DCC7C03-BADE-441B-81A4-27E75D0E17E1", sisma_id: "vZBUyCRvQwb", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Chissano CS", site_nid: "1090209"))
        clinicList.add(new LinkedHashMap(uuid: "19D51212-8642-415A-A8C6-BD07B3568941", sisma_id: "BH4CuoQulq4", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Julius Nyerere CS", site_nid: "1091211"))
        clinicList.add(new LinkedHashMap(uuid: "619CEF02-DEB0-4D1B-B7B7-6AE53FBC92B8", sisma_id: "trLNrYPHENJ", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Licilo CS", site_nid: "1090211"))
        clinicList.add(new LinkedHashMap(uuid: "DBDD1C3A-DF1A-4674-BEE8-0F79B6020659", sisma_id: "kNg8HO0K8yx", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Vladimir Lenine CS", site_nid: "1091215"))
        clinicList.add(new LinkedHashMap(uuid: "FFCD0160-7C2E-4492-B927-C1754A851DD4", sisma_id: "miji6JrpG1W", provinceCode: "09", province: "Gaza", districtCode: "08", district: "Limpopo", sitename: "Zongoene CS", site_nid: "1091214"))
        clinicList.add(new LinkedHashMap(uuid: "EE5E7DDF-9F04-447A-B2A4-6DC483DEBFBB", sisma_id: "ZHavDyn5tSN", provinceCode: "09", province: "Gaza", districtCode: "09", district: "Mabalane", sitename: "Combomune CS", site_nid: "1090807"))
        clinicList.add(new LinkedHashMap(uuid: "006E8FF2-984D-4CE5-A284-EC51A4DBB4E9", sisma_id: "k5cgG3xb8w9", provinceCode: "09", province: "Gaza", districtCode: "09", district: "Mabalane", sitename: "Mabalane CS", site_nid: "1090806"))
        clinicList.add(new LinkedHashMap(uuid: "03A20823-2064-47E6-8174-BBE7878E8B87", sisma_id: "Fa80DfDS7GU", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Betula CS", site_nid: "1090908"))
        clinicList.add(new LinkedHashMap(uuid: "A5BB30F8-77BD-4841-9CC2-6B3AC7731E2A", sisma_id: "zZ1zTtJcaxy", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Chibondzane CS", site_nid: "1090911"))
        clinicList.add(new LinkedHashMap(uuid: "25E98729-3873-42E5-8E51-4492C113ED0B", sisma_id: "RFKinyt5Hu0", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Chidenguele CS", site_nid: "1090906"))
        clinicList.add(new LinkedHashMap(uuid: "F3C7A027-25AA-4B33-98F3-92B5EBCCA46E", sisma_id: "HcKhYZStyYS", provinceCode: "09", province: "Gaza", districtCode: "06", district: "Chonguene", sitename: "CS Chicavane", site_nid: "1090918"))
        clinicList.add(new LinkedHashMap(uuid: "0F0E4809-14D4-4412-9EEA-B69F8817C99B", sisma_id: "io32E4vClN1", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Dengoine CS", site_nid: "1090907"))
        clinicList.add(new LinkedHashMap(uuid: "A9BA9026-68D0-4304-94AE-C4A986BD5231", sisma_id: "bGiYq1u7UmW", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Incadine CS", site_nid: "1090927"))
        clinicList.add(new LinkedHashMap(uuid: "42218DFE-270D-4631-8959-FFDE52860917", sisma_id: "H6zlDsf3Ijk", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Laranjeiras CS", site_nid: "1090920"))
        clinicList.add(new LinkedHashMap(uuid: "0801CD6C-FDB7-47EB-A723-368E6D257698", sisma_id: "D4lxzmURrss", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Macasselane CS", site_nid: "1090928"))
        clinicList.add(new LinkedHashMap(uuid: "67FDC15E-E918-4F1D-8F3D-B5E22E93902B", sisma_id: "LKoJ9fVwD0P", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Macuacua CS", site_nid: "1090912"))
        clinicList.add(new LinkedHashMap(uuid: "B0CB5797-04E2-4A35-947F-2C5D92591588", sisma_id: "cDJnEnaqahh", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Mandlakazi HR", site_nid: "1090901"))
        clinicList.add(new LinkedHashMap(uuid: "067F7019-64DC-464F-9FBF-2629C080E0B5", sisma_id: "BfWANkR4YnE", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Mausse CS", site_nid: "1090919"))
        clinicList.add(new LinkedHashMap(uuid: "C0E78D71-4A68-4EDA-9AB0-B4A6657C3CD3", sisma_id: "cnlTGIV229k", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Muzamane CS", site_nid: "1090916"))
        clinicList.add(new LinkedHashMap(uuid: "3C3FC5EB-13B6-4269-81DA-ABA2EE4F60E0", sisma_id: "uiS7iJ0qJT2", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Ndolene CS", site_nid: "1090903"))
        clinicList.add(new LinkedHashMap(uuid: "20AD409A-A4D5-4A92-A7D0-1A1A1146B5DD", sisma_id: "l2vuhDTe96v", provinceCode: "09", province: "Gaza", districtCode: "10", district: "Mandlakaze", sitename: "Tavane CS", site_nid: "1090921"))
        clinicList.add(new LinkedHashMap(uuid: "13B546C6-A8B3-4890-A6CE-6D8A648AB521", sisma_id: "RaDcA1HyTtJ", provinceCode: "09", province: "Gaza", districtCode: "11", district: "Mapai", sitename: "Mapai CS", site_nid: "1090408"))
        clinicList.add(new LinkedHashMap(uuid: "97B26D58-2B27-4F34-B5E6-296138028C53", sisma_id: "SZtEeVQsrJR", provinceCode: "09", province: "Gaza", districtCode: "13", district: "Massingir", sitename: "Massingir CS", site_nid: "1091106"))
        clinicList.add(new LinkedHashMap(uuid: "170A17DE-1710-4A26-8012-D61C6D2D1164", sisma_id: "Y2wOzBn5rho", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Chilaulane CS", site_nid: "1091207"))
        clinicList.add(new LinkedHashMap(uuid: "9BF9189D-278C-4087-9054-098A411539E6", sisma_id: "QHFrPc4QuA2", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Cidade de Xai - Xai CS", site_nid: "1090107"))
        clinicList.add(new LinkedHashMap(uuid: "B25AF53F-3C0A-4E72-BBE1-39CBF204B2FA", sisma_id: "UVhIPdyViqg", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Marien Nguabi CS", site_nid: "1090106"))
        clinicList.add(new LinkedHashMap(uuid: "E08D2E86-977C-43FA-944B-F055AA8A8561", sisma_id: "qjBWwENixcl", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Patrice Lumumba CS", site_nid: "1090108"))
        clinicList.add(new LinkedHashMap(uuid: "7C947ED6-1273-46F6-AC6E-0E4586660701", sisma_id: "ybPLcvFIMke", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Praia PS", site_nid: "1090109"))
        clinicList.add(new LinkedHashMap(uuid: "CA0297F5-4FC3-4441-97B7-A6A26492FDA4", sisma_id: "eKu7hG6MUNx", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Unidade 7 CS", site_nid: "1090113"))
        clinicList.add(new LinkedHashMap(uuid: "BA5230C6-0E8B-47E4-AD17-BC1F96CE3F51", sisma_id: "mazuLTBMejK", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Zimilene CS", site_nid: "1091221"))
        clinicList.add(new LinkedHashMap(uuid: "B3BA2676-4DE8-4863-BE19-CAA65248937D", sisma_id: "ffk7yEwFeOn", provinceCode: "08", province: "Inhambane", districtCode: "01", district: "Funhalouro", sitename: "Funhalouro CS", site_nid: "1080206"))
        clinicList.add(new LinkedHashMap(uuid: "686AFD4C-3FDB-4B9C-A0DB-2DDB8928CB1C", sisma_id: "Fcby0PGFbdX", provinceCode: "08", province: "Inhambane", districtCode: "02", district: "Govuro", sitename: "Doane CS", site_nid: "1080306"))
        clinicList.add(new LinkedHashMap(uuid: "DC0D6FF6-70F3-4ED7-B79D-AD07115C9D66", sisma_id: "QBBoCyDYFFx", provinceCode: "08", province: "Inhambane", districtCode: "02", district: "Govuro", sitename: "Maluvane CS", site_nid: "1080311"))
        clinicList.add(new LinkedHashMap(uuid: "0D3E1303-789B-4691-8C73-A3BE1A04F817", sisma_id: "dZFIgcfLY19", provinceCode: "08", province: "Inhambane", districtCode: "02", district: "Govuro", sitename: "Nova Mambone CS", site_nid: "1080309"))
        clinicList.add(new LinkedHashMap(uuid: "9907F370-5D01-49A0-B66A-D3CC76BFADAA", sisma_id: "o7X4s6lXh6V", provinceCode: "08", province: "Inhambane", districtCode: "02", district: "Govuro", sitename: "Pande CS", site_nid: "1080307"))
        clinicList.add(new LinkedHashMap(uuid: "C3B7049F-0733-4B90-881F-EAFB925AD9C0", sisma_id: "NVcXTlTtkw2", provinceCode: "08", province: "Inhambane", districtCode: "02", district: "Govuro", sitename: "Save CS", site_nid: "1080308"))
        clinicList.add(new LinkedHashMap(uuid: "182DD191-535B-49DD-A274-CC6CAB8966ED", sisma_id: "VjpiUDWADWj", provinceCode: "08", province: "Inhambane", districtCode: "03", district: "Homoine", sitename: "Homoine CS", site_nid: "1080406"))
        clinicList.add(new LinkedHashMap(uuid: "30B34BB4-D469-46ED-98F3-D02A550D9C1A", sisma_id: "TffjgsTbeMr", provinceCode: "08", province: "Inhambane", districtCode: "04", district: "Inhambane", sitename: "Balane ( Urbano ) CS", site_nid: "1080106"))
        clinicList.add(new LinkedHashMap(uuid: "45CA20FE-157A-4D9B-BB37-1439826BDA1E", sisma_id: "HDqLDHyRGMw", provinceCode: "08", province: "Inhambane", districtCode: "04", district: "Inhambane", sitename: "Muelé CS", site_nid: "1080111"))
        clinicList.add(new LinkedHashMap(uuid: "8F08870D-665D-41DF-AF08-1693DCA18A6B", sisma_id: "SexWG9310UI", provinceCode: "08", province: "Inhambane", districtCode: "05", district: "Inharrime", sitename: "Chongola CS", site_nid: "1080507"))
        clinicList.add(new LinkedHashMap(uuid: "ADA8C7E0-ED05-4978-9526-43EFEA72CDF5", sisma_id: "HxHwZkbpqpK", provinceCode: "08", province: "Inhambane", districtCode: "05", district: "Inharrime", sitename: "Inharrime CS", site_nid: "1080509"))
        clinicList.add(new LinkedHashMap(uuid: "B89CC58A-133F-4880-830B-EB93FFB7A4F7", sisma_id: "nWVovx1xTsC", provinceCode: "08", province: "Inhambane", districtCode: "05", district: "Inhassoro", sitename: "Inhassoro CS", site_nid: "1080606"))
        clinicList.add(new LinkedHashMap(uuid: "074AB044-460C-49E4-9B3C-E350542E87FF", sisma_id: "VgtO6XwEMIS", provinceCode: "08", province: "Inhambane", districtCode: "05", district: "Inhassoro", sitename: "Mangungumete CS", site_nid: "1080610"))
        clinicList.add(new LinkedHashMap(uuid: "4E3A8B7D-F453-4690-B1DA-A3E8A68110B8", sisma_id: "oQhSQA8p0hN", provinceCode: "08", province: "Inhambane", districtCode: "07", district: "Jangamo", sitename: "Cumbana CS", site_nid: "1080706"))
        clinicList.add(new LinkedHashMap(uuid: "E508486D-35FF-4441-A914-6A41D78F971A", sisma_id: "NfDh6zSORrX", provinceCode: "08", province: "Inhambane", districtCode: "07", district: "Jangamo", sitename: "Jangamo CS", site_nid: "1080707"))
        clinicList.add(new LinkedHashMap(uuid: "D02470EE-B921-48CE-A4DC-FF17AF4036E6", sisma_id: "b7dYpgnGhKG", provinceCode: "08", province: "Inhambane", districtCode: "08", district: "Mabote", sitename: "Mabote CS", site_nid: "1080806"))
        clinicList.add(new LinkedHashMap(uuid: "174723C4-757A-4A24-B935-31DBEF79767F", sisma_id: "XYFLANKJIE3", provinceCode: "08", province: "Inhambane", districtCode: "08", district: "Mabote", sitename: "Mussengue CS", site_nid: "1080807"))
        clinicList.add(new LinkedHashMap(uuid: "7DDDE2AC-4943-46FF-91C1-9C932529A876", sisma_id: "XjxdNDwOzHx", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Cangela CS", site_nid: "1080910"))
        clinicList.add(new LinkedHashMap(uuid: "1CD782D9-01E7-43F3-9142-62B5FD7C2781", sisma_id: "rUrRDrnX1uI", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Chihunze CS", site_nid: "1080919"))
        clinicList.add(new LinkedHashMap(uuid: "4959EA77-A0D8-40F5-9FE1-F257A9142E33", sisma_id: "x9o9t1l28H7", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Inhaloi CS", site_nid: "1080915"))
        clinicList.add(new LinkedHashMap(uuid: "49D302DF-FF66-44AA-A655-E2D0CB37D341", sisma_id: "LrUtRIpUODu", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Massinga CS", site_nid: "1080901"))
        clinicList.add(new LinkedHashMap(uuid: "6A1F2EE1-49FD-4A42-8FB1-CB5A17A82508", sisma_id: "dxr1EhQZWzs", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Muvamba CS", site_nid: "1080912"))
        clinicList.add(new LinkedHashMap(uuid: "EA155236-8847-40AE-8C35-002426F433E5", sisma_id: "h4XkAAWAHiB", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Nhachengue CS", site_nid: "1080906"))
        clinicList.add(new LinkedHashMap(uuid: "4D1740E0-421F-40CE-A712-ECA7A092F42F", sisma_id: "mwAMrYSSkYq", provinceCode: "08", province: "Inhambane", districtCode: "09", district: "Massinga", sitename: "Rio Das Pedras CS", site_nid: "1080913"))
        clinicList.add(new LinkedHashMap(uuid: "51AAC10A-2373-44C9-9F8C-C59CE2D0850E", sisma_id: "igEU9bHdyS0", provinceCode: "08", province: "Inhambane", districtCode: "10", district: "Maxixe", sitename: "Bembe CS", site_nid: "1081011"))
        clinicList.add(new LinkedHashMap(uuid: "CD7122EF-6137-460D-931F-2155350C3C94", sisma_id: "A7Ufxcr4R37", provinceCode: "08", province: "Inhambane", districtCode: "10", district: "Maxixe", sitename: "Chicuque HR", site_nid: "1081000"))
        clinicList.add(new LinkedHashMap(uuid: "4D9FD682-27E2-444C-8C54-04F3B8FC08AB", sisma_id: "Cy5YNcfXvW0", provinceCode: "08", province: "Inhambane", districtCode: "10", district: "Maxixe", sitename: "Mabil CS", site_nid: "1081013"))
        clinicList.add(new LinkedHashMap(uuid: "0A88F8FC-8AAA-4CDB-833D-50FF04A7C237", sisma_id: "UMosG8P8pN2", provinceCode: "08", province: "Inhambane", districtCode: "10", district: "Maxixe", sitename: "Manhala CS", site_nid: "1081016"))
        clinicList.add(new LinkedHashMap(uuid: "7AE21AD7-465A-4845-9B85-0BC2C9BF8FB6", sisma_id: "heCPyILQjNf", provinceCode: "08", province: "Inhambane", districtCode: "10", district: "Maxixe", sitename: "Maxixe CS", site_nid: "1081006"))
        clinicList.add(new LinkedHashMap(uuid: "901400DE-B345-4F55-9CFF-5432086E4006", sisma_id: "k4zmTsWQkoJ", provinceCode: "08", province: "Inhambane", districtCode: "11", district: "Morrumbene", sitename: "Cambine CS", site_nid: "1081107"))
        clinicList.add(new LinkedHashMap(uuid: "F6CF4478-B23C-411A-A59B-03F28943ED09", sisma_id: "Jx4GlX2Wr6J", provinceCode: "08", province: "Inhambane", districtCode: "11", district: "Morrumbene", sitename: "Mahangue PS", site_nid: "1081109"))
        clinicList.add(new LinkedHashMap(uuid: "6BEF1EC2-4A14-473F-AE44-2BC802470321", sisma_id: "v7yWEibceYk", provinceCode: "08", province: "Inhambane", districtCode: "11", district: "Morrumbene", sitename: "Morrumbene CS", site_nid: "1081111"))
        clinicList.add(new LinkedHashMap(uuid: "E9AF6F68-9A4A-4996-B223-572DA23EC84E", sisma_id: "wuabYpY6akz", provinceCode: "08", province: "Inhambane", districtCode: "11", district: "Morrumbene", sitename: "Mucoduene CS", site_nid: "1081114"))
        clinicList.add(new LinkedHashMap(uuid: "A694434B-1193-4AA3-9CE9-408C12F5AC4A", sisma_id: "n9BptyoB5Xr", provinceCode: "08", province: "Inhambane", districtCode: "11", district: "Morrumbene", sitename: "Sitila CS", site_nid: "1081110"))
        clinicList.add(new LinkedHashMap(uuid: "25ACFCCF-E663-404D-9402-085788CDCEF6", sisma_id: "WWSIjuHBXvz", provinceCode: "08", province: "Inhambane", districtCode: "12", district: "Panda", sitename: "Inhassune CS", site_nid: "1081206"))
        clinicList.add(new LinkedHashMap(uuid: "316705C1-ACAF-48C3-B9DA-E025E0042010", sisma_id: "VhZHSfryUrh", provinceCode: "08", province: "Inhambane", districtCode: "12", district: "Panda", sitename: "Panda CS", site_nid: "1081210"))
        clinicList.add(new LinkedHashMap(uuid: "7506872E-B960-44C0-9867-13CBF4BC0962", sisma_id: "kyPpXW19E9C", provinceCode: "08", province: "Inhambane", districtCode: "13", district: "Vilankulo", sitename: "Belane CS", site_nid: "1081306"))
        clinicList.add(new LinkedHashMap(uuid: "7EBE2412-165D-49A3-919A-112CEEEA3BCA", sisma_id: "cY8IyRdRHFY", provinceCode: "08", province: "Inhambane", districtCode: "13", district: "Vilankulo", sitename: "Chibuene CS", site_nid: "1081313"))
        clinicList.add(new LinkedHashMap(uuid: "7DE5924E-C1F7-4E18-947B-983B369AA141", sisma_id: "Z8IPE1w8g98", provinceCode: "08", province: "Inhambane", districtCode: "13", district: "Vilankulo", sitename: "Mapinhane CS", site_nid: "1081307"))
        clinicList.add(new LinkedHashMap(uuid: "A00988B3-90C4-4F7A-8C0A-DB2B3D8C7794", sisma_id: "LaTQk8ActWt", provinceCode: "08", province: "Inhambane", districtCode: "13", district: "Vilankulo", sitename: "Pambarra CS", site_nid: "1081308"))
        clinicList.add(new LinkedHashMap(uuid: "7A6217BE-7807-44A8-8ABD-D584D77ADDC9", sisma_id: "d6e2tYvdeEw", provinceCode: "08", province: "Inhambane", districtCode: "13", district: "Vilankulo", sitename: "Vilanculos HR", site_nid: "1081301"))
        clinicList.add(new LinkedHashMap(uuid: "33A1F07F-A198-4701-ADF7-C023C02D89A3", sisma_id: "ReHPLA8f629", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Cala CS", site_nid: "1081408"))
        clinicList.add(new LinkedHashMap(uuid: "83F133B0-2FD3-42FE-83D3-D4D408BB31F6", sisma_id: "MIWudQVawcx", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Canda CS", site_nid: "1081416"))
        clinicList.add(new LinkedHashMap(uuid: "E5A447C6-5315-4A92-B9A5-FF8C0144DA78", sisma_id: "QEGgb3I6Uyz", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Chitondo CS", site_nid: "1081404"))
        clinicList.add(new LinkedHashMap(uuid: "EBCDBC74-18C2-4B8E-B682-7E93AAD3BBC1", sisma_id: "S8PQUvE8Fxe", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Maundene CS", site_nid: "1081409"))
        clinicList.add(new LinkedHashMap(uuid: "2D9EE330-EA4B-4738-A366-DB6828E2523F", sisma_id: "mriTVN9GiBF", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Mavila CS", site_nid: "1081413"))
        clinicList.add(new LinkedHashMap(uuid: "E6E2B8AF-03EA-4060-AFBC-4A3A59F48ACC", sisma_id: "YA2Prv129Gl", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Muane CS", site_nid: "1081415"))
        clinicList.add(new LinkedHashMap(uuid: "8E8862D8-34E9-4991-824A-50AAA87021E8", sisma_id: "AhQpA3DVfJf", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Quissico CS", site_nid: "1081405"))
        clinicList.add(new LinkedHashMap(uuid: "AEFF499A-1756-4C0A-962D-9D4AD8EEE7B1", sisma_id: "Mx3AX9iOppd", provinceCode: "08", province: "Inhambane", districtCode: "14", district: "Zavala", sitename: "Zandamela CS", site_nid: "1081410"))
        clinicList.add(new LinkedHashMap(uuid: "3E8B66DA-8A66-497D-829D-63973078F581", sisma_id: "ZPnH5sZ4m0L", provinceCode: "04", province: "Zambezia", districtCode: "01", district: "Alto Molocue", sitename: "Alto Molócuè HR", site_nid: "1040200"))
        clinicList.add(new LinkedHashMap(uuid: "C7D393FC-FC01-4D83-B3F2-725E7F021ED2", sisma_id: "h1mzfd8QbVv", provinceCode: "04", province: "Zambezia", districtCode: "01", district: "Alto Molocue", sitename: "Bonifacio Gruveta CS", site_nid: "1040299"))
        clinicList.add(new LinkedHashMap(uuid: "9ED0B351-E8A7-4688-9C5F-DB83E38818F6", sisma_id: "lh2m54YWS1Z", provinceCode: "04", province: "Zambezia", districtCode: "01", district: "Alto Molocue", sitename: "Nauela CS", site_nid: "1040213"))
        clinicList.add(new LinkedHashMap(uuid: "28819598-7058-4CC2-80FF-F0EEA20F53A4", sisma_id: "azEkexvj0fC", provinceCode: "04", province: "Zambezia", districtCode: "03", district: "Derre", sitename: "Derre CS", site_nid: "1041803"))
        clinicList.add(new LinkedHashMap(uuid: "F03CDD83-722B-473D-BCBF-BFFF9BC0DCF2", sisma_id: "taw2TQq5BNj", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Alto Ligonha CS", site_nid: "1040406"))
        clinicList.add(new LinkedHashMap(uuid: "5DC29B5B-42D4-455E-95D3-300457F41BE1", sisma_id: "jFfiWVBJU4h", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Gilé HD", site_nid: "1040407"))
        clinicList.add(new LinkedHashMap(uuid: "8450D365-5AC1-4F45-B240-B78A11085ECC", sisma_id: "EB2UQSNSNi2", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Kayane CS", site_nid: "1040439"))
        clinicList.add(new LinkedHashMap(uuid: "E254398D-CD5F-4F87-BB4D-E3F353AC2A10", sisma_id: "o5aeA2RZeMa", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Mamala CS", site_nid: "1040455"))
        clinicList.add(new LinkedHashMap(uuid: "40C70AC3-E0CB-46C6-800C-3EDF7C0295B1", sisma_id: "WMOp7JC4D4N", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Moneia CS", site_nid: "1040408"))
        clinicList.add(new LinkedHashMap(uuid: "A317DE37-445A-490F-948C-2DD0D1D94939", sisma_id: "SsDqXBHH20l", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Muiane CS", site_nid: "1040409"))
        clinicList.add(new LinkedHashMap(uuid: "97B67AC9-81A2-4213-A0A9-3E3A15136B91", sisma_id: "LoGR7zQa7yf", provinceCode: "04", province: "Zambezia", districtCode: "04", district: "Gile", sitename: "Uapé CS", site_nid: "1040410"))
        clinicList.add(new LinkedHashMap(uuid: "15E36060-ED03-4DA7-88E9-71B0C9B7619E", sisma_id: "tRAjY7UkjXJ", provinceCode: "04", province: "Zambezia", districtCode: "05", district: "Gurue", sitename: "Gurue CS", site_nid: "1040506"))
        clinicList.add(new LinkedHashMap(uuid: "7955DD95-3D61-4384-8364-A2277FE30B90", sisma_id: "vb83ymdhgq2", provinceCode: "04", province: "Zambezia", districtCode: "05", district: "Gurue", sitename: "Lioma CS", site_nid: "1040507"))
        clinicList.add(new LinkedHashMap(uuid: "258C65F8-4CF0-417A-99EE-620DD4037895", sisma_id: "vLIM0qgg2sn", provinceCode: "04", province: "Zambezia", districtCode: "06", district: "Ile", sitename: "Ile-Sede CS", site_nid: "1040601"))
        clinicList.add(new LinkedHashMap(uuid: "CB8E7AC7-EA98-4C9E-94ED-40EF754BB3A8", sisma_id: "m0BznB7Jtl0", provinceCode: "04", province: "Zambezia", districtCode: "06", district: "Ile", sitename: "Mugulama CS", site_nid: "1040608"))
        clinicList.add(new LinkedHashMap(uuid: "E8D5782A-D834-4036-A74B-164B1D83D23E", sisma_id: "pjEEPqxds6K", provinceCode: "04", province: "Zambezia", districtCode: "06", district: "Ile", sitename: "Namanda CS", site_nid: "1040661"))
        clinicList.add(new LinkedHashMap(uuid: "42DB3DC6-BC0B-49BC-85E2-6C71DE51339D", sisma_id: "RlcapgUNuyW", provinceCode: "04", province: "Zambezia", districtCode: "06", district: "Ile", sitename: "Socone CS", site_nid: "1040613"))
        clinicList.add(new LinkedHashMap(uuid: "C449F35B-FE8B-4A26-B4D1-3FB878BB266B", sisma_id: "LHGAS5Zg3EP", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Bingagira CS", site_nid: "1040711"))
        clinicList.add(new LinkedHashMap(uuid: "388E3F8C-C347-4232-9F92-86E3A8FAE743", sisma_id: "cHfDkQ7a9Bc", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Cherimane", site_nid: "1040706"))
        clinicList.add(new LinkedHashMap(uuid: "2D3630D2-CACF-4941-8C8E-FCF019D42AA4", sisma_id: "BrHMD7aiCzH", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Gonhane CS", site_nid: "1040707"))
        clinicList.add(new LinkedHashMap(uuid: "9D431D47-A4FD-4AFD-90F9-583B3E89FF2D", sisma_id: "p8tmP48Fs34", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Inhassunge - Sede CS", site_nid: "1040708"))
        clinicList.add(new LinkedHashMap(uuid: "D0DC1534-024C-42D6-84E4-1620D12187BB", sisma_id: "sEoQCWYLNmJ", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Olinda CS", site_nid: "1040710"))
        clinicList.add(new LinkedHashMap(uuid: "A87B6D09-055D-4CD0-A62F-D1D599619EF3", sisma_id: "ei4J0kvrRJt", provinceCode: "04", province: "Zambezia", districtCode: "07", district: "Inhassunge", sitename: "Palane-Mucula CS", site_nid: "1040713"))
        clinicList.add(new LinkedHashMap(uuid: "8A281EDF-29BA-4437-94EC-3AC7448D59CF", sisma_id: "yHWofLrxn2s", provinceCode: "04", province: "Zambezia", districtCode: "09", district: "Lugela", sitename: "Lugela-Sede CS", site_nid: "1040806"))
        clinicList.add(new LinkedHashMap(uuid: "0E9F81D6-ECB4-4903-AF06-001D736F0C56", sisma_id: "ZTlrL9nvfnj", provinceCode: "04", province: "Zambezia", districtCode: "09", district: "Lugela", sitename: "Mulide CS", site_nid: "1040836"))
        clinicList.add(new LinkedHashMap(uuid: "965F4CA6-CA01-4EFC-9845-525A2E6E2815", sisma_id: "OXAIkwvH8mR", provinceCode: "04", province: "Zambezia", districtCode: "09", district: "Lugela", sitename: "Munhamade CS", site_nid: "1040807"))
        clinicList.add(new LinkedHashMap(uuid: "65460381-ECDD-4FA8-84B2-0DA762D4EB28", sisma_id: "t1cFqAEoXs5", provinceCode: "04", province: "Zambezia", districtCode: "09", district: "Lugela", sitename: "Namagoa CS", site_nid: "1040808"))
        clinicList.add(new LinkedHashMap(uuid: "36275AA6-F1C7-470C-AE38-C0430B6CE345", sisma_id: "FOISd25KRP9", provinceCode: "04", province: "Zambezia", districtCode: "09", district: "Lugela", sitename: "Puthine CS", site_nid: "1040814"))
        clinicList.add(new LinkedHashMap(uuid: "375AA3BF-39E6-4F21-BE5D-E82EB65B5DCB", sisma_id: "fPaQL2tj7GB", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Alto Mutola CS", site_nid: "1040916"))
        clinicList.add(new LinkedHashMap(uuid: "414A1EC7-01D1-4D51-BF44-D35866FBA55D", sisma_id: "vUIhWrlO9hZ", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Cabuir CS", site_nid: "1040902"))
        clinicList.add(new LinkedHashMap(uuid: "151BEDE2-D925-49B3-8C31-08A9069C349A", sisma_id: "gm3ZfKVnAXa", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Cariua CS", site_nid: "1040913"))
        clinicList.add(new LinkedHashMap(uuid: "8FA44157-D299-4D71-BE8C-B917B8019D96", sisma_id: "pG3rSKY0V8J", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Mabala CS", site_nid: "1040908"))
        clinicList.add(new LinkedHashMap(uuid: "B4219195-B4ED-4415-950B-48DF12D176E8", sisma_id: "zq2P2zwNJR0", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Maganja da Costa CS", site_nid: "1040906"))
        clinicList.add(new LinkedHashMap(uuid: "3C589EA8-0B9F-42E6-B80B-FC16DC61131C", sisma_id: "ZHXOgxEYqIi", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Mapira CS", site_nid: "1040936"))
        clinicList.add(new LinkedHashMap(uuid: "A757F40E-2FB5-4944-B88E-45FB29F08502", sisma_id: "vtJ9VodPfjt", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Moloua CS", site_nid: "1040909"))
        clinicList.add(new LinkedHashMap(uuid: "60F79762-F70A-4B66-B603-37EEF61A472A", sisma_id: "zqrPS9290Za", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Moneia CS", site_nid: "1040937"))
        clinicList.add(new LinkedHashMap(uuid: "01D4964D-B355-451D-997E-AF85667B49AF", sisma_id: "MqD8r6lEb9t", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Muzo CS", site_nid: "1040961"))
        clinicList.add(new LinkedHashMap(uuid: "D0924673-48C0-4DE1-9328-7BA9871871E5", sisma_id: "OyKga19Ad0s", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Namurrumo CS", site_nid: "1040917"))
        clinicList.add(new LinkedHashMap(uuid: "E2BFBBA8-8F9E-4953-9276-12DD537DAE42", sisma_id: "pLfvZA5Jzwz", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Nante CS", site_nid: "1040911"))
        clinicList.add(new LinkedHashMap(uuid: "132B9553-2AEC-4EDD-A2CB-94DBCD29A364", sisma_id: "ikRdfE5S3ru", provinceCode: "04", province: "Zambezia", districtCode: "10", district: "Maganja Da Costa", sitename: "Vila Valdez PS", site_nid: "1040915"))
        clinicList.add(new LinkedHashMap(uuid: "D04CC650-A4C8-4E67-BCA9-11EF05267B2D", sisma_id: "MzPWuwvhmVW", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Carico CS", site_nid: "1041007"))
        clinicList.add(new LinkedHashMap(uuid: "86CA5AE1-0FBC-4E97-BCA3-76441F4DC689", sisma_id: "acmeTh7JQoS", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Chitambo CS", site_nid: "1041020"))
        clinicList.add(new LinkedHashMap(uuid: "918D9C78-F9CD-4872-A03C-D4778DA3E362", sisma_id: "fmMirj04kvv", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Dachudua CS", site_nid: "1041016"))
        clinicList.add(new LinkedHashMap(uuid: "C0B8E227-958B-425E-A13A-3AF30A2D0425", sisma_id: "DpAZK5AmFte", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Dulanha CS", site_nid: "1041011"))
        clinicList.add(new LinkedHashMap(uuid: "BEA07476-7510-4688-920D-E1AF2983457A", sisma_id: "SBaCkzdMafE", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Gurgunha CS", site_nid: "1041018"))
        clinicList.add(new LinkedHashMap(uuid: "D82B36C1-EBBC-469B-AFB4-73131F668FE7", sisma_id: "nXZKnRiTazh", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Liciro CS", site_nid: "1041014"))
        clinicList.add(new LinkedHashMap(uuid: "B0B1F2A1-326E-4335-B2BA-E0538CF09B9D", sisma_id: "Pu3gs8CDrCi", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Majaua CS", site_nid: "1041010"))
        clinicList.add(new LinkedHashMap(uuid: "C483D6BA-5B2C-4C2D-B962-30C8150E78CF", sisma_id: "RZGpK9CDCD1", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Milange CS", site_nid: "1041006"))
        clinicList.add(new LinkedHashMap(uuid: "F363C80F-E2CD-4A93-B5C0-C36966922DB9", sisma_id: "HPGgftOMRjx", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Milange HD", site_nid: "1041000"))
        clinicList.add(new LinkedHashMap(uuid: "954A89B1-B2F2-437C-B1D9-4C4C55311CD4", sisma_id: "XVBs8yAxi91", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Mongue CS", site_nid: "1041015"))
        clinicList.add(new LinkedHashMap(uuid: "FBB6EF78-4FF3-4E90-B585-685BC73FBDFD", sisma_id: "jGSboiG6eRY", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Muanhambo CS", site_nid: "1041004"))
        clinicList.add(new LinkedHashMap(uuid: "8030F8F8-186C-4A5F-9093-0F66B796AD35", sisma_id: "RExvTlwFE0z", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Nambuzi PS", site_nid: "1041017"))
        clinicList.add(new LinkedHashMap(uuid: "80FB204C-354B-4EA4-AA91-F6B4C7CF7A3E", sisma_id: "MopGXR47b9w", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Sabelua CS", site_nid: "1041067"))
        clinicList.add(new LinkedHashMap(uuid: "26C1AA2C-7D52-4193-9240-11BAF612FFD2", sisma_id: "zrrv95nOTal", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Tengua CS", site_nid: "1041012"))
        clinicList.add(new LinkedHashMap(uuid: "EA844666-2930-4783-9D5E-12440C867331", sisma_id: "yYVw842Qiav", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Vulalo CS", site_nid: "1041013"))
        clinicList.add(new LinkedHashMap(uuid: "69E340E1-583F-408F-82D8-34AF7258EFAF", sisma_id: "nQHujgqz4dq", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "16 de Junho CS", site_nid: "1041137"))
        clinicList.add(new LinkedHashMap(uuid: "A283D3AD-E268-4D77-B489-07CC9BAC317E", sisma_id: "XmuwpVFIIzP", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Alto Benfica PS", site_nid: "1041114"))
        clinicList.add(new LinkedHashMap(uuid: "2046B4BF-593E-452B-A8CC-74C5943D66B9", sisma_id: "Q928lZ6LlJH", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Caiave CS", site_nid: "1041110"))
        clinicList.add(new LinkedHashMap(uuid: "04829165-ABD5-49B4-A06A-B3BFB051D90C", sisma_id: "tk3fmkp8brQ", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Chimbua CS", site_nid: "1041155"))
        clinicList.add(new LinkedHashMap(uuid: "AC1CD96D-F7F2-4CAC-A85D-AD48FBF172FA", sisma_id: "o18yFAiRSLk", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Intome CS", site_nid: "1041113"))
        clinicList.add(new LinkedHashMap(uuid: "E83D68FD-EA29-4B68-B8C9-12FD2C7F3EB1", sisma_id: "D1b6iPjiElw", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Magogodo CS", site_nid: "1041119"))
        clinicList.add(new LinkedHashMap(uuid: "C67CBB52-A65B-45CF-A589-1CD9805186DA", sisma_id: "xQhuUjetZxa", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Mataia CS", site_nid: "1041122"))
        clinicList.add(new LinkedHashMap(uuid: "0C09D584-81F0-47E0-AA22-2B19CE7EE4A8", sisma_id: "QjvbRHYyxXZ", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Mocuba CS", site_nid: "1041106"))
        clinicList.add(new LinkedHashMap(uuid: "F6056DBA-2B70-4270-BCF8-5204909FD134", sisma_id: "Vdog3fvSDkR", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Mocuba HR", site_nid: "1041100"))
        clinicList.add(new LinkedHashMap(uuid: "F6F0FCEE-B287-403F-A247-BD8556C142F7", sisma_id: "JDpQEn7xdQ0", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Mocuba Sisal CS", site_nid: "1041107"))
        clinicList.add(new LinkedHashMap(uuid: "45768532-EEA2-4A51-AC8B-5A9995EA7A5A", sisma_id: "YhrCOcMKfNi", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Muanaco CS", site_nid: "1041136"))
        clinicList.add(new LinkedHashMap(uuid: "9040CCFE-6BC4-40DE-B5BE-15628C90A868", sisma_id: "XXr8uouV8D4", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Muaquiua CS", site_nid: "1041109"))
        clinicList.add(new LinkedHashMap(uuid: "C1500B8C-0326-4126-9CB6-83A7E7670E78", sisma_id: "GIGxJHBpkAK", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Mugeba CS", site_nid: "1041108"))
        clinicList.add(new LinkedHashMap(uuid: "299A8755-5283-4F1C-AA9E-7BF4E95C1F2D", sisma_id: "cvuk2JFmhvU", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Muloe CS", site_nid: "1041112"))
        clinicList.add(new LinkedHashMap(uuid: "CA5BE81F-366C-4260-8E19-89676C74AE63", sisma_id: "UFWwgB2V6U5", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Munhiba CS", site_nid: "1041117"))
        clinicList.add(new LinkedHashMap(uuid: "6296A900-095B-4DF5-990E-B6037AF67817", sisma_id: "sGXZC8zIp7y", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Namabida CS", site_nid: "1041156"))
        clinicList.add(new LinkedHashMap(uuid: "AB36C3A1-4505-473A-A2BD-C438E4489D00", sisma_id: "OawoGhDwlwh", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Namagoa CS", site_nid: "1041158"))
        clinicList.add(new LinkedHashMap(uuid: "E04AE794-F190-4D69-A9A8-8A3DA152BFEF", sisma_id: "zqkV9145JNY", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Namanjavira CS", site_nid: "1041116"))
        clinicList.add(new LinkedHashMap(uuid: "2B582BAB-AEFA-4166-BE36-5005DFB6837C", sisma_id: "kHIK5j0G0Wa", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Nhaluanda CS", site_nid: "1041118"))
        clinicList.add(new LinkedHashMap(uuid: "439D47CA-B419-4832-8D35-ECD3DF0BA303", sisma_id: "mhTVybHoPaV", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Padre Usera (Privado) CS", site_nid: "1041111"))
        clinicList.add(new LinkedHashMap(uuid: "A292C2D4-C7DC-45EB-AC3E-586A82B35932", sisma_id: "loP8Lmq4nQi", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Pedreira CS", site_nid: "1041121"))
        clinicList.add(new LinkedHashMap(uuid: "9FDE95CC-C912-480A-A7C5-35A79A0AE9B6", sisma_id: "o8Df4lHAJ9O", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Samora Machel CS", site_nid: "1041120"))
        clinicList.add(new LinkedHashMap(uuid: "D09708E2-DE5F-4E6A-A85F-0851D335CA79", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Centro de Saúde Militar de Mocuba", site_nid: "4112301"))
        clinicList.add(new LinkedHashMap(uuid: "D7B55930-6834-4F07-A1A8-80E78B008C52", sisma_id: "QgijAKLokSL", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Bajone PS", site_nid: "1042008"))
        clinicList.add(new LinkedHashMap(uuid: "1848B7A1-7A19-4CA5-83E1-2153BDDBF4A2", sisma_id: "oTOB4IoT3ZW", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Gurai CS", site_nid: "1042003"))
        clinicList.add(new LinkedHashMap(uuid: "4A787475-137D-4165-93FC-A893F44C43B0", sisma_id: "ocPYIGEnWd5", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Ilha de Idugo CS", site_nid: "1042004"))
        clinicList.add(new LinkedHashMap(uuid: "8AD69093-F580-4CE1-9156-E523A641A27B", sisma_id: "xWJ6GWrDj2n", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Maneia CS", site_nid: "1042005"))
        clinicList.add(new LinkedHashMap(uuid: "7D867BBC-E3A2-4422-AAF0-743C7C606169", sisma_id: "LEy4sVLBJf0", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Missal PS", site_nid: "1042009"))
        clinicList.add(new LinkedHashMap(uuid: "331CA5A6-9DAB-4181-A76D-2385DA2EB8A8", sisma_id: "cb7fMqbswZv", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Mocubela CS", site_nid: "1042006"))
        clinicList.add(new LinkedHashMap(uuid: "A6E3C2F3-375D-4166-9CC4-570F18D99678", sisma_id: "K0lbSrY8var", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Naico CS", site_nid: "1042007"))
        clinicList.add(new LinkedHashMap(uuid: "1396149A-87D8-4CE8-B0A1-C044D20B736C", sisma_id: "hfXZElkqidE", provinceCode: "04", province: "Zambezia", districtCode: "13", district: "Mocubela", sitename: "Tapata CS", site_nid: "1042010"))
        clinicList.add(new LinkedHashMap(uuid: "123E539A-0478-4783-A7AA-CF9FE3D58148", sisma_id: "clbe60CHURD", provinceCode: "04", province: "Zambezia", districtCode: "14", district: "Molumbo", sitename: "Corromana CS", site_nid: "1042105"))
        clinicList.add(new LinkedHashMap(uuid: "66E18F79-3D95-4EA9-AC81-FE7C0F26B010", sisma_id: "SiZkiH1aed4", provinceCode: "04", province: "Zambezia", districtCode: "14", district: "Molumbo", sitename: "Molumbo CS", site_nid: "1042104"))
        clinicList.add(new LinkedHashMap(uuid: "F2FBD228-5109-4E34-AD4F-52F4F658DEC3", sisma_id: "ZSupYUGzdbJ", provinceCode: "04", province: "Zambezia", districtCode: "14", district: "Molumbo", sitename: "Namucumua CS", site_nid: "1042108"))
        clinicList.add(new LinkedHashMap(uuid: "99CD9114-C693-44A3-83FE-DE5156D591E8", sisma_id: "MRiHrjlbjPy", provinceCode: "04", province: "Zambezia", districtCode: "15", district: "Mopeia", sitename: "Chimuara CS", site_nid: "1041206"))
        clinicList.add(new LinkedHashMap(uuid: "FBBCFD75-164D-484D-9C1F-9810BC4093DF", sisma_id: "bV8283XigeD", provinceCode: "04", province: "Zambezia", districtCode: "15", district: "Mopeia", sitename: "Lua-Lua CS", site_nid: "1041208"))
        clinicList.add(new LinkedHashMap(uuid: "0F255BCE-B9E2-45CD-91D9-C4FC0AAA2FCE", sisma_id: "MOY29mOpX7i", provinceCode: "04", province: "Zambezia", districtCode: "15", district: "Mopeia", sitename: "Mopeia-Sede CS", site_nid: "1041209"))
        clinicList.add(new LinkedHashMap(uuid: "0D903DE0-DAA6-427E-9557-3D6E23F88896", sisma_id: "gF8verFv7Oe", provinceCode: "04", province: "Zambezia", districtCode: "16", district: "Morrumbala", sitename: "Cumbapo CS", site_nid: "1041308"))
        clinicList.add(new LinkedHashMap(uuid: "D0EF396D-38F5-40FF-B520-09DF8CDB8CCC", sisma_id: "GxHMluBeLY4", provinceCode: "04", province: "Zambezia", districtCode: "16", district: "Morrumbala", sitename: "Megaza CS", site_nid: "1041311"))
        clinicList.add(new LinkedHashMap(uuid: "56C634DA-28AF-4ACF-A9B8-8BE2D65EB495", sisma_id: "VcZD7lPIOXF", provinceCode: "04", province: "Zambezia", districtCode: "16", district: "Morrumbala", sitename: "Mepinha CS", site_nid: "1041312"))
        clinicList.add(new LinkedHashMap(uuid: "1245DD05-2B20-4628-B3AD-37E285655006", sisma_id: "PoZM9YEYKTx", provinceCode: "04", province: "Zambezia", districtCode: "16", district: "Morrumbala", sitename: "Morrumbala CS", site_nid: "1041336"))
        clinicList.add(new LinkedHashMap(uuid: "981CBB7C-F94A-4FF0-B814-601C90BF03F0", sisma_id: "fyTzJfizzhg", provinceCode: "04", province: "Zambezia", districtCode: "16", district: "Morrumbala", sitename: "Pinda CS", site_nid: "1041313"))
        clinicList.add(new LinkedHashMap(uuid: "B9FCB77F-4728-48C1-8C1C-72A662AFA71C", sisma_id: "V6RBgunLJA0", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Furquia PS", site_nid: "1041407"))
        clinicList.add(new LinkedHashMap(uuid: "8162773D-F03E-4C03-A42F-54D7B5E60EFF", sisma_id: "d6syDRFTsUw", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Macuse CS", site_nid: "1041408"))
        clinicList.add(new LinkedHashMap(uuid: "5730A5DA-41FB-40D1-BC69-3C71D9C6841D", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Centro de Saúde Militar de Macuse", site_nid: "1041420"))
        clinicList.add(new LinkedHashMap(uuid: "C3E30369-1D29-4F16-A3B4-6099A08153D8", sisma_id: "h1fKcjKBpnx", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Malei CS", site_nid: "1041409"))
        clinicList.add(new LinkedHashMap(uuid: "B8030A60-0266-40C6-9E78-63335F8D14CE", sisma_id: "xT0zdOHgm6H", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Mbawa CS", site_nid: "1041414"))
        clinicList.add(new LinkedHashMap(uuid: "0E0D250C-48C6-490E-B0EF-DF938E2A62C5", sisma_id: "tRCDpveXeIa", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Mexixine CS", site_nid: "1041410"))
        clinicList.add(new LinkedHashMap(uuid: "3B6AE3E2-C2FB-4357-9B9A-BE8AF1196467", sisma_id: "RaMdzv9q9IN", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Muceliua CS", site_nid: "1041417"))
        clinicList.add(new LinkedHashMap(uuid: "F4251DDF-88A8-4E6C-83E9-343CA405D388", sisma_id: "XIOTIzC7AXu", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Muebele CS", site_nid: "1041415"))
        clinicList.add(new LinkedHashMap(uuid: "788FF253-4190-4C45-8224-05A9E54DCA85", sisma_id: "BXaJyQ0pzPq", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Mugubia CS", site_nid: "1041418"))
        clinicList.add(new LinkedHashMap(uuid: "9683B660-B0AA-472B-978E-CF6013562520", sisma_id: "HDxcLgcoPQX", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Mutange PS", site_nid: "1041416"))
        clinicList.add(new LinkedHashMap(uuid: "DF3BC598-B033-4CB3-AEF3-2FCFD82B6346", sisma_id: "npq5bBdrdYf", provinceCode: "04", province: "Zambezia", districtCode: "18", district: "Namacurra", sitename: "Namacurra-Sede CS", site_nid: "1041406"))
        clinicList.add(new LinkedHashMap(uuid: "0AE2FD63-2DF3-4655-B878-8BA430498D8D", sisma_id: "lhFcxeeOArR", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Amoro CS", site_nid: "1041618"))
        clinicList.add(new LinkedHashMap(uuid: "23F04B2F-BBD5-4AD5-8E89-F6EEEA470077", sisma_id: "ug0XAOK5Etf", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Domela PS", site_nid: "1041620"))
        clinicList.add(new LinkedHashMap(uuid: "935890B1-583F-4D7C-82BF-58A7494BC296", sisma_id: "JlA9K4xoDfz", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Ilalane CS", site_nid: "1041606"))
        clinicList.add(new LinkedHashMap(uuid: "7D9A77DD-B279-4527-92CC-2C41D95359B1", sisma_id: "CvBiVKgJEs9", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Licuar CS", site_nid: "1041609"))
        clinicList.add(new LinkedHashMap(uuid: "2D61586C-9F40-4441-ADF5-6D3080621DC3", sisma_id: "lZb6EWul188", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Marrongane PS", site_nid: "1041612"))
        clinicList.add(new LinkedHashMap(uuid: "423E8F8C-2251-4B12-ACDF-E0FE958CBF6E", sisma_id: "NF9gWma41ZI", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Namacata CS", site_nid: "1041613"))
        clinicList.add(new LinkedHashMap(uuid: "91EC5E57-7888-4610-B004-5AFE392BA59D", sisma_id: "CrRQSY76xOD", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Nicoadala-Sede CS", site_nid: "1041614"))
        clinicList.add(new LinkedHashMap(uuid: "1BDE38FC-7BAF-4DC4-A237-E03A14BBC50A", sisma_id: "kHlzetd5vAW", provinceCode: "04", province: "Zambezia", districtCode: "20", district: "Nicoadala", sitename: "Quinta Girassol CS", site_nid: "1041617"))
        clinicList.add(new LinkedHashMap(uuid: "F2566618-8BB7-4C01-B3FE-E3F69A26B408", sisma_id: "a53u0Yd8O3F", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "7 de Abril CS", site_nid: "1041715"))
        clinicList.add(new LinkedHashMap(uuid: "B99C4FBB-1C6F-4925-9195-2D16FFF4D54F", sisma_id: "UTsfsU6WkO4", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Alto Maganha CS", site_nid: "1041737"))
        clinicList.add(new LinkedHashMap(uuid: "C263F09A-D162-455F-85D3-FA14872CA129", sisma_id: "XJeEBYiSSsU", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Impaca CS", site_nid: "1041716"))
        clinicList.add(new LinkedHashMap(uuid: "27CA10C6-4EB2-4061-8648-203CF3C4231C", sisma_id: "cJpbv04mJ4k", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Magiga CS", site_nid: "1041708"))
        clinicList.add(new LinkedHashMap(uuid: "4BCD1FD5-513D-4C15-97A1-76C3CC8D3D25", sisma_id: "aFsNwuQ9663", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Malema CS", site_nid: "1041709"))
        clinicList.add(new LinkedHashMap(uuid: "C167F5BB-F901-48DE-88E4-40F19B910671", sisma_id: "qmiomhs5J9E", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Mulela CS", site_nid: "1041711"))
        clinicList.add(new LinkedHashMap(uuid: "FDF02915-C193-49E9-945E-99CA336A83D5", sisma_id: "Fch2tUN1Gxo", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Muligode CS", site_nid: "1041712"))
        clinicList.add(new LinkedHashMap(uuid: "20AADC35-9A78-4CCB-8ABC-D044064E4353", sisma_id: "U1dDXDbV7en", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Naburi CS", site_nid: "1041713"))
        clinicList.add(new LinkedHashMap(uuid: "F1567308-3B03-464F-A612-DA1666FCFEE8", sisma_id: "u9HLeTpmdDb", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Pebane-Sede CS", site_nid: "1041706"))
        clinicList.add(new LinkedHashMap(uuid: "05A0FB07-B7E6-4A82-9AE5-31D70085057E", sisma_id: "c4oT26n6v1s", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Pele-Pele CS", site_nid: "1041757"))
        clinicList.add(new LinkedHashMap(uuid: "9006AC84-2C0D-45C6-923A-7CF9A1FC867E", sisma_id: "KTP9oq6N9Q9", provinceCode: "04", province: "Zambezia", districtCode: "21", district: "Pebane", sitename: "Tomeia PSA", site_nid: "1041717"))
        clinicList.add(new LinkedHashMap(uuid: "83336E1F-C9AA-48DD-B6F9-3D4350ED64B3", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Centro de Saúde Militar de Quelimane", site_nid: "104011301"))
        clinicList.add(new LinkedHashMap(uuid: "4DA984DC-AE5D-483A-9DC5-0C5F3D09953E", sisma_id: "gn7b9tGF4fB", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "17 de Setembro CS", site_nid: "1040106"))
        clinicList.add(new LinkedHashMap(uuid: "D96D3B49-71F7-44F8-98A1-F445B4E78927", sisma_id: "CeP2ONGsGWS", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "24 de Julho CS", site_nid: "1040107"))
        clinicList.add(new LinkedHashMap(uuid: "6A90F76D-DED3-41EF-A4AC-C586114C9291", sisma_id: "tsVToXeQi6c", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "4 de Dezembro CS", site_nid: "1040109"))
        clinicList.add(new LinkedHashMap(uuid: "D69DE13B-E984-49FC-9734-8D21EC851376", sisma_id: "quPE8YigS1Y", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Chabeco CS", site_nid: "1040114"))
        clinicList.add(new LinkedHashMap(uuid: "3FDC8215-B81C-4C1D-98FC-D0DF29F68E09", sisma_id: "xuXddFwLwM4", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Coalane CSURB", site_nid: "1040108"))
        clinicList.add(new LinkedHashMap(uuid: "027F0A4F-E990-4BF2-96B0-2BD7407D67BD", sisma_id: "IfOURzQKfjq", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Estação Malanha CS", site_nid: "1040119"))
        clinicList.add(new LinkedHashMap(uuid: "9D649873-7C67-4B28-A212-0B47216DFF3D", sisma_id: "vNPX0WKa05v", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Icidua CS", site_nid: "1040117"))
        clinicList.add(new LinkedHashMap(uuid: "0EB188B6-9472-4142-BDC8-1FCF3983C38C", sisma_id: "wqozbEGwiip", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Inhangulue CS", site_nid: "1040125"))
        clinicList.add(new LinkedHashMap(uuid: "A59BBD7A-4F7B-400D-A777-85B628F4B9B4", sisma_id: "xW0d1FlqTzR", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Ionge CS", site_nid: "1040120"))
        clinicList.add(new LinkedHashMap(uuid: "A09D534E-8247-4FD5-85A0-3F90590EE50F", sisma_id: "MssEnEO8wfN", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Madal CS", site_nid: "1040121"))
        clinicList.add(new LinkedHashMap(uuid: "56CF8F23-A4DB-425F-A152-2488685C94CA", sisma_id: "qgvEDUUuguh", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Maquival Rio CS", site_nid: "1040123"))
        clinicList.add(new LinkedHashMap(uuid: "D2D084AA-E4E8-4E89-A21B-56FA07FB13F4", sisma_id: "bYB2JfLO8Rp", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Maquival Sede CS", site_nid: "1040122"))
        clinicList.add(new LinkedHashMap(uuid: "5B52238A-29A2-409C-ADDA-982C7DD59430", sisma_id: "lPZ3nYBwnXN", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Micajune CS", site_nid: "1040116"))
        clinicList.add(new LinkedHashMap(uuid: "B3FF0D2A-4F99-4C61-A0BD-641ABF627B5F", sisma_id: "Oq6oTAkCwfb", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Namuinho CS", site_nid: "1040111"))
        clinicList.add(new LinkedHashMap(uuid: "18719D35-89A7-40DD-9D5E-BD62B45CE1DA", sisma_id: "QfHGC9q2SDX", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Quelimane HG", site_nid: "1040100"))
        clinicList.add(new LinkedHashMap(uuid: "FBA0FABF-3E35-4AAC-BE53-367D3922FB7D", sisma_id: "ZA5JlOtJ60w", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Quelimane_CCP", site_nid: "1040110"))
        clinicList.add(new LinkedHashMap(uuid: "121CF50D-72F5-4FF9-AB96-EAC07B44D05C", sisma_id: "hxqJ30hz6JQ", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Sangarivera CS", site_nid: "1040112"))
        clinicList.add(new LinkedHashMap(uuid: "1BF27360-38D3-4ECA-B02E-2BDF820B7498", sisma_id: "HamTWd99DjM", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Varela CS", site_nid: "1040124"))
        clinicList.add(new LinkedHashMap(uuid: "402DE1DF-D1DC-4D59-9C56-723F14FB556D", sisma_id: "uTaIppByr7U", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Zalala Mar PS", site_nid: "1040118"))
        clinicList.add(new LinkedHashMap(uuid: "87DA16D7-4FD8-4103-B251-8343A7000C14", sisma_id: "J2qZCU8X0BI", provinceCode: "03", province: "Nampula", districtCode: "01", district: "Angoche", sitename: "Angoche HR", site_nid: "1030201"))
        clinicList.add(new LinkedHashMap(uuid: "D3ADB75F-A3EF-4479-82E8-32F283B27EBC", sisma_id: "sD5mtPJhdS4", provinceCode: "03", province: "Nampula", districtCode: "01", district: "Angoche", sitename: "Namitoria CS", site_nid: "1030208"))
        clinicList.add(new LinkedHashMap(uuid: "AB2B5F6C-25FE-4AF4-A4F8-38DE36EF345E", sisma_id: "L1HkhrbrBW6", provinceCode: "03", province: "Nampula", districtCode: "02", district: "Erati", sitename: "Alua CS", site_nid: "1030306"))
        clinicList.add(new LinkedHashMap(uuid: "4779A54C-C9D3-4187-B890-6F6C24EA3B86", sisma_id: "ahLtZ1K3YIK", provinceCode: "03", province: "Nampula", districtCode: "02", district: "Erati", sitename: "Namapa HR", site_nid: "1030301"))
        clinicList.add(new LinkedHashMap(uuid: "2F2ECE0B-DFCD-4596-ADC2-2C628576D23F", sisma_id: "r2knPwVdIR7", provinceCode: "03", province: "Nampula", districtCode: "02", district: "Erati", sitename: "Odinepa CS", site_nid: "1030311"))
        clinicList.add(new LinkedHashMap(uuid: "939CEFD4-3AC2-4A68-967C-B318C66BD73D", sisma_id: "krTciqRg2QP", provinceCode: "03", province: "Nampula", districtCode: "03", district: "Ilha De Moçambique", sitename: "Ilha de Moçambique CS", site_nid: "1030406"))
        clinicList.add(new LinkedHashMap(uuid: "21ED02F7-A411-4B1B-AE0E-192B6CB67C45", sisma_id: "bINdrzyQeRE", provinceCode: "03", province: "Nampula", districtCode: "03", district: "Ilha De Moçambique", sitename: "Lumbo CS", site_nid: "1030407"))
        clinicList.add(new LinkedHashMap(uuid: "25B0B43A-FB8E-49A9-A521-2CA6DF63577F", sisma_id: "kb4hldnuilQ", provinceCode: "03", province: "Nampula", districtCode: "04", district: "Lalaua", sitename: "Lalaua CS", site_nid: "1030506"))
        clinicList.add(new LinkedHashMap(uuid: "05AF5A8A-2AA2-4046-9BEC-B11D128F9C48", sisma_id: "a1ymro2d2Mg", provinceCode: "03", province: "Nampula", districtCode: "07", district: "Malema", sitename: "Malema CS", site_nid: "1030606"))
        clinicList.add(new LinkedHashMap(uuid: "626DB880-7E75-4486-AA38-09DA14418E8E", sisma_id: "K9xQ8qRrjU3", provinceCode: "03", province: "Nampula", districtCode: "07", district: "Malema", sitename: "Mutuali CS", site_nid: "1030607"))
        clinicList.add(new LinkedHashMap(uuid: "333CBFCE-D471-4191-A4DD-B81FE88A10A4", sisma_id: "Pkt7RBoRcs2", provinceCode: "03", province: "Nampula", districtCode: "08", district: "Meconta", sitename: "Meconta CS", site_nid: "1030707"))
        clinicList.add(new LinkedHashMap(uuid: "55D3FE2E-DB4E-43AF-8991-C4D37C9CF211", sisma_id: "z5Q3BcsZoj1", provinceCode: "03", province: "Nampula", districtCode: "08", district: "Meconta", sitename: "Namialo CS", site_nid: "1030706"))
        clinicList.add(new LinkedHashMap(uuid: "BF4C9871-931B-46B6-8CCC-D1C074B54625", sisma_id: "u76Cu4oN14L", provinceCode: "03", province: "Nampula", districtCode: "09", district: "Mecuburi", sitename: "Mecuburi CS", site_nid: "1030806"))
        clinicList.add(new LinkedHashMap(uuid: "1BF5B115-4638-4003-9921-E92BEA412F80", sisma_id: "X5kyyss5GdA", provinceCode: "03", province: "Nampula", districtCode: "09", district: "Mecuburi", sitename: "Namina CS", site_nid: "1030808"))
        clinicList.add(new LinkedHashMap(uuid: "6E062C62-B5F2-4D84-B36B-B20615E8CB26", sisma_id: "SRpK06xbWgG", provinceCode: "03", province: "Nampula", districtCode: "12", district: "Mogovolas", sitename: "Iuluti CS", site_nid: "1031108"))
        clinicList.add(new LinkedHashMap(uuid: "2B53B731-11C9-4F90-B2D2-646F951B4CB9", sisma_id: "Z5pIRYaLVXz", provinceCode: "03", province: "Nampula", districtCode: "12", district: "Mogovolas", sitename: "Nametil CS", site_nid: "1031106"))
        clinicList.add(new LinkedHashMap(uuid: "8139BEC1-14E9-459F-A72D-48AEC8B9C957", sisma_id: "RyHDFZVUdXW", provinceCode: "03", province: "Nampula", districtCode: "12", district: "Mogovolas", sitename: "Nanhupo Rio CS", site_nid: "1031110"))
        clinicList.add(new LinkedHashMap(uuid: "C9F9994D-167A-4DE5-9E13-98EC96E3B84E", sisma_id: "YtbB4I85bc1", provinceCode: "03", province: "Nampula", districtCode: "13", district: "Moma", sitename: "Chalaua CS", site_nid: "1031207"))
        clinicList.add(new LinkedHashMap(uuid: "F023389A-F7B6-4E73-9C38-CFA2B75A46C4", sisma_id: "xyH9n8rRrG1", provinceCode: "03", province: "Nampula", districtCode: "13", district: "Moma", sitename: "Micane CS", site_nid: "1031218"))
        clinicList.add(new LinkedHashMap(uuid: "477145D5-FB57-4DDF-855B-B6936E85F46B", sisma_id: "DVoQpHahY89", provinceCode: "03", province: "Nampula", districtCode: "13", district: "Moma", sitename: "Moma HD", site_nid: "1031206"))
        clinicList.add(new LinkedHashMap(uuid: "46B5430E-7E23-45A4-8E78-899EE222051C", sisma_id: "as5VX4mDxBA", provinceCode: "03", province: "Nampula", districtCode: "13", district: "Moma", sitename: "Pilivili CS", site_nid: "1031215"))
        clinicList.add(new LinkedHashMap(uuid: "ADAB8C48-463D-4EC8-9BAF-05BDD87E2365", sisma_id: "B4ke2NbcqxU", provinceCode: "03", province: "Nampula", districtCode: "14", district: "Monapo", sitename: "Carapira CS", site_nid: "1031307"))
        clinicList.add(new LinkedHashMap(uuid: "44235896-1077-4C24-B66F-FEDC22E2F75D", sisma_id: "py1cEZtl6wI", provinceCode: "03", province: "Nampula", districtCode: "14", district: "Monapo", sitename: "Monapo CS", site_nid: "1031306"))
        clinicList.add(new LinkedHashMap(uuid: "1223C339-A878-42DB-AADA-6F5236E1A85E", sisma_id: "cRkt686vcyE", provinceCode: "03", province: "Nampula", districtCode: "14", district: "Monapo", sitename: "Monapo Rio CS", site_nid: "1031318"))
        clinicList.add(new LinkedHashMap(uuid: "4CF71E49-8265-450F-8C9D-F3D68899FB62", sisma_id: "NIxI3Pvsuxm", provinceCode: "03", province: "Nampula", districtCode: "14", district: "Monapo", sitename: "Natete CS", site_nid: "1031308"))
        clinicList.add(new LinkedHashMap(uuid: "DB9C9ACB-199B-42A6-A246-B0BB5665AA43", sisma_id: "CNbi7z4VCLv", provinceCode: "03", province: "Nampula", districtCode: "15", district: "Mossuril", sitename: "Matibane CS", site_nid: "1031411"))
        clinicList.add(new LinkedHashMap(uuid: "D99DDDC2-EC49-450C-A910-97BF48E3CF4D", sisma_id: "J3hyobXtsfP", provinceCode: "03", province: "Nampula", districtCode: "15", district: "Mossuril", sitename: "Mossuril CS", site_nid: "1031406"))
        clinicList.add(new LinkedHashMap(uuid: "EB78DD1C-F6E1-49F4-A76F-1B9F1B24B521", sisma_id: "nNJv4MR9VLh", provinceCode: "03", province: "Nampula", districtCode: "16", district: "Muecate", sitename: "Muecate CS", site_nid: "1031506"))
        clinicList.add(new LinkedHashMap(uuid: "D5D57BF2-3398-4C19-BFF6-802F3F24D9F8", sisma_id: "FKQlmp4fRv5", provinceCode: "03", province: "Nampula", districtCode: "17", district: "Murrupula", sitename: "Murrupula CS", site_nid: "1031606"))
        clinicList.add(new LinkedHashMap(uuid: "7AC5CFAF-369D-47D6-89EC-378592874C70", sisma_id: "HDdLtuYDPFN", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "A.D.P.P.Muzuane PS", site_nid: "1031710"))
        clinicList.add(new LinkedHashMap(uuid: "0895325E-C1F7-40EC-B953-20F3956C772D", sisma_id: "SUyP2dof8cq", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Akumi CS", site_nid: "1031715"))
        clinicList.add(new LinkedHashMap(uuid: "22EF457B-D3BA-467C-B4E7-DFAB9FBCD292", sisma_id: "MkvHWLUsNFY", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Murrupelane CS", site_nid: "1031708"))
        clinicList.add(new LinkedHashMap(uuid: "20665F47-0CBA-477C-B577-030BABA8FFB7", sisma_id: "kQVZ3wqQ6Kl", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Nacala Porto CS", site_nid: "1031700"))
        clinicList.add(new LinkedHashMap(uuid: "9C0E08D5-90BD-4D43-B038-DD57DFA9F7AA", sisma_id: "gtbgirQL12l", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Nacala Porto HD", site_nid: "1031701"))
        clinicList.add(new LinkedHashMap(uuid: "DEF53529-392A-42FD-94DF-A23D1E6C952B", sisma_id: "JFyHEgTwn4u", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Ontupaia CS", site_nid: "1031717"))
        clinicList.add(new LinkedHashMap(uuid: "005EB816-4010-4D75-B66A-E76CF8A91F42", sisma_id: "bfxmBecZsio", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Quissimajulo CS", site_nid: "1031707"))
        clinicList.add(new LinkedHashMap(uuid: "B1C5D188-24B9-43AC-895D-7E6FCF2043F8", sisma_id: "N/A", provinceCode: "03", province: "Nampula", districtCode: "19", district: "Nacala", sitename: "Centro de Saúde Militar do CEEFE de Nacala", site_nid: "1030201"))
        clinicList.add(new LinkedHashMap(uuid: "3190B860-3DB9-4CD1-A738-F1C09C62D3BD", sisma_id: "Opn3jD1gk3F", provinceCode: "03", province: "Nampula", districtCode: "18", district: "Nacala-A-Velha", sitename: "Barragem CS", site_nid: "1031808"))
        clinicList.add(new LinkedHashMap(uuid: "45846E34-F009-4C76-968D-8383A5824A9E", sisma_id: "NUkoHRjVxjj", provinceCode: "03", province: "Nampula", districtCode: "18", district: "Nacala-A-Velha", sitename: "Nacala-A-Velha CS", site_nid: "1031806"))
        clinicList.add(new LinkedHashMap(uuid: "FC75E62C-475A-4FFE-B696-1E189574C4B0", sisma_id: "XKPRBMxMPIt", provinceCode: "03", province: "Nampula", districtCode: "18", district: "Nacala-A-Velha", sitename: "Namalala CS", site_nid: "1031811"))
        clinicList.add(new LinkedHashMap(uuid: "6B3D583E-0207-451B-9E30-7686CBD52DA0", sisma_id: "YzncMK5euTo", provinceCode: "03", province: "Nampula", districtCode: "20", district: "Nacaroa", sitename: "Nacaroa CS", site_nid: "1031906"))
        clinicList.add(new LinkedHashMap(uuid: "880ECC52-DE30-4D01-A163-ABB64A4AFDF5", sisma_id: "N/A", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Hospital Militar de Nampula", site_nid: "1030101"))
        clinicList.add(new LinkedHashMap(uuid: "E21CF89D-63CC-44A6-B53D-6F24DB423CB2", sisma_id: "TBqm5OayabT", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "1 de Maio CS", site_nid: "1030108"))
        clinicList.add(new LinkedHashMap(uuid: "774A4F2D-A693-42D3-8030-77BB72EB298A", sisma_id: "pt3uFvNsHDF", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "25 de Setembro CS", site_nid: "1030106"))
        clinicList.add(new LinkedHashMap(uuid: "7CCB1B6E-98FC-4521-BDF7-DC030ACCFA11", sisma_id: "JpEgAbs8aWj", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Anchilo CS", site_nid: "1032007"))
        clinicList.add(new LinkedHashMap(uuid: "F42F5778-BB2B-476D-AE83-8A09D7C3355D", sisma_id: "Sw97r5J0WaV", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Anexo (Psiquiatrico) PS", site_nid: "1030115"))
        clinicList.add(new LinkedHashMap(uuid: "77847B29-09DB-4181-B80C-9B67A32F6BEB", sisma_id: "ozv8eDXMqmi", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Maratane CS", site_nid: "1032019"))
        clinicList.add(new LinkedHashMap(uuid: "24A4EEDD-9806-4CE9-BE4F-400950D3260F", sisma_id: "eU3cClvwfUq", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Marrere HG", site_nid: "1030107"))
        clinicList.add(new LinkedHashMap(uuid: "7FC1313C-A673-486B-8947-E6102297A2CF", sisma_id: "JZdznZYemtl", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Muhala Expansão CS", site_nid: "1030117"))
        clinicList.add(new LinkedHashMap(uuid: "E2370EA7-1C35-4BDE-AFFB-184B83CD349C", sisma_id: "PkZ4vKaw1DE", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Mutavarex CS", site_nid: "1030120"))
        clinicList.add(new LinkedHashMap(uuid: "4586CE7D-CFBE-4A56-B30E-5B78F6495EFF", sisma_id: "THmsb1VeNQM", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Namicopo CS", site_nid: "1030113"))
        clinicList.add(new LinkedHashMap(uuid: "7B0CB4D2-890E-42F5-9A46-FEB731E4E731", sisma_id: "Ii5aUoXNu3l", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Namiepe CS", site_nid: "1030110"))
        clinicList.add(new LinkedHashMap(uuid: "F1462D18-AE75-47AD-A2E0-296AD3321DEF", sisma_id: "cRkfIZfvar6", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Namutequeliua CS", site_nid: "1030112"))
        clinicList.add(new LinkedHashMap(uuid: "80DB75B9-564F-4718-854F-72826FBEAAFC", sisma_id: "nUbtOKZ73yL", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Napipine CS", site_nid: "1030109"))
        clinicList.add(new LinkedHashMap(uuid: "7518E064-586B-42B6-8E32-A71E19C6C73A", sisma_id: "MEEriHK2OlN", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Niarru CS", site_nid: "1030121"))
        clinicList.add(new LinkedHashMap(uuid: "838BA7DE-B464-43CE-9F54-E9E034FB6A71", sisma_id: "NZDdLAn0PH7", provinceCode: "03", province: "Nampula", districtCode: "22", district: "Rapale", sitename: "Namaita CS", site_nid: "1032008"))
        clinicList.add(new LinkedHashMap(uuid: "4790A926-A6C2-4943-BFDA-A796BA8398E3", sisma_id: "ZqeiHyiaQa7", provinceCode: "03", province: "Nampula", districtCode: "22", district: "Rapale", sitename: "Namucaua CS", site_nid: "1032017"))
        clinicList.add(new LinkedHashMap(uuid: "89E53120-C9B9-40BA-B92E-D367D58AFEB0", sisma_id: "KXuDpfkBJ09", provinceCode: "03", province: "Nampula", districtCode: "22", district: "Rapale", sitename: "Rapale CS", site_nid: "1032006"))
        clinicList.add(new LinkedHashMap(uuid: "FF278E15-DC0F-4373-BFAF-DA55F1F371F2", sisma_id: "PZJadYc1os9", provinceCode: "03", province: "Nampula", districtCode: "23", district: "Ribaué", sitename: "Iapala Monapo CS", site_nid: "1032106"))
        clinicList.add(new LinkedHashMap(uuid: "BBE12474-AE13-4EEC-A5B8-28D15EC0ED8F", sisma_id: "RqoMfI0nyMg", provinceCode: "03", province: "Nampula", districtCode: "23", district: "Ribaué", sitename: "Namiconha CS", site_nid: "1032110"))
        clinicList.add(new LinkedHashMap(uuid: "EC209488-BBC4-41F3-ADFC-6E95BFC149B9", sisma_id: "zVIAjMTsYT4", provinceCode: "03", province: "Nampula", districtCode: "23", district: "Ribaué", sitename: "Ribaue HR", site_nid: "1032101"))
        clinicList.add(new LinkedHashMap(uuid: "3F8FDDA6-CD5E-42F1-9D0C-84FBE01A54A7", sisma_id: "Djahp380CHM", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "CS Matola Santos", site_nid: "1100128"))
        clinicList.add(new LinkedHashMap(uuid: "9E1B5B9A-192B-46EC-93E8-B463A3B6E91B", sisma_id: "sbH1sYBe2Ww", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Cadeia Regional de Nampula", site_nid: "1030119"))
        clinicList.add(new LinkedHashMap(uuid: "DA6F8CB7-C8CD-4AA0-81B8-CF5E0BF1F1FB", sisma_id: "fFs8TOTuE5x", provinceCode: "09", province: "Gaza", districtCode: "14", district: "Xai-Xai", sitename: "Xai-Xai HP", site_nid: "1090100"))
        clinicList.add(new LinkedHashMap(uuid: "938A37EE-DE39-45C5-80FA-2BF68E172155", sisma_id: "MgUySvuhBF9", provinceCode: "10", province: "Maputo", districtCode: "05", district: "Matola", sitename: "Matola HP", site_nid: "1100101"))
        clinicList.add(new LinkedHashMap(uuid: "8AF12667-3F2E-4AE3-B95C-00F55394A804", sisma_id: "d4UFMg4l7mU", provinceCode: "08", province: "Inhambane", districtCode: "04", district: "Inhambane", sitename: "Inhambane HP", site_nid: "1080100"))
        clinicList.add(new LinkedHashMap(uuid: "9845F3DB-8B3F-4437-BF0A-DC06923C724C", sisma_id: "Wf5tpT4pUoB", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Hospital Central de Nampula HC", site_nid: "1030100"))
        clinicList.add(new LinkedHashMap(uuid: "237C5AEA-DEE2-4CEA-A751-1218EC782663", sisma_id: "N/A", provinceCode: "03", province: "Nampula", districtCode: "21", district: "Nampula", sitename: "Cadeia Provincial de Nampula", site_nid: ""))
        clinicList.add(new LinkedHashMap(uuid: "89BC74B8-7E7A-427D-86F9-47F30B336E17", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "11", district: "Milange", sitename: "Cadeia Civil Milange PS", site_nid: "1041005"))
        clinicList.add(new LinkedHashMap(uuid: "47A09A33-F9CF-4B7C-8206-327034DBC99B", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "12", district: "Mocuba", sitename: "Cadeia Civil Mocuba PS", site_nid: "1041104"))
        clinicList.add(new LinkedHashMap(uuid: "2AD1DA7D-9F4B-45EB-A0BB-2360B1860DC8", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "05", district: "Gurue", sitename: "Cadeia Civil Gurue PS", site_nid: "1040505"))

        return clinicList
    }

    List<Object> listClinic4() {
        List<Object> clinicList = new ArrayList<>()

        clinicList.add(new LinkedHashMap(uuid: "51bc78ed-c5a2-4211-ba17-76420da796ba", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Lisboa", site_nid: "04001"))
        clinicList.add(new LinkedHashMap(uuid: "0a2690ff-f3d9-425e-b303-0d31a4a4cee4", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Artur 3", site_nid: "04003"))
        clinicList.add(new LinkedHashMap(uuid: "d194fbf2-ba94-4506-b757-9a19b77e4f77", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Abdala", site_nid: "04004"))
        clinicList.add(new LinkedHashMap(uuid: "0e9999b6-6edc-472d-9eb4-22a80cd8aec2", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Contente", site_nid: "04006"))
        clinicList.add(new LinkedHashMap(uuid: "36ee873f-9af2-4a81-b048-6f52ad48518c", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Chabeco 786", site_nid: "04007"))
        clinicList.add(new LinkedHashMap(uuid: "f1454a21-8e4e-460e-886b-55729f307410", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Muzinga", site_nid: "04005"))
        clinicList.add(new LinkedHashMap(uuid: "oc9nWB75I1wFseXID18XC", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "01", district: "Alto Molócue", sitename: "Farmácia Mundial", site_nid: "0001"))
        clinicList.add(new LinkedHashMap(uuid: "a14f4e9f-89cb-4492-8fc2-9959e68e99b6", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmacia Quelimane", site_nid: "04002"))
        clinicList.add(new LinkedHashMap(uuid: "f5d006db-1e88-4469-a0a7-dc444fa9764a", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmac Universal", site_nid: "04008"))
        clinicList.add(new LinkedHashMap(uuid: "4a93cbf1-da0d-4657-bc41-93b30cd93b8e", sisma_id: "N/A", provinceCode: "04", province: "Zambezia", districtCode: "22", district: "Quelimane", sitename: "Farmac Moçambique", site_nid: "04009"))

        return clinicList
    }

    List<Object> listMenus() {
        List<Object> menus = new ArrayList<>()
        menus.add(new Menu(code: '01', description: 'Pacientes'))
        menus.add(new Menu(code: '02', description: 'Grupos'))
        menus.add(new Menu(code: '03', description: 'Stock'))
        menus.add(new Menu(code: '04', description: 'Dashboard'))
        menus.add(new Menu(code: '05', description: 'Relatorios'))
        menus.add(new Menu(code: '06', description: 'Administração'))
        menus.add(new Menu(code: '07', description: 'Migração'))
        menus.add(new Menu(code: '08', description: 'Tela Inicial'))
        menus.add(new Menu(code: '09', description: 'DCProvedor'))

        return menus
    }

    void resolvePackWithoutPackagedDrugs() {
        def lisPack = Pack.findAllByPackagedDrugsIsEmpty()
        for (Pack pack : lisPack) {

            PatientVisitDetails patientVisitDetails = PatientVisitDetails.findByPack(pack)
            Prescription prescription = Prescription.findById(patientVisitDetails.prescription.id)
            PrescriptionDetail prescriptionDetail = PrescriptionDetail.findByPrescription(prescription)

            TherapeuticRegimen therapeuticRegimen = TherapeuticRegimen.findById(prescriptionDetail.therapeuticRegimen.id)
            DispenseType dispenseType = DispenseType.findById(prescriptionDetail.dispenseType.id)
            def selectedDrug = null

            def listAssociatedDrugs = therapeuticRegimen.drugs
            PackagedDrug packagedDrug = new PackagedDrug()

            if (listAssociatedDrugs.size() == 1) {
                selectedDrug = listAssociatedDrugs.first()
            }

            for (Drug drug : listAssociatedDrugs) {

                if (dispenseType.code.equalsIgnoreCase("DM")) {
                    packagedDrug.quantitySupplied = 1
                    if (drug.packSize <= 60) {
                        selectedDrug = drug
                    }
                } else if (dispenseType.code.equalsIgnoreCase("DT")) {
                    packagedDrug.quantitySupplied = 3
                    if (drug.packSize == 90) {
                        selectedDrug = drug
                    }
                } else {
                    packagedDrug.quantitySupplied = 0
                    selectedDrug = listAssociatedDrugs.first()
                }
            }

            if (selectedDrug == null && listAssociatedDrugs.size() > 0)
                selectedDrug = listAssociatedDrugs.first()

            if (selectedDrug == null)
                selectedDrug = Drug.findById("4d2c441c-4393-489e-bd4c-bab5c7456a78")

            packagedDrug.id = UUID.randomUUID().toString()
            packagedDrug.drug = selectedDrug
            packagedDrug.amtPerTime = selectedDrug.defaultTreatment
            packagedDrug.timesPerDay = selectedDrug.defaultTimes
            packagedDrug.form = selectedDrug.defaultPeriodTreatment
            packagedDrug.nextPickUpDate = pack.nextPickUpDate
            packagedDrug.toContinue = true
            packagedDrug.creationDate = new Date()
            packagedDrug.pack = pack
            pack.addToPackagedDrugs(packagedDrug)
            pack.save(flush: true, failOnError: true)
        }
    }

    void resolvePrescriptionsWithoutPrescribedDrugs() {
        def lisPrescription = Prescription.findAllByPrescribedDrugsIsEmptyAndCreationDateIsNotNull()
        for (Prescription prescription : lisPrescription) {

            PrescriptionDetail prescriptionDetail = PrescriptionDetail.findByPrescription(prescription)

            TherapeuticRegimen therapeuticRegimen = TherapeuticRegimen.findById(prescriptionDetail.therapeuticRegimen.id)
            DispenseType dispenseType = DispenseType.findById(prescriptionDetail.dispenseType.id)
            def selectedDrug = null

            def listAssociatedDrugs = therapeuticRegimen.drugs
            PrescribedDrug prescribedDrug = new PrescribedDrug()

            if (listAssociatedDrugs.size() == 1) {
                selectedDrug = listAssociatedDrugs.first()
            }

            for (Drug drug : listAssociatedDrugs) {

                if (dispenseType.code.equalsIgnoreCase("DM")) {
                    prescribedDrug.prescribedQty = 1
                    if (drug.packSize <= 60) {
                        selectedDrug = drug
                    }
                } else if (dispenseType.code.equalsIgnoreCase("DT")) {
                    prescribedDrug.prescribedQty = 3
                    if (drug.packSize == 90) {
                        selectedDrug = drug
                    }
                } else {
                    prescribedDrug.prescribedQty = 0
                    selectedDrug = listAssociatedDrugs.first()
                }
            }

            if (selectedDrug == null && listAssociatedDrugs.size() > 0)
                selectedDrug = listAssociatedDrugs.first()

            if (selectedDrug == null)
                selectedDrug = Drug.findById("4d2c441c-4393-489e-bd4c-bab5c7456a78")

            prescribedDrug.id = UUID.randomUUID().toString()
            prescribedDrug.drug = selectedDrug
            prescribedDrug.amtPerTime = selectedDrug.defaultTreatment
            prescribedDrug.timesPerDay = selectedDrug.defaultTimes
            prescribedDrug.form = selectedDrug.defaultPeriodTreatment
            prescribedDrug.modified = false
            prescribedDrug.prescription = prescription
            prescription.addToPrescribedDrugs(prescribedDrug)
            prescription.save(flush: true, failOnError: true)
        }
    }


//    void resolvePatientsWithNoServiceIdentifierButInVisit() {
//        def listPatientsWithoutPatientServiceIdentifier = Patient.findAllByIdentifiersIsEmptyAnd()
//    }

    void resolvePatientsAddMatchId() {
        def listPatientsNoMatchId = Patient.findAllByMatchIdIsNull()
        def matchId = 1
        for (Patient patient : listPatientsNoMatchId) {
            patient.setMatchId(matchId)
            patient.save(flush: true, failOnError: true)
            matchId++
        }
    }

    void unitePatient() {
        def patientServiceValid = PatientServiceIdentifier.findByValue('0104010001/2013/00167')
        def patientServiceToDelete = PatientServiceIdentifier.findByValue('0104011401/2013/00167')
        def matchId = 1
        def matchIdd = 1
        def patientVisits = PatientVisit.findAllByPatient(patientServiceToDelete.patient)
        for (PatientVisit patientVisit : patientVisits) {
            patientVisit.setPatient(patientServiceValid.patient)
            PatientVisit.withTransaction {
                patientVisit.save()
            }
        }
        def episodes = Episode.findAllByPatientServiceIdentifier(patientServiceToDelete)
        for (Episode episode : episodes) {
            episode.setPatientServiceIdentifier(patientServiceValid)
            Episode.withTransaction {
                episode.save()
            }
        }
        Patient.withTransaction {
            def patientToDelete = patientServiceToDelete.patient
            patientServiceToDelete.episodes = []
            patientServiceToDelete.delete()
            patientToDelete.delete()

        }
    }

    void getEpisodesWithSameStartStopReason() {
        def startStopReason = StartStopReason.findByCode('NOVO_PACIENTE')
        def matchId = 1
        /*
        String queryString =
                "SELECT psi.id as patient_service_identifier_id, start_stop_reason_id, episode_date, COUNT(*) as episode_count " +
                        "FROM episode e " +
                        "JOIN patient_service_identifier psi ON e.patient_service_identifier_id = psi.id " +
                        "WHERE start_stop_reason_id = 'ff8081817c9791ee017c99bbb2aa0002' " +
                        "GROUP BY psi.id, start_stop_reason_id, episode_date " +
                        "HAVING COUNT(*) >= 2";
        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        List<Object[]> result = query.list()
    */
        def episodes = Episode.findAllByStartStopReason(startStopReason)

        def groupedEpisodes = episodes.groupBy {
            [it.patientServiceIdentifier, it.startStopReason, it.episodeDate]
        }.findAll {
            it.value.size() >= 2
        }.collect {
            it.key[0]
        }
        groupedEpisodes.each { item ->
            def episodesByService = Episode.findAllByPatientServiceIdentifierAndStartStopReason(item, startStopReason)
            if (episodesByService.size() == 2) {
                def patientVisitDetailsEp0 = PatientVisitDetails.findAllByEpisode(episodesByService[0])
                def patientVisitDetailsEp1 = PatientVisitDetails.findAllByEpisode(episodesByService[1])
                if (patientVisitDetailsEp0.size() >= patientVisitDetailsEp1.size()) {
                    patientVisitDetailsEp1.each { pvd ->
                        pvd.episode = episodesByService[0]
                        PatientVisitDetails.withTransaction {
                            pvd.save()
                        }
                        episodesByService[1].episodeDate = ConvertDateUtils.subtractDaysDate(episodesByService[1].episodeDate, 1)
                        Episode.withTransaction {
                            episodesByService[1].save()
                        }
                    }
                } else {
                    patientVisitDetailsEp0.each { pvd ->
                        pvd.episode = episodesByService[1]
                        PatientVisitDetails.withTransaction {
                            pvd.save()
                        }
                        episodesByService[0].episodeDate = ConvertDateUtils.subtractDaysDate(episodesByService[0].episodeDate, 1)
                        Episode.withTransaction {
                            episodesByService[0].save()
                        }
                    }
                }
            }
        }

    }

   void updateClinicName() {

       Clinic clinicToUpdate = Clinic.findByClinicName('Lapala Monapo CS');
       if (clinicToUpdate) {
           clinicToUpdate.setClinicName('Iapala Monapo CS')
           clinicToUpdate.save(flush: true, failOnError: true)
       }
    }

    void updateClinicDistrict() {
        List<String> clinicNames = Arrays.asList("Iapala Monapo CS", "Namiconha CS", "Ribaue HR");
           District district =   District.findByDescription('Rapale')

        List<Clinic> clinicsToUpdate = Clinic.findAllByClinicNameInListAndDistrict(clinicNames,district)
        clinicsToUpdate.each{clinicToUpdate ->
            clinicToUpdate.setDistrict(District.findByDescription('Ribaué'))
            clinicToUpdate.save(flush: true, failOnError: true)
        }

    }


    List<Object> listUiSection() {
        List<Object> uiSections = new ArrayList<>()
        uiSections.add(new LinkedHashMap(id:'3fb1aa37-f527-4040-b2d1-acd55f61e440',requestMapUrl: '/ui/prescription/add', name: 'add-prescription', menu: 'Pacientes', action: 'add', displayName: 'Adicionar',category:'Prescription'))
        uiSections.add(new LinkedHashMap(id:'88f4022b-4257-4f6a-93f2-55577bd74757',requestMapUrl: '/ui/prescription/delete', name: 'remove-prescription', menu: 'Pacientes', action: 'remove', displayName: 'Remover',category:'Prescription'))
        uiSections.add(new LinkedHashMap(id:'3280068d-3867-4be0-87ee-1e6886247c81',requestMapUrl: '/ui/episode/add', name: 'add-episode', menu: 'Pacientes', action: 'add', displayName: 'Adicionar',category:'Episode'))
        uiSections.add(new LinkedHashMap(id:'1de4360f-f84a-4745-8a9b-16b94450e330',requestMapUrl: '/ui/episode/edit', name: 'edit-episode', menu: 'Pacientes', action: 'add', displayName: 'Editar',category:'Episode'))
        uiSections.add(new LinkedHashMap(id:'5353a3cb-5b25-478f-ae77-fb2199c9edb2',requestMapUrl: '/ui/episode/close', name: 'close-episode', menu: 'Pacientes', action: 'close', displayName: 'Fechar',category:'Episode'))
        uiSections.add(new LinkedHashMap(id:'3103c833-08bc-43a5-b00b-f9c094e4d9bc',requestMapUrl: '/ui/episode/remove', name: 'remove-episode', menu: 'Pacientes', action: 'remove', displayName: 'Remover',category:'Episode'))
        uiSections.add(new LinkedHashMap(id:'bac0a7a2-01ad-4d10-9029-38eac4324758',requestMapUrl: '/ui/patientServiceIdentifier/add', name: 'add-patient-service-identifier', menu: 'Pacientes', action: 'add', displayName: 'Adicionar',category:'PatientServiceIdentifier'))
        uiSections.add(new LinkedHashMap(id:'35c1c977-9ec8-4d38-b218-1edd7fcde439',requestMapUrl: '/ui/patientServiceIdentifier/edit', name: 'edit-patient-service-identifier', menu: 'Pacientes', action: 'edit', displayName: 'Editar',category:'PatientServiceIdentifier'))
        uiSections.add(new LinkedHashMap(id:'9b57f474-f11f-4eb5-91a9-aabb9e760556',requestMapUrl: '/ui/patientServiceIdentifier/close', name: 'close-patient-service-identifier', menu: 'Pacientes', action: 'close', displayName: 'Fechar',category:'PatientServiceIdentifier'))
        uiSections.add(new LinkedHashMap(id:'f1531ab8-1dd0-4106-a122-9626740eb153',requestMapUrl: '/ui/pharmaceuticalAttention/add', name: 'add-pharmaceuticalAttention', menu: 'Pacientes', action: 'add', displayName: 'Adicionar',category:'PharmaceuticalAttention'))
        uiSections.add(new LinkedHashMap(id:'6f0f169a-d83d-44a8-a629-f4dae3c247f8',requestMapUrl: '/ui/pharmaceuticalAttention/delete', name: 'remove-pharmaceuticalAttention', menu: 'Pacientes', action: 'remove', displayName: 'Remover',category:'PharmaceuticalAttention'))
        uiSections.add(new LinkedHashMap(id:'33682e87-75c3-44c0-8a98-d54fc7c59594',requestMapUrl: '/ui/patient/add', name: 'add-patient', menu: 'Pacientes', action: 'add', displayName: 'Adicionar',category:'Patient'))
        uiSections.add(new LinkedHashMap(id:'68683bf1-b51a-4cb9-ba78-a71dd3a58bb6',requestMapUrl: '/ui/patient/edit', name: 'edit-patient', menu: 'Pacientes', action: 'edit', displayName: 'Editar',category:'Patient'))
        uiSections.add(new LinkedHashMap(id:'f1033876-0b91-480c-8a7f-51df982d66f3',requestMapUrl: '/ui/patient/unitDuplicates', name: 'unit-dup-patient', menu: 'Pacientes', action: 'unitDuplicates', displayName: 'Unir Duplicados',category:'Patient'))
        uiSections.add(new LinkedHashMap(id:'a5eafa7a-4f81-4c39-9dfd-ee3b7ecec039',requestMapUrl: '/ui/stockFile/open', name: 'open-stockFile', menu: 'Stock', action: 'open', displayName: 'Abrir Ficha de Medicamento',category:'Stock'))
        uiSections.add(new LinkedHashMap(id:'5843dcfd-964e-449c-99c5-664330c2addc',requestMapUrl: '/ui/distribution/add', name: 'add-distribution', menu: 'Stock', action: 'open', displayName: 'Distribuicao',category:'Distribution'))
        uiSections.add(new LinkedHashMap(id:'cedfab3b-5fd4-4268-bc67-67d0c88ad16e',requestMapUrl: '/ui/inventory/add', name: 'add-inventory', menu: 'Stock', action: 'open', displayName: 'Abrir Inventario',category:'Inventory'))
        uiSections.add(new LinkedHashMap(id:'45413a45-e90a-4117-9480-4e95b659d895',requestMapUrl: '/ui/inventory/edit', name: 'edit-inventory', menu: 'Stock', action: 'edit', displayName: 'Editar/Fechar Inventario',category:'Inventory'))
        uiSections.add(new LinkedHashMap(id:'8ca809b5-1bdd-4f69-b963-c082b9a0ccbb',requestMapUrl: '/ui/stockEntrance/add', name: 'add-stock-entrance', menu: 'Stock', action: 'add', displayName: 'Adicionar',category:'Stock'))
        uiSections.add(new LinkedHashMap(id:'6c0b6e4c-8fe2-44b3-a0ff-8558b73a46eb',requestMapUrl: '/ui/patient/load', name: 'load-patient', menu: 'Pacientes', action: 'load', displayName: 'Carregar Paciente',category:'Patient'))

        return uiSections
    }

    void initUiSections() {
        for (uiSectionObject in listUiSection()) {
            if (!UiSection.findById(uiSectionObject.id)) {
                UiSection uiSection = new UiSection()
                uiSection.id = uiSectionObject.id
                uiSection.name = uiSectionObject.name
                uiSection.displayName = uiSectionObject.displayName
                uiSection.category = uiSectionObject.category
                uiSection.action = uiSectionObject.action
                uiSection.requestMapUrl = uiSectionObject.requestMapUrl
                uiSection.menu = Menu.findByDescription(uiSectionObject.menu)
                uiSection.validate()
                uiSection.save(flush: true, failOnError: true)
            }
        }
    }

    void initAddReportsToRolesFix() {
        def nonAdminRoles = Role.executeQuery("""
            SELECT DISTINCT r.authority
            FROM Role r
            JOIN r.menus m
            WHERE r.active = true AND r.authority != 'ROLE_ADMIN'
            AND m.code = '05' AND m.description = 'Relatorios'
        """)

        // Join into a string
       // def rolesToAppend = nonAdminRoles.join(',')
        def httpMethods = [HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE]


        def matchingRequestMaps = Requestmap.findAllByUrlIlikeAndHttpMethodInList('%Report%', httpMethods)

            matchingRequestMaps.each { rm ->
                def currentRoles = rm.configAttribute?.split(',')?.toList() ?: []
                def allRoles = (currentRoles + nonAdminRoles).unique()
                rm.configAttribute = allRoles.join(',')
                rm.save(flush: true, failOnError: true)
            }



    }
    }