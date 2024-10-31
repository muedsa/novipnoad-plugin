package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.demoplugin.model.BangumiSearchResp
import com.muedsa.tvbox.demoplugin.model.BangumiDetailsResp
import com.muedsa.tvbox.demoplugin.model.BangumiSearch
import com.muedsa.tvbox.demoplugin.model.BangumiShinResp
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DanDanPlayApiService {

    @GET("v2/bangumi/shin")
    suspend fun bangumiShin(): BangumiShinResp

    @GET("v2/search/anime")
    suspend fun searchAnime(
        @Query("keyword") keyword: String,
        @Query("type") type: String = ""
    ): BangumiSearchResp<BangumiSearch>

    @GET("v2/bangumi/{animeId}")
    suspend fun getAnime(
        @Path("animeId") animeId: Int
    ): BangumiDetailsResp

}