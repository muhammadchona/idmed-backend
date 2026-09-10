package mz.org.fgh.sifmoz.backend.group

import grails.converters.JSON
import grails.rest.RestfulController
import grails.validation.ValidationException
import mz.org.fgh.sifmoz.backend.groupMember.GroupMember
import mz.org.fgh.sifmoz.backend.prescription.Prescription
import mz.org.fgh.sifmoz.backend.utilities.JSONSerializer

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK

import grails.gorm.transactions.Transactional


class groupMemberPrescriptionController extends RestfulController{

    GroupPrescriptionService groupPrescriptionService

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    groupMemberPrescriptionController() {
        super(GroupMemberPrescription.class)
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond groupPrescriptionService.list(params), model:[groupPrescriptionCount: groupPrescriptionService.count()]
    }

    def show(String id) {
        GroupMemberPrescription groupMemberPrescription = GroupMemberPrescription.findByIdAndUsed(id, false)
        if (groupMemberPrescription == null)
            render status: NO_CONTENT
        else
            render JSONSerializer.setJsonObjectResponse(groupMemberPrescription) as JSON
    }

    def getByMemberId(String memberId) {
        List<GroupMemberPrescription> groupMemberPrescriptions = GroupMemberPrescription.findAllByMember(GroupMember.findById(memberId))
        def latestGroupMemberPrescription  = null
        groupMemberPrescriptions.each {it ->
            if (latestGroupMemberPrescription == null ) {
                latestGroupMemberPrescription = it;
            } else if (it.prescription.prescriptionDate > latestGroupMemberPrescription.prescription.prescriptionDate) {
                latestGroupMemberPrescription = it;
            }
        }
        def groupMemberPrescription = latestGroupMemberPrescription
        if (groupMemberPrescription == null)
            render status: NO_CONTENT
        else
            render JSONSerializer.setJsonObjectResponse(groupMemberPrescription) as JSON
    }

    @Transactional
    def save() {

        GroupMemberPrescription groupPrescription = new GroupMemberPrescription()
        def objectJSON = request.JSON
        groupPrescription = objectJSON as GroupMemberPrescription

        groupPrescription.beforeInsert()
        groupPrescription.validate()

        if(objectJSON.id){
            groupPrescription.id = UUID.fromString(objectJSON.id)
            groupPrescription.prescription.id = UUID.fromString(objectJSON.prescription.id)
            groupPrescription.prescription.prescribedDrugs.eachWithIndex { item, index ->
                item.id = UUID.fromString(objectJSON.prescription.prescribedDrugs[index].id)
                item.clinic = groupPrescription.prescription.clinic
            }
            groupPrescription.prescription.prescriptionDetails.eachWithIndex { item, index ->
                item.id = UUID.fromString(objectJSON.prescription.prescriptionDetails[index].id)
                item.clinic = groupPrescription.prescription.clinic
            }
        }

        if (groupPrescription.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond groupPrescription.errors
            return
        }

        try {
            groupPrescriptionService.save(groupPrescription)
        } catch (ValidationException e) {
            respond groupPrescription.errors
            return
        }
        def groupMemberPrescriptionJson = JSONSerializer.setJsonObjectResponse(groupPrescription.prescription as Object)
        def groupMemberJson = JSONSerializer.setJsonObjectResponse(groupPrescription.member as Object)
        def result = JSONSerializer.setJsonObjectResponse(groupPrescription)
        if (groupMemberPrescriptionJson)
            result.put('prescription', groupMemberPrescriptionJson)
        else
            result.remove('prescription')
        if (groupMemberJson)
            result.put('member', groupMemberJson)
        else
            result.remove('member')
        render result as JSON
     //   respond groupPrescription, [status: CREATED, view:"show"]
    }

    @Transactional
    def update(GroupMemberPrescription groupPrescription) {
        if (groupPrescription == null) {
            render status: NOT_FOUND
            return
        }
        if (groupPrescription.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond groupPrescription.errors
            return
        }

        try {
            groupPrescriptionService.save(groupPrescription)
        } catch (ValidationException e) {
            respond groupPrescription.errors
            return
        }

        respond groupPrescription, [status: OK, view:"show"]
    }

    @Transactional
    def delete(String id) {
        if (id == null || groupPrescriptionService.delete(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }
}
