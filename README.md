# Hebrew Voice Assistant (Android)

Speak Hebrew -> on-device STT (`he-IL`) -> ML Kit he->en translation -> Claude parses the
command into a structured action -> executed through standard Android intents
(timer, alarm, dial, SMS, navigation, web search). Unrecognised commands are handed off to
the Google app / Gemini as English text (best effort; Google exposes no public Assistant API).

## Setup
1. Open this folder in a recent Android Studio, let it sync.
2. Add to `local.properties`: `ANTHROPIC_API_KEY=sk-ant-...`
3. Run on a device (needs Google speech services and internet for the first translation-model download).

## Next steps
- Contact-name lookup for calls/SMS (READ_CONTACTS), then direct CALL_PHONE.
- Foreground-service + floating button / wake word (Porcupine).
- Replace the embedded API key with a small backend proxy.
- Hebrew TTS confirmation of what was understood.
