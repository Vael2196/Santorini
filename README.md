# FIT3077 Repository, Team 024, Vadim Filyakin

## Santorini prototype

Hello there!

In order to speedrun through the project I decided to go straight ahead with developing Santorini, and ask queastions to the TAs as I go. 
The current snippet has all what I essentially envision to be the game board. There is also a class for the Builder, but I primarily made it to test the occupant field in the tile, and at this point it has no other use.
There is also a build method within tile, and I made it just so I could test whether the number for the level on a tile would visually update.

Other than that, the code is pretty crappy and unpolished, and I won't say I am proud of it. 
I will spend more time on it through the future iterations, but in case some of my understending of the game concepst seems wrong to you, **some healthy criticism will be greatly appreciated**.

### Execution Instructions

1. make a Swing call, instantiate a new instance of GameFrame from within the call
2. Hover over **File** (top left corner) --> **Project Structure** --> **Artifacts** --> **Add/+** (top left, closer to the middle of the screen) --> **JAR** --> **from modules...** --> Select either the class you have made in a previous step, or, alternatively, select Main in case you followed the alternative step. -Don't forget to tick **extract to the target JAR**-> **OK** x2 --> **Build** (top bar) --> **Build Artifacts** --> Select the one you just made --> **Build** as in a pop up sidebar --> **out**--> **artifacts** --> right-click the .jar file --> **Open in** --> **Explorer** --> double click
3. the game window should pop up shortly. As of now, there will just be a rectangular looking board with clickable tiles and numbers. The numbers will eventually represent the level of buildings on the tile.
4. You may click around the board as you deem necessary, nothing will happen though. Enjoy

**Alternatively (step 1):** Uncomment the code in Main.java. That is the document I used to actually test the stuff (which is no different from the instructions above btw). It'll do the same thing as step 1.