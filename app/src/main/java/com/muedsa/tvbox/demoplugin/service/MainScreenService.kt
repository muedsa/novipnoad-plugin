package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService

class MainScreenService(
    private val danDanPlayApiService: DanDanPlayApiService
) : IMainScreenService {

    private var rowSize: Int = 30

    override suspend fun getRowsData(): List<MediaCardRow> {
        val resp = danDanPlayApiService.bangumiShin()
        if (resp.errorCode != 0) {
            throw RuntimeException(resp.errorMessage)
        }
        if (resp.bangumiList.isEmpty())
            return emptyList()
        val rows = splitListBySize(resp.bangumiList, rowSize)
        return rows.mapIndexed { index, row ->
            MediaCardRow(
                title = "新番列表 ${index + 1}",
                cardWidth = 210 / 2,
                cardHeight = 302 / 2,
                list = row.map {
                    MediaCard(
                        id = it.animeId.toString(),
                        title = it.animeTitle,
                        detailUrl = it.animeId.toString(),
                        coverImageUrl = it.imageUrl
                    )
                }
            )
        }
    }

    private fun <T> splitListBySize(inputList: List<T>, size: Int): List<List<T>> {
        val result = mutableListOf<List<T>>()
        var index = 0
        while (index < inputList.size) {
            result.add(inputList.subList(index, (index + size).coerceAtMost(inputList.size)))
            index += size
        }
        return result
    }
}