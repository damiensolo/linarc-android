# Voice-to-Log: manual verification script

Everything except your actual voice is now real: real `RECORD_AUDIO` permission, real
`MediaRecorder` audio capture, real Android `SpeechRecognizer` transcription, a real
(rule-based) local parser over the real transcript, and real playback of the saved
file. I can't speak into the emulator's microphone myself, so this last mile —
"does it correctly transcribe an actual human voice" — needs you.

Entry point is the **Capture FAB** (not a Capture tab). After submit, the log must
appear on **Today** and any issue (e.g. Column 4 spalling) must pin on **Plan**.

## 1. One-time setup

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd Punchlist
./gradlew installDebug

# Pre-grant the mic permission so you skip the system dialog (optional — the app
# will prompt for it on first use anyway if you skip this).
adb shell pm grant com.solomondesign.punchlist android.permission.RECORD_AUDIO
```

If you're on a **physical device**, plug it in, enable USB debugging, and swap
`adb` targeting accordingly (`adb devices` to confirm). Emulators work too, but
**your host machine's microphone must be routed to the emulator** — in Android
Studio's emulator toolbar: **Extended controls (⋮) → Microphone → Virtual
microphone uses host audio input**, or launch with `-use-host-mic` (varies by
Studio version). Without that, `SpeechRecognizer` will hear only silence.

## 2. The test

1. Open the app as **Foreman** (default) → **Today**.
2. Optional: tap **Start My Day** and **Confirm**.
3. Tap the **Capture FAB** (bottom-end “+”) → **Voice daily log**.
4. Approve the microphone permission if prompted.
5. You should see **"RECORDING"**, a live timer, and the waveform reacting to real
   sound (try clapping near the mic — the bars should visibly jump).
6. Say, out loud, the example line from the spec:
   > "Hey, it's Hector. Me, Dave, and the crew worked on the structural framing in
   > Area B today. We used about 40 studs, but we ran out of the 12-footers
   > because the delivery got delayed. Rain started at 2 PM, so we had to tarp
   > everything and call it a day early. Also, we noticed some minor spalling on
   > the concrete slab near column 4, needs an inspector to look at it."
7. Watch the transcript update live under the waveform as you talk (this is the
   real, partial `SpeechRecognizer` output — expect it to lag/restart every few
   seconds of silence; that's the continuous-dictation loop re-arming, not a bug).
8. Tap **Stop & Parse**.
9. On **Proposed Site Logs**, expect roughly:
   - **Labor & Time Cards**: Hector Ortiz and Dave Miller, 8.0 hrs each (8.0 is a
     default — the spoken line never actually states hours, same as the spec).
   - **Materials Installed**: a "Stud" card, quantity 40.
   - **Site Delays & Weather**: a "Material Delivery" card and a "Weather Event"
     card (their hours will default to 1.0 unless you also say "X hours" near
     those words — the parser is regex-based, not a full LLM; see limitations).
   - **Automated Issues / Photos**: a "Spalling" card, location "Column 4".
10. Try editing an hours value (pencil icon) and deleting a card (✕) — both should
    update immediately.
11. Tap **Submit** → confirmation screen → **Done**.
12. Back on **Today**, you should see the new delay/issue (and a voice log card).
    Open **Plan** — a pin should sit near **Column 4**. Tap it.
13. **More → Voice logs** → tap the new entry.
14. Tap **Play Recording** — you should hear your actual voice played back (if you
    opted into saving audio). Tap again to pause.

## 3. If something looks wrong

- **Stuck on "Microphone access needed" after granting permission**: force-stop
  and relaunch the app (`adb shell am force-stop com.solomondesign.punchlist`).
- **Transcript never appears / stays "Listening…"**: the emulator's mic isn't
  routed to your host mic (see setup step above), or there's no network (the
  on-device model needs its language pack downloaded once; until then it falls
  back to Google's online recognizer, which needs connectivity).
- **A real error message appears under the waveform** (not "Listening…"): that's
  `SpeechRecognizer` genuinely failing (busy/network/audio) — it's surfaced as-is,
  not swallowed.
- **Nothing gets extracted even though you spoke clearly**: check the on-screen
  transcript first — if the *words* are right but *nothing* was extracted, the
  parser's keyword list didn't match (see limitations below); that's a parser
  gap, not a capture failure.
- **Today/Plan did not update after Submit**: the closed loop is broken — submitted
  cards must be published into the shared demo store, not only `DailyLogRepository`.

## 4. Known, deliberate limitations (ask before I'd change these)

- **The parser is regex/keyword matching, not a neural model.** It only
  recognizes a small hardcoded roster (`VoiceLogParser.kt`): 4 crew names, ~7
  material keywords, a handful of delay/issue keywords. A real on-device LLM
  (e.g. MediaPipe LLM Inference, or Gemini Nano via AICore) would generalize far
  better but is a much larger dependency + device-support surface — flagging
  per this project's "ask before adding a new dependency" rule rather than
  adding it unasked.
- **Submitted logs are in-memory only** (`DailyLogRepository` / demo store) — they
  disappear if the app process dies. Persisting them for real needs Room (a new
  dependency) or another storage layer — also flagging rather than adding.
- **Hours/quantities are only detected when actually spoken.** The spec's own
  example transcript doesn't state delay durations either — that JSON figure
  was the spec's *illustrative* LLM inference, not something in the audio.
