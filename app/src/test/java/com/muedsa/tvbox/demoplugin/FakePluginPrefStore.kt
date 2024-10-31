package com.muedsa.tvbox.demoplugin

import com.muedsa.tvbox.api.store.IPluginPerfStore
import com.muedsa.tvbox.api.store.PluginPerfKey

@Suppress("UNCHECKED_CAST")
class FakePluginPrefStore : IPluginPerfStore {

    private val store: MutableMap<String, Any> = mutableMapOf()

    override suspend fun <T> get(key: PluginPerfKey<T>): T? =
        store[key.name] as T?

    override suspend fun <T> getOrDefault(key: PluginPerfKey<T>, default: T): T =
        store[key.name] as T? ?: default

    override suspend fun filter(predicate: (String) -> Boolean): Map<String, Any> =
        store.filter { predicate(it.key) }

    override suspend fun <T> update(key: PluginPerfKey<T>, value: T) {
        store[key.name] = value as Any
    }

    override suspend fun <T> remove(key: PluginPerfKey<T>) {
        store.remove(key.name)
    }
}