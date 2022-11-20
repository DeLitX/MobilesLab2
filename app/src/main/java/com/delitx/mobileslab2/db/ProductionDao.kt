package com.delitx.mobileslab2.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delitx.mobileslab2.models.Production
import kotlinx.coroutines.flow.StateFlow

@Dao
interface ProductionDao {

    @Insert
    suspend fun insert(item: Production)

    @Query("select * from production")
    fun getAllFlow(): StateFlow<List<Production>>

    @Query("select * from production where mass >= 25000000")
    fun getHarvestHigher25MFlow(): StateFlow<List<Production>>

    @Query("select Avg(price) from production")
    fun getMeanPriceFlow(): StateFlow<Int>
}
