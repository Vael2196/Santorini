# FIT3077 Repository, Team 024, Vadim Filyakin

## Santorini prototype

Hello there!

In order to speedrun through the project I decided to go straight ahead with developing Santorini, and ask queastions to the TAs as I go. 
The current snippet has all what I essentially envision to be the game board. There is also a class for the Builder, but I primarily made it to test the occupant field in the tile, and at this point it has no other use.
There is also a build method within tile, and I made it just so I could test whether the number for the level on a tile would visually update.

Other than that, the code is pretty crappy and unpolished, and I won't say I am proud of it. 
I will spend more time on it through the future iterations, but in case some of my understending of the game concepst seems wrong to you, **some healthy criticism will be greatly appreciated**.

### Execution Instructions

1. first: make a Swing call, instantiate a new instance of GameFrame from within the call
2. second: the game window should pop up shortly. As of now, there will just be a rectangular looking board with clickable tiles and numbers. The numbers will eventually represent the level of buildings on the tile.
3. third: You may click around the board as you deem necessary, nothing will happen though. Enjoy

**Alternatively:** Uncomment the code in Main.java. That is the document I used to actually test the stuff (which is no different from the instructions above btw)