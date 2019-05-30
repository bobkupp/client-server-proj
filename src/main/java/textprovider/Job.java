package textprovider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

class Job {
    Job(Socket clientConnection) {
        this.clientConnection = clientConnection;
        this.writer = writer;
        this.reader = reader;
    }

    public void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    BufferedWriter getWriter() {
        return writer;
    }

    BufferedReader getReader() {
        return reader;
    }

    String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Socket getClientConnection() {
        return clientConnection;
    }

    public void setClientConnection(Socket clientConnection) {
        this.clientConnection = clientConnection;
    }

    private Socket clientConnection;

    private BufferedWriter writer;
    private BufferedReader reader;

    private String command;
}
