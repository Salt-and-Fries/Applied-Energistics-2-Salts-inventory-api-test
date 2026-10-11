# Final AE toolbar and footer corrections

The later [rectangular-toolbar refinement](salts-rectangular-toolbar.md) supersedes the stepped backing described below and is the current release. The earlier footer fixes remain included.

This follow-up addresses the three annotated screenshots and the user's clarification that compact toolbar columns should remain. Salts input is limited to its public Markdown guide/reference; no Salts source, example source or binary internals are read. The matching optional jar is used only for compilation and runtime.

The shared AE left toolbar now measures the full native painted bevel, including its hovered position, instead of treating the 16 x 16 input rectangle as the entire artwork. Its backing preserves the original button dimensions and native edge thickness. Adjacent columns form one continuous stepped silhouette, with adequate top, side and bottom space. Space below a partially filled outer column remains transparent. Declared external regions follow the painted columns rather than a broad enclosing rectangle. Visibility changes and returns rebuild the same geometry.

Pattern Encoding's last 22 x 22 mode tab retains its icon, original input area and native texture. Its two-column joining rail turns into the main footer at the bottom, eliminating the white downward notch. The correction runs after the native tabs are painted and applies regardless of the selected encoding mode or local-dialog return.

The Wireless Access Point's body grows from 77 to 92 GUI pixels to share the original link housing's lower baseline. The removed-inventory cut shrinks by the same 15 pixels, preserving the original logical screen height and all machine slot positions. A native clear strip replaces the newly exposed inventory artwork, and the main footer joins the housing as one continuous lower edge. The housing's one-column white rail turns into that footer instead of leaving a spur. Its arrow and real slot artwork retain their original positions and sizes.

These corrections affect only the optional hosted presentation. The standalone AE layout and the standard Salts-managed window controls remain intact.

## Acceptance

Verified artifact: `build/libs/appliedenergistics2-0.0.0-api5-final-contours-r2.jar`.

SHA-256: `bc8003c5471aa62a3ea19e2a69a98a3405cd2ebb7e7483461e7600c2990b124f`.

Target: Minecraft 26.3, NeoForge 26.3.0.43-beta, Salts Inventory Update 0.1.4 / API 5.

| GUI scale | GUI viewport | Native container coverage | Presentations | Hovered toolbar bevels checked | Original widget dimension comparisons |
| --- | --- | --- | --- | --- | --- |
| 2 | 640 x 480 | All 40 | 122 | 307 | 726 |
| 3 | 427 x 320 | All 40 | 122 | 307 | 726 |
| 4 | 320 x 240 | All 40 | 108 | 300 | 670 |

All 352 presentations passed native geometry and standard control-policy checks. The scale-2/3 runs enable Minimize; scale 4 uses the user's disabled setting. Each scale's 21 distinct painted toolbar states was visually reviewed, with exact pixel hashes mapping the repeated captures to those reviewed states. The complete painted bevel, including hover movement, stays inside its backing with side/top clearance and a clear native footer. Adjacent columns have no unfilled gap, and their declared external regions follow the actual stepped backing.

Nine footer views per scale were reviewed and pixel checked: Wireless Access Point, the base Pattern Encoding view, all four encoding modes, and returns from settings, key-type selection and the amount dialog. The Wireless Access Point's full four-row footer has a continuous palette across the former join. Pattern Encoding's final joining rail matches the lower footer instead of leaving a white notch. Machine-slot coordinates and original button/tab dimensions match the r6 reference.

The packaged Salts-present client passed real toolbar actions, search, scroll, sorting, inventory transfers, crafting, settings, autocrafting, title dragging and resizing. Its lifecycle suite passed actual resource reload/restoration, retained slots/search, the small viewport, management-menu Escape and modal blocking/dismissal. The same packaged jar passed the ordinary terminal suite with Salts removed from the runtime classpath.

Unit configurations with Salts present and absent each completed 549 tests: 548 passed, one skipped, zero failures/errors. The AE artifact contains no Salts classes and confines all Salts references to the optional integration packages. No Salts implementation or binary internals were inspected.

`build/final-contours-release-audit.json` binds all five successful client exits, all atlas coverage, reviewed states, pixel checks, dimension comparisons, unit results and package boundaries to the artifact hash. `build/final-contours-visual-review.json` records the inspected evidence. Test clients have exited, and both scratch preference files were restored byte-for-byte.

[Before/after comparison](../output/ui/AE_final_contours_before_after.png) uses actual packaged-client captures at the same GUI scale. `build/final-contours-release-scale2/review/`, `scale3/review/` and `scale4/review/` contain the complete focused toolbar and footer reviews. `build/final-contours-release-interactions/` contains the inspected lifecycle screenshots.
