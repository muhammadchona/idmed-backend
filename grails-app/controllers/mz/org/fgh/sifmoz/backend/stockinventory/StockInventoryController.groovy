package mz.org.fgh.sifmoz.backend.stockinventory

import grails.converters.JSON
import grails.rest.RestfulController
import grails.validation.ValidationException
import mz.org.fgh.sifmoz.backend.convertDateUtils.ConvertDateUtils
import mz.org.fgh.sifmoz.backend.stock.Stock
import mz.org.fgh.sifmoz.backend.stock.StockService
import mz.org.fgh.sifmoz.backend.utilities.JSONSerializer
import mz.org.fgh.sifmoz.backend.utilities.Utilities

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK

import grails.gorm.transactions.Transactional

class StockInventoryController extends RestfulController{

    IInventoryService inventoryService
    StockService stockService



    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    StockInventoryController() {
        super(Inventory)
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        render JSONSerializer.setObjectListJsonResponse(inventoryService.list(params)) as JSON
    }

    def show(String id) {
        render JSONSerializer.setJsonObjectResponse(inventoryService.get(id)) as JSON
    }

    @Transactional
    def close(String id, String endDate) {
        Inventory inventory = inventoryService.get(id)

        if (!Utilities.listHasElements(inventory.adjustments as ArrayList<?>)) {
            throw new RuntimeException("Não foram carregados os ajustes deste inventário, impossivel fechar!")
        } else {
            try {
                inventory.close()
                inventory.setEndDate(ConvertDateUtils.createDate(endDate, ConvertDateUtils.DDMM_DATE_FORMAT))
                def adjustmentsTemp = inventory.adjustments
                inventory.adjustments = []
                inventoryService.save(inventory)
                inventory.setAdjustments(adjustmentsTemp)
                inventoryService.processInventoryAdjustments(inventory)
                inventory.adjustments.each { adjustment ->
                    adjustment.save(flush: true)
                    Stock stock = adjustment.adjustedStock
                    stock.stockMoviment = adjustment.getBalance()
                    stockService.save(stock)
                }

            } catch (ValidationException e) {
                respond inventory.errors
                return
            }

            respond inventory, [status: OK, view: "show"]
        }
    }


    @Transactional
    def save() {
        Inventory inventory = new Inventory()
        def objectJSON = request.JSON
        inventory = objectJSON as Inventory


        inventory.beforeInsert()
        inventory.adjustments.eachWithIndex { item, index ->
            item.id = UUID.fromString(objectJSON.adjustments[index].id)
        }
        inventory.validate()

        if(objectJSON.id){
            inventory.id = UUID.fromString(objectJSON.id)
        }
        if (inventory.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond inventory.errors
            return
        }

        try {
            inventoryService.save(inventory)
        } catch (ValidationException e) {
            respond inventory.errors
            return
        }
        def result = JSONSerializer.setJsonObjectResponse(inventory)
        render result as JSON
    }

    @Transactional
    def update() {
        def objectJSON = request.JSON
        Inventory inventoryDb = Inventory.get(objectJSON.id)

        if (inventoryDb == null) {
            render status: NOT_FOUND
            return
        }

        inventoryDb.properties = objectJSON
        inventoryDb.adjustments.eachWithIndex { item, index ->
            objectJSON.adjustments.eachWithIndex { item2, index2 ->
                if (item.adjustedStockId == objectJSON.adjustments[index2].adjustedStockId)
                    item.id = UUID.fromString(objectJSON.adjustments[index].id)
            }
        }

        /*if (!inventoryDb.isOpen()) {
            throw new RuntimeException("O inventário já se encontra fechado.")
        }*/
        if (inventoryDb.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond inventoryDb.errors
            return
        }

        try {
            inventoryService.save(inventoryDb)
        } catch (ValidationException e) {
            respond inventoryDb.errors
            return
        }

        respond inventoryDb, [status: OK, view:"show"]
    }

    @Transactional
    def delete(String id) {


        if (id == null || inventoryService.removeInventory(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }

    def getByClinicId(String clinicId, int offset, int max) {
        respond inventoryService.getAllByClinicId(clinicId, offset, max)
    }

    @Transactional
    def isInventoryPeriod(String clinicId) {
        render inventoryService.isInventoryPeriod(clinicId)
    }

    @Transactional
    def hasInventoryInPreviousMonth(String clinicId) {
        render inventoryService.hasInventoryInPreviousMonth(clinicId)
    }
}