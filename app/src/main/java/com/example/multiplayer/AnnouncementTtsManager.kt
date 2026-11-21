package com.example.multiplayer

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TTS_TAG = "AnnouncementTTS"

data class AnnouncementPreset(
  val id: String,
  val title: String,
  val hindiText: String,
)

object AnnouncementPresets {
  const val WELCOME_ANNOUNCEMENT = "Welcome to the Bigg Boss – The House Reality Show!"

  val all: List<AnnouncementPreset> = listOf(
    AnnouncementPreset(
      id = "welcome_house",
      title = "Official House Welcome",
      hindiText = WELCOME_ANNOUNCEMENT,
    ),
    AnnouncementPreset(
      id = "living_gather",
      title = "Living Area Assembly",
      hindiText = "बिग बॉस चाहते हैं कि सभी घरवाले तुरंत मुख्य लिविंग एरिया में एकत्रित हों।",
    ),
    AnnouncementPreset(
      id = "nominations_begin",
      title = "Nominations Begin",
      hindiText = "घरवालों, इस सप्ताह की नॉमिनेशन प्रक्रिया अब शुरू हो रही है।",
    ),
    AnnouncementPreset(
      id = "voting_open",
      title = "BiggGram NPC Voting Open",
      hindiText = "बिगग्राम एनपीसी वोटिंग लाइन्स अब खुल चुकी हैं।",
    ),
    AnnouncementPreset(
      id = "cctv_notice",
      title = "24/7 CCTV Monitoring",
      hindiText = "सभी सामान्य क्षेत्रों की 24 घंटे सीसीटीवी कैमरों द्वारा निगरानी की जा रही है।",
    ),
    AnnouncementPreset(
      id = "eviction_time",
      title = "Eviction Announcement",
      hindiText = "अब समय आ गया है इस सप्ताह के एलिमिनेशन परिणाम की घोषणा करने का।",
    ),
  )
}

private data class QueuedTtsUtterance(
  val id: String,
  val message: String,
  val languageTag: String,
  val voiceGender: VoiceGender,
)

class AnnouncementTtsManager(context: Context) {
  private val appContext = context.applicationContext
  private var tts: TextToSpeech? = null
  private var femaleVoice: Voice? = null
  private var maleVoice: Voice? = null
  private var englishVoice: Voice? = null

  private val queueLock = Any()
  private val utteranceQueue = ArrayDeque<QueuedTtsUtterance>()
  private val spokenDeduplicationIds = LinkedHashSet<String>()
  private var isCurrentlySpeaking = false
  private var settings = TtsPlaybackSettings()

  private val _status = MutableStateFlow(TtsStatusSnapshot())
  val status: StateFlow<TtsStatusSnapshot> = _status.asStateFlow()

  init {
    tts = TextToSpeech(appContext) { initStatus ->
      if (initStatus == TextToSpeech.SUCCESS) {
        configureIndianHindiVoices()
        attachProgressListener()
      } else {
        Log.w(TTS_TAG, "TextToSpeech init failed with status=$initStatus")
        _status.value = TtsStatusSnapshot(
          ready = false,
          hindiAvailable = false,
          statusMessage = "TTS engine unavailable on this device.",
        )
      }
    }
  }

  private fun attachProgressListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) = Unit

      override fun onDone(utteranceId: String?) {
        advanceQueue()
      }

      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        advanceQueue()
      }

      override fun onError(utteranceId: String?, errorCode: Int) {
        advanceQueue()
      }
    })
  }

  private fun configureIndianHindiVoices() {
    val engine = tts ?: return
    val hiLocale = Locale.forLanguageTag("hi-IN")
    val langResult = engine.setLanguage(hiLocale)
    val hindiSupported = langResult != TextToSpeech.LANG_MISSING_DATA &&
      langResult != TextToSpeech.LANG_NOT_SUPPORTED

    val allVoices = try {
      engine.voices.orEmpty()
    } catch (e: Exception) {
      Log.w(TTS_TAG, "Could not enumerate TTS voices: ${e.message}")
      emptySet()
    }

    val hiVoices = allVoices.filter { v ->
      v.locale.language.equals("hi", ignoreCase = true)
    }

    femaleVoice = hiVoices.firstOrNull { v ->
      val n = v.name.lowercase()
      n.contains("female") || n.contains("-hia-") || n.contains("-hic-") || n.contains("in-x-hi")
    } ?: hiVoices.firstOrNull()

    maleVoice = hiVoices.firstOrNull { v ->
      val n = v.name.lowercase()
      (n.contains("male") && !n.contains("female")) || n.contains("-hid-") || n.contains("-hie-")
    } ?: hiVoices.lastOrNull()?.takeIf { it.name != femaleVoice?.name }

    englishVoice = allVoices.firstOrNull { v ->
      v.locale.language.equals("en", ignoreCase = true) && v.locale.country.equals("IN", ignoreCase = true)
    } ?: allVoices.firstOrNull { v -> v.locale.language.equals("en", ignoreCase = true) }

    val msg = when {
      !hindiSupported -> "hi-IN voice pack not installed; visual speaker banner active, audio uses system fallback."
      femaleVoice != null && maleVoice != null -> "Indian Hindi (hi-IN) & English (en-IN) speaker voices ready."
      femaleVoice != null -> "Indian Hindi (hi-IN) & English ready; Male uses pitch shift."
      else -> "hi-IN / en-IN active via system speech engine."
    }

    _status.value = TtsStatusSnapshot(
      ready = true,
      hindiAvailable = hindiSupported,
      hasFemaleVoice = femaleVoice != null,
      hasMaleVoice = maleVoice != null,
      statusMessage = msg,
    )
    drainNextIfIdle()
  }

  fun updateSettings(next: TtsPlaybackSettings) {
    settings = next
    if (!next.enabled) {
      stopAndClearQueue()
    }
  }

  fun speakPublicAnnouncement(
    dedupKey: String?,
    message: String,
    languageTag: String = settings.languageTag,
    voiceGender: VoiceGender = VoiceGender.FEMALE,
    isPrivateDm: Boolean = false,
  ): Boolean {
    // Hard privacy guarantee: Private DMs MUST NEVER be spoken through public house speakers
    if (isPrivateDm || !settings.enabled) return false
    val clean = message.trim()
    if (clean.isBlank()) return false

    synchronized(queueLock) {
      if (!dedupKey.isNullOrBlank()) {
        if (spokenDeduplicationIds.contains(dedupKey)) return false
        spokenDeduplicationIds.add(dedupKey)
        if (spokenDeduplicationIds.size > 120) {
          spokenDeduplicationIds.iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
        }
      }
      val uid = dedupKey?.takeIf { it.isNotBlank() } ?: "bb_${System.currentTimeMillis()}_${utteranceQueue.size}"
      utteranceQueue.addLast(QueuedTtsUtterance(uid, clean, languageTag, voiceGender))
    }
    return drainNextIfIdle()
  }

  fun speak(
    message: String,
    languageTag: String = settings.languageTag,
    voiceGender: VoiceGender = VoiceGender.FEMALE,
  ): Boolean {
    if (!settings.enabled) return false
    val clean = message.trim()
    if (clean.isBlank()) return false
    val engine = tts ?: return false
    synchronized(queueLock) {
      utteranceQueue.clear()
      isCurrentlySpeaking = false
    }
    return playUtteranceNow(engine, QueuedTtsUtterance("bb_imm_${System.currentTimeMillis()}", clean, languageTag, voiceGender))
  }

  private fun advanceQueue() {
    synchronized(queueLock) {
      isCurrentlySpeaking = false
    }
    drainNextIfIdle()
  }

  private fun drainNextIfIdle(): Boolean {
    val engine = tts ?: return false
    val next = synchronized(queueLock) {
      if (isCurrentlySpeaking || utteranceQueue.isEmpty() || !settings.enabled) return false
      isCurrentlySpeaking = true
      utteranceQueue.removeFirst()
    }
    return playUtteranceNow(engine, next)
  }

  private fun playUtteranceNow(engine: TextToSpeech, item: QueuedTtsUtterance): Boolean {
    val targetLocale = Locale.forLanguageTag(item.languageTag.ifBlank { settings.languageTag })
    engine.setLanguage(targetLocale)

    val isEnglish = targetLocale.language.equals("en", ignoreCase = true)
    val baseRate = settings.speechRate.coerceIn(0.6f, 1.6f)

    when (item.voiceGender) {
      VoiceGender.FEMALE -> {
        (if (isEnglish) englishVoice else femaleVoice)?.let { engine.voice = it }
        engine.setPitch(1.06f)
        engine.setSpeechRate(baseRate)
      }
      VoiceGender.MALE -> {
        if (!isEnglish && maleVoice != null) {
          engine.voice = maleVoice
          engine.setPitch(0.92f)
        } else {
          (if (isEnglish) englishVoice else femaleVoice)?.let { engine.voice = it }
          engine.setPitch(0.78f)
        }
        engine.setSpeechRate((baseRate * 0.96f).coerceIn(0.6f, 1.5f))
      }
    }

    val params = Bundle().apply {
      putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, item.id)
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume.coerceIn(0f, 1f))
    }
    val res = engine.speak(item.message, TextToSpeech.QUEUE_ADD, params, item.id)
    if (res != TextToSpeech.SUCCESS) {
      synchronized(queueLock) { isCurrentlySpeaking = false }
    }
    return res == TextToSpeech.SUCCESS
  }

  fun stopAndClearQueue() {
    synchronized(queueLock) {
      utteranceQueue.clear()
      isCurrentlySpeaking = false
    }
    stop()
  }

  fun stop() {
    try {
      tts?.stop()
    } catch (_: Exception) {
    }
  }

  fun shutdown() {
    synchronized(queueLock) {
      utteranceQueue.clear()
      isCurrentlySpeaking = false
    }
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {
    }
    tts = null
  }
}
