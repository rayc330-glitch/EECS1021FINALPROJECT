package com.eecs.f1;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        String portName = args[0];

        PicoClient     pico       = new PicoClient(portName);
        MessageParser  parser     = new MessageParser();
        GameController controller = new GameController(pico);

        pico.open(line -> controller.receiveEvent(parser.parse(line)));

        // wait for pico to start up and respond
        System.out.println("=== F1 Reaction Game ===");
        System.out.print("Connected to " + portName + ". Waiting for Pico to start up...");

        if (controller.waitForReady(5000)) {
            System.out.println(" Ready!");
        } else {
            System.out.println(" (No ready signal received — continuing anyway)");
        }

        // main game loop
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nPress 'r' to start a race, or 'q' to quit: ");

                if (!scanner.hasNextLine()) break;
                String input = scanner.nextLine().trim().toLowerCase();

                if (input.equals("q")) {
                    // quit game
                    break;

                } else if (input.equals("r")) {
                    // run a race
                    GameController.RaceResult result = controller.runRace();
                    printResult(result);

                } else if (!input.isEmpty()) {
                    System.out.println("Unknown command. Please press 'r' or 'q'.");
                }
            }
        }

        // clean up
        pico.send("RESET");         // turn everything off
        Thread.sleep(200);
        pico.close();
        System.out.println("Goodbye!");
    }

//print race results
    private static void printResult(GameController.RaceResult result) {
        System.out.println("---------------------------");
        if (result.isFalseStart) {
            System.out.println("Player " + result.falseStartBy + " had a FALSE START!");
            System.out.println("Player " + result.winner + " wins by default.");
        } else {
            System.out.println("Player " + result.winner + " wins!");
            System.out.println("Reaction time: " + result.reactionMs + " ms");

            // give special result output depending how well you did
            if (result.reactionMs < 200) {
                System.out.println("Incredible reflexes!");
            } else if (result.reactionMs < 300) {
                System.out.println("Great reaction!");
            } else if (result.reactionMs < 500) {
                System.out.println("Good reaction.");
            } else {
                System.out.println("Keep practising!");
            }
        }
        System.out.println("---------------------------");
    }
}
