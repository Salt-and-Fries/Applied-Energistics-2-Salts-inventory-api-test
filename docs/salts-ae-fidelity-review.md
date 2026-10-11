Historical API 3 review. For the current API 4 design and exact artifact, see [the polished UI release](salts-ui-polish-release.md).

# AE appearance fidelity review

Status: verified for Minecraft 26.3 / NeoForge 26.3.0.43-beta, with Salts Inventory Update 0.1.4 (API 3). This supersedes the earlier native-gray redesign.

The hosted UI now retains ordinary AE artwork and surviving control geometry, with the player-inventory section removed. All 41 desktop profiles inherit their ordinary AE styles. The native frame and shared player inventory are supplied by Salts; AE content is rendered directly into one window lifetime.

## Release and reports

Artifact: `build/libs/appliedenergistics2-0.0.0-salts-ae-fidelity.jar` (8,560,094 bytes).

SHA-256: `15349ccb13a945d9b0cc99c37d04e414264517e7a248093ef4de666d364bc9a2`.

- [Container UI fidelity report](../output/pdf/AE2_Salts_Container_UI_Fidelity_Report.pdf): 68 pages, 67 labeled/described images, all 40 containers plus modes, local forms, controlled errors and populated examples.
- [Ordinary AE versus Salts comparison](../output/pdf/AE2_Ordinary_vs_Salts_Fidelity_Comparison.pdf): 55 pages, 108 images. All 40 containers are paired, followed by representative modes and local forms. Each image has a label and description; paired images share a GUI-pixel display scale.
- [Layout standard](salts-window-layout-standard.md): rules for preserving the original layout, artwork, inventory crop, attached regions and interaction ownership.

Both PDFs contain this artifact hash. All 123 pages were rendered and visually reviewed. Checks verify coverage, image counts, labels, figure numbers, text bounds and separation between figure metadata and column labels.

## Implemented fidelity

- Original AE buttons, tabs, text fields, checkboxes, scrollbars, grids and process artwork are retained. Encoding tabs remain on their original right-side strip at their original 22-pixel size.
- Visible content height is separate from the original logical height. Bottom-anchored controls keep their original coordinates after the inventory crop.
- Only real player-inventory/hotbar presentation is removed. Crafting/filter destinations and original auxiliary panels remain.
- Complete amount, priority, settings, key-type, error, CPU and statistics forms retain their ordinary layout.
- Every visible attached control/panel/real slot has an API presentation region. View-cell slots that ordinary AE deliberately parks off-screen stay hidden without changing menu topology.
- Catalog resizing preserves original width and adds rows at the original pitch. Mode changes normalize active processing rows before deriving external bounds, preventing the former one-row shrink.
- Key Types uses stable type-id ordering in both ordinary and hosted views. Layout revision `ae-fidelity-2` migrates earlier faulty saved sizing.
- Idle real slots use AE rendering. Native gesture rendering is cleared when Minecraft reports released mouse buttons, including releases consumed before the definition callback. The Toolbox return capture confirms that the AE grid appearance resumes.

## Current evidence

| Evidence under `build/` | Result |
| --- | --- |
| `salts-ae-fidelity-final-build6.log` | Formatted `jar check` passed; 549 unit cases, 548 executed, one skipped, zero failures/errors. |
| `salts-ae-fidelity-shipped-packaged-present-server.log` and `salts-ae-fidelity-shipped-packaged-absent-server.log` | The same final jar passed all 77 required game tests in both configurations. |
| `salts-ae-fidelity-verified-packaged-present-client.log` and `salts-ae-fidelity-verified-packaged-absent-client.log` | Both terminal suites completed using the final packaged jar. Absent launch contains no Salts jar. |
| `salts-ae-fidelity-final-scale2e` | 88 native captures: all 40 containers, four encoding modes, local forms, seven controlled error presentations and returns. GUI 640x480, scale 2. |
| `ae-fidelity-final-baseline3` | 88 ordinary AE captures with Salts absent, from the same packaged jar and disposable fixture. |
| `salts-ae-fidelity-final-scale3c` | All 40 native base containers; GUI 427x320, scale 3. |
| `salts-ae-fidelity-rich-scale3c` | 19 captures across seven populated hosts, including all four populated modes and Toolbox pickup/return conservation. |
| `salts-ae-fidelity-final-scale4c` | All 40 native base containers; GUI 480x360, scale 4 at a 1920x1440 framebuffer. |
| `salts-ae-fidelity-final-scale2e/comparison-ae-fidelity-final-baseline3.json` | All 88 views have zero recorded layout/widget/slot coordinate differences, zero missing visible geometry and zero added geometry. |
| `salts-ae-fidelity-pdf-verification.json` | Final page/image coverage, labels, text bounds and complete rendering passed. |
| `salts-ae-fidelity-completion-audit.json` | Artifact hash, source/package profiles, test logs, all capture manifests and comparison/PDF gates checked against current files. |

Runtime geometry checks cover body slots inside the clip, external slots inside declared regions and attached regions inside the viewport. The only missing raw comparison entries are the original off-screen view-cell slots; they are not visible controls.

Native interaction checks include search/editing, scrolling/sorting, cursor withdrawals/deposits, real/virtual transfers, native real-slot drag distribution and conservation, manual crafting, plan/CPU synchronization, autocrafting, settings and return, title movement, original-width/row-pitch resizing, independent overlapping windows, foreground-only editing, focus cleanup and owner-removal pointer cancellation.

## Visual limits and coverage

The supplied guide does not document a per-window replacement skin for Salts outer frame, title bar or desktop controls. Its opaque fill is visible beneath transparent AE cutouts, including crafting panels and some CPU/plan dialogs. Removing the adapter fill did not remove that framework fill. Native real-slot gesture previews use the API renderer while a button is held; idle appearance uses AE rendering.

Live counts, power values, device ordering and hover state can differ between captures. Direct crafting-arrow, result-backdrop and opaque recipe-panel comparisons were pixel-identical; these were not missing artwork. Screenshot comparisons do not claim that every changing world pixel is identical.

Original attachments require sufficient GUI viewport space. Scale 4 was verified at 1920x1440; some complete original layouts cannot fit a 320x240 GUI viewport. Their geometry is preserved instead of silently adopting the previous compact redesign.

Controlled errors/amount data prove presentation and return behavior, not every corresponding server failure or a recipe-browser submission. Every resource pack, long custom name, gesture combination and third-party tooltip/recipe-browser mod has not been exhaustively verified. QA excludes tooltip/item-list mods after the previously recorded unrelated initialization issue.

## Source boundary and API documentation gap

Salts reference reads were limited to the supplied HTML, `docs/markdown/desktop_window_api.md` and permitted `api-examples`. No Salts implementation/API source or binary internals were inspected. All changes are in the AE fork; Salts is compile-only and optional. The shipped AE jar contains no bundled Salts classes.

Recipe-browser transfers remain disabled. The permitted material lacks `validateTransfer` signatures/request/context fields and `DesktopTransferDecision` construction. The user explicitly requested this issue in the end report instead of supplying signatures.
