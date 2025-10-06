package com.example.docai.document.storage

import java.io.InputStream

interface DocumentStorage {
    fun save(input: InputStream, filename: String, contentType: String): String
    fun load(storageKey: String): InputStream
    fun delete(storageKey: String)
}
