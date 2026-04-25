package com.eecs.f1

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *HOW IT WORKS
 *decides:
 *  when to light up each start light
 *  when to turn the lights off and shout "GO!"
 *  whether a button press is a false/jump start
 *  who won and how fast they reacted
 *
 *Comm with pico uses plain text
 *   "LIGHT_ON 2"        turn on second led
 *   "BUZZ 800 100"      beep buzzer 800hz for 100ms
 *   "START_TIMER"       start counting
 *
 * events recieved are GameEvent objects in q
 * 
 * serial reading happens in PicoClient
 * class runs on main thread
 * threads share LinkedBlockingQueue
 * background thread puts events in and this class takes them out
 */





public class GameController {

    private final PicoClient pico; //pico object that will be reffered to

    private final LinkedBlockingQueue<GameEvent> eventQueue = new LinkedBlockingQueue<>();
    //thread safe event q from pico
    //reads events one at a time

    private final Random random = new Random(); // random picks a wait time between the min and max 

    // time constants (can't be changed)
    private static final long MS_BETWEEN_LIGHTS   = 1000;  // 1sec between each light turning on
    private static final long MIN_HOLD_MS         = 500;   // shortest random wait time
    private static final long MAX_HOLD_MS         = 3000;  // longest random wait time
    private static final long WINNER_DISPLAY_MS   = 1500;  // winning side green light turns on for 1.5sec

    public GameController(PicoClient pico) {
        this.pico = pico;
    }

     //called by background serial thread everytime pico sends message
     //events are put into q and main will pick up
    public void receiveEvent(GameEvent event) {
        eventQueue.offer(event);
    }

     //wait for pico to be ready in determined time
     //false if time runs out
     //true if pico sends ready
    public boolean waitForReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            long timeLeft = deadline - System.currentTimeMillis();
            if (timeLeft <= 0) return false;

            GameEvent event = eventQueue.poll(timeLeft, TimeUnit.MILLISECONDS);
            if (event instanceof GameEvent.Ready) return true;
        }

        return false;
    }

    /**
     * Runs one full race from start to finish.
     *
     *Steps
     *1 reset everything
     *2 light up the 5 red led one by one while checking false start
     *3 wait random time amount while checking false start
     *4 turn lights off and start timer and output go
     *5 wait for button press
     *6 show winning result and tone
     *
     * @return  A RaceResult saying who won and how fast (or if there was a false start)
     */
    public RaceResult runRace() throws InterruptedException {

        //Step 1: clears past events
        eventQueue.clear();
        pico.send("RESET");
        Thread.sleep(400);          //gives time to reset
        eventQueue.clear();         //clears q

        System.out.println("Get ready...");

        //Step 2: turn on lights one by one
        for (int lightIndex = 0; lightIndex < 5; lightIndex++) {

            //turn light on and buzz
            pico.send("LIGHT_ON " + lightIndex);
            pico.send("BUZZ 800 80");

            // Shows light process on console
            printLightProgress(lightIndex + 1);

            //checks false start during led light up phase
            RaceResult falseStart = watchForFalseStart(MS_BETWEEN_LIGHTS);
            if (falseStart != null) return falseStart;
        }

        //Step 3: random delay ranging from 500ms to 3000ms
        long randomWait = MIN_HOLD_MS + random.nextInt((int)(MAX_HOLD_MS - MIN_HOLD_MS + 1));
        //checks false start during random hold phase
        RaceResult falseStart = watchForFalseStart(randomWait);
        if (falseStart != null) return falseStart;

        //Step 4
        pico.send("ALL_LIGHTS_OFF"); //turn off all lights
        pico.send("START_TIMER"); //start counting reaction time
        pico.send("BUZZ 1500 150"); //buzzer beeps go
        System.out.println("GO!");

        //Step 5: wait for response
        GameEvent event;
        while (true) {
            event = eventQueue.take();     // pauses until a signal is recieved

            if (event instanceof GameEvent.Reaction) {
                break;                     // if the event is a reaction then it exits
            }
        }

        //Step 6: winner winner chicken dinner
        //changes the generic event to a reaction event to access .player and .milliseconds
        GameEvent.Reaction reaction = (GameEvent.Reaction) event;
        System.out.println("Player " + reaction.player + " reacted in " + reaction.milliseconds + " ms!");

        //turn winner led on
        pico.send("WINNER_LED_ON " + reaction.player);

        //pitch based on reaction time
        if (reaction.milliseconds < 250) {
            pico.send("BUZZ 2000 400");   //fast = high pitch
        } else if (reaction.milliseconds < 400) {
            pico.send("BUZZ 1500 400");   //medium = medium pitch
        } else {
            pico.send("BUZZ 600 400");    //slow = low pitch
        }

        Thread.sleep(WINNER_DISPLAY_MS);  //turns off winning led after 1500ms
        pico.send("RESET");

        return RaceResult.normalWin(reaction.player, reaction.milliseconds);
    }

     //waits watches event queue for durationMs milliseconds
     //if button is pressed then false start
     //penalize player aand make them lose
    private RaceResult watchForFalseStart(long durationMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + durationMs;

        while (true) {
            long timeLeft = deadline - System.currentTimeMillis();
            if (timeLeft <= 0) return null;  //return nothing if times up with no event

            GameEvent event = eventQueue.poll(timeLeft, TimeUnit.MILLISECONDS);

            if (event instanceof GameEvent.ButtonPressed) {
                //if button pressed to early
                int offendingPlayer = ((GameEvent.ButtonPressed) event).player;
                int winningPlayer;
                if (offendingPlayer == 1){
                    winningPlayer = 2;
                }
                else{
                    winningPlayer = 1;
                }
                return handleFalseStart(offendingPlayer, winningPlayer);
            }
        }
    }

    //outputs for false starts
    //warning message
    //flashes lights
    //says that other player wins
    private RaceResult handleFalseStart(int offendingPlayer, int winningPlayer)
            throws InterruptedException {

        System.out.println("!!! FALSE START by Player " + offendingPlayer + " !!!");
        System.out.println("Player " + winningPlayer + " wins by default.");

        pico.send("RESET");
        
        // Flash all 5 lights 3 times as a penalty signal
        for (int flash = 0; flash < 3; flash++) {
            for (int i = 0; i < 5; i++) pico.send("LIGHT_ON " + i);
            pico.send("BUZZ 300 150");
            Thread.sleep(180);
            //imbedded loop turns on all lights and beeps buzzer

            //lights off
            pico.send("ALL_LIGHTS_OFF");
            Thread.sleep(180);
        }
        //outer loop does this 3 times

        //lights turn on and then resets afterwards
        pico.send("WINNER_LED_ON " + winningPlayer);
        Thread.sleep(WINNER_DISPLAY_MS);
        pico.send("RESET");

        return RaceResult.falseStart(offendingPlayer, winningPlayer);
    }

     //progress bar printer that makes [**---] for light 2 and such
    private void printLightProgress(int numberOfLightsOn) {
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 5; i++) {
            bar.append(i < numberOfLightsOn ? '*' : '-');
        }
        bar.append("] Light ").append(numberOfLightsOn);
        System.out.println(bar);
    }

    // saves the race results
    public static class RaceResult {

        public final int     winner;         //player num 1 or 2
        public final long    reactionMs;     //reaction time in ms (-1 if false start)
        public final int     falseStartBy;   //player who did it (0 if no false start)
        public final boolean isFalseStart;   //true if the race ended due to a false start

        private RaceResult(int winner, long reactionMs, int falseStartBy, boolean isFalseStart) {
            this.winner       = winner;
            this.reactionMs   = reactionMs;
            this.falseStartBy = falseStartBy;
            this.isFalseStart = isFalseStart;
        }

        //makes results for a normal race
        public static RaceResult normalWin(int player, long ms) {
            return new RaceResult(player, ms, 0, false);
        }

        //makes results for false race
        public static RaceResult falseStart(int offender, int winner) {
            return new RaceResult(winner, -1, offender, true);
        }
    }
}
