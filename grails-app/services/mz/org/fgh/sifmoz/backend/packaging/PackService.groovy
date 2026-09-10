package mz.org.fgh.sifmoz.backend.packaging


import grails.gorm.services.Service
import grails.gorm.transactions.Transactional
import groovy.sql.Sql
import mz.org.fgh.sifmoz.backend.clinic.Clinic
import mz.org.fgh.sifmoz.backend.dispenseType.DispenseType
import mz.org.fgh.sifmoz.backend.episode.Episode
import mz.org.fgh.sifmoz.backend.multithread.ReportSearchParams
import mz.org.fgh.sifmoz.backend.patient.Patient
import mz.org.fgh.sifmoz.backend.patientIdentifier.PatientServiceIdentifier
import mz.org.fgh.sifmoz.backend.patientVisit.PatientVisit
import mz.org.fgh.sifmoz.backend.patientVisitDetails.PatientVisitDetails
import mz.org.fgh.sifmoz.backend.prescription.Prescription
import mz.org.fgh.sifmoz.backend.reports.pharmacyManagement.linhasUsadas.LinhasUsadasReport
import mz.org.fgh.sifmoz.backend.reports.pharmacyManagement.mmia.MmiaRegimenSubReport
import mz.org.fgh.sifmoz.backend.reports.pharmacyManagement.mmia.MmiaReport
import mz.org.fgh.sifmoz.backend.reports.pharmacyManagement.segundasLinhas.SegundasLinhasReport
import mz.org.fgh.sifmoz.backend.reports.stock.BalanceteReport
import mz.org.fgh.sifmoz.backend.service.ClinicalService
import mz.org.fgh.sifmoz.backend.therapeuticLine.TherapeuticLine
import mz.org.fgh.sifmoz.backend.utilities.Utilities
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired


import javax.sql.DataSource
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat

@Transactional
@Service(Pack)
abstract class PackService implements IPackService {

    @Autowired
    DataSource dataSource

    @Autowired
    SessionFactory sessionFactory

    @Override
    List<Pack> getAllLastPackOfClinic(String clinicId, int offset, int max) {
        Session session = sessionFactory.getCurrentSession()

        String queryString =
                """
                select * 
                from patient_last_pack_vw 
                where clinic_id = :clinic offset :offset limit :max 
                """

        def query = session.createSQLQuery(queryString).addEntity(Pack.class)
        query.setParameter("clinic", clinicId)
        query.setParameter("offset", offset)
        query.setParameter("max", max)
        List<Pack> result = query.list()
        return result
    }

    @Override
    int countPacksByDispenseTypeAndServiceOnPeriod(DispenseType dispenseType, ClinicalService service, Clinic clinic, Date startDate, Date endDate) {
        int value = 0
        def sqlpack =
                """
                    select count(*)
                    from  PatientVisitDetails as pvd
                    inner join  pvd.pack as pk
                    inner join pk.clinic as cl
                    inner join pvd.prescription as pr
                    inner join pvd.episode as ep
                    inner join pr.prescriptionDetails as prd
                    inner join ep.patientServiceIdentifier as pid 
                    inner join pid.service as svc 
                    where pk.pickupDate >= :startDate 
                           and pk.pickupDate <= :endDate 
                           and cl.id = :clinic 
                           and prd.dispenseType.code = :dispenseTypeCode 
                           and ep.patientServiceIdentifier.service.code = :serviceCode
                """
        def count = Pack.executeQuery(sqlpack,
                [startDate: startDate, endDate: endDate, serviceCode: service.getCode(), clinic: clinic.getId(), dispenseTypeCode: dispenseType.getCode()])
        value = Integer.valueOf(count.get(0).toString())
        return value
    }

    @Override
    List<MmiaRegimenSubReport> getMMIARegimenStatisticTB(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate]
        def sql = new Sql(dataSource as DataSource)
        List<MmiaRegimenSubReport> mmiaRegimenSubReports = new ArrayList<>()



        String queryUtentesActivos = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 1
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 0
                                                        ELSE
                                                            0
                                                    END
                                            END
                                    END
                            END
                    END
                    ) AS total_inicio_tratamento
                FROM (
                   SELECT DISTINCT 
                     pat.id,
                     MAX(pk.pickup_date) AS pickupdate,
                     MAX(pk.id) AS packid,
                     pat.date_of_birth,
                     cs.code AS service_code,
                     ssr.code as ssr_code
                   FROM patient_visit_details pvd
                    INNER JOIN pack pk ON pk.id = pvd.pack_id
                    INNER JOIN episode ep ON ep.id = pvd.episode_id
                    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN patient pat ON pat.id = pv.patient_id
                    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN clinic c ON c.id = ep.clinic_id
                   WHERE 
                    DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                    and extract(days from pk.pickup_date - ep.episode_date) <= 3
                    AND cs.code = 'TPT'  
                    GROUP BY 1,4,5,6
                    ORDER BY 1
                ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                    WHERE 
                        cs.code = 'TPT'
                        AND dt.code IN ('DT', 'DM')
        """


        String queryUtentesEmManutencao = """                            
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 0
                                                        ELSE
                                                            1
                                                    END
                                            END
                                    END
                            END
                    END
                    ) AS total_manutencao
                FROM (
                   SELECT DISTINCT 
                     pat.id,
                     MAX(pk.pickup_date) AS pickupdate,
                     MAX(pk.id) AS packid,
                     pat.date_of_birth,
                     cs.code AS service_code,
                     ssr.code as ssr_code
                   FROM patient_visit_details pvd
                    INNER JOIN pack pk ON pk.id = pvd.pack_id
                    INNER JOIN episode ep ON ep.id = pvd.episode_id
                    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN patient pat ON pat.id = pv.patient_id
                    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN clinic c ON c.id = ep.clinic_id
                   WHERE 
                    DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                    AND extract(days from pk.pickup_date - ep.episode_date) > 3
                    AND cs.code = 'TPT'  
                    GROUP BY 1,4,5,6
                    ORDER BY 1
                ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id and p.patient_type not in ('FIM', 'NOVO')
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                   INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                    WHERE 
                        cs.code = 'TPT'
                        AND dt.code IN ('DT', 'DM')
        """

        String queryUtentesAFimTratamento = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 1
                                                        ELSE
                                                            0
                                                    END
                                            END
                                    END
                            END
                    END
        ) AS total_fim_tratamento
    FROM (
       SELECT DISTINCT 
         pat.id,
         MAX(pk.pickup_date) AS pickupdate,
         MAX(pk.id) AS packid,
         pat.date_of_birth,
         cs.code AS service_code,
         ssr.code as ssr_code
       FROM patient_visit_details pvd
        INNER JOIN pack pk ON pk.id = pvd.pack_id
        INNER JOIN episode ep ON ep.id = pvd.episode_id
        INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN patient pat ON pat.id = pv.patient_id
        INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN clinical_service cs ON cs.id = psi.service_id
        INNER JOIN clinic c ON c.id = ep.clinic_id
       WHERE 
        DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
        AND extract(days from pk.pickup_date - ep.episode_date) <= 3  
        AND cs.code = 'TPT'  
        GROUP BY 1,4,5,6
        ORDER BY 1
    ) patientstatistics
        INNER JOIN 
            pack pack ON pack.id = patientstatistics.packid
        INNER JOIN 
            patient_visit_details pvd ON pvd.pack_id = pack.id
        INNER JOIN 
            prescription p ON p.id = pvd.prescription_id
        INNER JOIN 
            prescription_detail pd ON pd.prescription_id = p.id
        INNER JOIN 
            episode ep ON ep.id = pvd.episode_id
        INNER JOIN 
            start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN 
            patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN 
            therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
        INNER JOIN 
            patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN 
            clinical_service cs ON cs.id = psi.service_id
        INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
        WHERE 
            cs.code = 'TPT'
            AND dt.code IN ('DT', 'DM')
        """

        String queryMaiorDe15Inicio = """
                SELECT
                    SUM(
                        CASE
                            WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                                AND patientstatistics.service_code = 'TPT' 
                                AND dt.code IN ('DT', 'DM')
                                AND ssr.code = 'NOVO_PACIENTE'
                            THEN 1
                            ELSE 
                                CASE 
                                    WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                        AND patientstatistics.service_code = 'TPT' 
                                        AND ssr.code = 'TRANSFERIDO_DE'
                                        AND dt.code IN ('DT', 'DM')
                                    THEN 0
                                    ELSE
                                        CASE 
                                            WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                                AND patientstatistics.service_code = 'TPT' 
                                                AND ssr.code = 'REINICIO_TRATAMETO'
                                                AND dt.code IN ('DT', 'DM')
                                            THEN 0
                                            ELSE
                                                CASE 
                                                    WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                        AND patientstatistics.service_code = 'TPT'
                                                        AND dt.code IN ('DT', 'DM')
                                                    THEN 0
                                                    ELSE
                                                        CASE 
                                                            WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                              AND patientstatistics.service_code = 'TPT'
                                                              AND dt.code IN ('DT', 'DM')
                                                            THEN 0
                                                            ELSE
                                                                0
                                                        END
                                                END
                                        END
                                END
                        END
                        ) AS total_inicio_tratamento_maiorDe15
                    FROM (
                       SELECT DISTINCT 
                         pat.id,
                         MAX(pk.pickup_date) AS pickupdate,
                         MAX(pk.id) AS packid,
                         pat.date_of_birth,
                         cs.code AS service_code,
                         ssr.code as ssr_code
                       FROM patient_visit_details pvd
                        INNER JOIN pack pk ON pk.id = pvd.pack_id
                        INNER JOIN episode ep ON ep.id = pvd.episode_id
                        INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                        INNER JOIN patient pat ON pat.id = pv.patient_id AND CAST (extract(year FROM age(:endDate, pat.date_of_birth)) AS INTEGER) >= 15
                        INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                        INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                        INNER JOIN clinical_service cs ON cs.id = psi.service_id
                        INNER JOIN clinic c ON c.id = ep.clinic_id
                       WHERE 
                        DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                        and extract(days from pk.pickup_date - ep.episode_date) <= 3
                        AND cs.code = 'TPT'  
                        GROUP BY 1,4,5,6
                        ORDER BY 1
                    ) patientstatistics
                        INNER JOIN 
                            pack pack ON pack.id = patientstatistics.packid
                        INNER JOIN 
                            patient_visit_details pvd ON pvd.pack_id = pack.id
                        INNER JOIN 
                            prescription p ON p.id = pvd.prescription_id
                        INNER JOIN 
                            prescription_detail pd ON pd.prescription_id = p.id
                        INNER JOIN 
                            episode ep ON ep.id = pvd.episode_id
                        INNER JOIN 
                            start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                        INNER JOIN 
                            patient_visit pv ON pv.id = pvd.patient_visit_id
                        INNER JOIN 
                            therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                        INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                        INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                        INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                        WHERE 
                            cs.code = 'TPT'
                            AND dt.code IN ('DT', 'DM')
            """

        String queryMaiorDe15Manutencao = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 0
                                                        ELSE
                                                            1
                                                    END
                                            END
                                    END
                            END
                    END
                    ) AS total_manutencao_maiorDe15
                FROM (
                   SELECT DISTINCT 
                     pat.id,
                     MAX(pk.pickup_date) AS pickupdate,
                     MAX(pk.id) AS packid,
                     pat.date_of_birth,
                     cs.code AS service_code,
                     ssr.code as ssr_code
                   FROM patient_visit_details pvd
                    INNER JOIN pack pk ON pk.id = pvd.pack_id
                    INNER JOIN episode ep ON ep.id = pvd.episode_id
                    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN patient pat ON pat.id = pv.patient_id AND CAST (extract(year FROM age(:endDate, pat.date_of_birth)) AS INTEGER) >= 15
                    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN clinic c ON c.id = ep.clinic_id
                   WHERE 
                    DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                    AND extract(days from pk.pickup_date - ep.episode_date) > 3
                    AND cs.code = 'TPT'  
                    GROUP BY 1,4,5,6
                    ORDER BY 1
                ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id and p.patient_type not in ('FIM', 'NOVO')
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                   INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                   INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                    WHERE 
                        cs.code = 'TPT'
                        AND dt.code IN ('DT', 'DM')
        """

        String queryMaiorDe15Fim = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 1
                                                        ELSE
                                                            0
                                                    END
                                            END
                                    END
                            END
                    END
        ) AS total_fim_tratamento_maiorDe15
    FROM (
       SELECT DISTINCT 
         pat.id,
         MAX(pk.pickup_date) AS pickupdate,
         MAX(pk.id) AS packid,
         pat.date_of_birth,
         cs.code AS service_code,
         ssr.code as ssr_code
       FROM patient_visit_details pvd
        INNER JOIN pack pk ON pk.id = pvd.pack_id
        INNER JOIN episode ep ON ep.id = pvd.episode_id
        INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN patient pat ON pat.id = pv.patient_id AND CAST (extract(year FROM age(:endDate, pat.date_of_birth)) AS INTEGER) >= 15
        INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN clinical_service cs ON cs.id = psi.service_id
        INNER JOIN clinic c ON c.id = ep.clinic_id
       WHERE 
        DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
        AND extract(days from pk.pickup_date - ep.episode_date) <= 3  
        AND cs.code = 'TPT'  
        GROUP BY 1,4,5,6
        ORDER BY 1
    ) patientstatistics
        INNER JOIN 
            pack pack ON pack.id = patientstatistics.packid
        INNER JOIN 
            patient_visit_details pvd ON pvd.pack_id = pack.id
        INNER JOIN 
            prescription p ON p.id = pvd.prescription_id
        INNER JOIN 
            prescription_detail pd ON pd.prescription_id = p.id
        INNER JOIN 
            episode ep ON ep.id = pvd.episode_id
        INNER JOIN 
            start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN 
            patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN 
            therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
        INNER JOIN 
            patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN 
             clinical_service cs ON cs.id = psi.service_id
        INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
        WHERE 
            cs.code = 'TPT'
            AND dt.code IN ('DT', 'DM')
        """

        def queryUtetentesEmDTInico = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code = 'DT'
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 1
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 0
                                                        ELSE
                                                            0
                                                    END
                                            END
                                    END
                            END
                    END
                    ) AS total_inicio_tratamento
                FROM (
                   SELECT DISTINCT 
                     pat.id,
                     MAX(pk.pickup_date) AS pickupdate,
                     MAX(pk.id) AS packid,
                     pat.date_of_birth,
                     cs.code AS service_code,
                     ssr.code as ssr_code
                   FROM patient_visit_details pvd
                    INNER JOIN pack pk ON pk.id = pvd.pack_id
                    INNER JOIN episode ep ON ep.id = pvd.episode_id
                    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN patient pat ON pat.id = pv.patient_id AND CAST (extract(year FROM age(:endDate, pat.date_of_birth)) AS INTEGER) >= 15
                    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN clinic c ON c.id = ep.clinic_id
                   WHERE 
                    DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                    and extract(days from pk.pickup_date - ep.episode_date) <= 3
                    AND cs.code = 'TPT'  
                    GROUP BY 1,4,5,6
                    ORDER BY 1
                ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                     INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                    WHERE 
                        cs.code = 'TPT'
                        and dt.code = 'DT'
        """

        def queryUtetentesEmDTManutencao = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio' or p.patient_type = 'NOVO')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code IN ('DT', 'DM')
                                                        THEN 0
                                                        ELSE
                                                            1
                                                    END
                                            END
                                    END
                            END
                    END
                    ) AS total_manutencao
                FROM (
                   SELECT DISTINCT 
                     pat.id,
                     MAX(pk.pickup_date) AS pickupdate,
                     MAX(pk.id) AS packid,
                     pat.date_of_birth,
                     cs.code AS service_code,
                     ssr.code as ssr_code
                   FROM patient_visit_details pvd
                    INNER JOIN pack pk ON pk.id = pvd.pack_id
                    INNER JOIN episode ep ON ep.id = pvd.episode_id
                    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN patient pat ON pat.id = pv.patient_id AND CAST (extract(year FROM age(:endDate, pat.date_of_birth)) AS INTEGER) >= 15
                    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN clinic c ON c.id = ep.clinic_id
                   WHERE 
                    DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
                    AND extract(days from pk.pickup_date - ep.episode_date) > 3
                    AND cs.code = 'TPT'  
                    GROUP BY 1,4,5,6
                    ORDER BY 1
                ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id and p.patient_type not in ('FIM', 'NOVO')
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
                    WHERE 
                        cs.code = 'TPT'
                        and dt.code = 'DT'
        """

        def queryUtetentesEmDTFim = """
            SELECT
                SUM(
                    CASE
                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio')
                            AND patientstatistics.service_code = 'TPT' 
                            AND dt.code IN ('DT', 'DM')
                            AND ssr.code = 'NOVO_PACIENTE'
                        THEN 0
                        ELSE 
                            CASE 
                                WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Transfer de')
                                    AND patientstatistics.service_code = 'TPT' 
                                    AND ssr.code = 'TRANSFERIDO_DE'
                                    AND dt.code IN ('DT', 'DM')
                                THEN 0
                                ELSE
                                    CASE 
                                        WHEN (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Reiniciar')
                                            AND patientstatistics.service_code = 'TPT' 
                                            AND ssr.code = 'REINICIO_TRATAMETO'
                                            AND dt.code IN ('DT', 'DM')
                                        THEN 0
                                        ELSE
                                            CASE 
                                                WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                                    AND patientstatistics.service_code = 'TPT'
                                                    AND dt.code IN ('DT', 'DM')
                                                THEN 0
                                                ELSE
                                                    CASE 
                                                        WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                          AND patientstatistics.service_code = 'TPT'
                                                          AND dt.code = 'DT'
                                                        THEN 1
                                                        ELSE
                                                            0
                                                    END
                                            END
                                    END
                            END
                    END
        ) AS total_fim_tratamento
    FROM (
       SELECT DISTINCT 
         pat.id,
         MAX(pk.pickup_date) AS pickupdate,
         MAX(pk.id) AS packid,
         pat.date_of_birth,
         cs.code AS service_code,
         ssr.code as ssr_code
       FROM patient_visit_details pvd
        INNER JOIN pack pk ON pk.id = pvd.pack_id
        INNER JOIN episode ep ON ep.id = pvd.episode_id
        INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN patient pat ON pat.id = pv.patient_id
        INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN clinical_service cs ON cs.id = psi.service_id
        INNER JOIN clinic c ON c.id = ep.clinic_id
       WHERE 
        DATE(pk.pickup_date) BETWEEN :startDate AND :endDate
        AND extract(days from pk.pickup_date - ep.episode_date) <= 3  
        AND cs.code = 'TPT'  
        GROUP BY 1,4,5,6
        ORDER BY 1
    ) patientstatistics
        INNER JOIN 
            pack pack ON pack.id = patientstatistics.packid
        INNER JOIN 
            patient_visit_details pvd ON pvd.pack_id = pack.id
        INNER JOIN 
            prescription p ON p.id = pvd.prescription_id
        INNER JOIN 
            prescription_detail pd ON pd.prescription_id = p.id
        INNER JOIN 
            episode ep ON ep.id = pvd.episode_id
        INNER JOIN 
            start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
        INNER JOIN 
            patient_visit pv ON pv.id = pvd.patient_visit_id
        INNER JOIN 
            therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
        INNER JOIN 
            patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
        INNER JOIN 
           clinical_service cs ON cs.id = psi.service_id
        INNER JOIN dispense_type dt ON dt.id = pd.dispense_type_id
        WHERE 
            cs.code = 'TPT'
            and dt.code = 'DT'
        """

        def queryTotalMedicamentosDispesados = """
            SELECT
                d.name AS drug_name,
                SUM(pd.quantity_supplied) AS total_dispensado
            FROM
                packaged_drug pd
                INNER JOIN
                pack p ON pd.pack_id = p.id
                INNER JOIN patient_visit_details pvd ON pvd.pack_id = p.id
                inner join episode ep on ep.id = pvd.episode_id
                INNER JOIN patient_visit pv ON pvd.patient_visit_id = pv.id
                inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                INNER JOIN clinical_service cs ON psi.service_id = cs.id
                INNER JOIN drug d ON pd.drug_id = d.id
            WHERE
                p.pickup_date BETWEEN :startDate AND :endDate
                AND cs.code = 'TPT'
            GROUP BY
                d.name, pd.drug_id
        """


        def maiorDe15 = 0
        def menorDe15 = 0

        def utentesDT = 0
        def utentesDM = 0

        def utentesActivos = sql.rows(queryUtentesActivos, params)
        def utentesManutencao = sql.rows(queryUtentesEmManutencao, params)
        def utentesFimTratamento = sql.rows(queryUtentesAFimTratamento, params)

        def maiorDe15Inicio
        def emDTInicio
        if(Utilities.listHasElements(utentesActivos as ArrayList<?>)) {
            def totalInicio = Integer.valueOf(String.valueOf(utentesActivos[0][0] != null ? utentesActivos[0][0] : "0"))
            maiorDe15Inicio = sql.rows(queryMaiorDe15Inicio, params)
            def maior15 = Integer.valueOf(String.valueOf(maiorDe15Inicio[0][0] != null ? maiorDe15Inicio[0][0] : "0"))

            maiorDe15 += maior15
            menorDe15 += totalInicio - maior15

            // DT & DM
            emDTInicio = sql.rows(queryUtetentesEmDTInico, params)
            def emDT =  Integer.valueOf(String.valueOf(emDTInicio[0][0] != null ? emDTInicio[0][0] : "0"))

            utentesDT += emDT
            utentesDM += totalInicio - emDT
        }

        def maiorDe15Manutencao
        def emDTManutencao
        if(Utilities.listHasElements(utentesManutencao as ArrayList<?>)) {
            def totalManutencao = Integer.valueOf(String.valueOf(utentesManutencao[0][0] != null ? utentesManutencao[0][0] : "0"))
            maiorDe15Manutencao = sql.rows(queryMaiorDe15Manutencao, params)
            def maior15 = Integer.valueOf(String.valueOf(maiorDe15Manutencao[0][0] != null ? maiorDe15Manutencao[0][0] : "0"))

            maiorDe15 += maior15
            menorDe15 += totalManutencao - maior15

            // DT & DM
            emDTManutencao = sql.rows(queryUtetentesEmDTManutencao, params)
            def emDT =  Integer.valueOf(String.valueOf(emDTManutencao[0][0] != null ? emDTManutencao[0][0] : "0"))

            utentesDT += emDT
            utentesDM += totalManutencao - emDT
        }

        def maiorDe15Fim
        def emDTFim
        if(Utilities.listHasElements(utentesFimTratamento as ArrayList<?>)) {
            def totalFim = Integer.valueOf(String.valueOf(utentesFimTratamento[0][0] != null ? utentesFimTratamento[0][0] : "0"))
            maiorDe15Fim = sql.rows(queryMaiorDe15Fim, params)
            def maior15 = Integer.valueOf(String.valueOf(maiorDe15Fim[0][0] != null ? maiorDe15Fim[0][0] : "0"))

            maiorDe15 += maior15
            menorDe15 += totalFim - maior15

            // DT & DM
            emDTFim = sql.rows(queryUtetentesEmDTFim, params)
            def emDT =  Integer.valueOf(String.valueOf(emDTFim[0][0] != null ? emDTFim[0][0] : "0"))

            utentesDT += emDT
            utentesDM += totalFim - emDT
        }

        def totalMedicamentosDispesados = sql.rows(queryTotalMedicamentosDispesados, params)



        addMmiaRegimenStatisticInListTemporaryForTB(menorDe15, maiorDe15, utentesActivos, utentesManutencao, utentesFimTratamento, utentesDT, utentesDM, totalMedicamentosDispesados, mmiaRegimenSubReports)

        return mmiaRegimenSubReports
    }

    def addMmiaRegimenStatisticInListTemporaryForTB(def menorDe15, def maiorDe15, def utentesActivos, def utentesManutencao, def utentesFimTratamento, def utentesDT, def utentesDM, def totalMedicamentosDispesados, List<MmiaRegimenSubReport> mmiaRegimenSubReports) {
        MmiaRegimenSubReport mmiaRegimenSubReport = new MmiaRegimenSubReport()
        mmiaRegimenSubReport.line = ""
        mmiaRegimenSubReport.lineCode = ""
        mmiaRegimenSubReport.regimen = ""
        mmiaRegimenSubReport.code = ""
        mmiaRegimenSubReport.line = ""
        mmiaRegimenSubReport.totalPatients = 0
        mmiaRegimenSubReport.lineCode = ""
        mmiaRegimenSubReport.cumunitaryClinic = 0

        // Adaptado para segurar Total de pacientes menores de 15 anos
        mmiaRegimenSubReport.totaldcline1 = menorDe15

        // Adaptado para segurar Total de pacientes maiores de 15 anos
        mmiaRegimenSubReport.totaldcline2 = maiorDe15

        // Adaptado para segurar Total de pacientes que iniciaram o tratamento
        if (Utilities.listHasElements(utentesActivos as ArrayList<?>)) {
            mmiaRegimenSubReport.totaldcline3 = Integer.valueOf(String.valueOf(utentesActivos[0][0] != null ? utentesActivos[0][0] : "0"))
        } else {
            mmiaRegimenSubReport.setTotaldcline3(0)
        }

        // Adaptado para segurar Total de pacientes em manutencao
        if (Utilities.listHasElements(utentesManutencao as ArrayList<?>)) {
            mmiaRegimenSubReport.totalline3 = Integer.valueOf(String.valueOf(utentesManutencao[0][0] != null ? utentesManutencao[0][0] : "0"))
        } else {
            mmiaRegimenSubReport.setTotalline3(0)
        }

        // Adaptado para segurar Total de pacientes que terminaram o tratamento
        if (Utilities.listHasElements(utentesFimTratamento as ArrayList<?>)) {
            mmiaRegimenSubReport.totaldcline4 = Integer.valueOf(String.valueOf(utentesFimTratamento[0][0] != null ? utentesFimTratamento[0][0] : "0"))
        } else {
            mmiaRegimenSubReport.setTotaldcline4(0)
        }

        // Adaptado para segurar Todos levantamentos marcados como Dispensa Trimestral
            mmiaRegimenSubReport.totalline1 = utentesDT


        // Adaptado para segurar Todos levantamentos marcados como Dispensa Mensal
            mmiaRegimenSubReport.totalline2 = utentesDM

        mmiaRegimenSubReport.line1 = "0"
        mmiaRegimenSubReport.line2 = "0"
        mmiaRegimenSubReport.line3 = "0"
        mmiaRegimenSubReport.line4 = "0"
        mmiaRegimenSubReport.totalline4 = 0
        mmiaRegimenSubReport.totalPatients = 0
        mmiaRegimenSubReport.cumunitaryClinic = 0
        // Adaptado para segurar Total de medicamentos dispensados no periodo
        if (Utilities.listHasElements(totalMedicamentosDispesados as ArrayList<?>)) {
            for (int i = 0; i < totalMedicamentosDispesados.size(); i++) {
                if ((totalMedicamentosDispesados[i][0]).toString().contains("[INH 100 cp] Isoniazida 100mg")) {
                    mmiaRegimenSubReport.line1 = (totalMedicamentosDispesados[i][1]).toString()
                    println(mmiaRegimenSubReport.line1)
                }
                if((totalMedicamentosDispesados[i][0]).toString().contains("[INH 300mg cp] Isoniazida 300mg")) {
                    mmiaRegimenSubReport.line2 = (totalMedicamentosDispesados[i][1]).toString()
                    println(mmiaRegimenSubReport.line2)
                }

                if((totalMedicamentosDispesados[i][0]).toString().contains("[LFX 100mg cp] Levofloxacina 100 mg Disp")) {
                    mmiaRegimenSubReport.line3 = (totalMedicamentosDispesados[i][1]).toString()
                    println(mmiaRegimenSubReport.line3)
                }

                if((totalMedicamentosDispesados[i][0]).toString().contains("[LFX 250mg cp] Levofloxacina 250mg")) {
                    mmiaRegimenSubReport.line4 = (totalMedicamentosDispesados[i][1]).toString()
                    println(mmiaRegimenSubReport.line4.toString())
                }

                if((totalMedicamentosDispesados[i][0]).toString().contains("3HP (Rifapentina+Isoniazida)")) {
                    def valorFloat = Double.parseDouble(totalMedicamentosDispesados[i][1].toString())
                    mmiaRegimenSubReport.totalline4 = (int) valorFloat
                }

                if((totalMedicamentosDispesados[i][0]).toString().contains("[Vit B6 25mg cp] Piridoxina (Vit B6) 25mg")) {
                    def valorFloat = Double.parseDouble(totalMedicamentosDispesados[i][1].toString())
                    mmiaRegimenSubReport.totalPatients = (int) valorFloat
                }

                if((totalMedicamentosDispesados[i][0]).toString().contains("[Vit B6 50mg cp] Piridoxina (Vitamina B6) 50mg")) {
                    def valorFloat = Double.parseDouble(totalMedicamentosDispesados[i][1].toString())
                    mmiaRegimenSubReport.cumunitaryClinic = (int) valorFloat
                }
            }
        }

        mmiaRegimenSubReports.add(mmiaRegimenSubReport)
    }

    @Override
    List<MmiaRegimenSubReport> getMMIARegimenStatistic(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)
        List<MmiaRegimenSubReport> mmiaRegimenSubReports = new ArrayList<>()

        String query = ""

        if (service.isTARV()) {
            query =
                    """
                    WITH latest_packs_by_patient AS (
    SELECT DISTINCT ON (pat.id)
        pat.id as patient_id,
        pat.date_of_birth,
        pk.id as pack_id,
        pk.pickup_date,
        cs.code as service_code,
        ep.id as episode_id,
        ssr.code as ssr_code
    FROM patient_visit_details pvd 
    INNER JOIN pack pk ON pk.id = pvd.pack_id
    INNER JOIN episode ep ON ep.id = pvd.episode_id
    INNER JOIN patient_visit pv ON pv.id = pvd.patient_visit_id
    INNER JOIN patient pat ON pat.id = pv.patient_id
    INNER JOIN patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
    INNER JOIN start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
    INNER JOIN clinical_service cs ON cs.id = psi.service_id
    WHERE pk.pickup_date BETWEEN :startDate AND :endDate
    AND (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR')
    ORDER BY pat.id, pk.pickup_date DESC, pk.id DESC
),
pack_details AS (
    SELECT 
        lp.patient_id,
        lp.pack_id,
        lp.ssr_code,
        package.isreferral,
        tr.code as tr_code,
        tr.regimen_scheme,
        tl.code as tl_code
    FROM latest_packs_by_patient lp
    INNER JOIN pack package ON package.id = lp.pack_id
    INNER JOIN patient_visit_details pvd ON pvd.pack_id = package.id
    INNER JOIN prescription p ON p.id = pvd.prescription_id
    INNER JOIN prescription_detail pd ON pd.prescription_id = p.id
    INNER JOIN therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
    LEFT JOIN therapeutic_line tl ON tl.id = pd.therapeutic_line_id
)
SELECT 
    tr_code as code,
    regimen_scheme,
    
    COUNT(CASE WHEN ssr_code NOT IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               THEN patient_id END) AS totadoentes,
    COUNT(CASE WHEN ssr_code NOT IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND (tl_code = '1' OR tl_code IS NULL) 
               THEN patient_id END) AS linhs1,
    COUNT(CASE WHEN ssr_code NOT IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '2' 
               THEN patient_id END) AS linha2,
    COUNT(CASE WHEN ssr_code NOT IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '3' 
               THEN patient_id END) AS linha3,
    COUNT(CASE WHEN ssr_code NOT IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '1_ALT' 
               THEN patient_id END) AS linhaAlt,
    
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               THEN patient_id END) AS totadoentesReferidos,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '1' 
               THEN patient_id END) AS linhsdc1,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '2' 
               THEN patient_id END) AS linhadc2,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '3' 
               THEN patient_id END) AS linhadc3,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND tl_code = '1_ALT' 
               THEN patient_id END) AS linhadcAlt,
    
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND isreferral = true 
               THEN patient_id END) AS totalReferidos,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND isreferral = true 
               AND tl_code = '1' 
               THEN patient_id END) AS totalrefline1,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND isreferral = true 
               AND tl_code = '2' 
               THEN patient_id END) AS totalrefline2,
    COUNT(CASE WHEN ssr_code IN ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') 
               AND isreferral = true 
               AND tl_code = '3' 
               THEN patient_id END) AS totalrefline3
FROM pack_details
GROUP BY tr_code, regimen_scheme
ORDER BY tr_code, regimen_scheme;
                    """
        } else {
            query =
                    """
                     select
                     tr.code,
                     tr.regimen_scheme,
                     count(CASE WHEN ssr.code <> 'REFERIDO_PARA' AND ssr.code <> 'VOLTOU_A_SER_REFERIDO_PARA' THEN 1 END) AS totadoentes,
                     count(CASE WHEN ssr.code = 'REFERIDO_PARA' OR ssr.code = 'VOLTOU_A_SER_REFERIDO_PARA' THEN 1 END) AS totadoentesReferidos,
                     count(CASE WHEN (ssr.code <> 'REFERIDO_PARA' AND ssr.code <> 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '1' THEN 1 END) AS linhs1,
                     count(CASE WHEN (ssr.code <> 'REFERIDO_PARA' AND ssr.code <> 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '2' THEN 1 END) AS linha2,
                     count(CASE WHEN (ssr.code <> 'REFERIDO_PARA' AND ssr.code <> 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '3' THEN 1 END) AS linha3,
                     count(CASE WHEN (ssr.code <> 'REFERIDO_PARA' AND ssr.code <> 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '1_ALT' THEN 1 END) AS linhaAlt,
                     count(CASE WHEN (ssr.code = 'REFERIDO_PARA' OR ssr.code = 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '1' THEN 1 END) AS linhsdc1,
                     count(CASE WHEN (ssr.code = 'REFERIDO_PARA' OR ssr.code = 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '2' THEN 1 END) AS linhadc2,
                     count(CASE WHEN (ssr.code = 'REFERIDO_PARA' OR ssr.code = 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '3' THEN 1 END) AS linhadc3,
                     count(CASE WHEN (ssr.code = 'REFERIDO_PARA' OR ssr.code = 'VOLTOU_A_SER_REFERIDO_PARA') AND tl.code = '1_ALT' THEN 1 END) AS linhadcAlt
                     FROM
                    (
                     select distinct pat.id,
                     max(pk.pickup_date) pickupdate,
                     max(pk.id) packid,
                     pat.date_of_birth,
                     cs.code service_code
                     from patient_visit_details pvd 
                     inner join pack pk on pk.id = pvd.pack_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join patient pat on pat.id = pv.patient_id
                     inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join clinical_service cs ON cs.id = psi.service_id
                     where ((Date(pk.pickup_date) BETWEEN :startDate AND :endDate) OR
                     pg_catalog.date(pk.pickup_date) < :startDate and pg_catalog.date(pk.next_pick_up_date) > :endDate 
                     AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC',
                                     'REFERIDO_PARA',
                                     'VOLTOU_A_SER_REFERIDO_PARA')
                     AND cs.code = :clinicalService
                     group by 1,4,5
                     order by 1
                    ) patientstatistics
                     inner join pack package on package.id = patientstatistics.packid
                     inner join patient_visit_details pvd on pvd.pack_id = package.id
                     inner join prescription p on p.id = pvd.prescription_id
                     inner join prescription_detail pd on pd.prescription_id = p.id
                     inner join therapeutic_line tl on tl.id = pd.therapeutic_line_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join dispense_type dt on dt.id = pd.dispense_type_id
                     inner join therapeutic_regimen tr on tr.id = pd.therapeutic_regimen_id
                     INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                     WHERE cs.code = :clinicalService
                     AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC',
                                     'REFERIDO_PARA',
                                     'VOLTOU_A_SER_REFERIDO_PARA')
                     GROUP BY 1,2
                    """
        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            for (int i = 0; i < list.size(); i++) {
                addMmiaRegimenStatisticInList(list[i], mmiaRegimenSubReports)
            }
            return mmiaRegimenSubReports
        }
    }

    def addLinhaUsadaInList(Object item, List<LinhasUsadasReport> linhasUsadasReports) {
        LinhasUsadasReport linhasUsadasReport = new LinhasUsadasReport()
        linhasUsadasReport.setCodigoRegime(String.valueOf(item[0]))
        linhasUsadasReport.setRegimeTerapeutico(String.valueOf(item[1]))
        linhasUsadasReport.setLinhaTerapeutica(String.valueOf(item[2]))
        linhasUsadasReport.setEstado(String.valueOf(item[3]))
        linhasUsadasReport.setTotalPrescricoes(Integer.valueOf(String.valueOf(item[4])))
        linhasUsadasReports.add(linhasUsadasReport)
    }

    def addSegundasLinhasInList(Object item, List<SegundasLinhasReport> segundasLinhasReports) {
        SegundasLinhasReport segundasLinhasReport = new SegundasLinhasReport()
        segundasLinhasReport.setCodigoRegime(String.valueOf(item[0]))
        segundasLinhasReport.setRegimeTerapeutico(String.valueOf(item[1]))
        segundasLinhasReport.setLinhaTerapeutica(String.valueOf(item[2]))
        segundasLinhasReport.setEstado(String.valueOf(item[3]))
        segundasLinhasReport.setTotalPrescricoes(Integer.valueOf(String.valueOf(item[4])))
        segundasLinhasReports.add(segundasLinhasReport)
    }

    def addBalanceteInList(Object item, List<BalanceteReport> balanceteReports) {
        BalanceteReport balanceteReport = new BalanceteReport()
        balanceteReport.setFnm(String.valueOf(item[0]))
        balanceteReport.setMedicamento(String.valueOf(item[1]))
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd")
            Date eventDate = inputFormat.parse(String.valueOf(item[2]))
            balanceteReport.setDiaDoEvento(eventDate)

            balanceteReport.setEntradas((int) Math.round(Double.valueOf(String.valueOf(item[3]))))
            balanceteReport.setSaidas((int) Math.round(Double.valueOf(String.valueOf(item[4]))))
            int perdasEAjustes =
                    (int) Math.round(Double.valueOf(String.valueOf(item[7]))) - (int) Math.round(Double.valueOf(String.valueOf(item[5])))
                    (int) Math.round(Double.valueOf(String.valueOf(item[8])))

            balanceteReport.setPerdasEAjustes(perdasEAjustes)
            balanceteReport.setStockExistente((int) Math.round(Double.valueOf(String.valueOf(item[6]))))
            balanceteReport.setUnidade(String.valueOf(item[9]))

            Date expireDate = inputFormat.parse(String.valueOf(item[10]))
            balanceteReport.setValidadeMedicamento(expireDate)

            balanceteReport.setNotas(String.valueOf(item[11]))
        } catch (ParseException e) {
            e.printStackTrace();
        }
        balanceteReports.add(balanceteReport)
    }

    @Override
    List<LinhasUsadasReport> getLinhasUsadas(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)
        List<LinhasUsadasReport> linhasUsadasReports = new ArrayList<>()

        String query = ""

        if (service.isTARV()) {
            query =
                    """
                    WITH unique_prescriptions AS (
                        SELECT DISTINCT
                            pd.id AS prescription_detail_id,
                            pd.prescription_id as prescription_id,
                            pd.therapeutic_regimen_id,
                            pd.therapeutic_line_id,
                            p.prescription_date
                        FROM
                            prescription_detail pd
                        INNER JOIN
                            prescription p ON pd.prescription_id = p.id
                        INNER JOIN
                            patient_visit_details pvd ON pvd.prescription_id = p.id
                        WHERE
                            p.prescription_date BETWEEN :startDate AND :endDate
                    )
                    SELECT
                        tr.code AS regime_code,
                        tr.description AS regime_description,
                        tl.description AS line_description,
                        tr.active AS regime_status,
                        COUNT(up.prescription_detail_id) AS total_prescriptions
                    FROM
                        unique_prescriptions up
                    INNER JOIN
                        therapeutic_regimen tr ON up.therapeutic_regimen_id = tr.id
                    INNER JOIN
                        therapeutic_line tl ON up.therapeutic_line_id = tl.id
                    INNER JOIN
                        patient_visit_details pvd ON pvd.prescription_id = up.prescription_id
                    INNER JOIN
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    WHERE
                        cs.code = 'TARV'
                    GROUP BY
                        tr.code, tr.description, tl.description, tr.active
                    ORDER BY
                        tr.code, tl.description;
                    """
        } else {
            // Futuramente
        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            for (int i = 0; i < list.size(); i++) {
                addLinhaUsadaInList(list[i], linhasUsadasReports)
            }
            return linhasUsadasReports
        }
    }

    @Override
    List<SegundasLinhasReport> getSegundasLinhas(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)
        List<SegundasLinhasReport> segundasLinhasReports = new ArrayList<>()

        String query = ""

        if (service.isTARV()) {
            query =
                    """
                     WITH unique_prescriptions AS (
                        SELECT DISTINCT
                            pd.id AS prescription_detail_id,
                            pd.prescription_id as prescription_id,
                            pd.therapeutic_regimen_id,
                            pd.therapeutic_line_id,
                            p.prescription_date
                        FROM
                            prescription_detail pd
                        INNER JOIN
                            prescription p ON pd.prescription_id = p.id
                        INNER JOIN
                            patient_visit_details pvd ON pvd.prescription_id = p.id
                        INNER JOIN 
                            clinic clinic ON pvd.clinic_id = :clinic
                        WHERE
                            p.prescription_date BETWEEN :startDate AND :endDate  
                    )
                    SELECT
                        tr.code AS regime_code,
                        tr.description AS regime_description,
                        tl.description AS line_description,
                        tr.active AS regime_status,
                        COUNT(up.prescription_detail_id) AS total_prescriptions
                    FROM
                        unique_prescriptions up
                    INNER JOIN
                        therapeutic_regimen tr ON up.therapeutic_regimen_id = tr.id
                    INNER JOIN
                        therapeutic_line tl ON up.therapeutic_line_id = tl.id
                    INNER JOIN
                        patient_visit_details pvd ON pvd.prescription_id = up.prescription_id
                     INNER JOIN
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    WHERE
                        cs.code = 'TARV' and tl.code = '2'
                    GROUP BY
                        tr.code, tr.description, tl.description, tr.active
                    ORDER BY
                        tr.code, tl.description;
                    """
        } else {
            // Futuramente
        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            for (int i = 0; i < list.size(); i++) {
                addSegundasLinhasInList(list[i], segundasLinhasReports)
            }
            return segundasLinhasReports
        }
    }

    @Override
    List<BalanceteReport> getBalanceteReport(ClinicalService service, Clinic clinic, Date startDate, Date endDate){

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)
        List<BalanceteReport> balanceteReports = new ArrayList<>()

        String query = ""

        if (service.isTARV()) {
            query =
                    """
                     WITH 
                        entrada AS (
                            SELECT 
                                s.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(se.date_received) AS event_date,
                                SUM(CEIL(s.units_received::double precision)) AS incomes,
                                0 AS outcomes,
                                0 AS positiveadjustment,
                                0 AS negativeadjustment,
                                0 AS losses,
                                0 AS stock,
                                s.expire_date,
                                COALESCE(se.notes, '') AS notes
                            FROM 
                                stock_entrance se
                            JOIN 
                                stock s ON se.id::text = s.entrance_id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            WHERE
                                date(se.date_received) >= :startDate AND date(se.date_received) <= :endDate
                            GROUP BY 
                                date(se.date_received), s.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id, se.notes
                            ORDER BY 
                                date(se.date_received) DESC
                        ),
                        saida AS (
                            SELECT 
                                pd.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(p.pickup_date) AS event_date,
                                0 AS incomes,
                                SUM(CEIL(pd.quantity_supplied)) AS outcomes,
                                0 AS positiveadjustment,
                                0 AS negativeadjustment,
                                0 AS losses,
                                0 AS stock,
                                s.expire_date,
                                '' AS notes
                            FROM 
                                packaged_drug pd
                            JOIN 
                                packaged_drug_stock pds ON pd.id::text = pds.packaged_drug_id::text
                            JOIN 
                                stock s ON s.id::text = pds.stock_id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            JOIN 
                                pack p ON p.id::text = pd.pack_id::text
                            WHERE
                                date(p.pickup_date) >= :startDate AND date(p.pickup_date) <= :endDate
                            GROUP BY 
                                date(p.pickup_date), pd.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id
                            ORDER BY 
                                date(p.pickup_date) DESC
                        ),
                        ajuste_positivo AS (
                            SELECT 
                                s.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(rsm.date) AS event_date,
                                0 AS incomes,
                                0 AS outcomes,
                                SUM(sa.adjusted_value) AS positiveadjustment,
                                0 AS negativeadjustment,
                                0 AS losses,
                                0 AS stock,
                                s.expire_date,
                                COALESCE(sa.notes, '') AS notes
                            FROM 
                                stock_adjustment sa
                            JOIN 
                                refered_stock_moviment rsm ON sa.reference_id::text = rsm.id::text
                            JOIN 
                                stock s ON sa.adjusted_stock_id::text = s.id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            JOIN 
                                stock_operation_type sot ON sa.operation_id::text = sot.id::text
                            WHERE 
                                sot.code = 'AJUSTE_POSETIVO' AND date(rsm.date) >= :startDate AND date(rsm.date) <= :endDate
                            GROUP BY 
                                date(rsm.date), s.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id, sa.notes
                            ORDER BY 
                                date(rsm.date) DESC
                        ),
                        ajuste_negativo AS (
                            SELECT 
                                s.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(rsm.date) AS event_date,
                                0 AS incomes,
                                0 AS outcomes,
                                0 AS positiveadjustment,
                                SUM(CEIL(sa.adjusted_value::double precision)) AS negativeadjustment,
                                0 AS losses,
                                0 AS stock,
                                s.expire_date,
                                COALESCE(sa.notes, '') AS notes
                            FROM 
                                stock_adjustment sa
                            JOIN 
                                refered_stock_moviment rsm ON sa.reference_id::text = rsm.id::text
                            JOIN 
                                stock s ON sa.adjusted_stock_id::text = s.id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            JOIN 
                                stock_operation_type sot ON sa.operation_id::text = sot.id::text
                            WHERE 
                                sot.code = 'AJUSTE_NEGATIVO' AND date(rsm.date) >= :startDate AND date(rsm.date) <= :endDate
                            GROUP BY 
                                date(rsm.date), s.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id, sa.notes
                            ORDER BY 
                                date(rsm.date) DESC
                        ),
                        perda AS (
                            SELECT 
                                s.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(ds.date) AS event_date,
                                0 AS incomes,
                                0 AS outcomes,
                                0 AS positiveadjustment,
                                0 AS negativeadjustment,
                                SUM(CEIL(sa.adjusted_value::double precision)) AS losses,
                                0 AS stock,
                                s.expire_date,
                                COALESCE(sa.notes, '') AS notes
                            FROM 
                                stock_adjustment sa
                            JOIN 
                                destroyed_stock ds ON sa.destruction_id::text = ds.id::text
                            JOIN 
                                stock s ON sa.adjusted_stock_id::text = s.id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            WHERE
                                date(ds.date) >= :startDate AND date(ds.date) <= :endDate
                            GROUP BY 
                                date(ds.date), s.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id, sa.notes
                            ORDER BY 
                                date(ds.date) DESC
                        ),
                        inventario AS (
                            SELECT 
                                s.drug_id,
                                d.fnm_code,
                                d.name,
                                d.pack_size,
                                date(i.end_date) AS event_date,
                                0 AS incomes,
                                0 AS outcomes,
                                SUM(
                                    CASE
                                        WHEN sot.code = 'AJUSTE_POSETIVO' THEN sa.adjusted_value
                                        ELSE 0
                                    END
                                ) AS positiveadjustment,
                                SUM(
                                    CASE
                                        WHEN sot.code = 'AJUSTE_NEGATIVO' THEN sa.adjusted_value
                                        ELSE 0
                                    END
                                ) AS negativeadjustment,
                                0 AS losses,
                                0 AS stock,
                                s.expire_date,
                                COALESCE(sa.notes, '') AS notes
                            FROM 
                                stock_adjustment sa
                            JOIN 
                                inventory i ON sa.inventory_id::text = i.id::text
                            JOIN 
                                stock s ON sa.adjusted_stock_id::text = s.id::text
                            JOIN 
                                drug d ON s.drug_id = d.id
                            JOIN 
                                stock_operation_type sot ON sa.operation_id::text = sot.id::text
                            WHERE 
                                i.end_date IS NOT NULL AND date(i.end_date) >= :startDate AND date(i.end_date) <= :endDate
                            GROUP BY 
                                date(i.end_date), s.drug_id, d.fnm_code, d.name, d.pack_size, s.expire_date, s.id, sa.notes
                            ORDER BY 
                                date(i.end_date) DESC
                        ),
                        stock_acumulado AS (
                            SELECT
                                s.drug_id,
                                SUM(CASE WHEN se.date_received < :startDate THEN CEIL(s.units_received::double precision) ELSE 0 END) AS total_entrada_ate_start,
                                SUM(CASE WHEN rsm.date < :startDate AND sot.code = 'AJUSTE_POSETIVO' THEN sa.adjusted_value ELSE 0 END) AS total_ajuste_positivo_ate_start,
                                SUM(CASE WHEN p.pickup_date < :startDate THEN CEIL(pd.quantity_supplied) ELSE 0 END) AS total_saida_ate_start,
                                SUM(CASE WHEN rsm.date < :startDate AND sot.code = 'AJUSTE_NEGATIVO' THEN sa.adjusted_value ELSE 0 END) AS total_ajuste_negativo_ate_start
                            FROM
                                stock s
                            LEFT JOIN stock_entrance se ON se.id::text = s.entrance_id::text
                            LEFT JOIN packaged_drug_stock pds ON s.id::text = pds.stock_id::text
                            LEFT JOIN packaged_drug pd ON pd.id::text = pds.packaged_drug_id::text
                            LEFT JOIN pack p ON p.id::text = pd.pack_id::text
                            LEFT JOIN stock_adjustment sa ON sa.adjusted_stock_id::text = s.id::text
                            LEFT JOIN refered_stock_moviment rsm ON sa.reference_id::text = rsm.id::text
                            LEFT JOIN stock_operation_type sot ON sa.operation_id::text = sot.id::text
                            WHERE
                                se.date_received < :startDate OR rsm.date < :startDate OR p.pickup_date < :startDate
                            GROUP BY
                                s.drug_id
                        )
                        SELECT DISTINCT ON (event_date, fnm_code, name, incomes, outcomes, losses, stock, positiveadjustment, negativeadjustment)
                            fnm_code,
                            name,
                            event_date,
                            incomes,
                            outcomes,
                            losses,
                            stock_acumulado.total_entrada_ate_start + stock_acumulado.total_ajuste_positivo_ate_start
                            - stock_acumulado.total_saida_ate_start - stock_acumulado.total_ajuste_negativo_ate_start  AS stock,
                        --    + (incomes + positiveadjustment - outcomes - negativeadjustment - losses) AS stock,
                            positiveadjustment,
                            negativeadjustment,
                            pack_size,
                            expire_date,
                            STRING_AGG(notes, ' ') AS notes
                        FROM (
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                entrada
                            UNION ALL
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                saida
                            UNION ALL
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                ajuste_positivo
                            UNION ALL
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                ajuste_negativo
                            UNION ALL
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                perda
                            UNION ALL
                            SELECT 
                                drug_id,
                                fnm_code,
                                name,
                                event_date,
                                incomes,
                                outcomes,
                                losses,
                                stock,
                                positiveadjustment,
                                negativeadjustment,
                                pack_size,
                                expire_date,
                                notes
                            FROM 
                                inventario
                        ) AS combined
                        JOIN stock_acumulado ON combined.drug_id = stock_acumulado.drug_id
                        WHERE 
                            event_date >= :startDate AND event_date <= :endDate
                        GROUP BY
                            fnm_code,
                            name,
                            event_date,
                            incomes,
                            outcomes,
                            losses,
                            positiveadjustment,
                            negativeadjustment,
                            pack_size,
                            expire_date,
                            stock_acumulado.total_entrada_ate_start,
                            stock_acumulado.total_ajuste_positivo_ate_start,
                            stock_acumulado.total_saida_ate_start,
                            stock_acumulado.total_ajuste_negativo_ate_start
                        ORDER BY 
                            event_date;
                    """
        } else {
            // Futuramente
        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            for (int i = 0; i < list.size(); i++) {
                addBalanceteInList(list[i], balanceteReports)
            }
            return balanceteReports
        }
    }


    def addMmiaRegimenStatisticInList(Object item, List<MmiaRegimenSubReport> mmiaRegimenSubReports) {
        MmiaRegimenSubReport mmiaRegimenSubReport = new MmiaRegimenSubReport()
        mmiaRegimenSubReport.code = String.valueOf(item[0])
        mmiaRegimenSubReport.regimen = String.valueOf(item[1])

        mmiaRegimenSubReport.totalPatients = Integer.valueOf(String.valueOf(item[2]))
        mmiaRegimenSubReport.totalline1 = Integer.valueOf(String.valueOf(item[3]))
        mmiaRegimenSubReport.totalline2 = Integer.valueOf(String.valueOf(item[4]))
        mmiaRegimenSubReport.totalline3 = Integer.valueOf(String.valueOf(item[5]))
        mmiaRegimenSubReport.totalline4 = Integer.valueOf(String.valueOf(item[6]))

        mmiaRegimenSubReport.cumunitaryClinic = Integer.valueOf(String.valueOf(item[7]))
        mmiaRegimenSubReport.totaldcline1 = Integer.valueOf(String.valueOf(item[8]))
        mmiaRegimenSubReport.totaldcline2 = Integer.valueOf(String.valueOf(item[9]))
        mmiaRegimenSubReport.totaldcline3 = Integer.valueOf(String.valueOf(item[10]))
        mmiaRegimenSubReport.totaldcline4 = Integer.valueOf(String.valueOf(item[11]))

        mmiaRegimenSubReport.line1 = TherapeuticLine.findByCode("1").code
        mmiaRegimenSubReport.line2 = TherapeuticLine.findByCode("2").code
        mmiaRegimenSubReport.line3 = TherapeuticLine.findByCode("3").code
        mmiaRegimenSubReport.line4 = TherapeuticLine.findByCode("1_ALT").code

        mmiaRegimenSubReport.line = ""
        mmiaRegimenSubReport.lineCode = ""
        mmiaRegimenSubReport.totalReferidos = Integer.valueOf(String.valueOf(item[12]))
        mmiaRegimenSubReport.totalrefline1 = Integer.valueOf(String.valueOf(item[13]))
        mmiaRegimenSubReport.totalrefline2 = Integer.valueOf(String.valueOf(item[14]))
        mmiaRegimenSubReport.totalrefline3 = Integer.valueOf(String.valueOf(item[15]))
        mmiaRegimenSubReports.add(mmiaRegimenSubReport)
    }

    @Override
    Object getMMIADispenseTypeStatisticOnPeriod(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {
        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)

        String query = ""
        if (service.isTARV()) {
            query = """
                    SELECT 
                     COUNT
                     (
                     CASE 
                     WHEN dt.code = 'DM'
                     THEN 1 
                     END
                     ) AS DM,
                     COUNT 
                     (
                     CASE 
                     WHEN dt.code = 'DT'
                     THEN 1 
                     END
                     ) AS DT,
                     COUNT
                     (
                     CASE 
                     WHEN dt.code = 'DS' OR (dt.code != 'DB' AND dt.code != 'DM' AND dt.code != 'DT' AND dt.code != 'DS')
                     THEN 1  
                     END
                     ) AS DS,
                     COUNT
                     (
                     CASE 
                     WHEN dt.code = 'DB'
                     THEN 1  
                     END
                     ) AS DB
                     FROM
                     (
                     select distinct pat.id,
                     max(pk.pickup_date) pickupdate,
                     max(pk.id) packid,
                     pat.date_of_birth,
                     cs.code service_code
                     from patient_visit_details pvd 
                     inner join pack pk on pk.id = pvd.pack_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join patient pat on pat.id = pv.patient_id
                     inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join clinical_service cs ON cs.id = psi.service_id
                     where pk.pickup_date::date >=:startDate AND pk.pickup_date::date <= :endDate
                     AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC')
                     AND (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR')
                     group by 1,4,5
                     order by 1
                     ) patientstatistics
                     inner join pack package on package.id = patientstatistics.packid
                     inner join patient_visit_details pvd on pvd.pack_id = package.id
                     inner join prescription p on p.id = pvd.prescription_id
                     inner join prescription_detail pd on pd.prescription_id = p.id
                     inner join therapeutic_line tl on tl.id = pd.therapeutic_line_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join dispense_type dt on dt.id = pd.dispense_type_id
                     inner join therapeutic_regimen tr on tr.id = pd.therapeutic_regimen_id
                     INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                     where (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR')
        
                """

        } else {
            query = """
                    select 
                     COUNT
                     (
                     CASE 
                     WHEN patientstatistics.service_code = :clinicalService 
                     AND dt.code = 'DM'
                     THEN 1 
                     END
                     ) AS DM,
                     COUNT 
                     (
                     CASE 
                     WHEN patientstatistics.service_code = :clinicalService 
                     AND dt.code = 'DT'
                     THEN 1 
                     END
                     ) AS DT,
                     COUNT
                     (
                     CASE 
                     WHEN patientstatistics.service_code = :clinicalService 
                     AND dt.code = 'DS'
                     THEN 1  
                     END
                     ) AS DS,
                     COUNT
                     (
                     CASE 
                     WHEN patientstatistics.service_code = :clinicalService 
                     AND dt.code = 'DB'
                     THEN 1  
                     END
                     ) AS DB
                     FROM
                     (
                     select distinct pat.id,
                     max(pk.pickup_date) pickupdate,
                     max(pk.id) packid,
                     pat.date_of_birth,
                     cs.code service_code
                     from patient_visit_details pvd 
                     inner join pack pk on pk.id = pvd.pack_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join patient pat on pat.id = pv.patient_id
                     inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join clinical_service cs ON cs.id = psi.service_id
                     where pk.pickup_date::date >=:startDate AND pk.pickup_date::date <= :endDate
                    AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC')
                     AND cs.code = :clinicalService
                     group by 1,4,5
                     order by 1
                     ) patientstatistics
                     inner join pack package on package.id = patientstatistics.packid
                     inner join patient_visit_details pvd on pvd.pack_id = package.id
                     inner join prescription p on p.id = pvd.prescription_id
                     inner join prescription_detail pd on pd.prescription_id = p.id
                     inner join therapeutic_line tl on tl.id = pd.therapeutic_line_id
                     inner join episode ep on ep.id = pvd.episode_id
                     inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                     inner join patient_visit pv on pv.id = pvd.patient_visit_id
                     inner join dispense_type dt on dt.id = pd.dispense_type_id
                     inner join therapeutic_regimen tr on tr.id = pd.therapeutic_regimen_id
                     INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                """

        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            return list[0]
        }

        return null
    }


    @Override
    MmiaReport getMMIAPatientStatisticOnPeriod(MmiaReport mmiaReport, ClinicalService service, Clinic clinic, Date startDate, Date endDate) {

        def starter = new java.sql.Date(startDate.time)
        def finalDate = new java.sql.Date(endDate.time)
        def params = [startDate: starter, endDate: finalDate, clinic: clinic.id, clinicalService: service.code]
        def sql = new Sql(dataSource as DataSource)
        String query = ''

        if (service.isTARV()) {
            query =
            """
                    SELECT  
                    COUNT (
                        CASE  
                            WHEN CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 0 AND 4    
                            THEN 1  
                        END
                    ) AS zeroquatro,        
                    COUNT (
                        CASE  
                            WHEN CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 5 AND 9  
                            THEN 1  
                        END
                    ) AS cinconove,        
                    COUNT (
                        CASE  
                            WHEN CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 10 AND 14  
                            THEN 1  
                        END
                    ) AS dezcatorze,        
                    COUNT (
                        CASE  
                            WHEN CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) >= 15 
                            THEN 1  
                        END
                    ) AS quinzemais, 
                    COUNT (
                        CASE  
                            WHEN ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio') 
                            AND ssr.code = 'NOVO_PACIENTE'
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS novos,        
                    COUNT (
                        CASE  WHEN (ssr.code  <>'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE') AND
                            (
                                (ssr.code = 'NOVO_PACIENTE' AND DATE(ep.episode_date + INTERVAL '3 days') < DATE(pack.pickup_date)) 
                                OR (ssr.code = 'ALTERACAO' AND DATE(ep.episode_date + INTERVAL '3 days') < DATE(pack.pickup_date)) 
                                OR (ssr.code = 'TRANSFERIDO_DE' AND DATE(ep.episode_date + INTERVAL '3 days') < Date(pack.pickup_date)) 
                                OR (
                                    ssr.code = 'MANUNTENCAO' 
                                    OR ssr.code = 'VOLTOU_REFERENCIA' 
                                    OR ssr.code = 'REINICIO_TRATAMETO' 
                                    OR ssr.code = 'REFERIDO_DC' 
                                    OR ssr.code = 'OUTRO' 
                                    OR ssr.code = 'INICIO_CCR'
                                )
                            )
                            THEN 1  
                        END
                    ) AS manutencao,        
                    COUNT (
                        CASE  
                            WHEN ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE' 
                            AND ssr.code = 'ALTERACAO' 
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS alteracao,        
                    COUNT (
                        CASE  
                            WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                            THEN 1  
                        END
                    ) AS transito, 
                    COUNT (
                        CASE  
                            WHEN ssr.code = 'TRANSFERIDO_DE'  
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS transferencia,     
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = 'PREP'  
                            THEN 1  
                        END
                    ) AS PREP,
                    
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = 'PPE'  
                            THEN 1  
                        END
                    ) AS PPE,
                    
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = 'CE' OR patientstatistics.service_code = 'CCR'    
                            THEN 1  
                        END
                    ) AS CE,
                    
                    COUNT (
                        CASE  
                            WHEN  dt.code = 'DM'  
                            THEN 1  
                        END
                    ) AS DM,
                    
                    COUNT (
                        CASE  
                            WHEN dt.code = 'DT'  
                            THEN 1  
                        END
                    ) AS DT,
                    COUNT (
                        CASE  
                            WHEN dt.code = 'DS' OR (dt.code != 'DB' AND dt.code != 'DM' AND dt.code != 'DT' AND dt.code != 'DS')
                            THEN 1  
                        END
                    ) AS DS,
                    COUNT (
                        CASE  
                            WHEN dt.code = 'DB'  
                            THEN 1  
                        END
                    ) AS DB
                    FROM 
                    (
                        SELECT DISTINCT 
                            pat.id,
                            MAX(pk.pickup_date) AS pickupdate,
                            MAX(pk.id) AS packid,
                            pat.date_of_birth,
                            cs.code AS service_code
                        FROM 
                            patient_visit_details pvd
                        INNER JOIN 
                            pack pk ON pk.id = pvd.pack_id
                        INNER JOIN 
                            episode ep ON ep.id = pvd.episode_id
                        INNER JOIN 
                            patient_visit pv ON pv.id = pvd.patient_visit_id
                        INNER JOIN 
                            patient pat ON pat.id = pv.patient_id
                        INNER JOIN 
                            patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                        INNER JOIN 
                            start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                        INNER JOIN 
                            clinical_service cs ON cs.id = psi.service_id
                        INNER JOIN
                            clinic c ON c.id = ep.clinic_id
                        WHERE 
                            ((Date(pk.pickup_date) BETWEEN :startDate AND :endDate))
                            AND ssr.code in (select code from start_stop_reason)
                            AND (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR') 
                        GROUP BY 
                            1,4,5
                        ORDER BY 
                            1
                    ) patientstatistics
                    INNER JOIN 
                        pack pack ON pack.id = patientstatistics.packid
                    INNER JOIN 
                        patient_visit_details pvd ON pvd.pack_id = pack.id
                    INNER JOIN 
                        prescription p ON p.id = pvd.prescription_id
                    INNER JOIN 
                        prescription_detail pd ON pd.prescription_id = p.id
                    LEFT JOIN 
                        therapeutic_line tl ON tl.id = pd.therapeutic_line_id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        dispense_type dt ON dt.id = pd.dispense_type_id
                    INNER JOIN 
                        therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    WHERE (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR')
            """
        } else {
            query =
                    """
                    SELECT  
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 0 AND 4    
                            THEN 1  
                        END
                    ) AS zeroquatro,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 5 AND 9  
                            THEN 1  
                        END
                    ) AS cinconove,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService 
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) BETWEEN 10 AND 14  
                            THEN 1  
                        END
                    ) AS dezcatorze,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND CAST (extract(year FROM age(:endDate, patientstatistics.date_of_birth)) AS INTEGER) >= 15 
                            THEN 1  
                        END
                    ) AS quinzemais, 
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND (p.patient_type = 'N/A' OR p.patient_type IS null OR p.patient_type = 'Inicio') 
                            AND dt.code = 'DM'
                            AND ssr.code = 'NOVO_PACIENTE'
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS novos,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND (
                                (ssr.code = 'NOVO_PACIENTE' AND DATE(ep.episode_date + INTERVAL '3 days') < DATE(pack.pickup_date)) 
                                OR (ssr.code = 'ALTERACAO' AND DATE(ep.episode_date + INTERVAL '3 days') < DATE(pack.pickup_date)) 
                                OR (ssr.code = 'TRANSFERIDO_DE' AND DATE(ep.episode_date + INTERVAL '3 days') < Date(pack.pickup_date)) 
                                OR (
                                    ssr.code = 'MANUNTENCAO' 
                                    OR ssr.code = 'ABANDONO' 
                                    OR ssr.code = 'VOLTOU_REFERENCIA' 
                                    OR ssr.code = 'REINICIO_TRATAMETO' 
                                    OR ssr.code = 'REFERIDO_DC' 
                                    OR ssr.code = 'OUTRO' 
                                    OR ssr.code = 'INICIO_CCR'
                                )
                            )
                            THEN 1  
                        END
                    ) AS manutencao,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND ssr.code = 'ALTERACAO' 
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS alteracao,        
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService 
                            AND (ssr.code = 'TRANSITO'  OR ssr.code = 'INICIO_MATERNIDADE')
                            THEN 1  
                        END
                    ) AS transito, 
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND ssr.code = 'TRANSFERIDO_DE'  
                            AND extract(days from pack.pickup_date - ep.episode_date) <= 3  
                            THEN 1  
                        END
                    ) AS transferencia,
                    
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND dt.code = 'DM'  
                            THEN 1  
                        END
                    ) AS DM,
                    
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND dt.code = 'DT'  
                            THEN 1  
                        END
                    ) AS DT,
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND dt.code = 'DS'  
                            THEN 1  
                        END
                    ) AS DS,
                    COUNT (
                        CASE  
                            WHEN patientstatistics.service_code = :clinicalService
                            AND ssr.code <> 'TRANSITO' AND ssr.code <> 'INICIO_MATERNIDADE'
                            AND dt.code = 'DB'  
                            THEN 1  
                        END
                    ) AS DB
                FROM 
                (
                    SELECT DISTINCT 
                        pat.id,
                        MAX(pk.pickup_date) AS pickupdate,
                        MAX(pk.id) AS packid,
                        pat.date_of_birth,
                        cs.code AS service_code
                    FROM 
                        patient_visit_details pvd
                    INNER JOIN 
                        pack pk ON pk.id = pvd.pack_id
                    INNER JOIN 
                        episode ep ON ep.id = pvd.episode_id
                    INNER JOIN 
                        patient_visit pv ON pv.id = pvd.patient_visit_id
                    INNER JOIN 
                        patient pat ON pat.id = pv.patient_id
                    INNER JOIN 
                        patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                    INNER JOIN 
                        start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                    INNER JOIN 
                        clinical_service cs ON cs.id = psi.service_id
                    INNER JOIN
                        clinic c ON c.id = ep.clinic_id
                    WHERE 
                        ((Date(pk.pickup_date) BETWEEN :startDate AND :endDate) OR
                        pg_catalog.date(pk.pickup_date) < :startDate and pg_catalog.date(pk.next_pick_up_date) > :endDate AND
                        DATE(pk.pickup_date + (INTERVAL '1 month'* cast (date_part('day',  cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) >= :startDate
                        AND DATE(pk.pickup_date + (INTERVAL '1 month'*cast (date_part('day', cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) <= :endDate)
                        AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC',
                                     'TRANSITO',
                                     'INICIO_MATERNIDADE')
                        AND cs.code = :clinicalService 
                    GROUP BY 
                        1,4,5
                    ORDER BY 
                        1
                ) patientstatistics
                INNER JOIN 
                    pack pack ON pack.id = patientstatistics.packid
                INNER JOIN 
                    patient_visit_details pvd ON pvd.pack_id = pack.id
                INNER JOIN 
                    prescription p ON p.id = pvd.prescription_id
                INNER JOIN 
                    prescription_detail pd ON pd.prescription_id = p.id
                INNER JOIN 
                    therapeutic_line tl ON tl.id = pd.therapeutic_line_id
                INNER JOIN 
                    episode ep ON ep.id = pvd.episode_id
                INNER JOIN 
                    start_stop_reason ssr ON ssr.id = ep.start_stop_reason_id
                INNER JOIN 
                    patient_visit pv ON pv.id = pvd.patient_visit_id
                INNER JOIN 
                    dispense_type dt ON dt.id = pd.dispense_type_id
                INNER JOIN 
                    therapeutic_regimen tr ON tr.id = pd.therapeutic_regimen_id
                INNER JOIN 
                    patient_service_identifier psi ON psi.id = ep.patient_service_identifier_id
                 INNER JOIN 
                    clinical_service cs ON cs.id = psi.service_id
                WHERE 
                    cs.code = :clinicalService
                    AND ssr.code in ('NOVO_PACIENTE',
                                     'INICIO_CCR',
                                     'TRANSFERIDO_DE',
                                     'REINICIO_TRATAMETO',
                                     'MANUNTENCAO',
                                     'OUTRO',
                                     'VOLTOU_REFERENCIA', 
                                     'REFERIDO_DC',
                                     'TRANSITO',
                                     'INICIO_MATERNIDADE')
                    """
        }

        def list = sql.rows(query, params)

        if (Utilities.listHasElements(list as ArrayList<?>)) {
            Object item = list[0]
            mmiaReport.totalPacientes04 = Integer.valueOf(String.valueOf(item[0]))
            mmiaReport.totalPacientes59 = Integer.valueOf(String.valueOf(item[1]))
            mmiaReport.totalPacientes1014 = Integer.valueOf(String.valueOf(item[2]))
            mmiaReport.totalPacientesAdulto = Integer.valueOf(String.valueOf(item[3]))
            mmiaReport.totalPacientesInicio = Integer.valueOf(String.valueOf(item[4]))
            mmiaReport.totalPacientesManter = Integer.valueOf(String.valueOf(item[5]))
            mmiaReport.totalPacientesAlterar = Integer.valueOf(String.valueOf(item[6]))
            mmiaReport.totalPacientesTransito = Integer.valueOf(String.valueOf(item[7]))
            mmiaReport.totalPacientesTransferidoDe = Integer.valueOf(String.valueOf(item[8]))
            mmiaReport.totalPacientesPREP = Integer.valueOf(String.valueOf(item[9]))
            mmiaReport.totalPacientesPPE = Integer.valueOf(String.valueOf(item[10]))
            mmiaReport.totalpacientesCE = Integer.valueOf(String.valueOf(item[11]))
            mmiaReport.dM = Integer.valueOf(String.valueOf(item[12]))
            mmiaReport.dtM0 = Integer.valueOf(String.valueOf(item[13]))
            mmiaReport.dsM0 = Integer.valueOf(String.valueOf(item[14]))
            mmiaReport.dbM0 = Integer.valueOf(String.valueOf(item[15]))
        }

        return mmiaReport
    }

    @Override
    List<Pack> getPacksByServiceOnPeriod(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {
        List<Pack> packList = new ArrayList<>()
        def sqlPacks = ""
        if (service.isTARV()) {
            sqlPacks =
                    """
                        select pk 
                        from PatientVisitDetails as pvd 
                        inner join  pvd.pack as pk  
                        inner join pvd.episode as ep 
                        where pk.pickupDate >= :startDate 
                               and pk.pickupDate <= :endDate 
                               and pk.clinic= :clinic 
                               and (ep.patientServiceIdentifier.service.code = :serviceCode OR ep.patientServiceIdentifier.service.code = :servicePrepCode)
                    """
            packList = Pack.executeQuery(sqlPacks,
                    [startDate: startDate, endDate: endDate, serviceCode: service.getCode(), servicePrepCode: 'PREP', clinic: clinic])
        } else {
            sqlPacks =
                    """
                        select pk 
                        from PatientVisitDetails as pvd 
                        inner join  pvd.pack as pk  
                        inner join pvd.episode as ep 
                        where pk.pickupDate >= :startDate 
                               and pk.pickupDate <= :endDate 
                               and pk.clinic= :clinic 
                               and ep.patientServiceIdentifier.service.code = :serviceCode
                    """
            packList = Pack.executeQuery(sqlPacks,
                    [startDate: startDate, endDate: endDate, serviceCode: service.getCode(), clinic: clinic])
        }

        return packList
    }

    @Override
    int countPacksByServiceOnPeriod(ClinicalService service, Clinic clinic, Date startDate, Date endDate) {
        int value = 0
        def sqlPackOnService =
                """
                select count(*) 
                from PatientVisitDetails as pvd
                inner join  pvd.pack as pk  
                inner join pvd.episode as ep 
                inner join ep.startStopReason as rsn 
                inner join pvd.patientVisit as pv 
                inner join pvd.prescription as pr 
                inner join pr.prescriptionDetails as prd 
                inner join pv.patient as pt 
                inner join pt.identifiers as pid 
                inner join pid.service as svc 
                where pk.pickupDate >= :startDate 
                       and pk.pickupDate <= :endDate 
                       and pk.clinic = :clinic 
                       and ep.patientServiceIdentifier.service.code = :serviceCode  
                """
        def count = Pack.executeQuery( sqlPackOnService,
                [startDate: startDate, endDate: endDate, serviceCode: service.getCode(), clinic: clinic])
        value = Integer.valueOf(count.get(0).toString())
        return value
    }

    //Query Historico de Levantamento de Pacientes Referidos
    @Override
    List<Pack> getPacksOfReferredPatientsByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {

        def sqlPackReffered = """
                        select pk from PatientVisitDetails as pvd
                        inner join Pack pk
                        inner join pvd.patientVisit as pv 
                        inner join pvd.episode as ep 
                        inner join ep.startStopReason as stp 
                        inner join ep.patientServiceIdentifier as psi 
                        inner join psi.patient as p 
                        inner join psi.service as s 
                        inner join ep.clinic c 
                        where s.code = :serviceCode and c.id = :clinicId and pk.pickupDate >= :startDate and pk.pickupDate <= :endDate 
                        and psi.id in (select psi2.id from Episode ep2 
                        inner join ep2.patientServiceIdentifier as psi2 
                        inner join ep2.startStopReason as stp2 
                        inner join psi2.service as s2 
                        where stp.code in ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') and s2.code = :serviceCode and ep2.episodeDate >= :startDate and ep2.episodeDate <= :endDate)
        """

        return Pack.executeQuery(sqlPackReffered,
                [serviceCode: clinicalService.code, clinicId: clinic.id, startDate: startDate, endDate: endDate])
    }

    @Override
    List getAbsentReferredPatientsByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {

        def sqlAbsent =
                """
                    select ep as episode,
                    pk.nextPickUpDate as dateMissedPickUp, 
                    p.cellphone as contact, 
                    (select pk4.pickupDate from PatientVisitDetails pvd2 
                    inner join pvd2.pack as pk4 
                    inner join pvd2.patientVisit as pv2 
                    inner join pvd2.episode as ep3 
                    inner join ep3.patientServiceIdentifier as psi3 
                    inner join psi3.service as s3 
                    where psi.patient = psi3.patient and s3.code = :serviceCode and pk4.pickupDate > pk.nextPickUpDate and pk4.pickupDate <= :endDate) as returnedPickUp 
                    from PatientVisitDetails pvd 
                    inner join pk.pack as pvd 
                    inner join pvd.patientVisit as pv 
                    inner join pvd.episode as ep 
                    inner join ep.startStopReason as stp 
                    inner join ep.patientServiceIdentifier as psi 
                    inner join psi.patient as p 
                    inner join psi.service as s 
                    inner join ep.clinic c 
                    where s.code = :serviceCode and c.id = :clinicId and pk.nextPickUpDate >= :startDate and pk.nextPickUpDate <= :endDate and DATE(pk.nextPickUpDate) + :days <= :endDate 
                    and psi.id in (select psi2.id from Episode ep2 
                    inner join ep2.patientServiceIdentifier as psi2 
                    inner join ep2.startStopReason as stp2 
                    inner join psi2.service as s2 
                    where stp.code = 'REFERIDO_PARA' and s2.code = :serviceCode and ep2.episodeDate >= :startDate and ep2.episodeDate <= :endDate) 
                    and pk.nextPickUpDate in (select max(pk2.nextPickUpDate) from PatientVisitDetails pvd2 
                    inner join pvd2.pack as pk2 
                    inner join pvd2.patientVisit as pv2 
                    inner join pvd2.episode as ep3 
                    inner join ep3.patientServiceIdentifier as psi3 
                    inner join psi3.service as s3 
                    where psi.patient = psi3.patient and s3.code = :serviceCode and pk2.nextPickUpDate  >= :startDate and pk2.nextPickUpDate <= :endDate)
                """
        def list = Pack.executeQuery(sqlAbsent,
                [serviceCode: clinicalService.code, clinicId: clinic.id, startDate: startDate, endDate: endDate, days: 3])

        return list
    }

    @Override
    List getAbandonmentByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def queryString =
                """
                WITH ResultTable AS (
                    SELECT DISTINCT ON (p.id)
                        pk.next_pick_up_date AS nextPickUpDate,
                        c.id AS clinicId,
                        p.id AS patient_id
                    FROM patient p
                    INNER JOIN (
                        SELECT
                            MAX(pack.pickup_date) pickupDate,
                            p.id patientid
                        FROM patient_visit_details pvdails
                        INNER JOIN pack ON pack.id = pvdails.pack_id
                        INNER JOIN patient_visit pv ON pvdails.patient_visit_id = pv.id
                        INNER JOIN patient p ON pv.patient_id = p.id
                        INNER JOIN patient_service_identifier psi ON psi.patient_id = pv.patient_id
                        INNER JOIN clinical_service cs ON cs.id = psi.service_id
                        WHERE cs.code = 'TARV'
                        GROUP BY 2
                    ) packAux ON packAux.patientid = p.id
                    INNER JOIN patient_visit pv ON pv.patient_id = p.id
                    INNER JOIN patient_visit_details pvd ON pvd.patient_visit_id = pv.id
                    INNER JOIN pack pk ON pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                    INNER JOIN clinic c ON pk.clinic_id = c.id AND c.id = :clinic_id
                    WHERE (pk.next_pick_up_date + INTERVAL '60 DAY') <= :endDate
                    GROUP BY p.id, pk.next_pick_up_date, clinicId
                )
                
                SELECT DISTINCT ON (psi.patient_id)
                    psi.value,
                    pat.first_names,
                    pat.middle_names,
                    pat.last_names,
                    pat.cellphone AS contact,
                    last_packs.nextPickUpDate,
                    last_packs.nextPickUpDate + INTERVAL '60 DAY' AS dateMissedPickUp,
                    pat.address
                FROM patient_service_identifier psi
                INNER JOIN ResultTable last_packs ON last_packs.patient_id = psi.patient_id
                INNER JOIN episode e ON psi.id = e.patient_service_identifier_id
                INNER JOIN start_stop_reason ssr ON e.start_stop_reason_id = ssr.id 
                    AND ssr.code IN ('NOVO_PACIENTE', 'INICIO_CCR', 'TRANSFERIDO_DE', 'ABANDONO', 'VOLTOU_REFERENCIA', 'REINICIO_TRATAMENTO', 'REFERIDO_DC', 'MANUTENCAO', 'OUTRO')
                INNER JOIN clinical_service cs ON psi.service_id = cs.id AND cs.code = 'TARV'
                INNER JOIN patient pat ON pat.id = last_packs.patient_id;    
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("clinic_id", clinic.id)
        List<Object[]> list = query.list()

        return list
    }

    @Override
    List getAbandonmentAndReturnByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def queryString =
                """               
                    WITH OrnemDeLivantamentos AS ( -- Pega todas packs de pacientes que nao estao na situacao de abandono em TARV e ordena de forna decrescente pela data de levantamento identificando as linhas 'ROW_NUMBER()'
                        SELECT
                            p.id AS patient_id,
                            pk.pickup_date,
                            pk.next_pick_up_date,
                            ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY pk.pickup_date DESC) AS row_num
                        FROM patient p
                        INNER JOIN patient_visit pv ON pv.patient_id = p.id
                        INNER JOIN patient_visit_details pvd ON pvd.patient_visit_id = pv.id
                        INNER JOIN pack pk ON pk.id = pvd.pack_id
                        INNER JOIN patient_service_identifier psi ON psi.patient_id = p.id
                        INNER JOIN clinical_service cs ON cs.id = psi.service_id
                        INNER JOIN clinic c ON pk.clinic_id = c.id AND c.id = :clinic_id
                        WHERE cs.code = 'TARV'
                    )
                    , SelectedPenultimoLevantamentoAbandono AS (-- Selecionar Os penultimos levantamentos que foram abandonos
                        SELECT
                            ol.patient_id,
                            ol.pickup_date AS penultimaPickUpDate,
                    ol.next_pick_up_date AS penultmaDPL,
                            ol.next_pick_up_date + INTERVAL '60 DAY' AS dateMissedPickUp
                        FROM OrnemDeLivantamentos ol
                        WHERE ol.row_num = 2
                        AND (ol.next_pick_up_date + INTERVAL '60 DAY') <= :endDate
                    )
                    , LastNextPickupDate AS ( -- Ultimo levantamento que e' na verdade a data de retorno dos nao voltaram a bandonar no periodo
                        SELECT
                            ol1.patient_id,
                            ol1.pickup_date AS dateReturned,
                            ol1.next_pick_up_date
                        FROM OrnemDeLivantamentos ol1
                        WHERE ol1.row_num = 1
                        AND (ol1.next_pick_up_date + INTERVAL '60 DAY') >= :endDate
                    )
                    SELECT DISTINCT ON (psi.patient_id)
                        psi.value,
                        pat.first_names,
                        pat.middle_names,
                        pat.last_names,
                        pat.cellphone AS contact,
                    penultimos.penultmaDPL as next_pick_up_date,
                        penultimos.dateMissedPickUp,
                        ultimos.dateReturned,
                        pat.address
                    FROM patient_service_identifier psi
                    INNER JOIN SelectedPenultimoLevantamentoAbandono penultimos ON penultimos.patient_id = psi.patient_id
                    INNER JOIN LastNextPickupDate ultimos ON ultimos.patient_id = psi.patient_id
                    INNER JOIN episode e ON psi.id = e.patient_service_identifier_id
                    INNER JOIN start_stop_reason ssr ON e.start_stop_reason_id = ssr.id AND ssr.code IN ('NOVO_PACIENTE', 'INICIO_CCR', 'TRANSFERIDO_DE', 'ABANDONO', 'VOLTOU_REFERENCIA', 'REINICIO_TRATAMENTO', 'REFERIDO_DC', 'MANUTENCAO', 'OUTRO')
                    INNER JOIN clinical_service cs ON psi.service_id = cs.id AND cs.code = 'TARV'
                    INNER JOIN patient pat ON pat.id = ultimos.patient_id
                    WHERE ultimos.dateReturned > penultimos.dateMissedPickUp  
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("clinic_id", clinic.id)
        List<Object[]> list = query.list()

        return list
    }

    @Override
    List getAbsentPatientsByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def queryString =
                """
                                with ResultTable AS  (
                                     select distinct ON (p.id)
                                     pk.next_pick_up_date as nextPickUpDate,
                                     c.id as clinicId,
                                     p.id as patient_id
                                     from patient p
                                 inner join (
                                select
                                MAX(pack.pickup_date) pickupDate, p.id patientid
                                from patient_visit_details pvdails
                                inner join pack on pack.id = pvdails.pack_id
                                inner join patient_visit pv on pvdails.patient_visit_id = pv.id
                                inner join patient p on pv.patient_id = p.id
                                inner join patient_service_identifier psi on psi.patient_id = pv.patient_id
                                inner join clinical_service cs on cs.id = psi.service_id
                                where cs.code = 'TARV'
                                group by 2
                                ) packAux on packAux.patientid = p.id
                                   inner join patient_visit pv on pv.patient_id = p.id
                                   inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id
                                   inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                                   inner join clinic c on pk.clinic_id = c.id and c.id =:clinic_id
                                   inner join prescription pre on pvd.prescription_id = pre.id
                                   inner join prescription_detail pre_det on pre_det.prescription_id = pre.id
                                   inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id
                                   inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id
                                     where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60
                                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0
                                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate
                                     group by p.id, pk.next_pick_up_date, clinicId
                                     )
                                     select distinct ON (psi.patient_id)
                                psi.value,
                                pat.first_names,
                                     pat.middle_names,
                                     pat.last_names,
                                     pat.cellphone as contact,
                                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade,
                                     last_packs.nextPickUpDate as dateMissedPickUp 
                                     from patient_service_identifier psi
                                 inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id
                                   inner join episode e on psi.id = e.patient_service_identifier_id
                                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                                                                           and ssr.code IN ('NOVO_PACIENTE', 'INICIO_CCR', 'TRANSFERIDO_DE','ABANDONO', 'VOLTOU_REFERENCIA','REINICIO_TRATAMENTO', 'REFERIDO_DC','MANUTENCAO', 'OUTRO')
                                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'
                                   inner join patient pat on pat.id = last_packs.patient_id
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("startDate", startDate)
        query.setParameter("clinic_id", clinic.id)
        List<Object[]> list = query.list()

        return list
    }


    @Override
    List getAbsentPatientsDTByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def queryString =
                """
                                with ResultTable AS  (
                                     select distinct ON (p.id)
                                     pk.next_pick_up_date as nextPickUpDate,
                                     c.id as clinicId,
                                     p.id as patient_id
                                     from patient p
                                 inner join (
                                select
                                MAX(pack.pickup_date) pickupDate, p.id patientid
                                from patient_visit_details pvdails
                                inner join pack on pack.id = pvdails.pack_id
                                inner join patient_visit pv on pvdails.patient_visit_id = pv.id
                                inner join patient p on pv.patient_id = p.id
                                inner join patient_service_identifier psi on psi.patient_id = pv.patient_id
                                inner join clinical_service cs on cs.id = psi.service_id
                                where cs.code = 'TARV'
                                group by 2
                                ) packAux on packAux.patientid = p.id
                                   inner join patient_visit pv on pv.patient_id = p.id
                                   inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id
                                   inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                                   inner join clinic c on pk.clinic_id = c.id and c.id =:clinic_id
                                   inner join prescription pre on pvd.prescription_id = pre.id
                                   inner join prescription_detail pre_det on pre_det.prescription_id = pre.id
                                   inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id
                                   inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id
                                   inner join dispense_type dspt on pre_det.dispense_type_id = dspt.id        
                                     where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60
                                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0
                                 AND   dspt.code = 'DT'  
                                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate
                                     group by p.id, pk.next_pick_up_date, clinicId
                                     )
                                     select distinct ON (psi.patient_id)
                                psi.value,
                                pat.first_names,
                                     pat.middle_names,
                                     pat.last_names,
                                     pat.cellphone as contact,
                                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade,
                                     last_packs.nextPickUpDate as dateMissedPickUp 
                                     from patient_service_identifier psi
                                 inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id
                                   inner join episode e on psi.id = e.patient_service_identifier_id
                                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                                                                           and ssr.code IN ('NOVO_PACIENTE', 'INICIO_CCR', 'TRANSFERIDO_DE','ABANDONO', 'VOLTOU_REFERENCIA','REINICIO_TRATAMENTO', 'REFERIDO_DC','MANUTENCAO', 'OUTRO')
                                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'
                                   inner join patient pat on pat.id = last_packs.patient_id
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("startDate", startDate)
        query.setParameter("clinic_id", clinic.id)
        List<Object[]> list = query.list()

        return list
    }


    @Override
    List getAbsentPatientsDSByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def queryString =
                """
                                with ResultTable AS  (
                                     select distinct ON (p.id)
                                     pk.next_pick_up_date as nextPickUpDate,
                                     c.id as clinicId,
                                     p.id as patient_id
                                     from patient p
                                 inner join (
                                select
                                MAX(pack.pickup_date) pickupDate, p.id patientid
                                from patient_visit_details pvdails
                                inner join pack on pack.id = pvdails.pack_id
                                inner join patient_visit pv on pvdails.patient_visit_id = pv.id
                                inner join patient p on pv.patient_id = p.id
                                inner join patient_service_identifier psi on psi.patient_id = pv.patient_id
                                inner join clinical_service cs on cs.id = psi.service_id
                                where cs.code = 'TARV'
                                group by 2
                                ) packAux on packAux.patientid = p.id
                                   inner join patient_visit pv on pv.patient_id = p.id
                                   inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id
                                   inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                                   inner join clinic c on pk.clinic_id = c.id and c.id =:clinic_id
                                   inner join prescription pre on pvd.prescription_id = pre.id
                                   inner join prescription_detail pre_det on pre_det.prescription_id = pre.id
                                   inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id
                                   inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id
                                   inner join dispense_type dspt on pre_det.dispense_type_id = dspt.id        
                                     where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60
                                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0
                                 AND   dspt.code = 'DS'  
                                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate
                                     group by p.id, pk.next_pick_up_date, clinicId
                                     )
                                     select distinct ON (psi.patient_id)
                                psi.value,
                                pat.first_names,
                                     pat.middle_names,
                                     pat.last_names,
                                     pat.cellphone as contact,
                                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade,
                                     last_packs.nextPickUpDate as dateMissedPickUp 
                                     from patient_service_identifier psi
                                 inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id
                                   inner join episode e on psi.id = e.patient_service_identifier_id
                                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                                                                           and ssr.code IN ('NOVO_PACIENTE', 'INICIO_CCR', 'TRANSFERIDO_DE','ABANDONO', 'VOLTOU_REFERENCIA','REINICIO_TRATAMENTO', 'REFERIDO_DC','MANUTENCAO', 'OUTRO')
                                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'
                                   inner join patient pat on pat.id = last_packs.patient_id
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("startDate", startDate)
        query.setParameter("clinic_id", clinic.id)
        List<Object[]> list = query.list()

        return list
    }




    @Override
    List<Pack> getActivePatientsReportDataByReportParams(ReportSearchParams reportSearchParams) {
        String queryString =
                """
                select distinct ON (p.id) 
                p.first_names,  
                    p.middle_names,   
                    p.last_names,   
                    p.gender,   
                    p.date_of_birth,   
                    p.cellphone, 
                    r3.pickUpDate, 
                    r3.nextPickUpDate, 
                    r1.t_line, 
                    r1.t_regimen, 
                    CASE
                   WHEN (r1.patient_type = 'N/A' OR r1.patient_type IS null OR r1.patient_type = 'Inicio') 
                        AND r1.dtcode = 'DM'
                        AND r2.code = 'NOVO_PACIENTE'
                        AND (Date(r2.episode_date) <= Date(r3.pickUpDate) AND r3.pickUpDate <= (r2.episode_date + INTERVAL '3 days')) 
                    THEN 'Inicio'
                    ELSE 
                        CASE 
                            WHEN (r1.patient_type = 'N/A' OR r1.patient_type IS null OR r1.patient_type = 'Transfer de')
                                AND r2.code = 'TRANSFERIDO_DE'
                                AND (Date(r2.episode_date) <= Date(r3.pickUpDate) AND r3.pickUpDate <= (r2.episode_date + INTERVAL '3 days'))
                            THEN 'Transfer de'
                            ELSE
                                CASE 
                                    WHEN (r1.patient_type = 'N/A' OR r1.patient_type IS null OR r1.patient_type = 'Reiniciar')
                                        AND r2.code = 'REINICIO_TRATAMETO'
                                        AND (Date(r2.episode_date) <= Date(r3.pickUpDate) AND r3.pickUpDate <= (r2.episode_date + INTERVAL '3 days')) 
                                    THEN 'Reiniciar'
                                    ELSE
                                        CASE 
                                            WHEN (r2.code = 'TRANSITO' OR r2.code = 'INICIO_MATERNIDADE')
                                            THEN 'Trânsito'
                                            ELSE
                                                CASE 
                                                    WHEN r2.code = 'TERMINO_DO_TRATAMENTO'
                                                         AND (Date(r2.episode_date) <= Date(r3.pickUpDate) AND r3.pickUpDate <= (r2.episode_date + INTERVAL '3 days'))
                                                    THEN 'Termino do Tratamento'
                                                    ELSE
                                                        'Manutenção'
                                                END
                                        END
                                END
                        END
                END as patient_type,
                    r.value 
                from patient p 
                inner join patient_visit pv on pv.patient_id = p.id 
                inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id 
                inner join ( 
                select  
                psi.patient_id as pat_id, 
                psi.value  
                from patient_service_identifier psi  
                inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV' 
                ) r on r.pat_id = p.id 
                inner join ( 
                select 
                pre.id, 
                tl.description as t_line, 
                tr.description as t_regimen, 
                pre.patient_type,
                dt.code as dtcode
                from prescription pre 
                inner join prescription_detail pd on pd.prescription_id = pre.id 
                inner join therapeutic_line tl on pd.therapeutic_line_id = tl.id 
                inner join therapeutic_regimen tr on pd.therapeutic_regimen_id = tr.id  
                inner join dispense_type dt ON dt.id = pd.dispense_type_id
                ) r1 on pvd.prescription_id = r1.id 
                inner join ( 
                select  
                e.id as episode_id,
                e.episode_date,
                ssr.code
                from episode e  
                inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                                                        AND ssr.code in ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC') 
                ) r2 on pvd.episode_id = r2.episode_id 
                inner join ( 
                select  
                pk.id as pack_id, 
                pk.next_pick_up_date as nextPickUpDate, 
                MAX(pk.pickup_date) AS pickUpDate 
                from pack pk 
                inner join clinic c on pk.clinic_id = c.id and c.id = :clinic_id 
                group by pk.id, pk.next_pick_up_date 
                ) r3 on pvd.pack_id = r3.pack_id 
                where r3.nextPickUpDate + INTERVAL '3 DAY' >= :endDate
                """
        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", reportSearchParams.endDate)
        query.setParameter("clinic_id", reportSearchParams.clinicId)
        List<Object[]> result = query.list()

        return result
    }

    @Override
    List getAbsentPatientsApssByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {
        def sql = new Sql(dataSource as DataSource)
        def lastResult = []
        def queryStringTB =
                """
                with ResultTable AS  (
                     select distinct ON (p.id)
                     pk.next_pick_up_date as nextPickUpDate,
                     c.id as clinicId,
                     p.id as patient_id
                     from patient p
                 inner join (
                 select
                 MAX(pack.pickup_date) pickupDate, p.id patientid
                 from patient_visit_details pvdails
                 inner join pack on pack.id = pvdails.pack_id
                 inner join patient_visit pv on pvdails.patient_visit_id = pv.id
                     inner join patient p on pv.patient_id = p.id
                    inner join patient_service_identifier psi on psi.patient_id = pv.patient_id
                 inner join clinical_service cs on cs.id = psi.service_id
                    inner join clinic c on pack.clinic_id = c.id and pack.clinic_id = :clinic_id
                    inner join clinic csec on csec.parent_clinic_id = c.id
                 where cs.code = 'TARV' or (cs.code = 'TARV' and csec.code = 'TB')
                 group by 2
                ) packAux on packAux.patientid = p.id
                 inner join patient_visit pv on pv.patient_id = p.id
                 inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id
                 inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                 inner join clinic c on pk.clinic_id = c.id
                 inner join episode ep on ep.id = pvd.episode_id 
                 inner join clinic csec ON csec.parent_clinic_id = c.id AND csec.id = ep.clinic_sector_id 
                 inner join prescription pre on pvd.prescription_id = pre.id
                 inner join prescription_detail pre_det on pre_det.prescription_id = pre.id
                 inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id
                 inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id
                 where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60
                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0
                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate and csec.code = 'TB'
                     group by p.id, pk.next_pick_up_date, clinicId
                     )
                     select distinct ON (psi.patient_id)
                 psi.value,
                 pat.first_names,
                     pat.middle_names,
                     pat.last_names,
                     pat.cellphone as contact,
                     pat.address,
                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade,
                     'TB' as served_service
                     from patient_service_identifier psi
                 inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id
                   inner join episode e on psi.id = e.patient_service_identifier_id
                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                   and ssr.code IN ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA','REFERIDO_DC')
                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'
                   inner join patient pat on pat.id = last_packs.patient_id
                """
        def queryStringTARV =
                """
                with ResultTable AS  ( 
                     select distinct ON (p.id) 
                     pk.next_pick_up_date as nextPickUpDate, 
                     c.id as clinicId, 
                     p.id as patient_id 
                     from patient p 
                 inner join ( 
                select 
                MAX(pack.pickup_date) pickupDate, p.id patientid 
                from patient_visit_details pvdails 
                inner join pack on pack.id = pvdails.pack_id 
                 inner join patient_visit pv on pvdails.patient_visit_id = pv.id 
                     inner join patient p on pv.patient_id = p.id 
                    inner join patient_service_identifier psi on psi.patient_id = pv.patient_id 
                 inner join clinical_service cs on cs.id = psi.service_id 
                where cs.code = 'TARV' 
                group by 2 
                ) packAux on packAux.patientid = p.id 
                    inner join patient_visit pv on pv.patient_id = p.id 
                    inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id 
                    inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate 
                    inner join clinic c on pk.clinic_id = c.id 
                    INNER JOIN episode ep on ep.id = pvd.episode_id
                    INNER JOIN clinic csec ON csec.parent_clinic_id = c.id AND csec.id = ep.clinic_sector_id
                    inner join prescription pre on pvd.prescription_id = pre.id 
                    inner join prescription_detail pre_det on pre_det.prescription_id = pre.id 
                    inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id 
                    inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id 
                     where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60 
                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0 
                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate 
                 AND csec.code not IN ('CCR', 'CPN', 'PREP', 'TB') 
                     group by p.id, pk.next_pick_up_date, clinicId 
                     ) 
                     select distinct ON (psi.patient_id) 
                psi.value, 
                pat.first_names, 
                     pat.middle_names, 
                     pat.last_names, 
                     pat.cellphone as contact, 
                     pat.address, 
                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade, 
                     'TARV' as served_service 
                     from patient_service_identifier psi 
                 inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id 
                   inner join episode e on psi.id = e.patient_service_identifier_id 
                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                   and ssr.code IN ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC') 
                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV' 
                   inner join patient pat on pat.id = last_packs.patient_id
                """

        def queryStringSectors =
                """
                with ResultTable AS  (
                     select distinct ON (p.id)
                     pk.next_pick_up_date as nextPickUpDate,
                     c.id as clinicId,
                     p.id as patient_id
                     from patient p
                 inner join (
                 select
                 MAX(pack.pickup_date) pickupDate, p.id patientid
                 from patient_visit_details pvdails
                 inner join pack on pack.id = pvdails.pack_id
                 inner join patient_visit pv on pvdails.patient_visit_id = pv.id
                     inner join patient p on pv.patient_id = p.id
                    inner join patient_service_identifier psi on psi.patient_id = pv.patient_id
                 inner join clinical_service cs on cs.id = psi.service_id
                 where cs.code = 'TARV'
                 group by 2
                ) packAux on packAux.patientid = p.id
                   inner join patient_visit pv on pv.patient_id = p.id
                   inner join patient_visit_details pvd on pvd.patient_visit_id = pv.id
                   inner join pack pk on pk.id = pvd.pack_id AND pk.pickup_date = packAux.pickupDate
                   inner join clinic c on pk.clinic_id = c.id and pk.clinic_id = :clinic_id
                   inner join episode ep on ep.id = pvd.episode_id 
                   inner join clinic csec ON csec.parent_clinic_id = c.id AND csec.id = ep.clinic_sector_id 
                   inner join prescription pre on pvd.prescription_id = pre.id
                   inner join prescription_detail pre_det on pre_det.prescription_id = pre.id
                   inner join therapeutic_regimen tr on pre_det.therapeutic_regimen_id = tr.id
                   inner join therapeutic_line tl on pre_det.therapeutic_line_id = tl.id
                     where EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) < 60
                 AND EXTRACT(DAY FROM (CURRENT_DATE - (pk.next_pick_up_date + INTERVAL '3 DAY'))) > 0
                 AND pk.next_pick_up_date >= :startDate AND pk.next_pick_up_date <= :endDate and csec.code =:sectorCode
                     group by p.id, pk.next_pick_up_date, clinicId
                     )
                     select distinct ON (psi.patient_id)
                 psi.value,
                 pat.first_names,
                     pat.middle_names,
                     pat.last_names,
                     pat.cellphone as contact,
                     pat.address,
                     EXTRACT(year FROM age(:endDate, pat.date_of_birth)) as idade,
                     :sectorCode as served_service
                     from patient_service_identifier psi
                   inner join ResultTable last_packs on last_packs.patient_id = psi.patient_id
                   inner join episode e on psi.id = e.patient_service_identifier_id
                   inner join start_stop_reason ssr on e.start_stop_reason_id = ssr.id 
                   and ssr.code IN ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA','REFERIDO_DC')                   
                   inner join clinical_service cs on psi.service_id = cs.id and cs.code = 'TARV'
                   inner join patient pat on pat.id = last_packs.patient_id
                """
        def params = [startDate: new java.sql.Date(startDate.time), endDate: new java.sql.Date(endDate.time)]
        List<Object[]> result = sql.rows(queryStringTARV, params)

       def sectorCodes = ['CPN','SAAJ','CCR']
        def params1 = [startDate: new java.sql.Date(startDate.time), endDate: new java.sql.Date(endDate.time), clinic_id: clinic.id]
        for (Object it : sql.rows(queryStringTB, params1)) {
            result.push(it)
        }


        sectorCodes.each {
            params1.put('sectorCode',it)
            for (Object item : sql.rows(queryStringSectors, params1)) {
                result.push(item)
            }
        }

   return result
}

    //Historico de Levantamentos
    @Override
    List<Pack> getPacksByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {

        def queryString = ""
            if(clinicalService.isTARV()){
                queryString = """
                select psi.value,
                p.first_names,
                p.middle_names,
                p.last_names,
                EXTRACT(year FROM age(:endDate, p.date_of_birth)) as idade,
                p.cellphone,
                CASE
                   WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Inicio') 
                        AND dt.code = 'DM'
                        AND ssr.code = 'NOVO_PACIENTE'
                        AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days')) 
                    THEN 'Inicio'
                    ELSE 
                        CASE 
                            WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Transfer de')
                                AND ssr.code = 'TRANSFERIDO_DE'
                                AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days'))
                            THEN 'Transfer de'
                            ELSE
                                CASE 
                                    WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Reiniciar')
                                        AND ssr.code = 'REINICIO_TRATAMETO'
                                        AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days')) 
                                    THEN 'Reiniciar'
                                    ELSE
                                        CASE 
                                            WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                            THEN 'Trânsito'
                                            ELSE
                                                CASE 
                                                    WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                         AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days'))
                                                    THEN 'Termino do Tratamento'
                                                    ELSE
                                                        'Manutenção'
                                                END
                                        END
                                END
                        END
                END as patient_type,
                 tr.description as regimeDescription,
                CASE  
                  WHEN dt.code = 'DT' 
                      THEN 
                        CASE 
                            WHEN pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                                THEN 'DT - TRANSPORTE' 
                            ELSE 'DT'
                        END 
                  WHEN dt.code = 'DS' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DS - TRANSPORTE' 
                          ELSE 'DS'
                      END 
                  WHEN dt.code = 'DM' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DM - TRANSPORTE' 
                          ELSE 'DM' 
                      END 
                  WHEN dt.code = 'DB' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DB - TRANSPORTE' 
                          ELSE 'DB' 
                      END 
                END AS tipodispensa,
                dm.description as dispenseMode,
                pack2.pickup_date,
                pack2.next_pick_up_date,
                ep.episode_date,
                prc.patient_status,
                ssr.code,
                cr.code as clinicsector,
                lastPack.clinic_name,
                pack2.provider_uuid
                from 
                (
                select distinct pat.id,
                 max(pk.pickup_date) pickupdate,
                 max(pk.id) packid,
                 pat.date_of_birth,
                 cs.code service_code,
                 crc.clinic_name
                 from patient_visit_details pvd
                 inner join pack pk on pk.id = pvd.pack_id
                 inner join episode ep on ep.id = pvd.episode_id
                 inner join patient_visit pv on pv.id = pvd.patient_visit_id
                 inner join patient pat on pat.id = pv.patient_id
                 inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                 inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                 inner join clinical_service cs ON cs.id = psi.service_id
                 inner join clinic c on c.id = ep.clinic_id
                 left join clinic crc on crc.id = pk.origin
                 where
                (Date(pk.pickup_date) BETWEEN :startDate AND :endDate)
                AND (cs.code = :serviceCode)
                AND c.id = :clinicId
                AND ssr.code in ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC','TRANSITO','INICIO_MATERNIDADE')
                group by 1,4,5,6
                ORDER BY 1 asc
                ) lastPack
                inner join pack pack2 ON pack2.id = lastPack.packid
                inner join patient_visit_details pvd ON pvd.pack_id = pack2.id
                inner join episode ep on ep.id = pvd.episode_id
                inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                inner join clinical_service cs ON cs.id = psi.service_id
                inner join patient_visit pv on pv.id = pvd.patient_visit_id
                inner join patient p ON p.id = psi.patient_id
                inner join prescription prc ON prc.id = pvd.prescription_id
                inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                inner join prescription_detail pred on pred.prescription_id = prc.id
                inner join therapeutic_regimen tr on tr.id = pred.therapeutic_regimen_id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                inner join dispense_mode dm ON dm.id = pack2.dispense_mode_id
                inner join clinic c on c.id = ep.clinic_id
                left join  clinic cr on cr.id = ep.clinic_sector_id
                where (cs.code = :serviceCode)
                """
            }else{

         queryString = """
                select psi.value,
                p.first_names,
                p.middle_names,
                p.last_names,
                EXTRACT(year FROM age(:endDate, p.date_of_birth)) as idade,
                p.cellphone,
                CASE
                   WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Inicio') 
                        AND dt.code = 'DM'
                        AND ssr.code = 'NOVO_PACIENTE'
                        AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days')) 
                    THEN 'Inicio'
                    ELSE 
                        CASE 
                            WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Transfer de')
                                AND ssr.code = 'TRANSFERIDO_DE'
                                AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days'))
                            THEN 'Transfer de'
                            ELSE
                                CASE 
                                    WHEN (prc.patient_type = 'N/A' OR prc.patient_type IS null OR prc.patient_type = 'Reiniciar')
                                        AND ssr.code = 'REINICIO_TRATAMETO'
                                        AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days')) 
                                    THEN 'Reiniciar'
                                    ELSE
                                        CASE 
                                            WHEN (ssr.code = 'TRANSITO' OR ssr.code = 'INICIO_MATERNIDADE')
                                            THEN 'Trânsito'
                                            ELSE
                                                CASE 
                                                    WHEN ssr.code = 'TERMINO_DO_TRATAMENTO'
                                                         AND (Date(ep.episode_date) <= Date(pack2.pickup_date) AND pack2.pickup_date <= (ep.episode_date + INTERVAL '3 days'))
                                                    THEN 'Termino do Tratamento'
                                                    ELSE
                                                        'Manutenção'
                                                END
                                        END
                                END
                        END
                END as patient_type,
                 tr.description as regimeDescription,
                CASE  
                  WHEN dt.code = 'DT' 
                      THEN 
                        CASE 
                            WHEN pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                                THEN 'DT - TRANSPORTE' 
                            ELSE 'DT'
                        END 
                  WHEN dt.code = 'DS' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DS - TRANSPORTE' 
                          ELSE 'DS'
                      END 
                  WHEN dt.code = 'DM' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DM - TRANSPORTE' 
                          ELSE 'DM' 
                      END 
                  WHEN dt.code = 'DB' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DB - TRANSPORTE' 
                          ELSE 'DB' 
                      END 
                END AS tipodispensa,
                dm.description as dispenseMode,
                pack2.pickup_date,
                pack2.next_pick_up_date,
                ep.episode_date,
                prc.patient_status,
                ssr.code,
                cr.code as clinicsector,
                lastPack.clinic_name,
                pack2.provider_uuid
                from 
                (
                select distinct pat.id,
                 max(pk.pickup_date) pickupdate,
                 max(pk.id) packid,
                 pat.date_of_birth,
                 cs.code service_code,
                 crc.clinic_name
                 from patient_visit_details pvd
                 inner join pack pk on pk.id = pvd.pack_id
                 inner join episode ep on ep.id = pvd.episode_id
                 inner join patient_visit pv on pv.id = pvd.patient_visit_id
                 inner join patient pat on pat.id = pv.patient_id
                 inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                 inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                 inner join clinical_service cs ON cs.id = psi.service_id
                 inner join clinic c on c.id = ep.clinic_id
                 left join clinic crc on crc.id = pk.origin
                 where
                (Date(pk.pickup_date) BETWEEN :startDate AND :endDate)
                AND cs.code = :serviceCode
                AND c.id = :clinicId
                AND ssr.code in ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC','TRANSITO','INICIO_MATERNIDADE')
                group by 1,4,5,6
                ORDER BY 1 asc
                ) lastPack
                inner join pack pack2 ON pack2.id = lastPack.packid
                inner join patient_visit_details pvd ON pvd.pack_id = pack2.id
                inner join episode ep on ep.id = pvd.episode_id
                inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                inner join clinical_service cs ON cs.id = psi.service_id
                inner join patient_visit pv on pv.id = pvd.patient_visit_id
                inner join patient p ON p.id = psi.patient_id
                inner join prescription prc ON prc.id = pvd.prescription_id
                inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                inner join prescription_detail pred on pred.prescription_id = prc.id
                inner join therapeutic_regimen tr on tr.id = pred.therapeutic_regimen_id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                inner join dispense_mode dm ON dm.id = pack2.dispense_mode_id
                inner join clinic c on c.id = ep.clinic_id
                left join  clinic cr on cr.id = ep.clinic_sector_id
                where cs.code = :serviceCode
                """
            }


        Session session = sessionFactory.getCurrentSession()
        def quertHistoricoTARV = session.createSQLQuery(queryString)
        quertHistoricoTARV.setParameter("endDate", endDate)
        quertHistoricoTARV.setParameter("startDate", startDate)
        quertHistoricoTARV.setParameter("serviceCode", clinicalService.code)
        quertHistoricoTARV.setParameter("clinicId", clinic.id)
        List<Object[]> list = quertHistoricoTARV.list()
        return list
    }

    //Historico de Levantamentos de Referidos
    @Override
    List<Pack> getReferredPatintsPacksByClinicalServiceAndClinicOnPeriod(ClinicalService clinicalService, Clinic clinic, Date startDate, Date endDate) {

        def queryString =
                """
                with ResultTable AS( 
                 SELECT p.id as patient_id, e.id as episode_id, psi.value as nid, e.referral_clinic_id referral_clinic_id, e.notes, MAX(e.episode_date) as last_episode_date  
                   FROM episode e 
                    inner join patient_service_identifier psi on e.patient_service_identifier_id = psi.id 
                    inner join clinical_service cs on cs.id =  psi.service_id and  cs.code = 'TARV' 
                    inner join patient p on p.id = psi.patient_id  
                    inner join start_stop_reason ssr on ssr.id = e.start_stop_reason_id 
                   where ssr.code in ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA') and e.episode_date between :startDate and :endDate and psi.clinic_id = :clinicId 
                   group by p.id, e.episode_date, e.id, psi.value, e.referral_clinic_id, e.notes  
                   order by e.episode_date desc  
                ) 
                select psi.value,
                p.first_names,
                p.middle_names,
                p.last_names,
                EXTRACT(year FROM age(:endDate, p.date_of_birth)) as idade,
                p.cellphone,
                prc.patient_type,
                tr.description as regimeDescription,
                 CASE  
                  WHEN dt.code = 'DT' 
                      THEN 
                        CASE 
                            WHEN pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                                THEN 'DT - TRANSPORTE' 
                            ELSE 'DT'
                        END 
                  WHEN dt.code = 'DS' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DS - TRANSPORTE' 
                          ELSE 'DS'
                      END 
                  WHEN dt.code = 'DM' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DM - TRANSPORTE' 
                          ELSE 'DM' 
                      END 
                  WHEN dt.code = 'DB' THEN 
                      CASE 
                          WHEN  pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                              THEN 'DB - TRANSPORTE' 
                          ELSE 'DB' 
                      END 
                END AS tipodispensa,
                dm.description as dispenseMode,
                pack2.pickup_date,
                pack2.next_pick_up_date 
                from patient p 
                inner join patient_service_identifier psi on psi.patient_id = p.id 
                INNER JOIN clinical_service cs ON psi.service_id = cs.id 
                inner join episode ep on ep.patient_service_identifier_id = psi.id 
                inner join patient_visit_details pvd ON pvd.episode_id = ep.id 
                INNER JOIN patient_visit pv on pv.id = pvd.patient_visit_id 
                inner join patient ON patient.id = psi.patient_id 
                inner join pack pack2 ON pack2.id = pvd.pack_id 
                inner join prescription prc ON prc.id = pvd.prescription_id 
                INNER JOIN start_stop_reason ssr on ssr.id = ep.start_stop_reason_id 
                INNER JOIN prescription_detail pred on pred.prescription_id = prc.id 
                inner join therapeutic_regimen tr on tr.id = pred.therapeutic_regimen_id 
                inner join dispense_type dt ON dt.id = pred.dispense_type_id 
                inner join dispense_mode dm ON dm.id = pack2.dispense_mode_id 
                inner join clinic c on c.id = ep.clinic_id 
                where 
                ((Date(pack2.pickup_date) BETWEEN :startDate AND :endDate) OR 
                pg_catalog.date(pack2.pickup_date) < :startDate and pg_catalog.date(pack2.next_pick_up_date) > :endDate AND 
                DATE(pack2.pickup_date + (INTERVAL '1 month'* cast (date_part('day',  cast (:endDate as timestamp) - cast (pack2.pickup_date as timestamp))/30 as integer))) >= :startDate 
                and DATE(pack2.pickup_date + (INTERVAL '1 month'*cast (date_part('day', cast (:endDate as timestamp) - cast (pack2.pickup_date as timestamp))/30 as integer))) <= :endDate) 
                and cs.code = 'TARV' 
                and c.id = :clinicId 
                and ssr.code in ('REFERIDO_PARA', 'VOLTOU_A_SER_REFERIDO_PARA')
                group by 1,2,3,4,5,6,7,8,9,10,11,12 
                ORDER BY PACK2.pickup_date asc
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("endDate", endDate)
        query.setParameter("startDate", startDate)
        query.setParameter("clinicId", clinic.id)
        List<Object[]> list = query.list()
        return list
    }

    /* NOVOS REPORTS [Para nao criar controllers novos"*/

    // RELATORIO DE PACIENTES EM DISPENSA SEMENSAL
    @Override
    List<Pack> getPacientesEmDispensaSemensal(ReportSearchParams reportSearchParams) {
        String queryString =
                """
               select distinct ON (p.id) 
                p.first_names,
                p.middle_names,
                p.last_names,
                EXTRACT(year FROM age(:endDate, p.date_of_birth)) as gender,
                p.date_of_birth,
                p.cellphone,
                pack2.pickup_date pickUpDate,
                pack2.next_pick_up_date nextPickUpDate,
                tl.description t_line,
                tr.regimen_scheme t_regimen,
                CASE
                   WHEN extract(days from pack2.pickup_date - firstPack.min_prescription_date) <= 3   
                    THEN 'Inicio'
                    ELSE 
                        CASE 
                            WHEN pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                            THEN 'DS - TRANSPORTE'
                            ELSE
                                 'Manutenção'
                        END
                END as patient_type,
                psi.value,
                prc.prescription_date,
                dm.description as dispenseMode,
                ep.episode_date,
                prc.patient_status,
                ssr.code
                from 
                (
                select distinct pat.id,
                 max(pk.pickup_date) pickupdate,
                 max(pk.id) packid,
                 pat.date_of_birth,
                 cs.code service_code
                 from patient_visit_details pvd
                 inner join pack pk on pk.id = pvd.pack_id
                 inner join patient_visit_details pvd0 ON pvd0.pack_id = pk.id
                 inner join prescription prc ON prc.id = pvd0.prescription_id
                 inner join prescription_detail pred on pred.prescription_id = prc.id
                 inner join dispense_type dt ON dt.id = pred.dispense_type_id
                 inner join episode ep on ep.id = pvd.episode_id
                 inner join patient_visit pv on pv.id = pvd.patient_visit_id
                 inner join patient pat on pat.id = pv.patient_id
                 inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                 inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                 inner join clinical_service cs ON cs.id = psi.service_id
                 inner join clinic c on c.id = ep.clinic_id
                 where
                ((Date(pk.pickup_date) BETWEEN :startDate AND :endDate) OR
                pg_catalog.date(pk.pickup_date) < :startDate and pg_catalog.date(pk.next_pick_up_date) > :endDate AND
                DATE(pk.pickup_date + (INTERVAL '1 month'* cast (date_part('day',  cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) >= :startDate
                AND DATE(pk.pickup_date + (INTERVAL '1 month'*cast (date_part('day', cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) <= :endDate)
                AND cs.code = 'TARV'
                AND c.id = :clinicId
                AND ssr.code in ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC')
                AND dt.code = 'DS' 
                group by 1,4,5
                ORDER BY 1 asc
                ) lastPack
                inner join
                (
                select patv.patient_id patid, min(presc.prescription_date) min_prescription_date
                from patient_visit_details patvd
                inner join prescription presc on presc.id = patvd.prescription_id
                inner join patient_visit patv on patv.id = patvd.patient_visit_id
                inner join prescription_detail pred on pred.prescription_id = presc.id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                where dt.code = 'DS'
                group by 1
                ) firstPack on firstPack.patid = lastPack.id
                inner join pack pack2 ON pack2.id = lastPack.packid
                inner join patient_visit_details pvd ON pvd.pack_id = pack2.id
                inner join episode ep on ep.id = pvd.episode_id
                inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                inner join clinical_service cs ON cs.id = psi.service_id
                inner join patient_visit pv on pv.id = pvd.patient_visit_id
                inner join patient p ON p.id = psi.patient_id
                inner join prescription prc ON prc.id = pvd.prescription_id
                inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                inner join prescription_detail pred on pred.prescription_id = prc.id
                inner join therapeutic_line tl on tl.id = pred.therapeutic_line_id 
                inner join therapeutic_regimen tr on tr.id = pred.therapeutic_regimen_id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                inner join dispense_mode dm ON dm.id = pack2.dispense_mode_id
                inner join clinic c on c.id = ep.clinic_id
                """

        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("startDate", reportSearchParams.startDate)
        query.setParameter("endDate", reportSearchParams.endDate)
        query.setParameter("clinicId", reportSearchParams.clinicId)
        List<Object[]> result = query.list()

        return result
    }

    @Override
    List<Pack> getPacientesEmDispensaTrimestral(ReportSearchParams reportSearchParams) {
        String queryString =
                """
               select distinct ON (p.id) 
                p.first_names,
                p.middle_names,
                p.last_names,
                EXTRACT(year FROM age(:endDate, p.date_of_birth)) as gender,
                p.date_of_birth,
                p.cellphone,
                pack2.pickup_date pickUpDate,
                pack2.next_pick_up_date nextPickUpDate,
                tl.description t_line,
                tr.regimen_scheme t_regimen,
                CASE
                   WHEN extract(days from pack2.pickup_date - firstPack.min_prescription_date) <= 3  
                    THEN 'Inicio'
                    ELSE 
                        CASE 
                            WHEN pack2.next_pick_up_date > :endDate AND Date(pack2.pickup_date) < :startDate
                            THEN 'DT - TRANSPORTE'
                            ELSE
                                 'Manutenção'
                        END
                END as patient_type,
                psi.value,
                prc.prescription_date,
                dm.description as dispenseMode,
                ep.episode_date,
                prc.patient_status,
                ssr.code
                from 
                (
                select distinct pat.id,
                 max(pk.pickup_date) pickupdate,
                 max(pk.id) packid,
                 pat.date_of_birth,
                 cs.code service_code
                 from patient_visit_details pvd
                 inner join pack pk on pk.id = pvd.pack_id
                 inner join patient_visit_details pvd0 ON pvd0.pack_id = pk.id
                 inner join prescription prc ON prc.id = pvd0.prescription_id
                 inner join prescription_detail pred on pred.prescription_id = prc.id
                 inner join dispense_type dt ON dt.id = pred.dispense_type_id
                 inner join episode ep on ep.id = pvd.episode_id
                 inner join patient_visit pv on pv.id = pvd.patient_visit_id
                 inner join patient pat on pat.id = pv.patient_id
                 inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                 inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                 inner join clinical_service cs ON cs.id = psi.service_id
                 inner join clinic c on c.id = ep.clinic_id
                 where
                ((Date(pk.pickup_date) BETWEEN :startDate AND :endDate) OR
                pg_catalog.date(pk.pickup_date) < :startDate and pg_catalog.date(pk.next_pick_up_date) > :endDate AND
                DATE(pk.pickup_date + (INTERVAL '1 month'* cast (date_part('day',  cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) >= :startDate
                AND DATE(pk.pickup_date + (INTERVAL '1 month'*cast (date_part('day', cast (:endDate as timestamp) - cast (pk.pickup_date as timestamp))/30 as integer))) <= :endDate)
                AND (cs.code = 'TARV' OR cs.code = 'PPE' OR cs.code = 'PREP' OR cs.code = 'CE' OR cs.code = 'CCR')
                AND c.id = :clinicId
                AND ssr.code in ('NOVO_PACIENTE','INICIO_CCR','TRANSFERIDO_DE','REINICIO_TRATAMETO','MANUNTENCAO','OUTRO','VOLTOU_REFERENCIA', 'REFERIDO_DC')
                AND dt.code = 'DT' 
                group by 1,4,5
                ORDER BY 1 asc
                ) lastPack
                inner join
                (
                select patv.patient_id patid, min(presc.prescription_date) min_prescription_date
                from patient_visit_details patvd
                inner join prescription presc on presc.id = patvd.prescription_id
                inner join patient_visit patv on patv.id = patvd.patient_visit_id
                inner join prescription_detail pred on pred.prescription_id = presc.id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                where dt.code = 'DT'
                group by 1
                ) firstPack on firstPack.patid = lastPack.id
                inner join pack pack2 ON pack2.id = lastPack.packid
                inner join patient_visit_details pvd ON pvd.pack_id = pack2.id
                inner join episode ep on ep.id = pvd.episode_id
                inner join patient_service_identifier psi on psi.id = ep.patient_service_identifier_id
                inner join clinical_service cs ON cs.id = psi.service_id
                inner join patient_visit pv on pv.id = pvd.patient_visit_id
                inner join patient p ON p.id = psi.patient_id
                inner join prescription prc ON prc.id = pvd.prescription_id
                inner join start_stop_reason ssr on ssr.id = ep.start_stop_reason_id
                inner join prescription_detail pred on pred.prescription_id = prc.id
                inner join therapeutic_line tl on tl.id = pred.therapeutic_line_id 
                inner join therapeutic_regimen tr on tr.id = pred.therapeutic_regimen_id
                inner join dispense_type dt ON dt.id = pred.dispense_type_id
                inner join dispense_mode dm ON dm.id = pack2.dispense_mode_id
                inner join clinic c on c.id = ep.clinic_id
                """
        Session session = sessionFactory.getCurrentSession()
        def query = session.createSQLQuery(queryString)
        query.setParameter("startDate", reportSearchParams.startDate)
        query.setParameter("endDate", reportSearchParams.endDate)
        query.setParameter("clinicId", reportSearchParams.clinicId)
        List<Object[]> result = query.list()

        return result
    }

    List<Pack> getAllPacksByPatientAndEpisode(Patient patient, Episode episode) {

        def patientVisits = PatientVisit.findAllByPatient(patient)
        def patientVisitDetails = PatientVisitDetails.findAllByPatientVisitInListAndEpisode(patientVisits,episode)
        def packs = Pack.findAllByIdInList(patientVisitDetails?.pack?.id,
                [sort: "pickupDate", order: "desc"])

        return packs
    }
}
