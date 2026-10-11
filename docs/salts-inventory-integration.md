Current integration: [standard native window controls](salts-standard-window-controls.md). Main AE windows now use the complete native expanded row or More/Close compression while retaining their original artwork. The [API 5 UI restoration](salts-api5-restoration.md) and API 4 records below describe earlier frozen releases.

# Historical API 4 integration — 0.1.4

AE2 now owns its complete visible desktop surface. Salt adds no outer frame, title bar, backing fill, host padding or implicit title/grip hit targets. Original native artwork, titles, widgets, surviving slots, tabs, Upgrades and Toolbox panels retain their dimensions. Logical player-inventory slots remain in the menu while their presentation moves to Salt's shared inventory.

The adapter uses committed surface/role geometry, computed read-only gesture snapshots and terminal notifications. Internal view changes cancel their former input ownership; same-type windows keep independent state. Native styles are instance-owned and refresh on actual resource reload. Existing native mod packets remain scoped to their authenticated hosted sessions.

Frozen acceptance package: `build/libs/appliedenergistics2-0.0.0-surface-api4-verified.jar`.

SHA-256: `5937AB3F018A6584FF54DA4B04B9595513B82C336CFC7CC3AC1E91DD7CE3CF26`.

It contains 41 desktop profiles and the API 4 discovery descriptor. It bundles no Salt classes, adds no required Salt dependency and keeps Salt references exclusively in its optional common/client provider packages. The same jar passed 77 required game tests in each dedicated-server configuration. Its native client suites passed with Salt present and absent. All three exact-package GUI-scale atlases completed 40 containers and 90 views each at scales 2, 3 and 4; all 270 captures were reviewed.

Native lifecycle assertions passed actual resource-pack replacement/restoration, retained search and all 41 logical slot objects/contents, a real 320×240 GUI viewport with unscaled artwork, typing through re-prepared hitboxes, management Escape without owner disposal, and declared modal popup blocking/dismissal. Screenshot timing waits for Minecraft's reload overlay to clear. The small-viewport image establishes the compact original UI; management behavior is established by assertions, and the separate modal image shows the declared popup.

Evidence uses the `surface-api4-verified` prefix under `build`. Process exits, assertion completion and visual review are separate results. The Salt repository's `API-SURFACE-0.1.4-PLAN.md` records the full completion audit.

AE recipe-browser transfer remains opt-in. Its native crafting/menu actions are supported; the Salt API separately documents and compiles a complete server-authorized recipe-transfer example.

The record below is historical. Its API 3 assumptions, artifact hashes and restricted source boundary describe the earlier developer's work, not this overhaul.

Formation Plane initializes its host placement setting during construction, before any full synchronization; this fixes the null-enum failure exposed by early detached-menu synchronization.

---

# Salts Inventory optional integration

Current target: [ordinary AE appearance with the player inventory cut off](salts-ae-fidelity-review.md). Final capture/PDF and packaged runtime verification passed. The earlier native-gray design is retained only as a [historical release record](salts-window-redesign-review.md).

Target: Minecraft 26.3, NeoForge 26.3.0.43-beta, Salts Inventory Update 0.1.4 / desktop API 3.

The same AE jar supports ordinary screens without Salts and the AE-fidelity desktop presentation with Salts. See [the layout standard](salts-window-layout-standard.md) and [the fidelity review](salts-ae-fidelity-review.md).

## Source boundary

Only the supplied HTML API guide, `docs/markdown/desktop_window_api.md` and permitted `api-examples` were used to understand Salts. No other Salts source, API source files or binary internals were read. Its release jar was used as a compile dependency and test mod. All implementation changes are in the AE fork.

## Build and install

Artifact: `build/libs/appliedenergistics2-0.0.0-salts-ae-fidelity.jar` (8,560,094 bytes).

SHA-256: `15349ccb13a945d9b0cc99c37d04e414264517e7a248093ef4de666d364bc9a2`.

Install this AE jar with AE's normal dependencies. Add the matching Salts jar to enable desktop windows; the same AE artifact also runs with Salts removed. It includes the integration descriptor and 41 desktop profiles, bundles no Salts classes and adds no mandatory Salts dependency.

For development builds, place the matching development-mapped jar at `libs/salts_inventory_update-26.3-NeoForge-0.1.4.jar`, or pass `-PsaltsInventoryJar=<absolute-path>`.

```powershell
$env:JAVA_HOME = 'C:\Users\texca\.jdks\ms-25.0.2'
$env:CI = 'true'
./gradlew.bat jar spotlessCheck test runGametest '-Pversion=0.0.0-salts-ae-fidelity'
./gradlew.bat check runGametest '-Pruntime_salts_inventory=true' '-Pversion=0.0.0-salts-ae-fidelity'
```

Salts is compile-only by default. The runtime property adds it to development runs/tests. Ordinary AE entrypoints do not initialize optional providers.

## Native window behavior

- The API v3 descriptor lazily loads separate common/client providers; both register all 40 AE menus.
- Controls, catalogs, machine diagrams and slots render directly into native content. Salts supplies the outer frame and shared player inventory. The original whole AE screen is never rendered as a nested window.
- Surviving tabs and buttons retain original AE dimensions, positions and sprites. Original Upgrades and Toolbox panels retain their geometry and explicit API regions. Intentionally off-screen view-cell slots stay hidden while retaining logical menu identity.
- All 41 desktop profiles inherit ordinary AE styles without separate slot/widget/background overrides. The original logical height preserves bottom anchors; the visible inventory crop is separate. Catalog height adds rows at the original 18-pixel pitch while preserving original width. Complete non-inventory dialogs retain their layout.
- Idle real slots use AE rendering, with native API rendering during real-slot gestures. Real slots use public hit/gesture contracts. Virtual catalogs/configuration displays remain outside authoritative menu topology. Only actual player-inventory slots are suppressed; crafting grids and other player-side destinations remain visible.
- Local forms replace content inside the same window and return to their parent. Server submenu transitions close superseded same-host owners through API handles; unrelated hosts remain independent.
- Per-opening UUIDs route bounded updates and authenticated actions. Handlers borrow Minecraft's shared inventory cursor, publish its result through the native cursor packet and clear the detached menu's borrowed stack. Real/virtual transfers and item conservation passed packaged checks.
- Search, preferences, sizes, handles and pointer state belong to each window. Geometry changes preserve search/topology. Foreground ownership routes keyboard input; focus loss and owner removal clear editing/pointer state. Callback contexts/graphics objects are not retained.
- Source keys distinguish view, dimension and locator; gesture ownership identifies shared physical backing inventories.

AE's extended NeoForge opening path did not reach the documented capture callback on this target. The integration uses standard native `openMenu` with a bounded AE-owned opening-data packet preserving ordinary-screen construction. Detached action scopes are suspended around opening so vanilla's close path starts from the ordinary inventory menu.

## Verification checkpoint

| Check | Result and evidence under `build/` |
| --- | --- |
| Build/formatting/unit suite | `salts-ae-fidelity-final-build6.log`: formatted `jar check` passed. 549 cases, 548 executed, one skipped, zero failures/errors. |
| Packaged servers | All 77 required tests passed with Salts present and absent: `salts-ae-fidelity-shipped-packaged-present-server.log`, `salts-ae-fidelity-shipped-packaged-absent-server.log`. |
| Packaged interaction clients | Native and ordinary terminal suites completed: `salts-ae-fidelity-verified-packaged-present-client.log`, `salts-ae-fidelity-verified-packaged-absent-client.log`. These suites use the final artifact. |
| Style source and package | All 41 profiles inherit ordinary styles and packaged copies match source. No bundled Salts classes: `salts-ae-fidelity-profile-audit.json`. |
| Final visual coverage | The sequential capture run, comparisons, scale checks and final PDFs are recorded in the fidelity review. All 40 containers and 88 paired views passed, with final scale/populated evidence and both verified PDFs. |

Native client checks cover synchronization, search/editing, scroll/sort, title movement, corner resizing, retained search/topology, Tab focus, withdrawals/deposits, real/virtual cursor transfers, counts, manual crafting, plan/CPU synchronization, autocrafting and return. Independent same-type windows, foreground-only text, focus cleanup and owner-removal pointer cancellation also passed. The current rich atlas passed Toolbox pickup/return and all four populated encoding modes; its acceptance result is recorded in the fidelity review.

The ordinary client covers search/storage/cursor/crafting/settings/autocrafting without Salts on its runtime classpath. Its existing Tab behavior is preserved. Unit tests alone do not establish provider discovery: the Salts-present JUnit environment reports a provider classloader mismatch; packaged client/server checks supply discovery evidence.

QA uses `-Pruntime_tooltip_mod=none -Pruntime_itemlist_mod=none` after an early Jade configuration initialization failure. Every external tooltip/recipe-browser mod has not been verified. Opt-in fixtures modify disposable `SaltsIntegrationQA` or `SaltsAtlasQA` worlds; they do not run during normal startup. All QA clients have been stopped.

## Illustrated deliverables

- [Redesigned UI report](../output/pdf/AE2_Salts_Redesigned_Container_UI_Report.pdf): 69 pages, 68 labeled/described images.
- [Side-by-side comparison](../output/pdf/AE2_Normal_vs_Salts_UI_Comparison.pdf): 41 pages, all 40 containers, 80 labeled/described images.

The normal baseline is ordinary AE without Salts because these containers are mod content. Both PDFs use actual screenshots, include visible attachments and identify the final release hash. Controlled error/amount fixtures and capture-state differences are labeled. Every page was rendered and reviewed; coverage and text bounds passed automated checks.

## Documentation gap and coverage limits

Recipe-browser transfers remain disabled. The allowed guide mentions `DesktopServerWindowHandler.validateTransfer` and `DesktopTransferDecision`, but omits the callback signature, request/context fields and decision construction API. The user declined additional signatures and requested this issue in the end report. No forbidden source or binary inspection filled the gap.

Controlled errors verify layout/return geometry, not every corresponding server failure. Salt outer chrome and native gesture previews remain documented visual differences. Complete original attachments require sufficient GUI viewport space. Every possible fluid/filter/creative/hotbar gesture, resource pack, long custom name and persistence combination has not been exhaustively tested. This report does not claim compatibility with all untested third-party mods or recipe-browser transfers.
