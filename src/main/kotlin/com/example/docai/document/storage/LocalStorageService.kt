package com.example.docai.document.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class LocalStorageService(
    @Value("\${storage.local.root}") private val root: String
) : DocumentStorage {

    init {
        Files.createDirectories(Path.of(root))
    }

    override fun save(input: InputStream, filename: String, contentType: String): String {
        val key = "${UUID.randomUUID()}_$filename"
        val targetPath = Path.of(root, key)
        Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        return key
    }

    override fun load(storageKey: String): InputStream {
        val path = Path.of(root, storageKey)
        require(Files.exists(path)) { "File not found: $storageKey" }
        return Files.newInputStream(path)
    }

    override fun delete(storageKey: String) {
        val path = Path.of(root, storageKey)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }
}
