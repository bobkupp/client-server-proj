package textprovider;

import java.io.BufferedWriter;

public class Job {
    public Job(BufferedWriter writer, String command) {
        this.writer = writer;
        this.command = command;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public String getCommand() {
        return command;
    }

    BufferedWriter writer;
    String command;
}
