package main;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Dylan on 12/09/16.
 */
class PiController {
    static final String SRC_FILE = "pwm.csv";
    private static final String PI_IP = "10.140.109.205";
    private static final String PRIVATE_KEY_FILE = "keys/rpi";
    private static final String DEST_FILE = "~/Arm/pwm.csv";

    private static PiController instance;
    private boolean isRunning = false;

    private PiController() {
    }

    static PiController getInstance() {
        if (instance == null) instance = new PiController();
        return instance;
    }

    // TODO save the data to a file
    void sendDataToPi(Runnable doneCallback) throws IOException,
            AlreadyRunningException {

        if (isRunning) throw new AlreadyRunningException();
        assert PI_IP.length() > 8; // make sure IP exists
        isRunning = true;

        String cmd = String.format("scp -i %s %s pi@%s:%s",
                PRIVATE_KEY_FILE, SRC_FILE, PI_IP, DEST_FILE);
        Process sshProcess = Runtime.getRuntime().exec(cmd);

        Scanner terminalScanner = new Scanner(sshProcess.getInputStream());

        (new Thread(() -> {
            while (terminalScanner.hasNext())
                System.out.println("\tTERMINAL OUT: " +
                        terminalScanner.nextLine());

            isRunning = false;
            doneCallback.run();
        })).start();
    }

    private class AlreadyRunningException extends Exception {
        AlreadyRunningException() {
            super("Already trying to send data.\nPlease wait for finish");
        }
    }
}
