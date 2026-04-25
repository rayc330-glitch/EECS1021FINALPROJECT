/*
 * F1 Reaction Time Game - Pico Firmware
 * Board: Raspberry Pi Pico 2 W
 */

//pin numbers
const int START_LIGHT_PINS[5] = {2, 3, 4, 5, 6};  //the 5 red F1 start lights
const int BUZZER_PIN           = 16;                //the buzzer
const int PLAYER1_BUTTON_PIN   = 15;                //player 1 reaction button
const int PLAYER2_BUTTON_PIN   = 14;                //player 2 reaction button
const int PLAYER1_WINNER_LED   = 7;                 //player 1 winner light
const int PLAYER2_WINNER_LED   = 8;                 //player 2 winner light

//reaction timer state
bool     timerIsRunning   = false;  //is the reaction window open?
unsigned long timerStartMs = 0;     //when did the timer start (in milliseconds)?

//button state (used to detect a single press, not a hold)
bool          player1WasPressed = false;
bool          player2WasPressed = false;
unsigned long player1LastChangeMs = 0;
unsigned long player2LastChangeMs = 0;

//how many milliseconds to ignore after a button changes (prevents false triggers)
const unsigned long DEBOUNCE_MS = 20;

//serial message buffer
String incomingMessage = "";


//stepup that runs only once when the pico boots up
void setup() {
  Serial.begin(115200);

  //set up all the start light pins as outputs and make sure they start OFF
  for (int i = 0; i < 5; i++) {
    pinMode(START_LIGHT_PINS[i], OUTPUT);
    digitalWrite(START_LIGHT_PINS[i], LOW);
  }

  //set up the buzzer and winner LEDs
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(PLAYER1_WINNER_LED, OUTPUT);
  pinMode(PLAYER2_WINNER_LED, OUTPUT);
  digitalWrite(PLAYER1_WINNER_LED, LOW);
  digitalWrite(PLAYER2_WINNER_LED, LOW);

  //set up the buttons as inputs.
  //INPUT_PULLUP means the pin reads HIGH when NOT pressed, LOW when pressed.
  pinMode(PLAYER1_BUTTON_PIN, INPUT_PULLUP);
  pinMode(PLAYER2_BUTTON_PIN, INPUT_PULLUP);

  //give the USB serial connection a moment to settle
  delay(200);

  //tell Java that the Pico is ready to go
  Serial.println("READY");
}


//loop
void loop() {
  //check if Java sent us a new command over USB
  while (Serial.available()) {
    char c = Serial.read();

    if (c == '\n') {
      //handling the command if we have a full line (full line mean it ends with a newline)
      incomingMessage.trim();  //remove any extra spaces or \r
      handleCommand(incomingMessage);
      incomingMessage = "";    //clear the buffer for the next message
    } else {
      incomingMessage += c;    //keep building up the message character by character
    }
  }

  //check both buttons every loop 
  checkButton(PLAYER1_BUTTON_PIN, 1, player1WasPressed, player1LastChangeMs);
  checkButton(PLAYER2_BUTTON_PIN, 2, player2WasPressed, player2LastChangeMs);
}



//command handling
void handleCommand(String message) {

  //ping to check if arduino is still alive
  if (message == "PING") {
    Serial.println("PONG");
  }

  //reset
  else if (message == "RESET") {
    doReset();
  }

  //ALL_LIGHTS_OFF: turn all 5 start lights off at once
  else if (message == "ALL_LIGHTS_OFF") {
    for (int i = 0; i < 5; i++) {
      digitalWrite(START_LIGHT_PINS[i], LOW);
    }
  }

  //START_TIMER: lights just went out, start counting!
  else if (message == "START_TIMER") {
    timerStartMs  = millis();  //record the exact moment the timer started
    timerIsRunning = true;
  }

  //STOP_TIMER: cancel the timer (e.g. after a false start)
  else if (message == "STOP_TIMER") {
    timerIsRunning = false;
  }

  //LIGHT_ON <index>: turn on one start light (0 to 4)
  else if (message.startsWith("LIGHT_ON ")) {
    int index = message.substring(9).toInt();  //grab the number after "LIGHT_ON "
    if (index >= 0 && index < 5) {
      digitalWrite(START_LIGHT_PINS[index], HIGH);
    }
  }

  //LIGHT_OFF <index>: turn off one start light (0 to 4)
  else if (message.startsWith("LIGHT_OFF ")) {
    int index = message.substring(10).toInt();
    if (index >= 0 && index < 5) {
      digitalWrite(START_LIGHT_PINS[index], LOW);
    }
  }

  //WINNER_LED_ON <player>: light up a winner LED (1 or 2)
  else if (message.startsWith("WINNER_LED_ON ")) {
    int player = message.substring(14).toInt();
    if (player == 1) digitalWrite(PLAYER1_WINNER_LED, HIGH);
    if (player == 2) digitalWrite(PLAYER2_WINNER_LED, HIGH);
  }

  //WINNER_LED_OFF <player>: turn off a winner LED (1 or 2)
  else if (message.startsWith("WINNER_LED_OFF ")) {
    int player = message.substring(15).toInt();
    if (player == 1) digitalWrite(PLAYER1_WINNER_LED, LOW);
    if (player == 2) digitalWrite(PLAYER2_WINNER_LED, LOW);
  }

  //BUZZ <frequency> <duration>: play a beep
  //example: "BUZZ 800 100" plays 800 Hz for 100 ms
  else if (message.startsWith("BUZZ ")) {
    //find the space between frequency and duration
    int spacePos = message.indexOf(' ', 5);  // search after "BUZZ "
    if (spacePos > 0) {
      int frequency = message.substring(5, spacePos).toInt();
      int duration  = message.substring(spacePos + 1).toInt();
      if (frequency > 0 && duration > 0) {
        tone(BUZZER_PIN, frequency, duration);  // plays the beep in the background
      }
    }
  }
}


//check if button is pressed and report it
void checkButton(int pin, int playerNumber, bool& wasPressed, unsigned long& lastChangeMs) {
  bool isPressed = (digitalRead(pin) == LOW);  // LOW means the button IS pressed
  unsigned long now = millis();

  //ignore the button if it changed state very recently (debounce)
  if (now - lastChangeMs < DEBOUNCE_MS) return;

  if (isPressed && !wasPressed) {
    //the button was just pressed
    lastChangeMs = now;
    wasPressed   = true;

    if (timerIsRunning) {
      //the timer is running — this is a real reaction!
      unsigned long reactionTime = now - timerStartMs;
      timerIsRunning = false;

      //tell Java: player X reacted in Y milliseconds
      Serial.print("REACTION ");
      Serial.print(playerNumber);
      Serial.print(" ");
      Serial.println(reactionTime);

    } else {
      //the timer is NOT running
      //example message: "BUTTON_PRESSED 2"
      Serial.print("BUTTON_PRESSED ");
      Serial.println(playerNumber);
    }

  } else if (!isPressed && wasPressed) {
    //updating the debounce timer
    lastChangeMs = now;
    wasPressed   = false;
  }
}


//Reset: turn everything off and clear all state
void doReset() {
  //turn off all 5 start lights
  for (int i = 0; i < 5; i++) {
    digitalWrite(START_LIGHT_PINS[i], LOW);
  }

  //turns off both winner LEDs
  digitalWrite(PLAYER1_WINNER_LED, LOW);
  digitalWrite(PLAYER2_WINNER_LED, LOW);

  //stop any buzzer tone
  noTone(BUZZER_PIN);

  //close the reaction timer
  timerIsRunning = false;

  //sync button state so a held-down button at reset doesn't fire immediately
  player1WasPressed = (digitalRead(PLAYER1_BUTTON_PIN) == LOW);
  player2WasPressed = (digitalRead(PLAYER2_BUTTON_PIN) == LOW);
}
