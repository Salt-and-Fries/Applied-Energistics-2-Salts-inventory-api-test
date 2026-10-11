# AE desktop UI polish audit

Status: complete and verified, 10 October 2026. All U01-U09 findings are resolved for the final artifact.

## Scope and source boundary

The current fork already uses public API 4 mod-owned surfaces. This pass improves the actual experience rather than repeating that migration. Salts reference reads are confined to its public Markdown/HTML guides, public signature reference, recipe-transfer reference and documentation/READMEs. No Salts implementation, API source, example source, tests, build scripts or binary internals are inspected. The Salts dependency is used only for compilation and runtime execution.

The initial review inspected all 90 actual views in `build/surface-api4-verified-scale2/manifest.csv`, covering 40 menu types, including populated content, all four encoding modes, settings, key types, amount forms, seven controlled errors, and return states. Diagnostic contact sheets are in `build/ui-polish-initial-review`. Existing ordinary AE captures and illustrated fidelity reports provide the visual identity reference. Earlier acceptance records are historical evidence, not proof of this pass's final result.

## Initial findings and intended changes

| ID | Priority | Initial problem | Intended resolution | Final evidence |
| --- | --- | --- | --- | --- |
| U01 | High | Inventory-cut machine and catalog surfaces end abruptly, leaving vertical edges and grids without a finished bottom border now that native chrome is gone. | Close the retained AE surface with resource-pack-aware original border artwork. Keep slots and intentionally transparent cutouts intact. | Resolved: four-pixel closing gutter and original border; live slot-clearance checks and all final scale-2 views pass. |
| U02 | High | ME Chest Storage, portable, wireless, crafting and pattern-encoding titles run into or under the search field. | Give title/search separate readable space or fit titles within an explicit safe area, retaining full names through accessible hover/narration. Review long custom names. | Resolved: separate catalog title/search rows; live measured title/editor separation and all catalog views pass. |
| U03 | High | Compact catalog toolbars continue far below the visible body, visually detaching settings and other actions from their owner. | Reflow original-size toolbar controls into a coherent attached arrangement that responds to available height. Keep all controls reachable and declare their regions. | Resolved: original 16-pixel controls wrap into attached columns; all compact catalogs reviewed. |
| U04 | Medium | Sky Stone Chest uses dark title text over a dark green panel. | Use an appropriate resource-controlled contrasting title color. | Resolved: contrasting resource-style title color; Sky Stone Chest capture reviewed. |
| U05 | Medium | Terminal settings contain inconsistent grouping space and faint disabled search-provider text; label hit targets/focus require review. | Improve group spacing, readable unavailable-state presentation, and keyboard/label interaction without altering preference semantics. | Resolved: measured settings and Key Types labels; native focus probe preserves focus and restores preference. |
| U06 | Medium | The only resize region is a small bottom-right corner; movement/management affordance is implicit. | Provide practical documented edge/corner roles where resizing is supported and explain movement/management through native AE hints. Avoid a second title bar. | Resolved: eight documented resize roles and title management hint; native management/lifecycle suite passes. |
| U07 | Medium | The mod-owned body declares a full visual/interactive rectangle even where AE has intentional stepped transparent gaps. | Align declared presentation regions with known AE surface segments and keep actual controls/slots higher priority. Verify overlap through cutouts. | Resolved: explicit stepped surface segments and attachment regions; overlap, modal and cancellation assertions pass. |
| U08 | Medium | Shared layout/documentation still describes API 3 chrome as unavoidable, and the illustrated reports show obsolete host framing. | Replace current rules with API 4 surface ownership and comfort criteria; generate final all-view and ordinary/hosted PDFs from the final artifact. | Resolved: API 4 layout standard and both illustrated reports; all 138 PDF pages rendered and visually accepted. |
| U09 | Verification | Existing acceptance packages and assertions predate this design pass. | Build a distinct final jar and verify packaged present/absent behavior, appropriate tests, full catalog at GUI scales 2/3/4, constrained viewport and representative interactions. | Resolved: final r3 package; 548 unit tests passed, one skipped; 77 game tests in each server configuration; six client runs; 270 hosted and 90 ordinary views; lifecycle/interaction assertions; both PDFs accepted. |

## Catalog disposition after the initial visual review

| Family | Reviewed base interfaces | Main design action |
| --- | --- | --- |
| Machine/storage bodies (10) | Quantum Network Bridge, Sky Stone Chest, ME Chest, Wireless Access Point, ME Drive, Vibration Chamber, Matter Condenser, Spatial IO Port, Inscriber, Molecular Assembler | Finish cropped borders; contrast and attachment seams; retain machine artwork and slot locations. |
| Configuration/tool windows (12) | Network Tool, Quartz Cutting Knife, ME Interface, ME Export Bus, ME Import Bus, ME IO Port, ME Storage Bus, Formation Plane, Storage Level Emitter, Energy Level Emitter, Cell Workbench, ME Pattern Provider | Finish surfaces and retain filter/quantity controls, upgrades and Toolbox behavior. |
| Complete dialogs (9) | Network Details (tool/controller), Crafting CPU, Stock Amount, Priority, Craft Amount, Crafting Plan, Crafting Status, Spatial Anchor | Existing complete borders and action layouts are a useful baseline; review long labels, focus and compact layout. No universal inventory crop. |
| Catalogs (9) | ME Terminal, ME Chest Storage, Portable Item Cell, Portable Fluid Cell, Wireless Terminal, Crafting Terminal, Wireless Crafting Terminal, Pattern Encoding Terminal, Pattern Access Terminal | Resolve title/search collision, compact toolbar arrangement, cropped edge and transparent shape. |

Settings, Key Types, Processing Amount, encoding modes, controlled errors and all return views were also reviewed. No frame replacement is needed for already complete dialogs. Improvements must solve observed problems; changing every control merely to make it different is not a design goal.

## Acceptance standard

- One mod-owned visible frame; no extra Salts title, backing fill or host padding.
- Finished borders and intentional attachment seams at integral GUI scales.
- Readable titles, labels and state information, with no overlapping controls or text.
- Coherent attached toolbars; all actions have accurate hit regions and visible focus/hover feedback.
- Actual slots, pointer gestures, shared cursor and authenticated menu actions retain their behavior.
- Every registered container and additional presentation has a final labeled capture and review disposition.
- The same final packaged jar runs with Salts present and absent.
- Final PDFs and layout documentation describe the final artifact and distinguish controlled fixtures from live server outcomes.

Final acceptance used fresh evidence for every item above. All four 90-view atlases were visually reviewed, including settings, types, amounts, modes, errors and returns. Runtime checks and PDF review identify the same final jar. The source boundary remained absolute.

The [per-view review catalog](salts-ui-review-catalog.csv) records dispositions for all 90 presentations. See [the release record](salts-ui-polish-release.md) for the exact artifact and verification boundaries.

Final artifact: `build/libs/appliedenergistics2-0.0.0-salts-ui-polished-r3.jar` (8,583,520 bytes). SHA-256: `d98e91d16f6471174620313034e77c5681ec761894b95efcf0c1169fb89d1fa7`. Complete acceptance: `build/ui-polish-final3-runtime-audit.json`.
