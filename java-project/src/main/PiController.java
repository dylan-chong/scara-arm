package main;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Created by Dylan on 12/09/16.
 */
class PiController {
    private static final String PI_IP = "10.140.108.72";

    private static PiController instance;
    private Process sshProcess;
    private boolean isRunning = false;

    private PiController() {
    }

    static PiController getInstance() {
        if (instance == null) instance = new PiController();
        return instance;
    }

    void sendDataToPi(Runnable doneCallback) throws IOException,
            AlreadyRunningException {

        if (isRunning) throw new AlreadyRunningException();
        assert PI_IP.length() > 8; // make sure IP exists


        // TODO NEXT test processbuilder - see if can run more commadns
        {
            String cmd = "ls"; // TODO run SSH command
            // sshProcess = Runtime.getRuntime().exec(cmd);
            ProcessBuilder processBuilder = new ProcessBuilder("ls");
            sshProcess = processBuilder.start();
        }

        Scanner terminalScanner = new Scanner(sshProcess.getInputStream());
        PrintStream terminalStream = new PrintStream(sshProcess.getOutputStream());

        (new Thread(() -> {
            isRunning = true;

            sshToPi(terminalScanner, terminalStream);

            terminalScanner.close();
            terminalStream.close();
            isRunning = false;
            doneCallback.run();
        })).start();
    }

    /**
     * Should only be called by sendDataToPi()
     *
     * @param terminalScanner For reading terminal
     * @param terminalStream  For writing commands to terminal
     */
    private void sshToPi(Scanner terminalScanner, PrintStream terminalStream) {
        // while (terminalScanner.hasNext()) {
        for (int a = 0; a < 2; a++) {
            logTerminalOutput(terminalScanner.nextLine());
        }

        runTerminalCommand("ls -a", terminalStream);
        terminalStream.flush();
        terminalStream.close();

        while (true) {
            if (terminalScanner.hasNext())
                logTerminalOutput(terminalScanner.nextLine());
        }
        // TODO watch output and respond to it TO SSH IN
    }

    /**
     * @param line A line that was read from the terminal
     */
    private void logTerminalOutput(String line) {
        System.out.println("\tTERMINAL OUT: " + line);
    }

    /**
     * Does not flush!
     *
     * @param command A command to write to the terminal
     */
    private void runTerminalCommand(String command, PrintStream ps) {
        System.out.println("\tTERMINAL INP: " + command);
        ps.println(command);
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
