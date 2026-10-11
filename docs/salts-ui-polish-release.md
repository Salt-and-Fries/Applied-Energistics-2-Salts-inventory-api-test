# AE2 API 4 polished UI release

Status: complete and verified, 10 October 2026.

Artifact: `build/libs/appliedenergistics2-0.0.0-salts-ui-polished-r3.jar`.

SHA-256: `d98e91d16f6471174620313034e77c5681ec761894b95efcf0c1169fb89d1fa7`.

## Result and design decisions

AE owns one complete visible API 4 surface. Salts supplies desktop management and the shared player inventory. The original AE border, tabs, buttons, slot grids, machine diagrams and state artwork remain recognizable; there is no second visible host frame.

The review covered 40 registered menu types and 90 runtime presentations. It included settings, resource types, amounts, all four encoding modes, seven controlled crafting errors, populated Toolbox gestures and return states. Complete dialogs keep their content; only actual player-inventory presentation is removed from machine/catalog surfaces.

The redesign closes inventory-cut edges with resource-aware original AE artwork and reserves a four-pixel closing gutter so borders never cover surviving slots. Catalogs give titles and search separate rows. Wide search fields tile the original interior artwork instead of leaving gaps or stretching the end caps. Compact toolbars wrap into attached columns at original control size. The Sky Stone Chest title has a contrasting foreground.

Settings use measured label heights and consistent group spacing. Disabled options remain identifiable and readable. Labels keep their full hit areas, and checkbox painting preserves keyboard focus after the pointer leaves. Key Types forms now retain enough measured width for complete Items and Fluids labels during repeated layout.

The surface describes intentional stepped gaps and actual attachment regions. Movement uses unused title space, with a hover hint for title dragging/right-click management. Resizable catalogs declare practical edge and corner roles. Resource reload rebinds terminal geometry and palette state. The layout revision prevents obsolete saved geometry from silently persisting.

## Artifact and verification evidence

`build/ui-polish-final3-package-audit.json` records 41 desktop profiles, API 4 discovery, no bundled Salts classes, no required Salts dependency, isolated optional-provider linkage and packaged/source profile equality after resource formatting. The unit suite contains 549 tests: 548 passed and one was skipped, with zero failures and zero errors.

Both packaged dedicated-server configurations passed all 77 required game tests. Client acceptance is recorded separately from screenshots and process exits in `build/ui-polish-final3-runtime-audit.json`.

Final atlases use the `ui-polish-final3` prefix: scale 2, scale 3, scale 4 and ordinary AE at scale 2 with Salts absent. Each atlas contains 40 menus and 90 captures. Final reports use the same jar on both sides and match image scale in GUI pixels.

Native interaction checks exercise search, scrolling, sorting, cursor transfers, crafting, settings and autocrafting. Lifecycle checks exercise an actual resource-pack replacement/restoration, retained state, a 320 by 240 GUI viewport, management behavior and declared modal popup dismissal. The focus probe restores the original preference after checking keyboard focus. Individual assertion logs and exit JSON files retain the artifact hash.

## Reports and layout rules

- `output/pdf/AE2_Salts_API4_Polished_UI_Report.pdf`: all 40 containers, 67 labeled and described presentations, 69 pages.
- `output/pdf/AE2_Ordinary_vs_Salts_API4_Polished_UI_Comparison.pdf`: 67 pairs at matched GUI-pixel scale, 134 labeled images, 69 pages.
- [Layout standard](salts-window-layout-standard.md): rules for a matching, comfortable mod-owned window.
- [Issue audit](salts-ui-polish-audit.md): initial problems, final dispositions and evidence.

PDF acceptance passed: every page was rendered, every figure has labels and descriptions, and all rendered layouts were visually reviewed. The final PDF acceptance record is `build/ui-polish-final3-pdf-audit.json`: all 138 pages were rendered and reviewed; text/image bounds and all 201 images were checked.

## Scope and practical limits

Salts reference reads were limited to public Markdown/HTML guides, reference declarations and snippets embedded in documentation, and referenced documentation/READMEs. No Salts implementation, API source, example source, tests, build scripts, generated source or binary internals were inspected. Its matching jar was compiled against and run as a dependency. All implementation changes are in the AE fork.

The updated documentation supplies the surface and gesture capabilities needed for the intended single-frame UI. No unresolved API design gap blocks this presentation. The earlier missing `validateTransfer` signature is now supplied by the public reference, so that documentation gap is closed. AE recipe-browser transfer remains opt-in; this visual task does not add transfer support. Controlled error screenshots establish presentation and return behavior, not that each server failure actually occurred. Compatibility with arbitrary external UI mods, resource packs and untranslated long labels is not exhaustively proven.

Earlier fidelity PDFs, API 3 assumptions and earlier artifact hashes remain historical records. This release record identifies the verified final artifact. No commit, push or publication was performed.

The ordinary-AE Toolbox capture fixture initially checked the hosted shared cursor. It was corrected to check the active ordinary menu cursor, then the final package completed the full ordinary sequence. This was a QA fixture issue; no UI behavior was changed for that correction.
