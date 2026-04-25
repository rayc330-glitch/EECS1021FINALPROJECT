package com.eecs.f1;
 
public class MessageParser {

    public GameEvent parse(String line) {

        // split input into array 
        // ex. "REACTION 1 245" becomes ["REACTION", "1", "245"]
        String[] words = line.trim().split("\\s+");

        if (words.length == 0 || words[0].isEmpty()) {
            return new GameEvent.Unknown(line);
        }

        String messageType = words[0];  // The first word tells us what kind of message this is

        switch (messageType) {

            // pico is ready
            case "READY":
                return new GameEvent.Ready();

            // reply to "ping"
            case "PONG":
                return new GameEvent.Pong();

            // gets button pressed and which player pressed it
            case "BUTTON_PRESSED":
                if (words.length >= 2) {
                    int player = Integer.parseInt(words[1]);
                    return new GameEvent.ButtonPressed(player);
                }
                break;

            // reaction + player who was faster + overall time
            case "REACTION":
                if (words.length >= 3) {
                    int  player      = Integer.parseInt(words[1]);
                    long reactionMs  = Long.parseLong(words[2]);
                    return new GameEvent.Reaction(player, reactionMs);
                }
                break;
        }

        // fallback, bad input
        return new GameEvent.Unknown(line);
    }
}
