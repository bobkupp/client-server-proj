package textprovider;

import java.io.BufferedWriter;

class Job {
    Job(BufferedWriter writer, String command) {
        this.writer = writer;
        this.command = command;
    }

    BufferedWriter getWriter() {
        return writer;
    }

    String getCommand() {
        return command;
    }

    private BufferedWriter writer;
    private String command;
}
