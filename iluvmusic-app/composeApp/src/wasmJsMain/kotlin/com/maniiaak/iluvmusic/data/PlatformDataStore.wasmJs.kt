package com.maniiaak.iluvmusic.data

//actual fun createPreferencesStorage(): PreferencesStorage {
//    return WasmPreferencesStorage()
//}
//
//class WasmPreferencesStorage : PreferencesStorage {
//    private companion object {
//        private const val DATASTORE_NAME = "settings"
//    }
//
//    // Use JS interop to access window.localStorage
//    private external fun getItem(key: String): String?
//    private external fun setItem(key: String, value: String)
//    private external fun removeItem(key: String)
//    private external fun clear()
//
//    override suspend fun getString(key: String): String? = getItem(key)
//
//    override suspend fun setString(key: String, value: String) {
//        setItem(key, value)
//    }
//
//    override suspend fun getInt(key: String): Int? =
//        getItem(key)?.toIntOrNull()
//
//    override suspend fun setInt(key: String, value: Int) {
//        setItem(key, value.toString())
//    }
//
//    override suspend fun getBoolean(key: String): Boolean? =
//        getItem(key)?.toBooleanStrictOrNull()
//
//    override suspend fun setBoolean(key: String, value: Boolean) {
//        setItem(key, value.toString())
//    }
//
//    override suspend fun remove(key: String) = removeItem(key)
//    override suspend fun clear() = clear()
//}