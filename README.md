# Autopsy Modern UI

A NetBeans plugin that modernizes Autopsy and lets you customize nearly everything about how it looks and how it's organized. It's built on **FlatLaf**, which already ships with Autopsy's NetBeans platform (FlatLaf 2.4 in Autopsy 4.22.1), so nothing extra is bundled. Autopsy's own files are never modified: every change lives in your user settings and can be reset.

## Where to find things

| What | Where |
|---|---|
| Quick settings (theme, accent, font, rows, tabs) | **Tools → Options → Modern UI** |
| **Modern UI Studio** (everything below) | **Tools → Modern UI Studio…**, **View → Modern UI Studio…**, or **Ctrl+Shift+U** |
| Light/dark toggle | **View → Toggle Light/Dark Theme**, the Workspace toolbar, or **Ctrl+Shift+L** |
| Workspace switcher | the **Workspace** toolbar, or **Window → Workspaces** |
| Case Workspace | opens in the main work area at startup, or **Window → Case Workspace** |
| Studio Navigator | opens on the left; reopen with **Window → Studio Navigator** |

## Case Workspace

The Case Workspace is a task-oriented starting page in the main work area. It groups common searches into **Triage**, **Find files**, **User activity**, and **Review**. Each shortcut opens the corresponding saved Navigator search in Autopsy's native result viewer; the original Table, Thumbnail, Summary, and other result tabs remain available. Use **Open Navigator** to browse all saved views, or **Customize** to change them.

## Modern UI Studio

### Theme
- **12 palettes:** Dracula, Nord, One Dark, Solarized (dark and light), Gruvbox, Catppuccin Mocha, Monokai Pro, GitHub (dark and light), Material Ocean, and Forensic Navy. Each palette brings its own light or dark base.
- **Override any UI property:** colors, sizes, arcs, fonts, row heights, scrollbars, tab underlines. "Add property…" lists everything the current theme defines, with live values and color swatches. `@` variables such as `@background` or `@accentColor` recolor everything derived from them.
- **Import and export** themes as `.properties` files so you can share them.
- **Apply now** (live) or **Save and restart** (the most reliable).

### Toolbars & menus
- **Hide, show and reorder** every toolbar button and menu item, including whole menus.
- **Icon-only toolbar:** button names move into tooltips. **Icon size:** small or large.
- **Lock toolbars:** hides the ⋮ drag handles. On by default.
- **Reset this group** restores Autopsy's original toolbar or menu.
- **Shortcuts and layout:** links to keyboard shortcuts, plus a **Reset window layout** button. Autopsy hides its own Reset Windows menu item.

Under the hood this edits the same configuration folders Autopsy's toolbars and menus are built from. It uses NetBeans' `_hidden` markers (the mechanism Autopsy itself uses) and writes only to your user config.

### Workspaces
Named window layouts, such as *Triage*, *Deep Dive* or *Reporting*. Switch to a workspace, then drag, resize, open or close panels, and the layout is saved automatically. These are built on NetBeans window "roles", and each one is stored in `config/Windows2Local-<name>`.

### Navigator
**One column:** the Navigator has separate **Case tree** and **Saved views** tabs, so either navigation tree can use the full panel height. The case tree is Autopsy's own tree (Data Sources, File Views, Data Artifacts, Analysis Results, OS Accounts, Tags, Score, Reports) with its Back, Forward and ⚙ buttons. It is the same tree, not a copy, so clicking a node updates the results exactly as before, and Autopsy's "go to" actions select nodes there. Autopsy's separate tree panel is tucked behind the Navigator: it can't be closed, and it has to stay open for Autopsy's features to work. Turn this off in Studio → Navigator to get Autopsy's tree column back.

Your own way of organizing a case, alongside Autopsy's tree. **Groups** hold **views**, and each view is a saved search with a live count. Clicking a view opens its results in **Autopsy's own result viewer**, so tables, thumbnails, content viewers, tagging and right-click actions all work normally. Ctrl+click opens a view in a new tab.

Navigator results also include a Modern UI **Overview** tab, added through Autopsy's result-viewer extension point. The built-in Table and Thumbnail tabs remain available, so this is a low-risk way to experiment with replacing parts of a core screen without changing Autopsy itself.

| View type | Parameter |
|---|---|
| File extensions | `jpg, png, heic` |
| MIME type | `image/%` |
| Artifact types | `TSK_WEB_HISTORY, TSK_WEB_DOWNLOAD` (or use the picker) |
| Tagged items | a tag name, or `*` |
| Modified recently | days |
| Large files | MB |
| Deleted files | none |
| Custom SQL filter | a WHERE clause on `tsk_files`, e.g. `name LIKE '%invoice%'` |

- **Starter layout:** 34 triage views across Media, Documents, Suspicious, User activity, Communication, Location and Review.
- **Test on open case:** checks a view's count before you save it.
- **Sharing:** import and export navigator layouts.
- **Read-only:** every query only reads the case database. Extension and MIME filters are escaped, so input can't change the query.
- **Moving the panel:** use the ⋮ button (left, right, bottom or floating).

## How the theme is applied

1. **Before NetBeans picks its theme:** NetBeans calls each plugin's `validate()` first. `Installer` sets the value `--laf` would set (the FlatLaf class for your theme) and registers your overrides with `FlatLaf.setGlobalExtraDefaults`.
2. **NetBeans starts in FlatLaf:** it installs its own FlatLaf window styling (module `org.netbeans.swing.laf.flatlaf`), so no window is ever built in the Windows look.
3. **After startup:** `restored()` confirms the theme, starts the dark-mode color fixer and applies the toolbar settings.
4. **Override order:** palette → Options settings → Studio overrides, so later layers win.
5. **Fallback:** if a future Autopsy version blocks the startup hook, the theme is switched after startup instead. Table headers left with old-theme renderers are rebuilt, because they would otherwise crash while painting and leave the window blank.

## Build and install

1. In NetBeans, go to **Tools → NetBeans Platforms**. A platform named exactly `Autopsy_4.22.1` must point at `C:\Program Files\Autopsy-4.22.1`.
2. **File → Open Project** → this folder.
3. Right-click the project → **Clean and Build**, then **Create NBM**. The plugin is written to `build\com-modernui-autopsy.nbm`.
4. In Autopsy: **Tools → Plugins → Downloaded → Add Plugins…**, then install. This update is version 2.8.
5. **Fully close and reopen Autopsy** after installing.

## If something looks wrong

- **Turn it off:** Tools → Options → Modern UI → uncheck "Use the modern look", then restart.
- **Emergency switch:** start Autopsy with `-J-Dmodernui.disable=true`, or uninstall the plugin under Tools → Plugins → Installed.
- **A toolbar or menu looks wrong:** Studio → Toolbars & menus → select it → **Reset this group**.
- **Window layout:** Studio → Toolbars & menus → **Reset window layout**, or switch back to the Default workspace.
- **Dark themes:** the experimental color fix catches most hard-coded panels. If one looks worse, uncheck the fix in Options.
- **Logs:** `messages.log` (Help → Open Log Folder) has "Modern UI applied (at startup)" or a warning explaining what happened.

## Where settings are stored

- **Basic settings:** Autopsy's user preferences.
- **Larger data:** `%APPDATA%\autopsy\config\ModernUI\` holds `theme-overrides.properties`, `navigator.txt`, `hidden-items.txt` and `order-snapshots.txt`.
- **Workspace layouts:** `%APPDATA%\autopsy\config\Windows2Local-<name>\`.

## Files

| Package | Purpose |
|---|---|
| `com.modernui.autopsy` | Startup (`Installer`), theme application (`ThemeApplier`), settings, dark-mode fixer, config store, `layer.xml` (navigator docking area, Workspace toolbar) |
| `…theme` | Palettes, overrides, Theme Studio tab |
| `…ui` | Toolbar/menu config editing (`UiConfig`), icon-only/size (`ToolbarCustomizer`), editor tab |
| `…workspace` | Workspace manager, Studio tab, toolbar/menu switcher |
| `…navigator` | Navigator window, layout model, case queries, result viewer bridge, editor tab |
| `…studio` | Studio window, light/dark toggle, shared UI helpers |
| `…options` | Tools → Options → Modern UI page |

## Limits

A plugin can't change the *inside* of Autopsy's own panels, such as the order of categories in Autopsy's tree or the content viewers' internals. The Navigator works around the first one by giving you your own tree. A fork of Autopsy would be needed for the rest.

## Version history

- **2.5:** fixes Navigator's Saved views tab being detached from its tabbed container
- **2.6:** moves the Navigator out of saved sliding-overlay modes and into a resizing side column whenever it opens
- **2.7:** keeps the Saved views tab inside its tabbed pane so it remains available alongside Case tree
- **2.8:** adds compact FlatLaf toolbar buttons with a subtle animated hover transition
- **2.4:** opens a task-oriented Case Workspace in the main work area, with direct shortcuts to common saved searches; native result tabs remain available
- **2.3:** organizes the merged Navigator into full-height Case tree and Saved views tabs instead of squeezing both into a vertical split
- **2.2:** experimental Modern UI Overview tab for results opened from Navigator views; built-in result tabs remain available
- **2.1:** one column. Autopsy's case tree (Data Sources, File Views, Analysis Results, Tags…) now lives at the top of the Navigator, with Back/Forward/⚙. Autopsy's own tree panel is tucked away. Toggle in Studio → Navigator.
- **2.0.2:** automatically resets a window layout saved while 2.0 was installed (one time; backup kept in `config/ModernUI/layout-backup`). New "Lock toolbars" option, on by default, hides the ⋮ drag handles.
- **2.0.1:** fixes the Navigator scrambling Autopsy's window layout (it now docks as its own column left of the tree) and fixes clipped Navigator labels and counts
- **2.0:** Modern UI Studio: 12 palettes, any-property theme editor with import and export, toolbar and menu editor (hide, show, reorder, icon-only, icon size), workspaces, Studio Navigator with 34 starter views and custom SQL, light/dark toggle, keyboard shortcuts
- **1.2:** theme chosen before any window exists. Fixes blank areas.
- **1.1:** custom accent, font and size, grid lines, tab style, dark-mode color fix, reset
- **1.0:** themes, accents, corners, row density, striped rows, modern title bar
