# Salts window redesign review

Historical release record: the current [AE appearance fidelity revision](salts-ae-fidelity-review.md) supersedes this native-style visual target. The artifact and PDFs below describe the earlier release.

The AE desktop layouts have been redesigned against [the layout standard](salts-window-layout-standard.md). This record identifies the final design, evidence and coverage limits. All changes are in the AE fork. Salts references were limited to the supplied HTML API guide, `desktop_window_api.md` and permitted `api-examples`; no Salts implementation source or binary internals were read.

## Release and illustrated reports

Artifact: `build/libs/appliedenergistics2-0.0.0-salts-layout-redesign.jar` (8,566,655 bytes).

SHA-256: `783733974DD7D8C2E17101907A0DCA1FE52FCCE747B57FDFC17C352EA351E891`.

- [Redesigned container UI report](../output/pdf/AE2_Salts_Redesigned_Container_UI_Report.pdf): 69 pages, 68 labeled/described images. Includes all 40 containers, representative attachments, four encoding modes, local forms, seven error presentations and populated examples.
- [Normal AE versus Salts comparison](../output/pdf/AE2_Normal_vs_Salts_UI_Comparison.pdf): 41 pages, all 40 containers in labeled side-by-side pairs with a description for each image. Ordinary AE with Salts absent is the baseline; AE containers do not exist in unmodified Minecraft.

Both PDFs identify this release hash. Every page was rendered and visually reviewed. Automated checks verified page/image counts, paired coverage, labels, figure numbers and text bounds. Each comparison pair uses GUI scale 2 and the same GUI-pixel scale. Separate window preferences and live values can differ between captures; the covers explain these differences.

## Design acceptance

| Standard section | Final implementation and evidence |
| --- | --- |
| 1. One native frame | Salts supplies the frame, caption and lifetime. AE renders content directly; the old outer screen and repeated player-inventory panel are suppressed. All 40 base captures show this presentation. |
| 2. Protruding tabs | Encoding modes use native Creative-style top tabs. Tools/navigation attach to edges; long rails wrap onto the bottom. Shared contours, selected seams, hover and focus treatment replace internal toolbar gutters. All four modes and real tab clicks were checked. |
| 3. Attached panels | Upgrades, view cells and Toolbox have explicit visual/hit/placement bounds and exclusive state. Panels follow the parent and compact when needed. Hidden slots retain menu identity without visible hit areas. Toolbox pickup/return conserved all four fixture cards. |
| 4. Content-based sizing | Forty registrations use 37 distinct desktop layouts plus four local-view profiles. Machines/dialogs fit content; enabled filter rows drive height. Default, minimum and requested sizes share the desktop calculation. |
| 5. Spacing/alignment | Slots retain 18-pixel pitch and 16-pixel icons. Labels/fields/actions use deliberate padding and groups. Populated Interface amount controls expand the body. Key Types short labels remain on one line. |
| 6. Surfaces/controls | Neutral gray bodies, recessed slots/lists and native checkbox/radio/button/tab sprites surround AE icons. Real slots use the public native slot path. Selected, hovered, focused and disabled controls have distinct treatment. |
| 7. Titles/hierarchy | All 40 menus have meaningful native captions and generated translations. Dynamic network statistics, quantities, plan status and CPU information remain visible. Local forms identify their operation in the body. |
| 8. Machine/empty states | Process arrows and idle outlines are independent of the old screen texture. Workbench/Assembler explain missing prerequisites. Connected wireless catalogs, populated filters, recipe modes and controlled errors were reviewed. |
| 9. Desktop behavior | Catalogs gain rows/columns at 18-pixel pitch; lists gain readable space. An explicit 8-pixel corner grip requests native size changes. Title movement, grip resizing, preserved search/topology, overlap ownership, focus transfer and pointer cancellation passed the native suite. |
| 10. Public API contract | Optional providers, per-window state, external regions, native slots and authenticated actions remain isolated behind the documented API. Context/graphics objects are not retained. Both runtime configurations passed packaged checks. |
| 11. Visual coverage | All 40 base menus at GUI scales 2/3/4 and all 40 ordinary AE baselines. Supplements cover panels, modes, local-view returns, upgraded/populated states and error layouts. |

Captured vanilla menus, Creative/player inventory and mount inventory informed the standard. Native title movement was also observed with a real Windows drag after toggling the native lock. The API-owned corner grip sends documented size requests while preserving one outer frame; virtual catalogs do not depend on an unverified border-drag path.

## Final visual evidence

Each directory has original screenshots and a `manifest.csv` recording menu identity, body/window bounds, visible-region bounds and GUI dimensions.

| Directory under `build/` | Coverage | Completion, 9 October 2026 |
| --- | --- | --- |
| `salts-layout-final-scale2` | 125 captures: all 40 bases plus panels, modes, forms, errors and returns | 14:55:53 |
| `salts-layout-final-scale3` | All 40 base containers | 14:57:56 |
| `salts-layout-final-scale4` | All 40 bases at the compact viewport | 14:59:57 |
| `ae-layout-final-baseline` | All 40 ordinary AE containers, Salts absent | 15:01:33 |
| `salts-layout-rich-final` | 35 captures across seven populated hosts, including four encoding modes | 15:07:24 |

Captures were refreshed after the resize grip and Key Types label correction. Runtime geometry checks verified active body slots inside the content clip, attachment slots inside external regions and visible external controls inside the viewport. PDF page 62 uses the populated Storage Bus with its Upgrades panel open to avoid a transient hover tooltip obscuring the image.

Atlas clients idle after `ATLAS COMPLETE` and were stopped after capture. Their Gradle launch logs may therefore end with a termination failure; acceptance uses completed atlas markers, manifests and geometry checks. Generated build evidence may be cleaned; the PDFs preserve the visual record.

## Final build/runtime verification

| Check | Result and evidence under `build/` |
| --- | --- |
| Build/formatting | `salts-layout-release-build.log`: `jar spotlessCheck` passed. Final hash, descriptor and 41 profiles verified; no Salts classes bundled. |
| Present checks | `salts-layout-final-present-verification.log`: `check runGametest` passed, including all 77 required game tests. |
| Absent checks | `salts-layout-final-absent-verification.log`: `test runGametest` passed, including all 77 required game tests. |
| Unit result | 549 cases: 548 executed/passed, one skipped; zero failures/errors in final JUnit XML. |
| Packaged native client | `salts-layout-packaged-present-client.log`: complete at 14:48:26, including movement, resizing, cursor/crafting, same-type instances, keyboard ownership, focus cleanup and owner-removal pointer cancellation. |
| Packaged ordinary client | `salts-layout-packaged-absent-client.log`: complete at 14:50:21, Salts removed from runtime classpath. |
| Final packaged servers | `salts-layout-packaged-present-server.log` and `salts-layout-packaged-absent-server.log`: final jar passed all 77 required game tests in each configuration; common provider discovery verified with Salts. |

The earlier focus assertion clicked an editor covered by another window's title bar. The corrected fixture raises the background window first, clicks its revealed editor, and separately closes an owner during pointer selection. Both checks passed. All QA clients have been stopped.

## Known documentation gap and coverage limits

Recipe-browser transfers remain disabled: the permitted material omits the `validateTransfer` signature, request/context fields and `DesktopTransferDecision` construction API. The user declined more signatures and requested this issue in the end report. No forbidden inspection filled the gap.

Error and processing-amount images use controlled presentation data; they do not prove each server failure/transfer occurred. Verification covers the recorded fixtures, not every fluid/filter/hotbar gesture, custom name, resource pack, persistence combination or external tooltip/recipe-browser mod. Default Minecraft notifications and HUD/chat may appear outside windows in compact captures. See [the integration report](salts-inventory-integration.md) for installation and runtime details.
