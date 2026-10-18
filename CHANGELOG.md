# Changelog

## 2.21
- Fix: 2.20 failed to load ("NoClassDefFoundError: com/formdev/flatlaf/ui/FlatButtonUI"). NetBeans
  doesn't export FlatLaf's ui package to other modules, so the custom-painted controls now load
  through their own class loader (`sigil.FlatBridge`). `nbproject/project.properties` adds the
  FlatLaf jar to the compile classpath (`cp.extra`) so NetBeans can still build the project.

## 2.20
- Cyber-Sigilism is now a full presentation, and the default theme:
  - Spec palette (void black, ultraviolet, lavender, cold cyan) across every FlatLaf-styled control.
  - Custom-painted buttons, tabs, progress bars, sliders, check boxes, radio buttons, table headers,
    sort arrows, tree expanders, popup menus, tooltips and group boxes.
  - Vector ornament library: pointed arches, rose windows, sigils, thorn dividers, corner pieces.
  - Case Workspace becomes a row of arch windows under a turning rose window.
  - Display typefaces (Pirata One, UnifrakturCook, Cinzel; SIL OFL) for headings only.
  - Toolbar wordmark, status-bar ingest sigil, framed Overview tab, Navigator heading.
  - New Studio tab: presentation, ornament, glow, motion, display type, density, primary/secondary light.

## 2.19
- Case Workspace, Overview tab and Workspace toolbar buttons simplified.
- README rewritten; version history moved here.

## 2.18
- Clicking evidence in the merged case tree opens Autopsy's directory listing.
- Group rows in Saved views show item totals instead of the number of views.

## 2.17
- Case Workspace shows the data source count and ingest state.
- Overview tab lists the query criteria and the file/artifact split.

## 2.16
- Palettes (design profiles) selectable from the Options page.
- Palettes also style table headers, selection, menus, tooltips and progress bars.

## 2.14 – 2.15
- Case Workspace layout reworked; cards reflow to one or two columns.
- Navigator icons per view type, counts after the label.
- Professional Forensics palette.

## 2.13
- "Restore default layout" in Studio > Workspaces.

## 2.12
- Case Workspace always docks in the editor area.
- Rottweiler icon limited to the window icon and Add Data Source button.

## 2.11
- Opening a saved view selects the first result so the content viewer fills in.

## 2.10
- Optional line icons for common toolbar buttons, with a toggle to restore the originals.

## 2.9
- Workspace toolbar gets Case Workspace / Navigator / Studio / theme buttons.
- Icon-only toolbar on by default.
- Gothic Cyber Sigilism palette.

## 2.8
- Flat toolbar buttons with a hover fade.

## 2.5 – 2.7
- Fixes for the Saved views tab detaching from the Navigator.
- Navigator always opens as a docked column, never as a sliding overlay.

## 2.4
- Case Workspace start page.

## 2.3
- Navigator splits Case tree and Saved views into tabs instead of a vertical split.

## 2.2
- Overview result tab for views opened from the Navigator.

## 2.1
- Autopsy's case tree moves into the Navigator (one left column instead of two).

## 2.0.2
- One-time reset of window layouts saved under 2.0 (backup in `config/ModernUI/layout-backup`).
- "Lock toolbars" option, on by default.

## 2.0.1
- Fix: Navigator docking scrambled the main window layout.
- Fix: clipped Navigator labels.

## 2.0
- Studio window: palettes, property editor, toolbar/menu editor, workspaces, Navigator.

## 1.2
- Theme is set before any window is created (fixes blank areas from switching at runtime).

## 1.1
- Custom accent, font, grid lines, tab style, dark-mode color fix.

## 1.0
- Themes, accents, corner radius, row height, striped rows, FlatLaf title bar.
