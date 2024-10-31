package com.muedsa.tvbox.demoplugin

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaCardType

fun checkMediaCardRows(rows: List<MediaCardRow>) {
    rows.forEach { checkMediaCardRow(it) }
}

fun checkMediaCardRow(row: MediaCardRow) {
    check(row.title.isNotEmpty())
    check(row.list.isNotEmpty())
    check(row.cardWidth > 0)
    check(row.cardHeight > 0)
    row.list.forEach {
        checkMediaCard(card = it, cardType = row.cardType)
    }
}

fun checkMediaCard(card: MediaCard, cardType: MediaCardType) {
    check(card.id.isNotEmpty())
    check(card.title.isNotEmpty())
    check(card.detailUrl.isNotEmpty())
    if (cardType != MediaCardType.NOT_IMAGE) {
        check(card.coverImageUrl.isNotEmpty())
    }  else {
        check(card.backgroundColor > 0)
    }
}