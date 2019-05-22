package textprovider;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class TextProvider {
    private ServerSocket serverEndpoint;
    private LinkedList<Socket> connectionQueue = new LinkedList<>();
    private static int inFileLineCount;
    private static String inputFilePath;
    private ConnectionHandler connectionHandler;

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid number of arguments, expected: {input_file_path}\n");
            System.exit(-1);
        }
        TextProvider tp = new TextProvider();

        inputFilePath = args[0];
        inFileLineCount = tp.getInputFileCount(inputFilePath);
        if (inFileLineCount < 0) {
            System.out.println("Input file is not a valid text file: " + args[0]);
            System.exit(-1);
        }
        if (tp.startConnectionHandler()) {
            tp.processConnections();
        }
    }

    private int getInputFileCount(String inFilePath) {
        int lineCount = -1;

        if (!(new File(inFilePath)).isFile()) {
            System.out.println("Input file is not a valid text file: " + inFilePath);
            return(-1);
        }
        Runtime run = Runtime.getRuntime();

        // example command: cat sample_data_1.txt | wc -l
        String sedCommand = "cat " + inFilePath + " | wc -l";
        try {
            Process pr = run.exec(sedCommand);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = buf.readLine();
            lineCount = Integer.parseInt(line);
        } catch (IOException ioe) {
            System.out.println("Exception received trying to get input file line count, err: " + ioe.getMessage());
        } catch (InterruptedException ie) {
            System.out.println("Interrupted while trying to get line count from input file, err: " + ie.getMessage());
        }
        return lineCount;
    }

    private Boolean startConnectionHandler() {
        final int LISTENER_PORT = 10322;
        boolean startupComplete = false;

        try {
            serverEndpoint = new ServerSocket(LISTENER_PORT);
            startupComplete = true;
        } catch (IOException ioe) {
            System.out.println("Exception caught trying to open server socket, err: " + ioe.getMessage());
        }
        connectionHandler = new ConnectionHandler(inFileLineCount, inputFilePath, connectionQueue);
        connectionHandler.run();

        return startupComplete;
    }

    private void processConnections() {

        Thread connectionThread = new Thread(connectionHandler);
        while (connectionThread.isAlive()) {
            try {
                connectionQueue.add(serverEndpoint.accept());
            } catch (IOException ioe) {
                System.out.println("Exception caught while accepting client connection, err: " + ioe.getMessage());
            }
        }
        try {
            serverEndpoint.close();
        } catch (IOException ioe) {
            System.out.println("Exception caught while closing ServerSocket, err: " + ioe.getMessage());
        }
    }

}
