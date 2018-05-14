package activitystreamer.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;

public class Connection extends Thread {

    private static final Logger log = LogManager.getLogger();
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader inreader;
    private PrintWriter outwriter;
    private boolean open = false;
    private Socket socket;
    private boolean term = false;
    private final String remoteId;

    Connection(Socket socket) throws IOException {
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        inreader = new BufferedReader(new InputStreamReader(in));
        outwriter = new PrintWriter(out, true);
        this.socket = socket;
        open = true;
        remoteId = socket.getInetAddress() + ":" + socket.getPort();
        start();
    }

    public String getRemoteId() {
        return remoteId;
    }
    
    public static String generateRemoteId(String inetAddress, String port) {
            return inetAddress + ":" + port;
    }
    
    // if the remote identification is the same, then we think the two conncetion
    // is the same
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof Connection) {
            Connection remoteCon = (Connection) obj;
            return this.getRemoteId().equals(remoteCon.getRemoteId());
        }
        return false;
    }

    /*
	 * returns true if the message was written, otherwise false
     */
    public boolean writeMsg(String msg) {
        if (open) {
            outwriter.println(msg);
            outwriter.flush();
            System.out.println("\n--Sending "+msg+" to: "+socket);
            return true;
        }
        return false;
    }

    public void closeCon() {
        if (open) {
            log.info("closing connection " + Settings.socketAddress(socket));
            try {
                term = true;
                inreader.close();
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                // already closed?
                log.error("received exception closing the connection " + Settings.socketAddress(socket) + ": " + e);
            }
        }
    }

    public void run() {
        String data = "";
        try {
            //If Control.process() return true, then while loop finish
            while (!term && (data = inreader.readLine()) != null) {
                    term = Control.getInstance().process(this, data);               
            }

            Control.getInstance().connectionClosed(this);
            closeCon();
            in.close();

        } catch (IOException e) {
            log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);
            Control.getInstance().connectionClosed(this);
        }
        open = false;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isOpen() {
        return open;
    }
}

