## Changelog

### r81

- Fixed crash caused by the driving a wagon with movement path on

### r81

- Fixed bug with item quality display

### r80

- Added hotkey `Ctrl`+`H` which hides crops and displays all trees as stumps
- Added hotkey `Alt`+`P` to toggle movement paths for players and some animals (based on Ender's client)
- Added optional alarm which triggers when pony power goes below 10%
- Added optional background for item quality text

### r79

- Hopefully fixed client crashes happening during frequent map updates (e.g. on mining) 

### r78

- Changed hotkeys for equipment slots to use `Shift` instead of `Ctrl`
- Added option to always show extended tooltip
- Fixed hotkeys not working on opened stockpiles
- Hopefully fixed random crashes related to the radius display

### r77

- Made another attempt to optimize excessive memory usage related to the minimap changes at `r75`

### r76

- Made some optimizations that hopefully fix short client freezes 

### r75

- Merged latest vanilla changes fixing issues with minimap
- Added radius display for troughs

### r74

- Added hotkey `W` for drinking from all flasks and teapots in the inventory
- Draggable widgets now really shouldn't go off-screen (I hope), at least between client restarts

### r73

- Added option to change radius of the autopicker
- `Shift`+`Alt`+`Right Click` now transfers items of the same type and quality

### r72

- Replaced option to hide kin names for objects with the hotkey `Shift`+`I`
- Fixed crash that could happen on clicks inside the chat window
- Fixed crash on mouse wheel inside the study window
- `Lock` and `Auto` check boxes now should properly synchronize state between the study window and the character screen to avoid confusing behaviour
 
### r71

- Adjusted item quality display to the changes in the recent update
- Added option to hide kin names over hearthfires and objects other than the player

### r70

- Added hotkeys for equipment slots. They allow to put an item on cursor to the slot when it's empty, or to take the item from the slot otherwise
- Chat messages in the format `hs:<secret>` now recognized as hearth secrets and can be clicked to kin the person
- Merged latest vanilla client changes that might fix crashes with `IndexOutOfBoundsException`

### r69

- Added option to disable weather effects
- Added slider that adjustes nighvision brightness. See `Display->Game` settings
- Changed grid overlay color

### r68

- Fixed non-disappearing radiuses

### r67

- Moved all custom options to the separate panel accessible through the `Custom settings` in the settings menu
- Added option to show timestamps in chat
- Added option to show percentages on the hourglass
- Added option to disable autostudy from cupboards (sorry for the pearl, Avu)
- Added radius display for beehives and made radiuses less intrusive
- Added mode that automatically transfers arrows from the quiver to the inventory (see `eXtended` menu)

### r66

- Added hotkey `Q` to pick nearby forageable shown on the minimap. To update list of forageables you have to remove `config/custom-icons.xml` file from the client folder and restart client

### r65

- Autostudy picks up curiosities from all opened cupboards now

### r64

- Added command `kinfile <path/to/file>` that can add kins by secrets listed in the specified file
- Fixed wrong error message for filling tasks with unlimited amount of items to use

### r63

- Toned down ambient lighting
- Changed the number of coals to fill a smelter to 11 

### r62

- Added hotkey `Alt`+`T` to toggle on/off tile transitions
- Added tool to fill smelters with exactly 12 coals
- Added option to show growth stage on fully grown trees which can be changed in `Display Settings`
- Fixed ambient lighting
- Fixed issue with grid overlay now always working right after logging in

### r61

- Made it possible to move chat window
- Remade chat tabs to align horizontally
- All draggable widgets (belts, chat, minimap, study) now shouldn't disappear from the screen when it is resized

### r60

- Merged latest vanilla client changes

### r59

- Fixed wrong belt behavoir during combat
- Fixed issue with inablity to click the object under the player on the minimap

### r58

- Made it possible to drag and rotate the belt and equipment slots
- Added optional belt for Fn keys where it's possible to drop custom menu items (like `Plant Tree`, `Fill Trough`, etc). It can be hidden through the option in `Misc settings`.
- Changed chat UI and added ability to change chat width
- Added tool for picking all nearby mussels
- Added total armor display to the equipment window (from romovs)
- Tuned down chipping, quern and ant sounds
- Fixed issue with a recipe not adding properly to the list of the recent recipes when it is used from the belt
- Added hotkey `Alt`+`W` to hide UI and player characters

### r57

- Added tools for filling nearest troughs and coops with edibles and tar kilns with blocks. They are available through the extended menu.

### r56

- Added total time, LP/H/Slot and LP/H/MW to the tooltip for curiosities. This information is not displayed for some items since it is incomplete and it might be not accurate at the moment
- Made minimap borderless
- Fixed issue with empty minimap when its size is too big

### r55

- Fixed issues with new crafting window and redesigned it a bit
- Added ability to cycle through recent recipes with `Shift`+`Tab`

### r54

- Improved tree planting tool (which was done in a rather half-assed way before). Now it clicks automatically on the "Plant tree" menu item and displays various error messages. Also fixed non-working `Enter` key which was supposed to work for a tile selection as alternative to clicking `Done` button
- Added ability to switch between 5 recently used recipes in the crafting window
- Added hotkey `Ctrl`+`Home` to turn the camera in the northern direction
- Made quick access slots bigger
- Merged latest changes from the vanilla client

### r53

- Added option to use `Ctrl` instead of `Alt` for quality transfer
- Added tool for planting trees in the center of the tile. Take treeplanter's pot, hide it using `Alt`+`H`, then use the tool from the extended menu to select a tile

### r52

- Fixed wrong quality display: essence was mixed with substance

### r51

- Fixed issue with view radius position when minimap is offset
- Fixed flashing black screen which was constantly appearing during movement

### r50

- Added overlay that shows object damage, plant and tree growth stages from bdew's client. It can be toggled on/off with `Alt`+`I`
- Added ability to transfer items from lowest to highest quality using `Alt`+`Shift`+`Wheel`
- Fixed issue with "fat" grid lines when party or radius outlines are shown
- Fixed issue with disappearing party outline when party member goes out of sight and back
- Minimap view radius uses distance measured in "server" grids which makes it precise (based on legacy XCom's client)

### r49

- Made account manager optional. It is disabled by default and can be enabled through the option in `Misc Settings`

### r48

- Added account manager from Ender's client

### r47

- Added option to toggle on/off flower menu animations. They're disabled by default and can be enabled back through the option in `Misc Settings`
- Added circle highlight for party members. It can be disabled through the option in `Display Settings`

### r46

- Added mode that prevents accidental aggro on players marked with green, cyan and yellow colors. It can be toggled on with `Alt`+`F`

### r45

- Fixed broken custom icon config.

### r44

- Added option to toggle tracking on login
- Grey color is used for unknown players now
- Fixed mass transfer for different types of meat
- Added ability to hide some useless critters and forageables
- Most bushes, fruit and nut trees now have an icon instead of the text

Note: to see changes related to minimap icons you have to remove file `config/custom-icons.config`

### r43

- Merged latest changes from vanilla client, including added describe action

### r42

- Improved deck switcher: deck can be selected by arrow keys and Enter as well, like in craft and build windows
- Added rename button to the Combat Schools tab
- Item progress percentage now shows when quality display is toggled off

### r41

- Implemented ability to name saved decks using double-click
- Implemented deck switcher. To switch decks press `Alt`+`K` and then press number displayed in the deck list

### r40 (2015-10-04)

- Added sound alarm for unknown players. It can be disabled through the option in `Misc settings`.
- Added slider for alarm volume into `Audio settings`
- Both alarm and auto hearth now trigger on players marked with Red color
- Added item quality display toggled by `Alt`+`Q`. There are 3 modes of quality display: All (show all qualities), Average (show average quality) and Max (show best quality). Preferred one can be selected using a slider in `Display settings`
- Replaced item progress percentage with the vertical progress bar from legacy Ender's client
- Added cursor item hide feature from bdew's client. Use `Alt`+`H` to hide/show item on cursor
- Added auto study from bdew's client

### r39 (2015-10-01)

- Added optional autohearth. Can be enabled through the options in `Misc settings`

### r38 (2015-09-29)

- Do not display defense bars requested to be removed

### r37 (2015-09-29)

- Added more visible defense bars. Can be disabled through the option in `Misc settings`
- Added build menu similar to the existing craft menu accessible on `Alt`+`B`
- Objects can be inspected with `Alt`+`Shift`+`Click`

### r36 (2015-09-28)

- Added option to display simplified crops (from Amber client). See `Video settings`
- Changed speed hotkey to `Shift`+`R`
- Cupboard size is now optional and can be changed through the `Display settings`

### r35 (2015-09-25)

- Added display of stone column radius
- Flattened cupboards 

### r34 (2015-09-24)

- Merged vanilla client changes

### r33 (2015-09-22)

- Merged vanilla client changes
- Changed toggle grid hotkey to `Alt`+`G`
- Changed toggle nightvision hotkey to `Alt`+`N`

### r32 (2015-09-19)

- Fixed moon cycle duration

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