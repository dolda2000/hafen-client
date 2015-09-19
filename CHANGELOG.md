## Changelog

### r31 (2015-09-19)

- Added game time indicator from legacy haven. Moon phase might be misleading since there isn't clear moon indication in the current game and it might've changed somehow

### r30 (2015-09-18)

- Added button allowing to show grid lines on the minimap
- Added option to display "server" grids. It can be enabled in Display settings.

### r29 (2015-09-17)

- Added notifications for attribute changes
- Added display of in-game time. It can be turned off in Display settings.

### r28 (2015-09-17)

- Added potential quick fix for the mansion loading bug

### r27 (2015-09-16)

- Players riding a horse should be visible on the minimap now
- Made custom icon size configurable

### r26 (2015-09-15)

- Made experimental fix for crashes that happen when game window is resized

### r25 (2015-09-15)

- Added option to use old paving tile set instead of gneiss paving

### r24 (2015-09-14)

- Implemented ability to transfer items between inventories in the order by the average quality using `Alt`+`Wheel`

### r23 (2015-09-14)

- Added world tooltip from the bdew's client showing plant stages, tree growth level and object damage
- `Alt`+`D` allows to toggle mine support radius

### r22 (2015-09-14)

- Fixed disappearing default icons when custom icons toggles are saved or reloaded

### r21 (2015-09-14)

- Other players are displayed on the minimap now
- Overhauled UI for custom icon toggles
- All unknown bushes, rocks and trees are displayed on the minimap by default
- Custom icon toggles can be manually added (or removed) through the `config/custom-icons.config` file
- Added command `:icons reload` to reload manually edited custom icon configuration
- `Alt`+`Click` on the map tile or the object will show its resname
- `Alt`+`Click` on the minimap icon will show its resname
- Fixed wrong amount of hours on when estimate is longer than 24 hours

### r20 (2015-09-10)

- Custom minimap icons can be toggled on/off with `Alt`+`R`
- Double click now activates the selected recipe in the quick search window
- Added stockpile transfer from the Ender's client. Use `Shift`+`Click` or `Ctrl`+`Click` to put/remove single item, hold `Alt` to move all.

### r19 (2015-09-10)

- Implemented grid overlay which can be toggled with `Ctrl`+`G`

### r18 (2015-09-09)

- Fixed crash on character creation (due to a crafting list being empty)
- Fixed crash in case when craft button resources aren't in the cache (or just not loaded instantly)

### r17 (2015-09-08)

- Added window with quick search for crafting recipes. Open it with `Alt`+`C` and start typing a part of recipe name. Navigate between recipes with arrow keys and hit `Enter` when needed recipe is selected.

### r16 (2015-09-08)

- Fixed compatibility issues with OpenJDK 6

### r15 (2015-09-08)

- Merged vanilla client changes

### r14 (2015-09-08)

- Added study window. Use hotkey `Alt`+`S` to toggle it on/off
- Added hunger and FEP meters. Both meters can be disabled in Display settings
- Added button to show approximate view radius on the minimap
- Minimap can be folded with the `Alt`+`M` hotkey now
- Client can display very rough estimate of completion time (for curious and other items with progress meters). It displays in the extended tooltip once item progress was updated at least two times and progress changed for at least 2%. This estimate shouldn't be really relied upon and  sometimes it displays very weird values

### r13 (2015-09-06)

- Added completion percent overlay text to items (made by @bdew-hnh)
- Added ability to lock study inventory (made by @romovs)
- Changed client window skin

### r12 (2015-09-06)

- Fixed client crashes that happen when items with the name similar to the name of the enabled toggle are dropped to the ground

### r11 (2015-09-05)

- Client now logs all errors to the file `logs/client.log` 

### r10 (2015-09-05)

- Made Darki's minimap icons clickable
- Fixed transparent minimap roads
- Fixed crash when `Esc` is pressed on the icon toggles window

### r9 (2015-09-05)

- Mass transfer for inventories (made by @EnderWiggin)
  - `CTRL`+`ALT`+`Click` drops all similar items
  - `SHIFT`+`ALT`+`Click` transfers all similar items
- Add option to show online/offline notifications for kins

### r8 by Darki (2015-09-04) 

- Optional display of trees, bushes and rocks on the minimap
- Increased chat font size
- Unlimited zoom

### r7 (2015-09-04)

- Merged changes from the vanilla client
- Removed option to always show kin names since default client does it now
- Fixed bug with displaying party members outside of the minimap window

### r6 (2015-09-03)

- Implemented movement while holding left mouse button

### r5 (2015-09-03)

- Added ability to drag minimap with the middle mouse button
- Added quick access to hand slots (made by @EnderWiggin)

### r4 (2015-09-02)

- Show party member directions on the minimap

### r3 (2015-09-01)

- Replaced Fraktur font
- Added option to disable minimap saving

### r2 (2015-09-01)

- Merged vanilla client changes

### r1 (2015-09-01)

- Minimap caching
- Floating resizable minimap window
- Nightvision (can be toggled with the shortcut `CTRL`+`N`)
- Option to make kin names always visible
- Added option to disable flavor objects (made by @romovs)
- Added option to disable camera snapping during rotation