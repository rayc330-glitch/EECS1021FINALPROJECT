package com.eecs.f1;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

//bridges pico firmware and java software
public class PicoClient {

    private final SerialPort port;      // usb serial port
    private Thread listenerThread;      // background thread for listening to messages
    private volatile boolean running = false;  //set to false to stop listening to messages

//creates a pico client with the port and baudrate
    public PicoClient(String portName) {
        this.port = SerialPort.getCommPort(portName);
        this.port.setBaudRate(115200);  // Must match the speed set in the Pico firmware
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
    }

//lists serial ports, not used at all (left in for compatability reasons)
    public static void listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.println("Available serial ports:");
        for (SerialPort p : ports) {
            System.out.printf("  %-20s  %s%n",
                    p.getSystemPortName(),
                    p.getDescriptivePortName());
        }
    }

//opens a serial port and begins listening to pico
    public void open(Consumer<String> onMessageReceived) {
        if (!port.openPort()) {
            throw new RuntimeException("Could not open port: " + port.getSystemPortName()
                    + ". Is the Pico plugged in?");
        }

        running = true;

        // start a background thread that just sits and waits for messages from the pico
        // every time a full line arrives, it calls onMessageReceived with that line
        listenerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(port.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while (running && (line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        onMessageReceived.accept(trimmed);
                    }
                }

            } catch (Exception e) {
                if (running) {
                    System.err.println("Lost connection to Pico: " + e.getMessage());
                }
            }
        }, "pico-listener");

        listenerThread.setDaemon(true);  // shuts down thread when done
        listenerThread.start();
    }

//sends plain text file to pico
    public void send(String command) {
        try {
            OutputStream out = port.getOutputStream();
            out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            System.err.println("Failed to send command: " + e.getMessage());
        }
    }

    //closes port, stops listening thread
    public void close() {
        running = false;
        port.closePort();
    }
}
