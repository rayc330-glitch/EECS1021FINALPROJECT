package com.eecs.f1

/**
 *represents every scenario that can happen in the game
 *
 *every message received from the pico gets turned into GameEvent.type by parser
 *
 *we use objects instead of strings so everything is easier to understand
 * 
 *five events for five messages pico can send
 *
 *Pico message          ->  GameEvent type
 *
 *"READY"               ->  GameEvent.Ready
 *"PONG"                ->  GameEvent.Pong
 *"BUTTON_PRESSED 1"    ->  GameEvent.ButtonPressed  (player = 1)
 *"REACTION 2 318"      ->  GameEvent.Reaction       (player = 2, ms = 318)
 *anything else         ->  GameEvent.Unknown
 */
public abstract class GameEvent {

    //pico turned on and is ready to receive commands
    //pico message: "READY"
    public static class Ready extends GameEvent {
        @Override
        public String toString() { return "Pico is ready!"; }
    }

    //pico replied to our PING command
    //pico message: "PONG"
    public static class Pong extends GameEvent {
        @Override
        public String toString() { return "Pico replied: pong!"; }
    }

    //player pressed their button OUTSIDE the reaction timer window = false start
    //pico message: "BUTTON_PRESSED <player>" 1 or 2
    public static class ButtonPressed extends GameEvent {
        public final int player;  //1 or 2

        public ButtonPressed(int player) {
            this.player = player;
        }

        @Override
        public String toString() { return "Player " + player + " pressed their button (outside timer)"; }
    }

    //player pressed their button INSIDE the reaction timer window = not false start
    //pico message: "REACTION <player> <ms>" 1 or 2 + reaction time
    public static class Reaction extends GameEvent {
        public final int  player;       //which player won
        public final long milliseconds; //with what reaction time

        public Reaction(int player, long milliseconds) {
            this.player       = player;
            this.milliseconds = milliseconds;
        }

        @Override
        public String toString() {
            return "Player " + player + " reacted in " + milliseconds + " ms!";
        }
    }

    //recieved unknown message
    public static class Unknown extends GameEvent {
        public final String rawMessage;  // the original text we couldn't understand

        public Unknown(String rawMessage) {
            this.rawMessage = rawMessage;
        }

        @Override
        public String toString() { return "Unknown message from Pico: \"" + rawMessage + "\""; }
    }
}
