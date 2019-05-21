package textprovider;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class JobWorker implements Runnable {

    private final int LARGE_FILE_MIN_SIZE = 256 * 1024;
    Boolean useInMemoryLookup = false;
    ArrayList<String> fileContents;
    private String inputFilePath;

    LinkedList<Job> jobQueue;

    public JobWorker(String inputFilePath, LinkedList<Job>jobQueue) {
        this.jobQueue = jobQueue;
        this.inputFilePath = inputFilePath;
        useInMemoryLookup = prepareInputFile();
    }

    private Boolean prepareInputFile() {
        if (new File(inputFilePath).length() >LARGE_FILE_MIN_SIZE) {
            return false;
        }
        fileContents = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(inputFilePath));

            String line;
            fileContents.add("dummy line - because array is 0-based and requests are 1-based");
            while ((line = in.readLine()) != null) {
                fileContents.add(line);
            }
        } catch (IOException ioe) {
            System.out.println("Exception received while reading input file: " + inputFilePath + ", error: " + ioe.getMessage());
            System.exit(-1);
        }
        return true;
    }

    public void run() {
        System.out.println("JobWorker thread started ...");
        while(true) {
            String line;
            try {
                Job job = jobQueue.removeFirst();
                String[] lineNumberStringArray = job.command.split(TextProvider.PROTO_GET + " ");
                if (lineNumberStringArray.length != 1) {
                    System.out.println("Invalid content in GET request: " + job.command);
                    continue;
                }
                try {
                    int lineNum = Integer.parseInt(lineNumberStringArray[0]);
                    if (useInMemoryLookup) {
                        line = fileContents.get(lineNum);
                    } else {
                        Runtime run = Runtime.getRuntime();

                        // example command: cat sample_data_1.txt | sed -n '25p'
                        String sedCommand = "cat " + inputFilePath + " | sed -n \'" + lineNumberStringArray[0] + "p\'";
                        Process pr = run.exec(sedCommand);
                        pr.waitFor();
                        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                        line = buf.readLine();
                    }
                    job.writer.write(line);
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid content (not integer) in GET request: " + job.command);
                } catch (IOException ioe) {
                    System.out.println("Exception caught while trying to get line number from *large* file, err: " + ioe.getMessage());
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted while trying to get line number from *large* file, err: " + ie.getMessage());
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
