package mz.org.fgh.sifmoz.backend.drugDistributor

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import grails.rest.RestfulController
import grails.validation.ValidationException
import mz.org.fgh.sifmoz.backend.clinic.Clinic
import mz.org.fgh.sifmoz.backend.drug.Drug
import mz.org.fgh.sifmoz.backend.stock.IStockService
import mz.org.fgh.sifmoz.backend.stock.Stock
import mz.org.fgh.sifmoz.backend.stockDistributorBatch.StockDistributorBatch
import mz.org.fgh.sifmoz.backend.stockDistributorBatch.StockDistributorBatchService
import mz.org.fgh.sifmoz.backend.stockadjustment.StockReferenceAdjustment
import mz.org.fgh.sifmoz.backend.stockadjustment.StockReferenceAdjustmentService
import mz.org.fgh.sifmoz.backend.stockentrance.StockEntrance
import mz.org.fgh.sifmoz.backend.stockentrance.StockEntranceService
import mz.org.fgh.sifmoz.backend.stockoperation.StockOperationType
import mz.org.fgh.sifmoz.backend.stockrefered.ReferedStockMoviment
import mz.org.fgh.sifmoz.backend.stockrefered.ReferedStockMovimentService
import mz.org.fgh.sifmoz.backend.utilities.JSONSerializer

import static org.springframework.http.HttpStatus.*

class DrugDistributorController extends RestfulController {

    StockDistributorBatchService stockDistributorBatchService
    StockReferenceAdjustmentService stockReferenceAdjustmentService
    ReferedStockMovimentService referedStockMovimentService
    IDrugDistributorService drugDistributorService
    StockEntranceService stockEntranceService

    IStockService stockService
    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    DrugDistributorController() {
        super(Stock)
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond drugDistributorService.list(params)
    }

    def show(String id) {
        render JSONSerializer.setJsonObjectResponse(drugDistributorService.get(id)) as JSON
    }

    @Transactional
    def save() {
        DrugDistributor drugDistributor = new DrugDistributor()
        def objectJSON = request.JSON
        drugDistributor = objectJSON as DrugDistributor

        drugDistributor.beforeInsert()
        drugDistributor.validate()

        if (objectJSON.id) {
            drugDistributor.id = UUID.fromString(objectJSON.id)
        }
        if (drugDistributor.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond drugDistributor.errors
            return
        }

        try {
            makeDistribution(drugDistributor)
        } catch (ValidationException e) {
            respond drugDistributor.errors
            return
        }

        respond drugDistributor, [status: CREATED, view: "show"]
    }

    @Transactional
    def update() {
        def objectJSON = request.JSON

        DrugDistributor drugDistributor = DrugDistributor.findWhere(id: objectJSON.id)

        if (drugDistributor == null) {
            render status: NOT_FOUND
            return
        }
        if (drugDistributor.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond drugDistributor.errors
            return
        }

        try {

            drugDistributor.quantity = Integer.parseInt(objectJSON.getAt("quantity").toString())
            drugDistributor.drug = Drug.findWhere(id: objectJSON?.getAt("drug")?.getAt("id")?.toString())
            drugDistributor.clinic = Clinic.findWhere(id: objectJSON?.getAt("clinic")?.getAt("id")?.toString())
            drugDistributorService.save(drugDistributor)
        } catch (ValidationException e) {
            respond drugDistributor.errors
            return
        }

        respond drugDistributor, [status: OK, view: "show"]
    }

    @Transactional
    def delete(String id) {

        if (id == null || drugDistributorService.delete(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }


    void makeDistribution(DrugDistributor drugDistributor) {

        drugDistributorService.save(drugDistributor)

        int quantityAux = drugDistributor.quantity
        List<Stock> stocks = stockService.getValidStockByDrug(drugDistributor.getDrug())
        if (stocks.size() == 0) render status: NO_CONTENT

        ReferedStockMoviment reference = new ReferedStockMoviment()
        reference.id = UUID.randomUUID()
        reference.setClinic(drugDistributor.getStockDistributor().getClinic())
        reference.setOrderNumber("Ordem_Ajuste_Distribuicao")
        reference.setOrigin("Ajuste_Distribuicao")
        reference.setDate(new Date())
        reference.setQuantity(quantityAux)
        reference.updateStatus = 'P'

        // Validate if there is stock
        for (Stock stock : stocks) {

            StockReferenceAdjustment stockReferenceAdjustment = new StockReferenceAdjustment()
            StockDistributorBatch stockDistributorBatch = new StockDistributorBatch()
            stockDistributorBatch.id = UUID.randomUUID()
            stockDistributorBatch.setStock(stock)
            stockDistributorBatch.setDrugDistributor(drugDistributor)
            stockDistributorBatch.setStockDistributor(drugDistributor.getStockDistributor())
            if (quantityAux != 0) {
                if (stock.stockMoviment >= quantityAux) {
                    //Create Negative adjustment
                    stock.stockMoviment = stock.stockMoviment - quantityAux
                    stockReferenceAdjustment.adjustedValue = quantityAux
                    stockReferenceAdjustment.setBalance(stock.stockMoviment)
                    stockDistributorBatch.setQuantity(quantityAux)
                    quantityAux = 0

                } else {
                    quantityAux = quantityAux - stock.stockMoviment
                    stockReferenceAdjustment.adjustedValue = stock.stockMoviment
                    stockReferenceAdjustment.setBalance(stock.stockMoviment)
                    stockDistributorBatch.setQuantity(stock.stockMoviment)
                    stock.stockMoviment = 0
                }
                stockService.save(stock)
                stockDistributorBatchService.save(stockDistributorBatch)

                stockReferenceAdjustment.id = UUID.randomUUID()
                stockReferenceAdjustment.setOperation(StockOperationType.findByCode("AJUSTE_NEGATIVO"))
                stockReferenceAdjustment.setAdjustedStock(stock)

                stockReferenceAdjustment.setCaptureDate(drugDistributor.getStockDistributor().getCreationDate())
                stockReferenceAdjustment.setClinic(drugDistributor.getStockDistributor().getClinic())
                stockReferenceAdjustment.setNotes("Distribuicao de medicamentos para os sectores clinicos")
                stockReferenceAdjustment.setReference(reference)
                reference.adjustments = new HashSet<StockReferenceAdjustment>()
                reference.adjustments.add(stockReferenceAdjustment)
                //  stockReferenceAdjustmentService.save(stockReferenceAdjustment)
                referedStockMovimentService.save(reference)

            } else {
                break;
            }
        }
    }


    @Transactional
    def updateDrugDistributorStatus(String idDrugDistributor, String status) {
        DrugDistributor drugDistributor = drugDistributorService.get(idDrugDistributor)
        List<StockDistributorBatch> batchs = stockDistributorBatchService.getStockDistributorBatchByDrugDistributorId(idDrugDistributor)

        // A repeated confirmation (for example, after a client timeout/retry)
        // must not credit the receiving clinic's stock more than once.
        if (status.equalsIgnoreCase("C") && drugDistributor.status?.equalsIgnoreCase("C")) {
            respond drugDistributor, [status: OK, view: "show"]
            return
        }

        drugDistributor.setStatus(status)
        if (status.equalsIgnoreCase("C")) {
            List<Stock> newStockList = new ArrayList<>()
            StockEntrance entrance = stockEntranceService.getByClinicIdAndOrderNumber(drugDistributor.clinicId,"Dist_" + drugDistributor.stockDistributor.orderNumber)
            if (!entrance) entrance = getStockEntranceInstance(drugDistributor)
            for (StockDistributorBatch batch : batchs) {
                // Every confirmed distribution is a new receipt. The same batch
                // number may legitimately appear in different stock entrances,
                // so it must not be merged into an arbitrary existing Stock row.
                generateNewStocksList(batch, newStockList, entrance)
            }

            if (!newStockList.isEmpty()) {
                newStockList.each { Stock receivedStock ->
                    entrance.addToStocks(receivedStock)
                }
                stockEntranceService.save(entrance)
            }
        }  else if (status.equalsIgnoreCase("A") ||   status.equalsIgnoreCase("R")  ) {
            // processo para anular ordem 

            // 1 reverter o valor do stock moviment nos lotes  (stocks) debitados
            List<StockDistributorBatch> batches = StockDistributorBatch.findAllByDrugDistributor( drugDistributor)
            for (StockDistributorBatch batch: batches) {
                Stock stock = batch.getStock()
                stock.stockMoviment = stock.stockMoviment +batch.quantity
                stockService.save(stock)

                // Criar um ajuste positivo (anulacao)
                ReferedStockMoviment reference = new ReferedStockMoviment()
                reference.id = UUID.randomUUID()
                reference.setClinic(batch.getStockDistributor().getClinic())
                reference.setOrderNumber("Anulacao_Ajuste_Distribuicao")
                reference.setOrigin("Anulacao_Ajuste_Distribuicao")
                reference.setDate(new Date())
                reference.setQuantity(batch.quantity)
                reference.updateStatus = 'P'

                StockReferenceAdjustment stockReferenceAdjustment = new StockReferenceAdjustment()
                stockReferenceAdjustment.adjustedValue = batch.quantity
                stockReferenceAdjustment.setBalance(stock.stockMoviment)
                stockReferenceAdjustment.id = UUID.randomUUID()
                stockReferenceAdjustment.setOperation(StockOperationType.findByCode("AJUSTE_POSETIVO"))
                stockReferenceAdjustment.setAdjustedStock(stock)
                stockReferenceAdjustment.setCaptureDate(batch.getStockDistributor().getCreationDate())
                stockReferenceAdjustment.setClinic(batch.getStockDistributor().getClinic())
                stockReferenceAdjustment.setNotes("Anulacao  de Distribuicao de medicamentos")
                stockReferenceAdjustment.setReference(reference)

                reference.adjustments = new HashSet<StockReferenceAdjustment>()
                reference.adjustments.add(stockReferenceAdjustment)
                referedStockMovimentService.save(reference)
            }
        }
        drugDistributorService.save(drugDistributor)
        respond drugDistributor, [status: OK, view: "show"]
    }

    private StockEntrance getStockEntranceInstance(DrugDistributor drugDistributor) {
        StockEntrance entrance = new StockEntrance()
        entrance.setOrderNumber("Dist_" + drugDistributor.getStockDistributor().getOrderNumber())
        entrance.setClinic(drugDistributor.getClinic())
        entrance.setCreationDate(new Date())
        entrance.setDateReceived(new Date())
        entrance.setIsDistribution(true)
        entrance.setNotes("Entrada criada aparitir de distribuicao")
        entrance.id = UUID.randomUUID()
        return entrance
    }

    private void generateNewStocksList(StockDistributorBatch batch, ArrayList<Stock> newStockList, StockEntrance entrance) {
        Stock newStock = new Stock()
        newStock.id = UUID.randomUUID()
        newStock.setClinic(batch.getDrugDistributor().getClinic())
        newStock.setBatchNumber(batch.getStock().getBatchNumber())
        newStock.setCenter(batch.getStock().getCenter())
        newStock.setManufacture(batch.getStock().getManufacture())
        newStock.setExpireDate(batch.getStock().getExpireDate())
        newStock.setUnitsReceived(batch.getQuantity())
        newStock.setDrug(batch.getDrugDistributor().getDrug())
        newStock.setEntrance(entrance)
        newStock.setStockMoviment(batch.getQuantity())
        newStockList.push(newStock)
    }

    def getByClinicId(String clinicId, int offset, int max) {
        return drugDistributorService.getAllByClinicId(clinicId, offset, max)
    }

    def getDistributionsByStatus(String  clinicSectorId, String status) {
            render   JSONSerializer.setObjectListJsonResponse(DrugDistributor.findAllByClinicAndStatus(Clinic.findById(clinicSectorId),status)) as JSON
    }

}
