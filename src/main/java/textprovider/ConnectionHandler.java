package textprovider;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ArrayList;

public class ConnectionHandler implements Runnable {

    private LinkedList<Socket> connectionQueue;
    private ArrayList<LinkedList<Job>> workerQueues;

    private  int inFileLineCount;
    private String inputFilePath;

    final static String PROTO_GET = "GET";
    private final String PROTO_QUIT = "QUIT";
    private final String PROTO_SHUTDOWN = "SHUTDOWN";

    ConnectionHandler(int inFileLineCount, String inputFilePath, LinkedList<Socket> connectionQueue) {
        this.inFileLineCount = inFileLineCount;
        this.connectionQueue = connectionQueue;
        this.inputFilePath = inputFilePath;
    }

    public void run() {
        initialize();
        processCommands();
    }

    private void initialize() {
        // instantiate and run worker threads
        final int WORKER_THREAD_COUNT = 4;
        workerQueues = new ArrayList<>(WORKER_THREAD_COUNT);
        for (int i = 0; i < WORKER_THREAD_COUNT; i++) {
            LinkedList<Job> workerQueue = new LinkedList<>();
            workerQueues.add(workerQueue);
            JobWorker jw = new JobWorker(inFileLineCount, inputFilePath, workerQueue);
            jw.run();
        }
    }

    private void processCommands() {
        int nextWorkerQueue = 0;

        String command = "";

        while (!command.equals(PROTO_SHUTDOWN)) {
            Socket clientConnection = connectionQueue.removeFirst();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
                if (reader.ready()) {
                    command = reader.readLine();
                    if (command.startsWith(PROTO_GET + " ")) {
                        // make request for worker, queuing job with writer/command
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));
                        Job job = new Job(writer, command);
                        workerQueues.get(nextWorkerQueue++ % 4).add(job);
                    } else if (command.equals(PROTO_QUIT)) {
                        clientConnection.close();
                    } else if (command.equals(PROTO_SHUTDOWN)) {
                        clientConnection.close();
                    }
                } else {
                    // connection received, but client hasn't sent request yet
                    // may want to hand this off to a thread, so connection handler task can continue accepting/delegating requests
                    System.out.println("Connection accepted, but no data ready to be read - what to do ?????????");
                }
            } catch (IOException ioe) {
                System.out.println("Exception caught while accepting client connection, err: " + ioe.getMessage());
            }
        }
    }

}
