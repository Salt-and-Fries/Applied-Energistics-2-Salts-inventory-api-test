# Rectangular AE toolbar backing

The shorter left columns now extend to the tallest column's footer. Every hosted AE left toolbar has one rectangular backing with a continuous lower bevel. Button positions, sizes, artwork and input areas are unchanged. This supersedes the stepped backing in `0.0.0-api5-final-contours-r2`.

Verified artifact: `build/libs/appliedenergistics2-0.0.0-api5-rectangular-toolbar-r1.jar`.

SHA-256: `31042bf006a7226283494d8ad8684daef52ab246917b317e324e8fda7a669872`.

The packaged native client passed all 40 containers and 90 presentations at GUI scale 2. A focused scale-4 run passed nine container families and 41 presentations at 320 x 240 GUI pixels, covering terminals, their settings/mode/dialog returns, portable cells and the Inscriber. Assertions verified equal column heights and a continuous flat bottom border across all 126 toolbar presentations. All 458 button paint-bound comparisons match the previous build exactly. The 21 distinct toolbar drawings at scale 2 and eight at scale 4 were visually inspected.

The build and optional package-boundary checks passed. No Salts source or binary internals were read. Both temporary preference files were restored byte-for-byte, and the test clients exited. This is a hosted-background-only change; the standalone rendering branch is unchanged.

Machine-readable acceptance: `build/rectangular-toolbar-release-audit.json`.

[Before/after capture](../output/ui/AE_rectangular_toolbar_before_after.png).
