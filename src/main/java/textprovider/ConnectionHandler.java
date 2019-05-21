package textprovider;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ArrayList;

public class ConnectionHandler {
    ConnectionHandler tp;
    public static String PROTO_GET = "GET";
    public static String PROTO_QUIT = "QUIT";
    public static String PROTO_SHUTDOWN = "SHUTDOWN";
    private final int WORKER_THREAD_COUNT = 4;
    private ServerSocket serverEndpoint;
    public ArrayList<LinkedList<Job>> workerQueues = new ArrayList(WORKER_THREAD_COUNT);
    private int nextWorkerQueue = 0;
    private static String inFilePath;

    public void ConnectionHandler(String inFilePath) {
        if (args.length != 2) {
            System.out.println("Invalid number of arguments, expected: " + args[0] + " {input_file_path}\n");
            System.exit(-1);
        }
        ConnectionHandler.inFilePath = args[1];
        ConnectionHandler tp = new ConnectionHandler();
        if (tp.initialize()) {
            tp.processCommands();
        }
    }

    private Boolean initialize() {
        Boolean startupComplete = false;

        for (int i = 0; i < WORKER_THREAD_COUNT; i++) {
            LinkedList<Job> workerQueue = new LinkedList<Job>();
            workerQueues.add(workerQueue);
            JobWorker jw = new JobWorker(inFilePath, workerQueue);
            jw.run();
        }
        try {
            serverEndpoint = new ServerSocket(LISTENER_PORT);
            startupComplete = true;
        } catch (IOException ioe) {
            System.out.println("Exception caught trying to open server socket, err: " + ioe.getMessage());
        }
        return startupComplete;
    }

    private void processCommands() {
        String command = new String("");


        while (!command.equals(PROTO_SHUTDOWN)) {
            try {
                Socket clientConnection = serverEndpoint.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
                if (reader.ready()) {
                    command = reader.readLine();
                    if (command.startsWith(PROTO_GET + " ")) {
                        // make request for worker, queuing job with writer/command
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));
                        Job job = new Job(writer, command);
                        workerQueues.get(nextWorkerQueue % 4).add(job);
                    } else if (command.equals(PROTO_QUIT)) {
                        clientConnection.close();
                    } else if (command.equals(PROTO_SHUTDOWN)) {
                        clientConnection.close();
                        serverEndpoint.close();
                    }
                } else {
                    // connection received, but client hasn't sent request yet
                    // may want to hand this off to a thread, so main server task can continue accepting/delegating requests
                    System.out.println("Connection accepted, but no data ready to be read - what to do ?????????");
                }
            } catch (IOException ioe) {
                System.out.println("Exception caught while accepting client connection, err: " + ioe.getMessage());
            }
        }
    }

}
