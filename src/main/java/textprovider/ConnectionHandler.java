package textprovider;

import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.NoSuchElementException;

public class ConnectionHandler implements Runnable {

    private LinkedList<Socket> connectionQueue;
    private LinkedList<Future> workerFutures;

    private  int inFileLineCount;
    private String inputFilePath;
    private String threadName;
    private LinkedList<Job> jobQueue;

    static final int CONNECTION_WAIT_MILLISECONDS = 10;
    private final int JOB_WORKER_COUNT = 4;
    private ExecutorService executorService;

    ConnectionHandler(String threadName, int inFileLineCount, String inputFilePath, LinkedList<Socket> connectionQueue) {
        this.threadName = threadName;
        this.inFileLineCount = inFileLineCount;
        this.connectionQueue = connectionQueue;
        this.inputFilePath = inputFilePath;
    }

    public void run() {
        initialize();
        processConnections();
    }

    private void initialize() {
        jobQueue = new LinkedList<>();
        workerFutures = new LinkedList<>();
        executorService = Executors.newFixedThreadPool(JOB_WORKER_COUNT);
        for (int i = 0; i < JOB_WORKER_COUNT; i++) {
            System.out.println("... ConnectionHandler::instantiating job worker, thread: " + i);
            JobWorker jw = new JobWorker(inFileLineCount, inputFilePath, jobQueue);
            workerFutures.add(executorService.submit(jw));
        }
    }

    private void processConnections() {
        boolean inShutdown = false;
        while (!inShutdown) {
            if (connectionQueue.peekFirst() != null) {
                try {
                    Socket clientConnection = connectionQueue.removeFirst();
                    System.out.println("... ConnectionHandler: GOT CONNECTION");
                        Job job = new Job(clientConnection);
                        jobQueue.add(job);
                } catch (NoSuchElementException nsee) {
                    System.out.println("Exception received trying to get element from connection queue, err: " + nsee.getMessage());
                }
            } else {
                try {
                    for (Future future : workerFutures) {
                        if (future.isDone()) {
                            inShutdown = true;
                        }
                    }
                    Thread.sleep(CONNECTION_WAIT_MILLISECONDS);
                } catch (InterruptedException ie) {
                    System.out.println("Received exception while waiting for new connections, err: " + ie.getMessage());
                }
            }
        }
    }

    protected void finalize() {
        System.out.println("ConnectionHandler has terminated");
    }
}
