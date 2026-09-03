package com.solomondesign.app.ui.voicenote

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * EN<->ES text translation for Voice notes. An interface so screens can be exercised without
 * ML Kit; the real implementation is [MlKitNoteTranslator].
 */
interface NoteTranslator {
    /**
     * Fire-and-forget: starts downloading both translation models so a later [translate] is
     * instant. Safe to call repeatedly; a no-op once the models are on the device.
     */
    fun prepare()

    suspend fun translate(text: String, from: VoiceNoteLanguage): Result<String>

    /** Releases native translator resources. The instance is unusable afterwards. */
    fun close()
}

/**
 * On-device ML Kit translation — offline once the ~30MB-per-language models have downloaded
 * (which [prepare] kicks off over any network). Nothing spoken ever leaves the device.
 */
class MlKitNoteTranslator : NoteTranslator {

    private val clients = mutableMapOf<VoiceNoteLanguage, Translator>()

    /** One client per direction, keyed by source language (the target is always the other). */
    private fun client(from: VoiceNoteLanguage): Translator = clients.getOrPut(from) {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(mlKitCode(from))
                .setTargetLanguage(mlKitCode(from.other()))
                .build(),
        )
    }

    override fun prepare() {
        val conditions = DownloadConditions.Builder().build()
        VoiceNoteLanguage.entries.forEach { from ->
            client(from).downloadModelIfNeeded(conditions)
        }
    }

    override suspend fun translate(text: String, from: VoiceNoteLanguage): Result<String> {
        if (text.isBlank()) return Result.success("")
        return suspendCancellableCoroutine { continuation ->
            client(from).translate(text)
                .addOnSuccessListener { translated ->
                    if (continuation.isActive) continuation.resume(Result.success(translated))
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resume(Result.failure(error))
                }
        }
    }

    override fun close() {
        clients.values.forEach { it.close() }
        clients.clear()
    }

    private fun mlKitCode(language: VoiceNoteLanguage): String = when (language) {
        VoiceNoteLanguage.ENGLISH -> TranslateLanguage.ENGLISH
        VoiceNoteLanguage.SPANISH -> TranslateLanguage.SPANISH
    }
}
