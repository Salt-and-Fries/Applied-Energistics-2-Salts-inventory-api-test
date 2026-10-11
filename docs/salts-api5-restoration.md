# AE UI restoration for Salts API 5

Final candidate: `build/libs/appliedenergistics2-0.0.0-api5-ui-restoration-r15.jar`.

SHA-256: `b9ff9776b3431dc0e5790edb985b2979ba05883fe2d31089848c715495e413bd`.

Target: Minecraft 26.3, NeoForge 26.3.0.43-beta, Salts Inventory Update 0.1.4 / public API 5.

The visual reference is `output/pdf/AE2_Salts_API4_Polished_UI_Report.pdf` and its ordinary-versus-Salts comparison. Existing original AE frame, texture, tab, slot and layout paths are preserved. Public Salts documentation is the only source for its contracts; no Salts source or binary internals were inspected.

## Implemented changes

- The optional descriptor declares API 5 and the compile/runtime dependency is the matching 26.3 NeoForge release artifact. API 5 dependency SHA-256: `f3f4c7a64a040ed18e86653f68fce445f3e8fa8945c36d7a3509423966dd390a`.
- Original AE surfaces retain their original native tab textures, dimensions and protruding placement. All 79 captured TabButton instances match the polished and ordinary references. Four 11-pixel window controls occupy reserved title space; native tabs are not resized to accommodate them.
- Close, Pin/Unpin, Minimize/Restore and More use documented manager routing and state snapshots. AE paints the controls and the collapsed title. Salts supplies action validation, tooltips and keyboard traversal.
- Controls reserve title space and avoid header widgets and full native slot artwork. ME Drive uses clear space above its first cell row; Cell Workbench places controls before its source cell. Crafting-error messages occupy a separate row beneath the controls. Preferred-size updates do not immediately expand minimized windows.
- The equipped Network Tool is represented by a real `NetworkToolMenu` companion. A stable actual tool-stack identity and per-owner source guard prevent conflating inventories or authorizing a replaced tool.
- The parent receives an association notification using the documented server-to-client payload hook. Its original Toolbox panel is hidden only after successful acquisition. Logical slot IDs and native inventory rules are retained.
- The companion has one compact AE frame, the original slot texture, a Toolbox title for dragging and no independent management controls. New lifetimes dock beside the parent's complete prepared outline, including protruding native side panels. Further associations retain position.
- Parents using the same tool share one companion; different tools remain distinct. Closing one owner preserves it for another, while closing the last disposes it once. It hides when all parents are unavailable and restores without stealing focus when one becomes eligible. Local view changes reflow default docking, retaining user placement unless the expanded owner covers it. Capture readiness waits for completed companion rendering.
- All menu hosts for the same tool stack share the backing inventory while live, and the documented physical-owner mapping deduplicates equivalent slot representations. Weak identity keys and weak values avoid retaining discarded tools.

## Verification of this exact packaged jar

| Gate | Evidence and result |
| --- | --- |
| All native containers | `build/api5-r15-scale2`, `scale3`, `scale4`: 40 containers and 90 captures each; all 270 images visually reviewed in 45 galleries |
| Tab geometry and package | Each atlas's `package-and-tab-audit.json`: 79 matching tab instances, API 5 descriptor, no bundled Salts classes, linkage confined to optional providers |
| Layout assertions | Every native atlas checks title/editor separation, controls versus slots/error text, finished borders, attachment regions and exactly one real companion presentation |
| Controls and companion lifecycle | `build/api5-r15-scale2-packaged-present-client.log`: actual control activation, keyboard access, sharing, drag, visibility, cursor conservation, source replacement, distinct tools and last-owner disposal |
| Native terminal operations | `build/api5-r15-lifecycle-packaged-present-client.log`: search, scroll, sort, transfers, manual crafting, settings and autocrafting |
| Retained state/environment | Same lifecycle log: actual resource-pack replacement/restoration, 320x240 GUI viewport, retained slot objects/contents and search, re-prepared input, management and modal dismissal |
| Ordinary AE without Salts | `build/api5-r15-ordinary-scale2`: 40 containers/90 captures, with 67 distinct presentations reviewed in the comparison PDF; `build/api5-r15-ordinary-terminal-packaged-absent-client.log`: native terminal/crafting suite |
| Dedicated servers | `build/api5-r15-packaged-present-server.log` and `absent-server.log`: all 77 required game tests passed per configuration, process exit 0 |
| Unit suites | `build/api5-r15-unit-audit.json`: 549 tests per configuration, 548 passed and 1 skipped, zero failures/errors; separate XML snapshots retained |
| Artifact/process provenance | Six client exit audits record this jar hash, exit 0 and completed assertions; server audit records the same hash |
| Scratch preferences | `build/api5-r15-preferences-restoration.json`: original runtime preferences restored byte-for-byte; all packaged clients stopped |

Build command: `gradlew.bat compileClientJava jar -Pversion=0.0.0-api5-ui-restoration-r15 --console=plain` (quote the version argument in PowerShell).

Unit commands: `gradlew.bat test -Pruntime_salts_inventory=true --console=plain`, then the corresponding `false` configuration.

Packaged launchers: `build/run-surface-packaged-client.ps1` and `build/run-layout-packaged-server.ps1`. Client runs are serial because they share the scratch run directory. Atlases use `-Atlas -Variants -Populated`; native scale 2 also exercises `-Companion`. Lifecycle and ordinary-terminal checks are separate runs.

Control availability respects Salts preferences. The scratch profile enabled minimizing for acceptance; its original settings were restored afterward. An installation with `minimizableWindows=false` correctly disables minimizing.

## Updated image reports

- [Restored container catalog](../output/pdf/AE2_Salts_API5_Restored_UI_Report.pdf): 69 pages and 67 labeled, described runtime figures.
- [Ordinary AE versus Salts comparison](../output/pdf/AE2_Ordinary_vs_Salts_API5_Restored_UI_Comparison.pdf): 69 pages and 134 runtime images paired at the same GUI-pixel display scale.

Both use the same final AE jar. All 138 pages were rendered and visually checked. Page/image counts, captions and artifact identity were audited. Repeated return states remain in runtime evidence but are omitted from the illustrated sequence. Controlled crafting-error and processing-amount fixtures do not establish every corresponding server outcome.

## Source boundary and earlier transfer issue

No Salts implementation, API source, example source, tests, build scripts or binary internals were inspected. Its matching jar was copied, hashed, compiled against and run. Scratch runtime preferences in the AE run directory were inspected as data and restored afterward.

The earlier guide omitted `validateTransfer`, request/context and decision-construction signatures, which the user asked to retain in the end report. The current public API 5 reference now documents these declarations. This UI restoration does not add recipe-browser transfer support; native AE crafting and inventory operations are verified separately.

The requirement-by-requirement release audit is `build/api5-r15-completion-audit.json`. Earlier API 3/4 records and probe builds are historical, not release evidence for this candidate.
