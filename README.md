# EECS1021FINALPROJECT

## This Guide Will tell you how to set up the 2-player F1 Reaction game both on the hardware and software side.
By:
Johnson Su
Raymond Chu 
Stanley Su
Abidullah Khan

### Hardware Setup
#### Components Needed:
- Raspberry Pi Pico 2
- 5 Red LEDs
- 2 Green LEDs
- 7 330 Ohm Resistors
- 2 Buttons
- 1 Passive piezo buzzer
- Breadboard + jumper wires
- Microusb cable

#### Pin assignments:
These will be our start lights
Red LEDs: GP2,3,4,5,6 -> 5 330 Ohm Resistors -> GND

These will indicate who won. Player one will use GP7,15 and player 2 will use GP8,14
Green LEDS: GP7,8 -> 2 330 Ohm Resistors -> GND

Buttons: GP15,14 -> GND

Buzzer: GP16 -> GND
Ground rail: breadboard GND rail -> GP38
It is reccomended to plug everything that goes to ground into the rail for organization

### Firmware Setup
1. Open "firmware/F1_Reaction_Game/F1_Reaction_Game.ino" in arduino IDE
2. Connect to the pico via usb
3. Go to the dropdown next to debugging and select raspbery pi pico 2 and the COM port its connect to (on windows)
4. Upload

### Java Setup
We will use maven to bridge between java and the pico
From the command line:
mvn exec:java -Dexec.args="COM7"
Replace "COM7" with your port

In the IDE:
1. File -> Open java-app/
2. Click Load Maven Project, and wait for it to complete
3. Open Main.java, click the green triangle next to main(). The first run will fail without a port argument, thats normal
4. Top-right dropdown -> Edit configurations
5. in the proper arguments field, enter the port name (ex. "COM7")
6. clicl modify options -> check "add vm options" then in the field enter:  --enable-native-access=ALL-UNNAMED
7. all good!

### How to play
1. Start the Java code, and while its running, head to the output and press r+enter to start a race
2. 5 lights start to turn on, and after all 5 are on, they will turn off after a random period of time
3. First player to click their button first wins, indicated by the green light
4. The output will display the winner's reaction time
5. Once you're done, press q + enter to quit

Notes: Don't press the button before the light turns on, thats a jumpstart, and the other player will win by default
This game works perfectly fine as a one player game, just ignore the other button
