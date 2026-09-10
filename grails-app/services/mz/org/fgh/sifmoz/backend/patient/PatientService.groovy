package mz.org.fgh.sifmoz.backend.patient


import grails.gorm.services.Service
import grails.gorm.transactions.Transactional
import groovy.sql.Sql
import mz.org.fgh.sifmoz.backend.clinic.Clinic
import mz.org.fgh.sifmoz.backend.clinicSector.ClinicSector
import mz.org.fgh.sifmoz.backend.convertDateUtils.ConvertDateUtils
import mz.org.fgh.sifmoz.backend.distribuicaoAdministrativa.District
import mz.org.fgh.sifmoz.backend.distribuicaoAdministrativa.Province
import mz.org.fgh.sifmoz.backend.episode.Episode
import mz.org.fgh.sifmoz.backend.episodeType.EpisodeType
import mz.org.fgh.sifmoz.backend.healthInformationSystem.HealthInformationSystem
import mz.org.fgh.sifmoz.backend.multithread.ReportSearchParams
import mz.org.fgh.sifmoz.backend.patientIdentifier.PatientServiceIdentifier
import mz.org.fgh.sifmoz.backend.service.ClinicalService
import mz.org.fgh.sifmoz.backend.startStopReason.StartStopReason
import mz.org.fgh.sifmoz.backend.utilities.Utilities
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.sql.DataSource

@Transactional
@Service(Patient)
abstract class PatientService implements IPatientService {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    DataSource dataSource

    private static final String CODE_PATTERN = '^[0-9/_-]+$'

    private static final String NAME_PATTERN = '^[a-zA-ZÀ-ÿ\\s]+$'

    @Override
    List<Patient> search(Patient patient, int offset, int limit) {
        def queryString = "select p from Patient p where 1 = 1"
        Map<String, Object> parameters = [:]

        if (patient.firstNames && patient.lastNames) {
            queryString += """
        and (
            lower(unaccent(p.firstNames)) like lower(unaccent(:firstNames))
            and lower(unaccent(p.lastNames)) like lower(unaccent(:lastNames))
        )
    """
            parameters["firstNames"] = "%" + patient.firstNames + "%"
            parameters["lastNames"] = "%" + patient.lastNames + "%"
        } else if (patient.firstNames) {
            queryString += """
        and (
            lower(unaccent(p.firstNames)) like lower(unaccent(:name))
        )
    """
            parameters["name"] = "%" + patient.firstNames + "%"
        } else if (patient.lastNames) {
            queryString += """
        and (
            lower(unaccent(p.firstNames)) like lower(unaccent(:name))
        )
    """
            parameters["name"] = "%" + patient.lastNames + "%"
        }
        if (patient.identifiers.first().value != null) {
            queryString += " and p.id in (select distinct(psi.patient.id) from PatientServiceIdentifier psi where psi.value like :identifierValue)"
            parameters["identifierValue"] = "%" + patient.identifiers.first().value.trim() + "%"
        }
        Session session = sessionFactory.currentSession
        def query = session.createQuery(queryString)

        parameters.each { key, value ->
            query.setParameter(key, value)
        }

        query.setFirstResult(offset)
        query.setMaxResults(limit)
        List<Patient> patientList = query.list()

        return patientList
    }

    @Override
    List<Patient> search(String searchString, String clinicId) {
        def sql = new Sql(dataSource as DataSource)
        String mainQuery = null
        if (isCode(searchString)) {
            mainQuery = " select p.id "+
                        " from patient p "+
                        " inner join patient_service_identifier psi on psi.patient_id = p.id "+
                        " where psi.value like '%"+searchString+"%' "+
                        " AND psi.clinic_id = :clinicId "+
                        " order by p.first_names "

        }else
        if (isName(searchString)) {
            mainQuery = " select p.id from patient p " +
                        " where (lower(p.first_names) like lower(:searchString) OR" +
                        " lower(p.middle_names) like lower(:searchString) OR " +
                        " lower(p.last_names) like lower(:searchString)) " +
                        " AND p.clinic_id =:clinicId" +
                        " order by p.first_names "
        }

        List patients = mainQuery!= null ? sql.rows(mainQuery, [clinicId: clinicId, searchString: searchString, max: 500]): []

        def lastPatientList = !patients.isEmpty() ? Patient.findAllByIdInList(patients.getAt('id')): []

        return lastPatientList
    }

    @Override
    Long count(Patient patient) {
        return Patient.countByFirstNamesIlikeOrMiddleNamesIlikeOrLastNamesIlike("%${patient.firstNames}%", "%${patient.middleNames}%", "%${patient.lastNames}%")
    }

    @Override
    List<Patient> getAllByClinicId(String clinicId, int offset, int max) {
        return Patient.findAllByClinic(Clinic.findById(clinicId), [offset: offset, max: max])
    }

    @Override
    List getAllPatientsInClinicSector(String clinicSector, int offset, int max) {
        def sql = new Sql(dataSource as DataSource)

        def getPatientsSql = '''
            SELECT p.id FROM patient_service_identifier p
            INNER JOIN (SELECT max(e.episode_date), e.patient_service_identifier_id FROM episode e
                        WHERE e.clinic_sector_id = :clinicSector
                        GROUP BY 2
                        ) lastEpisode on lastEpisode.patient_service_identifier_id = p.id
                        LIMIT :max OFFSET :offset
        '''

        List patientsId = sql.rows(getPatientsSql, [clinicSector: clinicSector, max: max, offset: offset])

        return patientsId
    }

    @Override
    List getPatientWithoutDispense(ReportSearchParams reportSearchParams) {

        def queryString = "select pat.first_names , pat.middle_names , pat.last_names ," +
                "  psi.value ,pat.his_uuid, pat.creation_date  from patient pat " +
                " inner join patient_service_identifier psi   on (pat.id = psi.patient_id) " +
                " where pat.id  not in (select pv.patient_id from patient_visit pv  )" +
                " AND psi.clinic_id =:clinic_id" +
                " and pat.creation_date >=:startDate and pat.creation_date <=:endDate"

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", reportSearchParams.endDate)
        query.setParameter("startDate", reportSearchParams.startDate)
        query.setParameter("clinic_id", reportSearchParams.clinicId)
        List<Object[]> list = query.list()
        return list
    }

    @Override
    List getAllExpectedPatients(ReportSearchParams reportSearchParams) {
        def queryString = " select distinct ON (p.id) " +
                "                p.first_names,  " +
                "                    p.middle_names,   " +
                "                    p.last_names,  " +
                "                    p.cellphone, " +
                "                    r3.nextPickUpDate," +
                "                    r1.t_regimen, " +
                "                     r1.dt_description, " +
                "                     r3.clinicName, " +
                "                    r.value, " +
                "                    cls.clinic_name " +
                "                from patient p  " +
                "                inner join patient_visit pv on pv.patient_id = p.id  " +
                "                inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id  " +
                "                inner join episode epi on epi.id = pvd.episode_id  " +
                "                inner join clinic cls on cls.id = epi.clinic_sector_id  " +
                "                inner join (  " +
                "                select  " +
                "                psi.patient_id as pat_id,  " +
                "                psi.value  " +
                "                from patient_service_identifier psi   " +
                "                inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'  " +
                "                ) r on r.pat_id = p.id  " +
                "                inner join (  " +
                "                select  " +
                "                pre.id,  " +
                "                tl.description as t_line,  " +
                "                tr.description as t_regimen,  " +
                "                pre.patient_type, " +
                "                dt.code as dtcode, " +
                "             dt.description as dt_description " +
                "                from prescription pre  " +
                "                inner join prescription_detail pd on pd.prescription_id = pre.id  " +
                "                inner join therapeutic_line tl on pd.therapeutic_line_id = tl.id  " +
                "                inner join therapeutic_regimen tr on pd.therapeutic_regimen_id = tr.id   " +
                "                inner join dispense_type dt ON dt.id = pd.dispense_type_id " +
                "                ) r1 on pvd.prescription_id = r1.id  " +
                "                inner join (  " +
                "                select   " +
                "                pk.id as pack_id,  " +
                "                pk.next_pick_up_date as nextPickUpDate, " +
                "                MAX(pk.pickup_date) AS pickUpDate," +
                "                c.clinic_name as clinicName " +
                "                from pack pk  " +
                "                inner join clinic c on pk.clinic_id = c.id" +
                "                group by pk.id, pk.next_pick_up_date,  c.clinic_name " +
                "                ) r3 on pvd.pack_id = r3.pack_id  " +
                "                where   Date(r3.nextPickUpDate) >= :startDate and Date(r3.nextPickUpDate) <= :endDate "
        //and   r3.nextPickUpDate >=current_timestamp

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", reportSearchParams.endDate)
        query.setParameter("startDate", reportSearchParams.startDate)
//        query.setParameter("clinic_id", reportSearchParams.clinicId)

        List<Object[]> list = query.list()
        return list
    }

    Long countPatientSearchResult(Patient patient) {
        def queryString = "select count(p.id) from Patient p where 1 = 1"
        Map<String, Object> parameters = [:]

        if (patient.firstNames && patient.lastNames) {
            queryString += """
        and (
            lower(unaccent(p.firstNames)) like lower(unaccent(:firstNames))
            and lower(unaccent(p.lastNames)) like lower(unaccent(:lastNames))
        )
    """
            parameters["firstNames"] = "%" + patient.firstNames + "%"
            parameters["lastNames"] = "%" + patient.lastNames + "%"
        } else if (patient.firstNames) {
            queryString += """
        and (
            lower(unaccent(p.firstNames)) like lower(unaccent(:name))
        )
    """
            parameters["name"] = "%" + patient.firstNames + "%"
        } else if (patient.lastNames) {
            queryString += """
        and (
             lower(unaccent(p.lastNames)) like lower(unaccent(:name))
        )
    """
            parameters["name"] = "%" + patient.lastNames + "%"
        }
        if (patient.identifiers.first().value != null) {
            queryString += " and p.id in (select distinct(psi.patient.id) from PatientServiceIdentifier psi where psi.value like :identifierValue)"
            parameters["identifierValue"] = "%" + patient.identifiers.first().value.trim() + "%"
        }

        Session session = sessionFactory.currentSession
        def query = session.createQuery(queryString)

        parameters.each { key, value ->
            query.setParameter(key, value)
        }
        Long totalCount = query.uniqueResult()


        return totalCount

    }

    List findPossibleDuplicatePatients() {
        String sqlQuery = "SELECT records.value, p.first_names, p.last_names, " +
                "p.date_of_birth, p.gender, records.amount " +
                "FROM PatientServiceView p, " +
                "(SELECT a.value, a.first_names, a.last_names, " +
                "COUNT(a.value) AS amount " +
                "FROM PatientServiceView a, PatientServiceView b " +
                "WHERE a.first_names = b.first_names " +
                "AND a.last_names = b.last_names " +
                "GROUP BY a.value, a.first_names, a.last_names) AS records " +
                "WHERE p.value = records.value " +
                "AND records.amount > 1 " +
                "GROUP BY records.value, p.first_names, p.last_names, " +
                "records.amount, p.date_of_birth, p.gender " +
                "ORDER BY p.first_names, p.last_names";

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(sqlQuery)
        List<Object[]> list = query.list()
        return list
    }

    List<Patient> getAllPatientsIsAbandonment(int offset, int max, String clinic_id) {

        def params = [max: max, offset: offset, clinic: clinic_id]
        def sql = new Sql(dataSource as DataSource)
        List<Patient> patients = new ArrayList<>()

        def query =
                """
                    select distinct(lastEpisode.patient_id) from episode ep,
                    (
                      SELECT MAX(e.episode_date) lastEpisodeDate, psi.id patient_service_identifier_id, psi.patient_id
                      FROM episode e
                      inner join patient_service_identifier psi on psi.id = e.patient_service_identifier_id
                      where e.clinic_id = :clinic
                      group by 2,3
                    )lastEpisode
                    where ep.is_abandonmentdc = true 
                    and ep.episode_date = lastEpisode.lastEpisodeDate 
                    and ep.patient_service_identifier_id = lastEpisode.patient_service_identifier_id 
                    and ep.clinic_id = :clinic LIMIT :max OFFSET :offset
                """

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            patients = Patient.findAllByIdInList(list)
        }

        return patients
    }

//    List findPossibleDuplicatePatients() {
//        String sqlQuery = "SELECT records.value, p.first_names, p.last_names, " +
//                "p.date_of_birth, p.gender, records.amount " +
//                "FROM PatientServiceView p, " +
//                "(SELECT a.value, a.first_names, a.last_names, " +
//                "COUNT(a.value) AS amount " +
//                "FROM PatientServiceView a, PatientServiceView b " +
//                "WHERE a.first_names = b.first_names " +
//                "AND a.last_names = b.last_names " +
//                "GROUP BY a.value, a.first_names, a.last_names) AS records " +
//                "WHERE p.value = records.value " +
//                "AND records.amount > 1 " +
//                "GROUP BY records.value, p.first_names, p.last_names, " +
//                "records.amount, p.date_of_birth, p.gender " +
//                "ORDER BY p.first_names, p.last_names";
//
//        Session session = sessionFactory.getCurrentSession()
//        def query = session.createSQLQuery(sqlQuery)
//        List<Object[]> list = query.list()
//        return list
//    }

    Patient savePatientFromPoc(def objectJSON) {
        if (!objectJSON?.empty && objectJSON) {
            Patient patient = Patient.findWhere(id: objectJSON.patientUuid)
            if (!patient) {
                patient = new Patient()
                patient.addMatchId()
                patient.addClinic()
                patient.firstNames = objectJSON.firstName
                patient.middleNames = objectJSON.middleName
                patient.lastNames = objectJSON.lastName
                patient.dateOfBirth = Boolean.parseBoolean(objectJSON.birthdateEstimated) == false ? ConvertDateUtils.createDate(objectJSON.birthDate, "yyyy-MM-dd") : ConvertDateUtils.getDateFromDayAndMonthAndYear(1, 1, Integer.parseInt(objectJSON.birthDate))
                patient.gender = objectJSON.gender == 'Male' ? 'Masculino' : 'Feminino'
                patient.province = Province.findByDescription(objectJSON.province) == null ? patient.clinic.province : Province.findByDescription(objectJSON.province)
                patient.district = District.findByDescription(objectJSON.district) == null ? patient.clinic.district : District.findByDescription(objectJSON.district)
                // patient.postoAdministrativo = ''
                // patient.bairro = ''
                patient.cellphone = objectJSON.phoneNumber?.replaceFirst('^\\+', '')
                patient.alternativeCellphone = objectJSON.phoneNumber?.replaceFirst('^\\+', '')
                patient.id = objectJSON.patientUuid
                patient.hisUuid = objectJSON.patientUuid
                patient.hisLocation = objectJSON.locationUuid
                patient.hisLocationName = objectJSON.locationName
                patient.his = HealthInformationSystem.findByAbbreviation('OpenMRS')
                patient.hisSyncStatus = 'N'
                patient.save(flush: true)
                //   Thread.sleep(2000)
                //    patient.refresh()
                objectJSON?.clinicalHistory?.each { clinicalHistory ->
                    def psi = createPatientServiceIdentifier(patient, clinicalHistory)
                    createEpisode(psi, clinicalHistory)
                }
                return patient
            }
        }
    }


    PatientServiceIdentifier createPatientServiceIdentifier(Patient patient, def clinicalHistory) {
        PatientServiceIdentifier psi = new PatientServiceIdentifier()
        psi.beforeInsert()
        psi.value = clinicalHistory.nid
        psi.startDate = ConvertDateUtils.createDate(clinicalHistory.admissionDate, "yyyy-MM-dd")
        psi.prefered = true
        psi.service = ClinicalService.get(clinicalHistory.serviceCode)
        psi.identifierType = ClinicalService.get(clinicalHistory.serviceCode).identifierType
        psi.patient = patient
        psi.state = 'Activo'
        psi.origin = psi.clinic
        psi.save(flush: true)
        return psi
    }

    void createEpisode(PatientServiceIdentifier psi, def clinicalHistory) {
        Episode episode = new Episode()
        episode.beforeInsert()
        episode.episodeDate = ConvertDateUtils.createDate(clinicalHistory.admissionDate, "yyyy-MM-dd")
        episode.startStopReason = StartStopReason.findByCode(clinicalHistory.programStatus)
        episode.episodeType = EpisodeType.findByCode('INICIO')
        episode.clinicSector = ClinicSector.get(clinicalHistory.clinicalSector)
        episode.origin = episode.clinic
        episode.patientServiceIdentifier = psi
        episode.isAbandonmentDC = false
        episode.residentInCountry = true
        episode.notes = clinicalHistory.programStatus
        episode.validate()
        episode.save(flush: true)
        //  psi.episodes.addAll(episode)
        //   psi.save(flush: true)
    }

    static boolean isCode(String input) {
        return input != null && input  ==~ (CODE_PATTERN)
    }

    static boolean isName(String input) {
        return input != null && input  ==~ (NAME_PATTERN)
    }

}