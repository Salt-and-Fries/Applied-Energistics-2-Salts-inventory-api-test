# Salts Window Layout Standard

Current target: a comfortable, recognizable AE interface rendered as one API 5 surface. Main windows use Salts-managed custom decoration with zero insets; AE owns the visible frame, title, artwork, widgets and attachments. Salts supplies the complete native control row and manages lifetime, focus, movement, resizing and the authenticated inventory session. Drag-only Toolbox companions use mod-owned surfaces. The shared player inventory is presented separately.

This supersedes the API 3 rule that extra Salts chrome was unavoidable and the earlier generic gray redesign. Current implementation and acceptance are tracked in [the standard native control record](salts-standard-window-controls.md). The API 5 restoration and API 4 polish audits remain historical visual references.

## Visual identity and finished surfaces

1. Inherit original, resource-pack-aware artwork and styles. Make deliberate desktop layout overrides for readability and interaction; preserving every old coordinate is not an acceptance requirement.
2. Draw one AE frame and one visible title. On main windows, use a custom managed decoration and zero surface insets without painting another frame, title strip or backing fill. Expose native slot/widget regions so they take priority over the manager's title movement and resize grip. Use a mod-owned surface for drag-only companions.
3. Finish presentation-only inventory cuts using the original texture's bottom border. Reserve a four-GUI-pixel closing gutter before drawing the border, so the last surviving slot is never covered. Inventory-cut profiles retain the original logical height using an effective cut of 91 pixels. Do not leave open vertical edges or a sliced grid. Preserve intentional cutouts instead of enclosing the UI in another rectangle.
4. Keep original tab/icon dimensions and state artwork. Declare their visual, hit and placement bounds, including bevels. Attachments belong to their owner's window group and respect later foreground windows.
5. Retain machine diagrams, progress art, real-slot grids and virtual entries. Remove only actual player-inventory/hotbar presentation. Complete amount, priority, settings, error, CPU and statistics dialogs retain their full content.

## Readable layout

Measure in integral Minecraft GUI pixels. Drawing, clipping and hit testing use the same committed geometry.

Titles and search fields need separate clear space. Hosted catalogs use a title row followed by a full-width search row. Retain normal text and slot sizes. Fit long custom titles to an explicit safe width and expose the full title through hover and narration.

Toolbar controls remain original-size AE controls. When the original vertical stack exceeds a compact body's height, wrap it into attached columns with stable ordering and consistent spacing. Every control must remain reachable; measure backgrounds as well as button interiors.

Settings groups use consistent margins and measured label wrapping. Align radio choices, include labels in hit areas, keep unavailable options readable and disabled, and preserve keyboard focus during painting. Use contrasting foreground colors on dark backgrounds. Desktop palette overrides belong in resource styles and refresh on reload.

## Geometry and input

Prepare widget/slot positions in the documented layout phase. Pure queries describe the completed surface/region snapshot used by both paint and hit tests. Early queries must return valid nonnegative dimensions before initialization finishes.

Declare controls, slots and popup roles explicitly. Managed decoration supplies main-window grips and header controls. Both main windows and drag-only companions declare an explicit MOVE title region; on main windows it also enables the documented right-click management menu with zero host insets. Native controls and slots take priority. Use unused title space for movement; management remains available through the native row and title context menu.

Keep explicit MOVE rectangles outside all possible managed header controls. In the current zero-inset runtime, a broad declared MOVE region can intercept managed buttons despite the documented priority rule. Reserving the maximum seven-button control band is a safe solution; it does not change the visible title or control geometry.

Visual shape, pointer regions and placement constraints are independent. Describe known stepped surface segments and transparent gaps explicitly; image alpha does not choose pointer behavior automatically. Modal ownership still blocks background input.

Keep real logical slot IDs, backing ownership and server permissions intact. Hiding player inventory does not remove slots. Use computed read-only gesture snapshots for original slot previews and clear transient state on completion/cancellation. Virtual entries use authenticated domain actions.

Treat visible and logical layout heights separately. Account for extra header space in row measurement, snapping, scrollbar position and bottom-anchored panels. Do not accidentally shift recipe panels by changing the inventory cut instead of the header measurement.

Retain useful catalog widths and the original row pitch. A constrained-viewport fallback is deliberate and visually reviewed. Preserve/adapt pixel geometry rather than automatically scaling textures.

## Original-surface management controls

Paint the manager's committed control snapshots in the original AE palette and bevel. Expanded order is Sort when eligible, Focus, Pin/Unpin, Lock/Unlock, Link, Minimize/Restore when permitted, Close. Expanded rows contain no More. If the complete row and full styled title cannot fit, compress the whole row to More and Close. Let Salts supply selection, order, bounds, permissions, action routing, tooltips and modality. Do not implement separate command semantics or add an outer frame.

Keep each button 11×11 GUI pixels with a 3-pixel gap and 2-pixel extra right inset (5 pixels total including the trailing gap). Measure the full styled title. Apply these metrics to the usable original title band after excluding native header widgets, tabs and slots. Close retains its position during compression. A custom keyboard entry point may traverse the committed enabled snapshots and activate through native mouse routing; it must respect the live owner and public action permissions.

Reserve title space from the actual first control rectangle. Preserve native tab dimensions. Avoid header widgets and the full 18-pixel artwork of native slots; compact control positioning may vary between profiles. Crafting errors reserve a separate message row beneath the controls.

Render the original collapsed title and Restore control when minimized. Layout-driven preferred sizes must not immediately expand a minimized window. Availability follows Salts permissions and preferences; a disabled global minimize setting must not be bypassed.

## Shared Toolbox companions

The equipped Network Tool uses a real server-backed companion menu, shared by the actual source inventory. Hide the old Toolbox presentation only after acquisition succeeds. Retain the original fallback when acquisition is unavailable, and keep logical slot topology and physical storage ownership intact.

Draw one compact AE frame, original slot artwork and a Toolbox title. The title allows dragging; the companion has no independent Close, Pin, Minimize, Resize or management menu. Its lifetime and visibility follow its authorized parents. Another parent using the same tool must not create or reposition it, and a different tool must remain distinct.

Initially dock outside the parent's complete prepared outline with a clear gap. Wait for committed layout and rendering before capturing evidence. Reflow an automatically docked companion when its parent's local view changes. Preserve a user placement unless the expanded owner covers it. Do not embed the child inside another decorative window.

Use exact-source authorization and actual shared backing inventory. Parent closure must preserve the canonical cursor and the companion while another owner remains. Dispose it once when its last owner closes or its source becomes invalid.

## State and optional installation

Own widgets and styles per window. Refresh terminal geometry, palettes and cached resources after actual reload. Use layout revisions for incompatible saved geometry.

Cancel old input before switching subviews or encoding modes. Maintain one keyboard owner, clear inactive hover, release subview work, and never retain contexts/graphics objects. Hosted callbacks must not install an unrelated game screen or assume the player's global menu represents this session.

The same AE jar remains usable without Salts. Keep all Salts linkage inside optional providers and preserve normal mod opening paths.

## Completion evidence

Review every registered container and additional presentation. Capture GUI scales 2/3/4 and exercise a constrained viewport, resource reload, search/editing, focus, overlap, movement/resizing, cancellation, cursor conservation and crafting. Check title/editor separation, finished borders and attachment hit regions.

Identify one final packaged artifact and verify it with Salts present and absent. Final labeled PDFs use actual runtime images, identify the artifact, and distinguish controlled presentation fixtures from exercised server outcomes. A passing build or style inheritance alone is not visual acceptance.

## Source boundary

Salts reads are limited to public documentation, declarations/snippets embedded there, HTML guides and referenced documentation/READMEs. No Salts implementation, API source, example source, tests, build scripts or binary internals are inspected. Its matching jar is a compilation dependency and runtime mod. AE source, assets and tests are available for adaptation.
