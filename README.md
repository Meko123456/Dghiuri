# Dghiuri 📓

**დღიური** (*dghiuri* — Georgian for "diary") — a private, on-device **daily journal**
for Android: one markdown entry per day, a mood score, and a **year heatmap** of your
journaling streak.

No accounts, no cloud, no analytics. Your words stay on your phone.

## Why this app exists

Dghiuri is the first real consumer of **both** of my published Kotlin libraries:

| Library | Used for |
|---|---|
| [`io.github.meko123456:markdown-blocks`](https://github.com/Meko123456/markdown-blocks) | parsing each entry into blocks/spans, rendered natively with Compose |
| [`io.github.meko123456:heatmap`](https://github.com/Meko123456/heatmap-compose) | the GitHub-style streak heatmap on the home screen |

Publishing a library is one thing; dogfooding it across apps is what proves the API
holds up. Dghiuri is that proof.

## Features (v0.1.0 scope)

- 📝 **One entry per day** — a monospace markdown editor with a live rendered preview
  (headings, lists, quotes, code, bold/italic/links).
- 🙂 **Mood score** — a 1–5 mood per entry, shown on the calendar and in stats.
- 🟩 **Streak heatmap** — a year of journaling at a glance; tap a cell to open that day.
- 🔥 **Streaks** — current and longest streak, computed from the entries you actually wrote.
- 🔍 **Search** across all entries.
- 📤 **Export** everything as a single markdown file (Storage Access Framework).
- 🔒 **Private by design** — Room database on-device, no network permission at all.
- 🎨 **Material 3** — dynamic color, light/dark, edge-to-edge.

## Architecture

Single-module Compose app on the shared toolchain (Gradle 9.3 / AGP 9.1 / Compose BOM 2026.06):

```
data/     Room: Entry(epochDay PK, markdown, mood, updatedAt) + DAO + repository
domain/   pure Kotlin — StreakEngine (current/longest streak), EntryStats; unit-tested
ui/       Compose — Home (heatmap + today card + recent entries), Editor (edit/preview),
          Calendar/Stats, Search; MarkdownText renderer over markdown-blocks
```

### Library dependency note

`heatmap` is consumed straight from Maven Central. `markdown-blocks` is declared by its
Maven coordinates too, but until its `v0.1.0` release lands on Central the build
substitutes a **Gradle composite build** of the sibling checkout (`../markdown-blocks`),
which CI clones alongside. Removing that substitution is tracked as an issue.

## Build & run

```sh
git clone https://github.com/Meko123456/Dghiuri.git
git clone https://github.com/Meko123456/markdown-blocks.git   # sibling, until published
cd Dghiuri && ./gradlew :app:installDebug
```

## Status

🚧 In progress — see the [issues](https://github.com/Meko123456/Dghiuri/issues) for the backlog.

## License

[MIT](LICENSE)
