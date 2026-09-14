package com.android.messaging.testutil

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

internal fun androidManifestDocument(): Document {
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(androidManifestFile())
}

internal fun Document.elementsByTagName(tagName: String): List<Element> {
    return getElementsByTagName(tagName).toElements()
}

internal fun Element.elementsByTagName(tagName: String): List<Element> {
    return getElementsByTagName(tagName).toElements()
}

private fun NodeList.toElements(): List<Element> {
    return (0 until length).map { index -> item(index) as Element }
}

private fun androidManifestFile(): File {
    val workingDir = requireNotNull(System.getProperty("user.dir"))
    var directory: File? = File(workingDir)

    while (directory != null) {
        val candidate = File(directory, "AndroidManifest.xml")
        if (candidate.exists()) {
            return candidate
        }
        directory = directory.parentFile
    }

    error("Could not locate AndroidManifest.xml from $workingDir")
}
