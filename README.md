# Santorini Repository

Greetings. This repo consists of all the files made by yours truly, Vadim Filyakin (not my proudest work, ngl).

Here is the list of features added throughout Sprint 3:
- Introducing a brand new god power into the game: Triton.
- The game now supports save snapshots and load features. 
(As a sidenote, due to some architectural inconsistencies in the way we designed the Controller classes, as of now, 
Save feature only works in Standard mode. 
Also, once the game has been booted up from a save file, the user need to double click on the board to continue since we are using enums for the game phase, 
and there is no way around the select phase in the begining that doesn't involve me refractoring the whole thing).
- An 80% functional Game mode/Level/Map Editor. The users can access it by clickig the "Level Editor" button on the starting screen. 
Once they are done, they are to close and then reopen the application again to be able to enjoy the newly made game mode.
While in the Editor, the users can do the following:
    - Click on the tiles they want to turn into holes. When clicked again, the tile reverts back to normal.
    - Change the editor mode through the toggle to the right of the board to place buildings/workers.
    - Allow building/moving (doesn't work with Tag).
    - Allow god powers
    - Select number of players (one or two)
    - Select number of workers per player (up to 5)
    - Select god power for each player
    - Select core "engine" (defines win conditions; e.g. The way to win in Tag is to get to a tile occupied by another player)
    - Upload background music (.wav only)
    - Upload background image (.jpg only)
    - Enter a name for the new game mode
    - A button to save the game mode.
- As a bonus, a new game mod has been implemented. It basically mimics the rules of both Tag and original Santorini (move and build). Nothing too complicated.


The directory structure goes as following:
- assets
    - ...(Here go all of the custom game mode assets)...
- src
    - assets
        - default background + images
    - Frame  <--(All the front end and interuction related classes, e.g. Views and Controllers)
        - Boards
            - ...
        - Memento    <---(save state logic)
            - ...
        - ... Controllers and Views...
    - GameEntities   <--(All the data holders/ Model level classes)
        - Gods
            - ...
        - ...Building, Player, and Worker
    - Main <--(Stale code; left for future use)
    - StartingScreen <--(Current executable)
