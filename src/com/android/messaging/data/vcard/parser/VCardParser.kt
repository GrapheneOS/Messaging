package com.android.messaging.data.vcard.parser

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.datamodel.media.CustomVCardEntry
import com.android.messaging.datamodel.media.CustomVCardEntryConstructor
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import com.android.vcard.VCardConfig
import com.android.vcard.VCardEntryCounter
import com.android.vcard.VCardInterpreter
import com.android.vcard.VCardParser as LibVCardParser
import com.android.vcard.VCardParser_V21
import com.android.vcard.VCardParser_V30
import com.android.vcard.VCardParser_V40
import com.android.vcard.VCardSourceDetector
import com.android.vcard.exception.VCardException
import com.android.vcard.exception.VCardNestedException
import com.android.vcard.exception.VCardVersionException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface VCardParser {
    suspend fun parse(vCardUri: String): List<CustomVCardEntry>
}

internal class VCardParserImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : VCardParser {

    override suspend fun parse(vCardUri: String): List<CustomVCardEntry> {
        if (vCardUri.isBlank()) {
            return emptyList()
        }

        return withContext(ioDispatcher) {
            parseBlocking(vCardUri.toUri())
        }
    }

    private fun parseBlocking(uri: Uri): List<CustomVCardEntry> {
        val detector = VCardSourceDetector()
        if (!detectSource(uri, detector)) {
            return emptyList()
        }

        val entries = mutableListOf<CustomVCardEntry>()
        val vcardType = resolveVCardType(detector)
        val constructor = CustomVCardEntryConstructor(vcardType, null)
        constructor.addEntryHandler(collectingEntryHandler(entries))

        val parsed = readOneVCardFile(
            uri = uri,
            vcardType = vcardType,
            interpreter = constructor,
            throwNestedException = false,
        )

        return when {
            parsed -> entries
            else -> emptyList()
        }
    }

    private fun detectSource(
        uri: Uri,
        detector: VCardSourceDetector,
    ): Boolean {
        return try {
            readOneVCardFile(
                uri = uri,
                vcardType = VCardConfig.VCARD_TYPE_UNKNOWN,
                interpreter = detector,
                throwNestedException = true,
            )
        } catch (exception: VCardNestedException) {
            LogUtil.w(LogUtil.BUGLE_TAG, "Nested vCard while detecting source", exception)

            readOneVCardFile(
                uri = uri,
                vcardType = detector.estimatedType,
                interpreter = VCardEntryCounter(),
                throwNestedException = false,
            )
        }
    }

    private fun resolveVCardType(detector: VCardSourceDetector): Int {
        return when (val estimatedType = detector.estimatedType) {
            VCardConfig.VCARD_TYPE_UNKNOWN -> VCardConfig.getVCardTypeFromString(DEFAULT_VCARD_TYPE)
            else -> estimatedType
        }
    }

    private fun collectingEntryHandler(
        entries: MutableList<CustomVCardEntry>,
    ): CustomVCardEntryConstructor.EntryHandler {
        return object : CustomVCardEntryConstructor.EntryHandler {
            override fun onStart() = Unit
            override fun onEnd() = Unit

            override fun onEntryCreated(entry: CustomVCardEntry) {
                entries.add(entry)
            }
        }
    }

    private fun readOneVCardFile(
        uri: Uri,
        vcardType: Int,
        interpreter: VCardInterpreter,
        throwNestedException: Boolean,
    ): Boolean {
        return try {
            parseWithVersionFallback(
                uri = uri,
                vcardType = vcardType,
                interpreter = interpreter,
            )
        } catch (exception: VCardNestedException) {
            if (throwNestedException) {
                throw exception
            }

            LogUtil.w(LogUtil.BUGLE_TAG, "Nested vCard entry ignored", exception)
            false
        } catch (exception: VCardException) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Failed to parse vCard", exception)
            false
        } catch (exception: IOException) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Failed to read vCard", exception)
            false
        } catch (exception: SecurityException) {
            LogUtil.e(LogUtil.BUGLE_TAG, "No permission to read vCard", exception)
            false
        }
    }

    private fun parseWithVersionFallback(
        uri: Uri,
        vcardType: Int,
        interpreter: VCardInterpreter,
    ): Boolean {
        val parserFactories = listOf<(Int) -> LibVCardParser>(
            { version -> VCardParser_V21(version) },
            { version -> VCardParser_V30(version) },
            { version -> VCardParser_V40(version) },
        )

        for ((index, createParser) in parserFactories.withIndex()) {
            if (index > 0 && interpreter is CustomVCardEntryConstructor) {
                interpreter.clear()
            }

            val parsed = tryParseWithVersion(
                uri = uri,
                vcardType = vcardType,
                interpreter = interpreter,
                createParser = createParser,
                isLastVersion = index == parserFactories.lastIndex,
            )

            if (parsed != null) {
                return parsed
            }
        }

        return false
    }

    private fun tryParseWithVersion(
        uri: Uri,
        vcardType: Int,
        interpreter: VCardInterpreter,
        createParser: (Int) -> LibVCardParser,
        isLastVersion: Boolean,
    ): Boolean? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return false

        return try {
            inputStream.use {
                createParser(vcardType).apply {
                    addInterpreter(interpreter)
                }.parse(it)
            }
            true
        } catch (exception: VCardVersionException) {
            LogUtil.e(LogUtil.BUGLE_TAG, "vCard version not supported", exception)

            when {
                isLastVersion -> false
                else -> null
            }
        }
    }

    private companion object {
        private const val DEFAULT_VCARD_TYPE = "default"
    }
}
