# MindFS for Android

Unofficial native Android client for the self-hosted [MindFS](https://github.com/igrmk/mindfs) AI-agent gateway. One app manages MindFS on multiple devices — every node you run (laptop, cloud box, NAS) becomes a card in the same cockpit.

> This is a personal fork-line project, not affiliated with the MindFS project. It speaks MindFS's REST/HTTP API directly; **no MindFS code or assets are included in this repository.**

## Status — v0.1.0 (scaffold, batch v110)

| Works now | Next batches |
|---|---|
| Multi-node management (add / edit / test / select, health + latency) | v111 protocol layer complete + session list |
| Projects tab: live workspace roots of the selected node | v112 streaming chat + new session |
| Bottom tabs (Activity / Chats / Projects / Tasks / Me), theme, start tab | v113 activity feed + notifications |
| Dual APKs: modern (Android 12+) & legacy (Android 9+) | v114 files + tasks + floating ball |

## Nodes

Add a node with its base URL, e.g. `http://100.118.126.86:7331` or `https://tx.example:8443`. Plain-HTTP nodes are allowed only after an explicit in-app confirmation — MindFS nodes usually live on trusted LAN/Tailscale networks. Node settings stay on-device; nothing syncs anywhere.

## Build

Requirements: JDK 21, Android SDK 36. The wrapper pins Gradle 8.14.3.

```bash
./gradlew :app:assembleModernDebug :app:assembleLegacyDebug   # debug APKs
./gradlew :app:check                                          # lint + unit tests
```

Release builds sign with a local, gitignored `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword). CI (`release.yml`) rebuilds on tags `vX.Y.Z` / `vX.Y.Z-fengx.N`, verifies the signature (v2 scheme) and the `versionName`, then publishes the Release.

## License

[Apache License 2.0](LICENSE). The stable client layers (design system, streaming patterns, Keystore storage, CI pipeline) are adapted from the `codeg-android` client, which is also Apache-2.0 — see [NOTICE](NOTICE).
