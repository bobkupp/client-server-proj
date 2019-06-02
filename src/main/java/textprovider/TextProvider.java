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
    private Thread connectionThread;

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
        System.exit(0);
    }

    private Boolean startConnectionHandler() {
        final int LISTENER_PORT = 10322;
        boolean startupComplete = false;

        try {
            serverEndpoint = new ServerSocket(LISTENER_PORT);
            serverEndpoint.setSoTimeout(1000);
            startupComplete = true;
        } catch (IOException ioe) {
            System.out.println("Exception caught trying to open server socket, err: " + ioe.getMessage());
        }
        System.out.println("... starting connection handler");
        ConnectionHandler connectionHandler = new ConnectionHandler("Connection Handler", inFileLineCount, inputFilePath, connectionQueue);
        connectionThread = new Thread(connectionHandler);
        connectionThread.start();


        return startupComplete;
    }

    private void processConnections() {

        System.out.println("processConnections: connectionThread state: " + connectionThread.getState());
        while (connectionThread.isAlive()) {
            try {
//                System.out.println("TextProvider: doing accept, conn thread state: " + connectionThread.getState());
                connectionQueue.add(serverEndpoint.accept());
            } catch (IOException ioe) {
//                System.out.println("Exception caught while accepting client connection, err: " + ioe.getMessage());
            }
        }
        try {
            serverEndpoint.close();
        } catch (IOException ioe) {
            System.out.println("Exception caught while closing ServerSocket, err: " + ioe.getMessage());
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
        String[] shellCommand = {"/bin/sh", "-c", "cat " + inFilePath + " | wc -l"};
        try {
            Process pr = run.exec(shellCommand);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = buf.readLine();
            lineCount = Integer.parseInt(line.trim());
        } catch (IOException ioe) {
            System.out.println("Exception received trying to get input file line count, err: " + ioe.getMessage());
        } catch (InterruptedException ie) {
            System.out.println("Interrupted while trying to get line count from input file, err: " + ie.getMessage());
        }
        return lineCount;
    }
}
