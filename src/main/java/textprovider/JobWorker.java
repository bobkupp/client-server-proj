package textprovider;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class JobWorker implements Runnable {

    private Boolean useInMemoryLookup;
    private ArrayList<String> fileContents;
    private int inFileLineCount;
    private String inputFilePath;
    private final String okayResponse = "OK/r/n";
    private final String errorResponse = "ERR/r/n";

    private LinkedList<Job> jobQueue;

    JobWorker(int inFileLineCount, String inputFilePath, LinkedList<Job>jobQueue) {
        this.jobQueue = jobQueue;
        this.inFileLineCount = inFileLineCount;
        this.inputFilePath = inputFilePath;
    }

    public void run() {
        System.out.println("JobWorker thread started ...");
        while(true) {
            String line;
            try {
                Job job = jobQueue.removeFirst();
                String[] lineNumberStringArray = job.getCommand().split(ConnectionHandler.PROTO_GET + " ");
                int requestedLineNumber = Integer.parseInt(lineNumberStringArray[0]);
                if (lineNumberStringArray.length != 1) {
                    System.out.println("Invalid content in GET request: " + job.getCommand());
                    continue;
                }
                try {
                    // send response
                    String responseText;
                    if (requestedLineNumber <= inFileLineCount) {
                        try {
                            Runtime run = Runtime.getRuntime();

                            // example command: cat sample_data_1.txt | sed -n '25p'
                            String sedCommand = "cat " + inputFilePath + " | sed -n \'" + lineNumberStringArray[0] + "p\'";
                            Process pr = run.exec(sedCommand);
                            pr.waitFor();
                            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                            line = buf.readLine();
                            job.getWriter().write(okayResponse + line);
                        } catch (InterruptedException ie) {
                            System.out.println("Exception received trying to get line from input file, err: " + ie.getMessage());
                            job.getWriter().write(okayResponse);
                        }
                    } else {
                        job.getWriter().write(errorResponse);
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid content (not integer) in GET request: " + job.getCommand());
                } catch (IOException ioe) {
                    System.out.println("Exception caught while trying to get line number from *large* file, err: " + ioe.getMessage());
//                } catch (InterruptedException ie) {
//                    System.out.println("Interrupted while trying to get line number from *large* file, err: " + ie.getMessage());
                }
            } catch (NoSuchElementException nsee) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    System.out.println("Received exception while waiting for new requests, err: " + ie.getMessage() + ", shutting down ...");
                    System.exit(-1);
                }
            }
        }
    }
}
