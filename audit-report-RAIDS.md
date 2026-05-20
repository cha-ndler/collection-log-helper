# drop_rates.json audit -- RAIDS

Total sources audited: **7**

| Bucket | Count |
|---|---|
| verified | 1 |
| flagged-fixable | 0 |
| flagged-research | 6 |
| error | 0 |

## Findings

| Source | Category | Check | Field | Current | Suggested | Severity | Bucket | Note |
|---|---|---|---|---|---|---|---|---|
| Chambers of Xeric | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |
| Chambers of Xeric (Challenge Mode) | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |
| Theatre of Blood | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |
| Tombs of Amascut | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |
| Tombs of Amascut (300 Invocation) | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |
| Tombs of Amascut (500 Invocation) | RAIDS | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |

## Verified (1)

- Theatre of Blood (Hard Mode)
