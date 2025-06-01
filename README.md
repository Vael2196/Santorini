# FIT3077 Repository

Greetings. This repo consists of all the files made by members of Tuesday04pm_Team024 (Sprint 1 & 2), 
including yours truly, Vadim Filyakin (not my proudest work, but I made all the features for the latest Sprint myself).

Here is the list of features added throughout Sprint 3:
-Introducing a brand new god power into the game: Triton.
-The game now supports save snapshots and load features. 
(As a sidenote, due to some architectural inconsistencies in the way we designed the Controller classes, as of now, 
Save feature only works in Standard mode. 
Also, once the game has been booted up from a save file, the user need to double click on the board to continue since we are using enums for the game phase, 
and there is no way around the select phase in the begining that doesn't involve me refractoring the whole thing).
-An 80% functional Game mode/Level/Map Editor. The users can access it by clickig the "Level Editor" button on the starting screen. 
Once they are done, they are to close and then reopen the application again to be able to enjoy the newly made game mode.
While in the Editor, the users can do the following:
    -Click on the tiles they want to turn into holes. When clicked again, the tile reverts back to normal.
    -Change the editor mode through the toggle to the right of the board to place buildings/workers.
    -Allow building/moving (doesn't work with Tag).
    -Allow god powers
    -Select number of players (one or two)
    -Select number of workers per player (up to 5)
    -Select god power for each player
    -Select core "engine" (defines win conditions; e.g. The way to win in Tag is to get to a tile occupied by another player)
    -Upload background music (.wav only)
    -Upload background image (.jpg only)
    -Enter a name for the new game mode
    -A button to save the game mode.
- As a bonus, a new game mod has been implemented. It basically mimics the rules of both Tag and original Santorini (move and build). Nothing too complicated.


The directory structure goes as following:
-assets
    -...(Here go all of the custom game mode assets)...
-src
    -assets
        -default background + images
    -Frame  <--(All the front end and interuction related classes, e.g. Views and Controllers)
        -Boards
            -
        -Memento    <---(save state logic)
            -
        -... Controllers and Views...
    -GameEntities   <--(All the data holders/ Model level classes)
        -Gods
            -
        -...Building, Player, and Worker
    -Main <--(Stale code; left for future use)
    -StartingScreen <--(Current executable)



Related Links:
User Stories Google Doc
https://docs.google.com/document/d/1bQfKgKLm3USmfcOGMAPXRr5s06VoufCDc2nB0q9RyIY/edit?usp=sharing

Domain Model Lucid Chart & Doc
https://lucid.app/lucidchart/5b441dfc-f61e-4710-abb1-0f3fbde804ac/edit?viewport_loc=-502%2C-243%2C2678%2C992%2C0_0&invitationId=inv_2012fd35-11a6-4ab6-b331-d269927384f4

https://docs.google.com/document/d/1UMumT0b7SmSgCepfJrm69mjeN_tCn_rsv6Ir40HeOFs/edit?usp=sharing

Team Information Google Doc
https://docs.google.com/document/d/1txBi3O2lVhHtbD2jJ2R4EvZN8AYMXSP34ozEXstA-K8/edit?usp=sharing

Lowfi Prototypes Doc
https://docs.google.com/document/d/1uxXY5fTlMEPGOci3yAWQkX9q15i3iqnDOtOMgpSq2mw/edit?usp=sharing
