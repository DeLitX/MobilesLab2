package com.delitx.mobileslab2.data

import com.delitx.mobileslab2.db.ProductionDao
import com.delitx.mobileslab2.models.Production
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductionRepositoryImpl @Inject constructor(
    private val productionDao: ProductionDao
) : ProductionRepository {

    private val dispatcher = Dispatchers.IO
    private val scope = CoroutineScope(dispatcher)

    override val allProductions: StateFlow<List<Production>> =
        productionDao.getAllFlow()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    override val harvestHigher25MFlow: StateFlow<List<Production>> =
        productionDao.getHarvestHigher25MFlow()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    override val meanPriceFlow: StateFlow<Int> =
        productionDao.getMeanPriceFlow()
            .filterNotNull()
            .stateIn(scope, SharingStarted.Eagerly, 0)

    override suspend fun addItem(production: Production) = withContext(dispatcher) {
        productionDao.insert(production)
    }
}
