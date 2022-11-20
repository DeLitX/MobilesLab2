package com.delitx.mobileslab2.data

import com.delitx.mobileslab2.db.ProductionDao
import com.delitx.mobileslab2.models.Production
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductionRepositoryImpl @Inject constructor(
    private val productionDao: ProductionDao
) : ProductionRepository {

    private val dispatcher = Dispatchers.IO

    override val allProductions: StateFlow<List<Production>> =
        productionDao.getAllFlow()
    override val harvestHigher25MFlow: StateFlow<List<Production>> =
        productionDao.getHarvestHigher25MFlow()
    override val meanPriceFlow: StateFlow<Int> =
        productionDao.getMeanPriceFlow()

    override suspend fun addItem(production: Production) = withContext(dispatcher) {
        productionDao.insert(production)
    }
}
