package textprovider;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class TextProvider {
    private ServerSocket serverEndpoint;
    private LinkedList<Socket> connectionQueue = new LinkedList<Socket>();
    private static String inFilePath;
    private ConnectionHandler connectionHandler;

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid number of arguments, expected: {input_file_path}\n");
            System.exit(-1);
        }
        inFilePath = args[0];
        if (!(new File(inFilePath)).isFile()) {
            System.out.println("Input file is not a valid text file: " + inFilePath);
            System.exit(-1);
        }
        TextProvider tp = new TextProvider();
        if (tp.startConnectionHandler()) {
            tp.processConnections();
        }
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
        connectionHandler = new ConnectionHandler(inFilePath, connectionQueue);
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
