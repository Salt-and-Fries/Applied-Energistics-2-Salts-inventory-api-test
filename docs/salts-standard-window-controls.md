# Standard Salts controls on AE windows

The AE fork now uses the updated public API's complete native controls on all main desktop windows and their local subviews. It retains a single original AE frame, title and resource-aware artwork. The shared Toolbox companion remains mod-owned and drag-only.

The current verified build is `build/libs/appliedenergistics2-0.0.0-api5-rectangular-toolbar-r1.jar`, SHA-256 `31042bf006a7226283494d8ad8684daef52ab246917b317e324e8fda7a669872`. The latest refinement is recorded in [the rectangular-toolbar acceptance](salts-rectangular-toolbar.md); the broader control and footer verification is preserved in [the final toolbar/footer corrections](salts-final-contours.md). The earlier narrow Crafting Status clipping issue described below remains resolved.

## Implementation

Main windows declare a SALT surface with zero insets and a custom `DesktopWindowDecoration`. Salts chooses the available controls, ordering, compression, bounds and action permissions. AE paints those committed rectangles with its palette and bevel after the original content; collapsed windows paint their original title frame in the decoration callback.

Both main windows and companions declare clear title space as an explicit MOVE region. This preserves the documented title context menu on main windows with zero host insets. The main region conservatively excludes the largest seven-button row: runtime testing found that a broad declared MOVE region could intercept managed controls despite the guide's priority rule. Native widget/slot regions are separately declared. The companion's drag-only policy still denies management.

Expanded order is Sort when eligible, Focus, Pin, Lock, Link, Minimize when enabled, Close. Expanded rows do not include More. A title that cannot fit alongside the complete permitted row causes the entire row to become More and Close. Buttons remain 11×11 GUI pixels, gaps remain 3 pixels, and Close retains the trailing 5-pixel edge margin. AE excludes original header widgets and slots from the usable title band and reserves six pixels between the actual rendered title and the first control.

The manager receives the full styled title of the current AE view, including overrides. AE shortens only the visible original title within the committed safe area. Local settings, amount, priority, encoding and crafting views therefore use the same native control policy as their parent.

Alt+F10 enters the committed enabled header controls. Tab and Shift+Tab traverse them, Enter and Space activate through the native screen's control routing, and Escape leaves control focus. A NeoForge pre-input hook keeps this entry point ahead of native widget traversal. It accepts only a live, keyboard-focused AE owner on the current screen whose public focus permission is available. Normal pointer input, focus loss and disposal release header focus. No independent Pin, Lock, Link or Sort command implementation bypasses Salts.

The runtime's native paint snapshots did not consistently expose selected Pin/Minimize states. AE reads the public `DesktopWindowHandle.state()` fields for those paint flags and Lock while retaining native bounds, enabled, hover and pressed states. These are visual corrections to public snapshots; native action routing remains authoritative.

## Earlier control baseline verification

Verified artifact: `build/libs/appliedenergistics2-0.0.0-api5-standard-controls-r12.jar`.

Target: Minecraft 26.3, NeoForge 26.3.0.43-beta, Salts Inventory Update 0.1.4 / desktop API 5. The matching optional dependency hash is `f3f4c7a64a040ed18e86653f68fce445f3e8fa8945c36d7a3509423966dd390a`.

SHA-256: `06e61b52f0598a223ac94a9391762eea6c3fd1a6f615d4e66e43b67cb10b3d47`.

Unit runs with Salts present and absent each completed 549 tests: 548 passed, one skipped, zero failures/errors. Each packaged dedicated-server configuration passed all 77 required game tests. Native client evidence below uses this exact packaged jar rather than exploded development classes.

- `build/standard-controls-move-clearance-packaged-present-client-exit.json`: exit 0, exact artifact hash, all focused control and companion assertions complete. Mouse Pin/Unpin and Minimize/Restore, title-driven full/compact switching, More/Escape, keyboard activation, sharing, drag, visibility, cursor conservation, different sources, source replacement and last-owner disposal passed.
- `build/standard-controls-native-lifecycle-packaged-present-client-exit.json`: exit 0, exact artifact hash, full terminal and lifecycle assertions complete. Search, scrolling, native sorting, transfers, crafting, settings, overlapping owners, title dragging and resizing passed. Actual resource reload/restoration, retained slots/search, the 320×240 GUI viewport, title-menu Escape and modal blocking/dismissal passed.
- `build/standard-controls-interactions/`: inspected runtime captures of the native More menu, restored/collapsed original frames, shared/replaced Toolbox sources, resource reload, constrained viewport and modal blocking. The small-viewport capture establishes layout; title-menu behavior is established by assertions.
- `build/standard-controls-final-ordinary-packaged-absent-client-exit.json`: exit 0, exact artifact hash, standalone terminal assertions complete with Salts removed from the runtime classpath. Ordinary search, scroll, sorting, inventory interactions, crafting and settings passed.

| Native atlas | Framebuffer | GUI viewport | Minimize setting | Result |
| --- | --- | --- | --- | --- |
| Scale 2 | 1280×960 | 640×480 | Enabled | 40 containers / 90 views; all 15 galleries reviewed |
| Scale 3 | 1280×960 | 427×320 | Enabled | 40 containers / 90 views; all 15 galleries reviewed |
| Scale 4 | 2560×1920 | 640×480 | Disabled | 40 containers / 90 views; all 15 galleries reviewed |

Each atlas validates complete native order or exactly More/Close, 11×11 buttons, three-pixel gaps, six-pixel title separation, and no overlaps with original widgets, slots or crafting-error messages. All 79 tab instances per atlas retain dimensions matching both the polished API 4 and ordinary references. The jar bundles no Salts classes and confines Salts references to the optional common/client integration packages.

Final machine-readable record: `build/standard-controls-final-audit.json`. It binds client exits, assertions, all 270 captures, unit/server results, package checks and the retained narrow-viewport failure to the artifact hash. Test processes exited and the scratch Salts preferences were restored byte-for-byte.

The earlier API 5 restoration PDFs describe the frozen r15 artifact and its old four-control row. They are historical visual references, not captures of this release.

## Resolved limitation in the earlier baseline

At GUI scale 4 in a 1280×960 framebuffer (320×240 GUI pixels), the unchanged Crafting Status surface plus its 100-pixel CPU-selection attachment is wider than the viewport. Its left panel extends 17 GUI pixels off-screen. The strict atlas check failed and the evidence is retained in `build/standard-controls-scale4-320gui/`; the header controls themselves fit and retain the correct five-button order with Minimize disabled. This control update does not redesign that existing panel.

The frozen r15 scale 4 atlas used a 2560×1920 framebuffer (640×480 GUI pixels). The complete scale 4 control audit uses that same viewport. The current scale 3 atlas also passed a narrower 1280×960 framebuffer (about 427×320 GUI pixels). Full-atlas evidence and the separate 320×240 terminal lifecycle check must not be described as all windows fitting the minimum viewport.

## Source boundary

Salts input was limited to the public Markdown guides and declarations embedded in their reference. No Salts implementation, API source, example source, tests, build scripts or binary internals were read. Its matching jar was used only for compilation and runtime. Minecraft/NeoForge public declarations, AE source, AE assets, runtime screenshots and public state snapshots were used for implementation and verification.
