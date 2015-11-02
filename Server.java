/**
 * Created by John on 9/19/2015.
 */

import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned off to stop the server
    private boolean isOn;
    private boolean uniqueUser = true;

    // server constructor
    public Server(int port) {
        // the port
        this.port = port;
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
    }

    // start the server
    public void start() {
        isOn = true;

        // try creating server using port- default 1500
        try {
            // socket used by server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (isOn) {
                // message saying server is up and waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();    // accept connection
                // if start failed
                if (!isOn)
                    break;


                ClientThread t = new ClientThread(socket);  // make a client thread

                while (uniqueUser) {
                    int repeatUsers = 0;    // how many users have that user name (including this user)
                    for (int i = 0; i < al.size(); ++i) {   // search array list
                        ClientThread ct = al.get(i);
                        if (t.username.equals(ct.username))
                            repeatUsers++;
                    }
                    // if no one has that user, ok
                    if (repeatUsers < 1) {
                        al.add(t);  // save it in the ArrayList
                        t.start();  // start the thread
                        broadcast(t.username + " just connected.");
                        t.sOutput.writeObject("List of the users connected");
                        for (int i = 0; i < al.size(); ++i) {   //show online users
                            ClientThread ct = al.get(i);
                            t.sOutput.writeObject((i + 1) + ": " + ct.username);
                        }
                        break;
                    }
                    // if someone already has that username
                    else {
                        t.sOutput.writeObject("Error code 0: " + t.username + " is already taken, please choose another one.");
                        t.sOutput.writeObject("List of the users connected");
                        for (int i = 0; i < al.size(); ++i) {   // show which user names have been taken
                            ClientThread ct = al.get(i);
                            t.sOutput.writeObject((i + 1) + ") " + ct.username);
                        }
                        uniqueUser = false;   // make them try again
                        t.close();
                    }
                }
            }

            // server start failed or turned off, close streams and socket
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // couldn't create server
        catch (IOException e) {
            String msg = "Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }


    // display event on console
    private void display(String msg) {
        System.out.println(msg);
    }

    // broadcast msg to all clients
    private synchronized void broadcast(String msg) {
        System.out.println(msg);

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for (int i = al.size(); --i >= 0; ) {
            ClientThread ct = al.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(msg)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // remove client from array list on EXIT
    synchronized void remove(int id) {
        // scan the array list for the client id
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            if (ct.id == id) {
                al.remove(i);   //and remove it
                return;
            }
        }
    }

    public static void main(String[] args) {
        // start server on port 8080
        int portNumber = 8080;

        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    // Client thread class- one instance for each client
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        // i/o streams
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // Unique id (easier for disconnecting)
        int id;
        // the Username of the Client
        String username;
        // chat message
        ChatMsg cm;

        // Constructor
        ClientThread(Socket socket) {
            // unique id for array list
            id = ++uniqueId;
            // assign socket
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                // create streams
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException, but using strings so should work always
            catch (ClassNotFoundException e) {
            }
        }

        // run until exit
        public void run() {
            String delims = "[ ]+";
            // loop until EXIT
            boolean isOn = true;
            while (isOn) {
                // read a String (which is an object)
                try {
                    cm = (ChatMsg) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }

                // Switch on the type of message
                switch (cm.getType()) {

                    case 0: // EXIT
                        broadcast(username + " exited the room");
                        remove(id);
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            broadcast((i + 1) + ") " + ct.username);
                        }
                        isOn = false;
                        break;

                    case 1: // register new user
                        String tempUser = cm.getMessage();
                        int repeatUsers = 0;    // how many users have that user name (including this user)
                        for (int i = 0; i < al.size(); i++) {   // search array list
                            ClientThread ct = al.get(i);
                            if (tempUser.equals(ct.username))
                                repeatUsers++;
                        }
                        // if no one has that user, ok
                        if (repeatUsers < 1) {
                            username = cm.getMessage();
                            broadcast(username + " just connected.");
                            writeMsg("List of the users connected");
                            for (int i = 0; i < al.size(); ++i) {   //show online users
                                ClientThread ct = al.get(i);
                                writeMsg((i + 1) + ": " + ct.username);
                            }
                        }
                        // if someone already has that username
                        else {
                            writeMsg("Error code 0: " + tempUser + " is already taken");    // tell them its taken
                            writeMsg("List of the users connected");                        // and show them all the taken usernames
                            for (int i = 0; i < al.size(); ++i) {   // show which user names have been taken
                                ClientThread ct = al.get(i);
                                writeMsg((i + 1) + ") " + ct.username);
                            }
                        }
                        break;

                    case 2: // broadcast message
                        broadcast(username + ": " + cm.getMessage());
                        break;

                    case 3: // send private message
                        String[] pm = cm.getMessage().split(delims);
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            if (pm[0].equals(ct.username)) {
                                writePM(pm[1], ct);
                                //System.out.println(ct.username);
                            }
                        }

                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients

            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
            }
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }

        // Write string to client console
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }

        // Write private message to client
        private boolean writePM(String msg, ClientThread pct) {
            // check if receiving client is still connected
            if (!pct.socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                pct.sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}

