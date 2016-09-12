package main;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Dylan on 12/09/16.
 */
class PiController {
    private static final String PI_IP = "10.140.108.72";
    private static PiController instance;
    private Process sshProcess;
    private boolean isRunning = false;

    private PiController() { }

    static PiController getInstance() {
        if (instance == null) instance = new PiController();
        return instance;
    }

    void sendDataToPi(Runnable doneCallback) throws IOException,
            AlreadyRunningException {

        if (isRunning) throw new AlreadyRunningException();
        assert PI_IP.length() > 8; // make sure IP exists

        if (sshProcess == null) {
            sshProcess = Runtime.getRuntime().exec("TODO");
            // TODO NEXT run SSH command
        }

        Scanner terminalScanner = new Scanner(sshProcess.getInputStream());

        (new Thread(() -> {
            isRunning = true;

            while (terminalScanner.hasNext()) {
                System.out.println("\tTERMINAL OUT: " +
                        terminalScanner.nextLine());
            }

            // TODO AFTER watch output and respond to it TO SSH IN

            // Done
            doneCallback.run();
            isRunning = false;
        })).start();
    }

    private class AlreadyRunningException extends Exception {
        AlreadyRunningException() {
            super("Process already running");
        }
    }

    private class InvalidOutputException extends Exception {
        InvalidOutputException(String reason) {
            super(reason);
        }
    }
}
