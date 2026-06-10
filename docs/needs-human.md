# Needs-Human — live-play confirmations the autonomous loop cannot perform

> Rows here require an active OSRS session (login, real network, real account data) and
> cannot be advanced out of game. Each entry carries the in-game recipe so a human can
> confirm it in one sitting. Recipes mirror the style of `post-merge-ux-confirmation.md`.
> When a row is confirmed, mark it `[x]` here and flip the matching row in
> `v1-hub-validation-checklist.md`.

---

## T1.4 (#488/#577 / PR #795) — external-sync opt-in + failure modes

Code-side opt-in hardening shipped in PR #795 (both sync flags default OFF, descriptions
warn "submits your RSN to a third-party server", consent re-checked immediately before each
request). The remaining sub-clauses are live-play only.

| # | Step | Expected | Confirms |
|---|---|---|---|
| 1 | Open the plugin config → **Sync** section on a fresh profile. | Both "Auto-sync collectionlog.net on login" and "Auto-sync TempleOSRS KC on login" are **unchecked by default**; each description reads that it submits your RSN to a third-party server. | T1.4 opt-in default (code-confirmed; eyeball only) |
| 2 | Enable **TempleOSRS** sync, log in with a valid RSN that has KC on TempleOSRS, wait a few ticks after `LOGGED_IN`. | Per-source kill counts populate (panel KC / synced indicator updates); no hang. | T1.4 "sync populates data" |
| 3 | Enable **collectionlog.net** import, log in with a valid RSN present on collectionlog.net. | Obtained-items list imports and the panel rebuilds; toast confirms item count. | T1.4 "sync populates data" |
| 4 | With a flag enabled, log in with an **invalid / non-existent RSN**. | Fails cleanly — clear "user not found" / failure toast, no stack trace, no UI freeze. | T1.4 "invalid RSN fails cleanly" |
| 5 | With a flag enabled, take the machine **offline** (or block the host), then log in. | Request fails fast within the timeout; no EDT hang, no infinite spinner; failure surfaced. | T1.4 "offline fails without hanging" |
| 6 | With a flag enabled, log in, then **disable** the flag during the few-tick window before the request fires (best-effort race check). | No request leaves the client once disabled (consent re-check at `doImport`/`doSync`). | T1.4 / PR #795 consent re-check |
