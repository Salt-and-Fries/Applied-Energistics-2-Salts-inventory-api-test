# Fixes from the AE visual issues report

The later annotated screenshots identified three remaining presentation defects. They are corrected in [the final toolbar/footer follow-up](salts-final-contours.md), whose `0.0.0-api5-final-contours-r2` build is the current release. That follow-up supersedes the relevant r6 toolbar and footer visual acceptance details below.

The source report is `output/pdf/AE2_Salts_API5_Visual_Issues_Review.pdf`, SHA-256 `fe3caaa65546c34349f2469d403a127455e1d01abe02555761080bbfd7f9cda5`. Its reference build is `0.0.0-api5-standard-controls-r12`.

All twelve findings are implemented and verified using the first recommended approach. Finishing corrections retained that approach; no alternate design was substituted. The verified release is `0.0.0-api5-visual-fixes-r6`.

| Finding | Implementation of the first recommendation | Required verification |
| --- | --- | --- |
| F01 | On narrow desktops the CPU list sits above the job table inside the main AE frame. Native-height rows scroll independently. | All CPU rows and the final row's selection remain reachable at 320 x 240 GUI pixels. |
| F02 | The original-size Help toolbar follows the outer edge of the connected CPU selector on wide desktops. | Complete CPU heading and Help artwork remain clear at all scales. |
| F03 | The Wireless Access Point footer and output housing form one stepped contour using AE texture samples. | Inspect the join at native pixel size without an open edge or duplicated corner. |
| F04 | The encoding body's height includes the last mode tab's complete extent. The inventory cut is two pixels shorter. | All four modes and amount-dialog returns close on the intended footer without reducing tab size. |
| F05 | Catalog closing edges sample native AE corners and span the complete catalog and scrollbar outline. | Six affected catalog families, settings returns, and resized states have continuous closing edges. |
| F06 | Catalog minimum rows account for the active upgrade housing and its bevel. Four upgrades select at least three catalog rows. | Complete artwork and original logical slot identities remain intact in compact Portable Item Cells. |
| F07 | Export Grid sits in a measured footer inside Network Details. Wider device cells use the normal font size with compact large-number formatting and exact tooltips. | Both hosts, large counts, scrolling, and action placement remain readable and usable. |
| F08 | The complete Drive grid and its native housing move down two pixels; the body grows by the same amount. | Standard four-pixel control gutter and clear first-slot artwork. |
| F09 | Synchronized availability and job state choose waiting, unavailable, idle, or active content. Empty-table hints have a clear native table background. | No premature Idle before synchronization; verify each state and real crafting behavior. |
| F10 | Crafting Plan uses full-size status/count lines in measured rows, preserving native item icons and exact hover quantities. | Mixed item/fluid entries, large values, and final-row scrolling. |
| F11 | All crafting-error subviews retain a stable Crafting Error title, including their collapsed presentation. | Seven errors, full/compact native rows, minimize/restore, and return navigation. |
| F12 | Each distinct Toolbox lifetime receives a short title badge; its parent title tooltip identifies the same badge. Shared owners use the same real companion. | Two sources with matching card contents keep distinct badges after dragging; shared lifetime and drag-only policy remain intact. |

Ordinary AE artwork and layouts remain the standalone path. Salts remains optional. Salts materials read for this task were restricted to public documentation; no Salts source or binary internals were inspected. The matching jar is used only for compilation and execution.

Controlled presentation fixtures exercise loading/unavailable/active states, mixed crafting quantities, and errors without pretending that those are observed server failures. Normal server synchronization, real crafting, sharing and disposal require separate runtime checks.

## Acceptance record

Artifact: `build/libs/appliedenergistics2-0.0.0-api5-visual-fixes-r6.jar`.

SHA-256: `44a34450c5b603ec1ce0a37732239c7ba33170a1da1b72ed9264d303af9e909c`.

Target: Minecraft 26.3, NeoForge 26.3.0.43-beta, Salts Inventory Update 0.1.4 / desktop API 5. Salts is compile-only and optional. The AE jar bundles no Salts classes; Salts references are confined to the optional integration packages.

| Native atlas | Framebuffer | GUI viewport | Minimize setting | Accepted evidence |
| --- | --- | --- | --- | --- |
| Scale 2 | 1280 x 960 | 640 x 480 | Enabled | 40 containers, 122 presentations, all 21 galleries reviewed |
| Scale 3 | 1280 x 960 | 427 x 320 | Enabled | 40 containers, 122 presentations, all 21 galleries reviewed |
| Scale 4 | 1280 x 960 | 320 x 240 | Disabled | 40 containers, 108 presentations, all 18 galleries reviewed |

All 352 packaged-client captures were inspected. Native assertions verify the full permitted control order or exactly More/Close, 11 x 11 buttons, three-pixel gaps, six-pixel title clearance, and clear slots/widgets. Each atlas retains all 90 baseline presentations and the original dimensions of all 79 baseline tab instances. The scale-4 run uses the user's disabled-Minimize setting; the other runs exercise enabled Minimize and all seven error views' collapsed/restored presentations.

The final native suites also verify selection and scrolling through five CPUs, selecting the final CPU and returning to the first; four synchronized CPU/job states; a five-entry mixed item/fluid plan with large quantities and final-row scrolling; and fifteen network device groups with large counts and scrolling. These controlled fixtures supplement the real synchronization and crafting checks rather than claiming that synthetic failure states occurred on the server.

The packaged terminal and companion run passed real search, scroll, sort, inventory transfers, crafting, settings, autocrafting, sharing, title dragging, keyboard controls, source replacement, cursor conservation and last-owner disposal. Two distinct tools with matching card contents retained distinct visible Toolbox badges after both were dragged. The lifecycle run passed actual resource reload/restoration, resizing, retained slots/search, the 320 x 240 viewport, management-menu Escape and modal blocking/dismissal. The standalone client passed the ordinary terminal suite with Salts removed from its runtime classpath.

Unit runs with Salts present and absent each completed 549 tests: 548 passed, one skipped, zero failures/errors. Each packaged dedicated-server configuration passed all 77 required game tests.

`build/visual-fixes-release-audit.json` binds all six client exits and their assertions, atlas coverage, visual reviews, original tab dimensions, unit/server results, package checks, source hashes and all twelve findings to the final artifact. `build/visual-fixes-release-interactions/` contains inspected companion/lifecycle screenshots and native-pixel footer-join details. The Wireless Access Point step, encoding footer/tab baseline and six catalog footer joins passed this detailed visual inspection.

No test clients or servers remain running. Both temporary preference files were restored byte-for-byte from `build/visual-fixes-baseline/preferences`. `build/visual-fixes-work-checkpoint.json` records completion; no unresolved finding from the source report remains.
