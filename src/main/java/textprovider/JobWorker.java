package textprovider;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class JobWorker implements Callable {

    private int inFileLineCount;
    private String inputFilePath;
    private final String okayResponse = "OK\r\n";
    private final String errorResponse = "ERR\r\n";
    private final static String PROTO_GET = "GET";
    private final static String PROTO_QUIT = "QUIT";
    private final static String PROTO_SHUTDOWN = "SHUTDOWN";

    private Runtime run;

    private static LinkedList<Job> jobQueue;

    JobWorker(int inFileLineCount, String inputFilePath, LinkedList<Job> jobQueue) {
        this.inFileLineCount = inFileLineCount;
        this.inputFilePath = inputFilePath;
        this.jobQueue = jobQueue;
        run = Runtime.getRuntime();
    }

    public String call() {
        String returnValue = "done";
        boolean inShutdown = false;
        try {
            while (!inShutdown) {
                String line;
                Job job = getNextJob();
                if (job != null) {
                    try {
                        Socket client = job.getClientConnection();
                        job.setReader(new BufferedReader(new InputStreamReader(client.getInputStream())));
                        job.setWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));
                        boolean jobComplete = false;

                        while (!jobComplete) {
                            String command = PROTO_QUIT;  // default, unless we get command from stream
                            int lineNumber = 0;
                            line = job.getReader().readLine();
                            if (line != null) {
                                System.out.println("... JobWorker: read command: " + line);
                                String[] lineNumberStringArray = line.split(" ");
                                command = lineNumberStringArray[0];
                                if (lineNumberStringArray.length > 1) {
                                    lineNumber = Integer.parseInt(lineNumberStringArray[1]);
                                }
                            }

                            switch (command) {
                                case PROTO_QUIT:
                                    System.out.println("JobWorker: got QUIT");
                                    client.close();
                                    jobComplete = true;
                                    break;
                                case PROTO_SHUTDOWN:
                                    System.out.println("JobWorker: got SHUTDOWN");
                                    jobComplete = true;
                                    inShutdown = true;
                                    returnValue = "shutdown";
                                    break;
                                case PROTO_GET:
                                    try {
                                        // send response
                                        if (lineNumber <= inFileLineCount) {
                                            try {

                                                // get requested line from file: example command: cat sample_data_1.txt | sed -n '25p'
                                                String[] sedCommand = {"/bin/sh", "-c", "cat " + inputFilePath + " | sed -n \'" + lineNumber + "p\'"};
                                                Process pr = run.exec(sedCommand);
                                                pr.waitFor();
                                                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                                                line = buf.readLine();

                                                System.out.println("JobWriter: sending response: " + okayResponse + line);
                                                job.getWriter().write(okayResponse);
                                                job.getWriter().write(line + "\n");
                                                job.getWriter().flush();
                                            } catch (InterruptedException ie) {
                                                System.out.println("Exception received trying to get line from input file, err: " + ie.getMessage() + " " + this.toString());
                                                job.getWriter().write(okayResponse);
                                                job.getWriter().flush();
                                            }
                                        } else {
                                            System.out.println("JobWriter: sending response: " + errorResponse);
                                            job.getWriter().write(errorResponse);
                                            job.getWriter().flush();
                                        }
                                    } catch (IOException ioe) {
                                        System.out.println("Exception caught while trying to get line number, err: " + ioe.getMessage());
                                    }
                                    break;
                                default:
                                    System.out.println("Received invalid command: " + command + " " + this.toString());
                            }
                        }
                    } catch (NoSuchElementException nsee) {
                        try {
                            Thread.sleep(ConnectionHandler.CONNECTION_WAIT_MILLISECONDS);
                        } catch (InterruptedException ie) {
                            System.out.println("Received exception while waiting for new requests, err: " + ie.getMessage() + ", shutting down ...");
                            System.exit(-1);
                        }
                    } catch (IOException ioe) {
                        System.out.println("Exception caught while accepting client connection, err: " + ioe.getMessage());
                    }
                } else {
                    try {
                        Thread.sleep(ConnectionHandler.CONNECTION_WAIT_MILLISECONDS);
                    } catch (InterruptedException ie) {
                        System.out.println("Received exception while waiting for new requests, err: " + ie.getMessage() + ", shutting down ...");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION in JOBWORKER, err: " + e.getMessage());
            Throwable t = e.getCause();
            System.err.println("Uncaught exception is detected! " + t
                    + " st: " + Arrays.toString(t.getStackTrace()));
        }
        System.out.println("JobWorker: exiting with returnValue: " + returnValue);
        return returnValue;
    }

    private static synchronized Job getNextJob() {
        Job job = null;
        if (jobQueue.peekFirst() != null) {
            System.out.println("JobWorker found job, getting it ...");
            job = jobQueue.removeFirst();
        }
        return job;
    }

    protected void finalize() {
        System.out.println("JobWorker has TERMINATED, " + this.toString());
    }

}
