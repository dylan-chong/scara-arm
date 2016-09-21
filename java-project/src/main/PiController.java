package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created by Dylan on 12/09/16.
 */
class PiController {
    private static final String SRC_FILE = "pwm.csv";
    private static final String PI_IP = "10.140.37.57";
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

    /**
     * Saves the pwmString to a file and calls scp to transfer the data
     * to the raspberry pi.
     *
     * @param pwmString    The CSV text to send to the pi
     * @param doneCallback Function to call when no there's more terminal output
     * @throws IOException             File or SCP command problem
     * @throws AlreadyRunningException Only one process allowed
     */
    void sendDataToPi(String pwmString, Runnable doneCallback)
            throws IOException, AlreadyRunningException {

        if (isRunning) throw new AlreadyRunningException();
        assert PI_IP.length() > 7 : "IP is probably nonexistent";
        assert pwmString.length() > 0;
        isRunning = true;

        savePWMFile(pwmString);

        // Call scp

        String cmd = String.format("scp -i %s %s pi@%s:%s",
                PRIVATE_KEY_FILE, SRC_FILE, PI_IP, DEST_FILE);
        Process sshProcess = Runtime.getRuntime().exec(cmd);

        // Watch output

        Scanner terminalScanner = new Scanner(sshProcess.getInputStream());
        new Thread(() -> {
            System.out.println("WATCHING TERMINAL OUTPUT");

            int lines = 0;
            while (terminalScanner.hasNext()) {
                System.out.println("\tTERMINAL OUT: " +
                        terminalScanner.nextLine());
                lines++;
            }

            System.out.println("NO MORE TERMINAL OUTPUT (total of " + lines +
                    " lines)");

            isRunning = false;
            doneCallback.run();
        }).start();
    }

    private void savePWMFile(String pwmString) throws FileNotFoundException {
        File file = new File(SRC_FILE);
        assert !file.isDirectory();

        PrintWriter writer = new PrintWriter(file);
        writer.print(pwmString);
        writer.flush();
        writer.close();
    }

    private class AlreadyRunningException extends Exception {
        AlreadyRunningException() {
            super();
        }
    }
}
