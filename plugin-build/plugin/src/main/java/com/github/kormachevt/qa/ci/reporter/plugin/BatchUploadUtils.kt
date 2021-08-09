package com.github.kormachevt.qa.ci.reporter.plugin

import khttp.extensions.fileLike
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64
import kotlin.collections.ArrayList

fun getChunkedFileJSONs(resultsDir: String, batch: Int): List<JSONObject> {
    val chunksOfFiles = File(resultsDir).walk()
        .toList()
        .drop(1)
        .map { it.fileLike() }
        .chunked(batch)
    val resultsList = ArrayList<JSONObject>()
    chunksOfFiles.forEach { it ->
        val array = JSONArray()
        it.forEach {
            val item = JSONObject()
            item.put("file_name", it.name)
            item.put("content_base64", convertToBase64(it.contents))
            array.put(item)
        }
        resultsList.add(JSONObject().put("results", array))
    }
    return resultsList
}

private fun convertToBase64(attachment: ByteArray): String {
    return Base64.getEncoder().encodeToString(attachment)
}
