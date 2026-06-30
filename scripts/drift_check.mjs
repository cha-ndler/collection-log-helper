#!/usr/bin/env node
// drift_check.mjs - toolkit-independent drift detector for Collection Log Helper.
//
// Purpose: surface when new OSRS game data lands so the plugin's drop_rates.json
// can be updated retroactively - WITHOUT depending on the runelite-dev-toolkit MCP.
// It talks directly to the same upstreams the toolkit wraps, over plain HTTPS:
//
//   1. abextm/osrs-cache (GitHub)      - authoritative item/NPC ids the plugin uses.
//        commits API -> latest cache REVISION string ("Cache version YYYY-MM-DD-revN").
//        A new revision == a game update dropped new/changed data.
//   2. OSRS Wiki MediaWiki API          - semantics: new items, drop rates, requirements.
//        recentchanges + search -> pages touched since we last looked.
//   3. (optional) raw abextm flatcache  - exact id<->name, parsed via @abextm/cache2.
//
// It is read-only: it reports a drift verdict + candidate work, and persists the
// last-seen markers under .drift-cache/ (gitignored) so the next run is a diff. Wire it to a
// schedule (or run on demand); feed its candidate list into the add-source pipeline.
//
// Usage: node scripts/drift_check.mjs [--since YYYY-MM-DDTHH:MM:SSZ] [--json]

import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const DROP_RATES = join(ROOT, "src/main/resources/com/collectionloghelper/drop_rates.json");
const STATE_DIR = join(ROOT, ".drift-cache");
const STATE_FILE = join(STATE_DIR, "last-seen.json");
const UA = "collection-log-helper-drift-check/1.0 (data maintenance)";

const argv = process.argv.slice(2);
const asJson = argv.includes("--json");
const sinceArg = (() => { const i = argv.indexOf("--since"); return i >= 0 ? argv[i + 1] : null; })();

async function httpJson(url) {
  const res = await fetch(url, { headers: { "User-Agent": UA, Accept: "application/json" } });
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  return res.json();
}

function loadState() {
  if (existsSync(STATE_FILE)) {
    try { return JSON.parse(readFileSync(STATE_FILE, "utf8")); } catch { /* fall through */ }
  }
  return { cacheRev: null, lastRunIso: null };
}

function saveState(s) {
  mkdirSync(STATE_DIR, { recursive: true });
  writeFileSync(STATE_FILE, JSON.stringify(s, null, 2));
}

// 1. abextm cache revision (the freshness gate - matches what the plugin's ids use).
async function abextmRevision() {
  const c = await httpJson("https://api.github.com/repos/abextm/osrs-cache/commits/master");
  const msg = c.commit?.message ?? "";
  const rev = (msg.match(/Cache version[^\n]*/) || [msg.split("\n")[0]])[0].trim();
  return { sha: c.sha, rev, date: c.commit?.committer?.date };
}

// 2. Wiki pages touched recently (the semantics signal). Filters to mainspace and to
//    titles that look collection-log relevant; broaden as needed.
async function wikiRecentChanges(sinceIso) {
  const params = new URLSearchParams({
    action: "query", list: "recentchanges", rcnamespace: "0", rclimit: "200",
    rcprop: "title|timestamp", rctype: "edit|new", format: "json",
  });
  if (sinceIso) params.set("rcend", sinceIso);
  const d = await httpJson(`https://oldschool.runescape.wiki/api.php?${params}`);
  return d.query?.recentchanges ?? [];
}

// 3. Plugin's currently-covered item ids (what we already guide for).
function pluginItemIds() {
  const data = JSON.parse(readFileSync(DROP_RATES, "utf8"));
  const ids = new Set();
  let items = 0;
  for (const src of data) {
    for (const it of src.items ?? []) { if (typeof it.itemId === "number") { ids.add(it.itemId); items++; } }
  }
  return { ids, items, sources: data.length };
}

async function main() {
  const state = loadState();
  const [rev, plugin] = await Promise.all([abextmRevision(), Promise.resolve(pluginItemIds())]);

  const cacheChanged = state.cacheRev && state.cacheRev !== rev.rev;
  const since = sinceArg || state.lastRunIso || null;
  let wiki = [];
  let wikiErr = null;
  try { wiki = await wikiRecentChanges(since); } catch (e) { wikiErr = String(e); }

  // Heuristic interest filter: wiki edits whose title hints at new/clog content.
  const INTEREST = /(collection log|\bpet\b|jar of|\brelic\b|\bmedallion\b|\bvestige\b|\bward\b|\bicon\b)/i;
  const flagged = wiki.filter(c => INTEREST.test(c.title)).slice(0, 40);

  const report = {
    generatedFrom: { abextm: rev, wikiSince: since, wikiChanges: wiki.length },
    drift: {
      cacheRevisionChanged: !!cacheChanged,
      previousCacheRev: state.cacheRev,
      currentCacheRev: rev.rev,
      verdict: cacheChanged
        ? "NEW CACHE REVISION - a game update landed; re-validate drift candidates and check for new clog items/ids."
        : (state.cacheRev ? "no cache change since last run" : "first run - baseline recorded"),
    },
    plugin: { sources: plugin.sources, coveredItemIds: plugin.ids.size, totalItemEntries: plugin.items },
    wikiCandidates: flagged.map(c => ({ title: c.title, timestamp: c.timestamp })),
    wikiError: wikiErr,
  };

  saveState({ cacheRev: rev.rev, lastRunIso: new Date().toISOString().replace(/\.\d+/, "") });

  if (asJson) { console.log(JSON.stringify(report, null, 2)); return; }
  console.log(`abextm cache: ${rev.rev}  (${rev.date}, sha ${rev.sha.slice(0, 10)})`);
  console.log(`plugin: ${report.plugin.sources} sources, ${report.plugin.coveredItemIds} distinct item ids`);
  console.log(`drift verdict: ${report.drift.verdict}`);
  console.log(`wiki changes since ${since || "(no baseline)"}: ${report.generatedFrom.wikiChanges}` + (wikiErr ? ` (ERROR: ${wikiErr})` : ""));
  if (report.wikiCandidates.length) {
    console.log(`clog-relevant wiki edits to review (${report.wikiCandidates.length}):`);
    for (const c of report.wikiCandidates) console.log(`  - ${c.title}  (${c.timestamp})`);
  }
}

main().catch(e => { console.error("drift_check failed:", e); process.exit(1); });
