<p align="center">
  <img src="ui/common/src/main/res/drawable-nodpi/logo_monochrome.png" alt="PodPulse logo" width="128" height="128"/>
</p>

<h1 align="center">PodPulse</h1>

<p align="center">
  An open-source podcast client for Android, focused on a calmer interface,<br>
  a louder voice track, and summaries you can actually skim.
</p>

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg" alt="License: GPL v3"></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen.svg" alt="Platform: Android 8.0+">
  <img src="https://img.shields.io/badge/Architecture-Modular%20Gradle-orange.svg" alt="Architecture: modular Gradle">
</p>

---

## About

PodPulse is a fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod), the podcast manager most
people in this space already know. We track upstream regularly, so fixes and new feed-parsing work land
here too, and the ground rules that made AntennaPod worth using in the first place are unchanged:
RSS and Atom only, no commercial tracking, no account required to listen.

What we have added sits mostly on top of that: a redesigned player, a colour system that actually
responds to the palette you pick, a hardware loudness boost for quiet spoken-word recordings, and an
optional sync backend.

## What is different

### The player

The episode title is the headline now, with the show name as smaller context above it. That matches
how people read a queue — you already know which podcast you subscribed to, you want to know which
episode this is.

The cover art is rounded, and a shownotes shortcut sits in its corner rather than floating over it.

Below that is a row of quick actions: sleep timer, share, add to queue, playback speed, download, and
favourite. Each one clears the 44dp minimum touch target and is focusable, so they work with TalkBack
or a d-pad rather than being touch-only.

The progress bar is a waveform. It ripples while audio is playing and settles flat when you pause or
drag, which makes the scrub position much easier to read than a thin line.

The play button is 72dp and takes its colour from whichever palette you have selected, with a subtle
light-to-dark sheen over the top so it does not read as flat.

### Colour and theming

There are six palettes — indigo, coral, emerald, cyan, amber, and purple — each with light, dark, and
true-black variants. A palette now drives surfaces, chips, and selection highlights, not just the
accent colour, so switching themes changes the whole screen instead of one button.

The dark themes use cool near-black neutrals (`#0f1115` for the background, `#1a1d23` for raised
surfaces) rather than a coloured tint, so the palette accent stays the only thing carrying colour.

### Voice Boost

Quiet recordings are a real problem for spoken word, especially at low volume or over a phone speaker.
Voice Boost applies the platform `LoudnessEnhancer` from 0 to 12 dB, with four presets if you would
rather not dial in a number. The gain curve is scaled so it stops short of clipping.

### AI episode summaries

A dialog that turns a long episode into bullet points with tappable timestamps, so you can jump to the
part you actually care about. Summaries come from the PodPulse sync backend rather than running
on-device, which means you can self-host the model choice and keep it off a third party.

### PodPulse Cloud Sync

`sync-server/` is a FastAPI service over SQLite in WAL mode, speaking the Gpodder 2.0 protocol that
AntennaPod already uses. It syncs subscriptions and playback position across devices, and caches
episode summaries. It is entirely optional — the app is fully functional without it.

## Building

PodPulse uses AntennaPod's modular Gradle setup.

You need JDK 17 or newer, and Android SDK API 34 with build-tools 34.0.0.

```bash
./gradlew :app:assembleDebug
```

Two APKs come out of that:

- `app/build/outputs/apk/play/debug/app-play-debug.apk` — includes Chromecast support
- `app/build/outputs/apk/free/debug/app-free-debug.apk` — Google-free variant

Tests:

```bash
./gradlew :app:testPlayDebugUnitTest
./gradlew :playback:service:test
```

## Credits and licence

Built on [AntennaPod](https://github.com/AntennaPod/AntennaPod) — © AntennaPod contributors.
PodPulse changes by Mohamed Kamel and contributors.

Distributed under the [GNU General Public License v3.0](LICENSE).
