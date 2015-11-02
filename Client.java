/**
 * Created by John on 9/19/2015.
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    private ObjectInputStream sInput;        // to read from the socket
    private ObjectOutputStream sOutput;        // to write on the socket
    private Socket socket;

    // the server, the port and the username
    private String server, username;
    private int port;

    // Constructor
    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    // function to start client
    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }

        // if fail then report error
        catch (Exception ec) {
            display("Error connecting to server:" + ec);
            return false;
        }

        // if  connected, show address and port (debugging)
        String connected = "Connected on " + socket.getInetAddress() + ":" + socket.getPort();
        display(connected);

        // create data streams I/O
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // create thread to listen from server
        new ListenFromServer().start();

        // Send username to the server as string
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success
        return true;
    }

    // show message on screen
    private void display(String msg) {
        System.out.println(msg);      // show msg in console
    }

    // method to send message to server
    void sendMessage(ChatMsg msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    // disconnection method that closes streams and socket
    private void disconnect() {     // necessary try-catches
        try {
            if (sInput != null) sInput.close();
        }
        catch (Exception e) {
        }
        try {
            if (sOutput != null) sOutput.close();
        }
        catch (Exception e) {
        }
        try {
            if (socket != null) socket.close();
        }
        catch (Exception e) {
        }
    }

    public static void main(String[] args) {
        //start using java Client host port
        int portNumber = Integer.parseInt(args[1]);
        String serverAddress = args[0];
        String userName = "Anon";

        Scanner scan = new Scanner(System.in);
        String msg = scan.nextLine();
        String delims = "[ ]+";
        String[] check = msg.split(delims);

        // make sure user registers a username first
        while (userName.equals("Anon")) {
            if (check[0].equals("REG") && (check.length == 2))
                userName = check[1];
            else {
                System.out.println("Error code 4: Sending user not registered");
                System.out.println("Please register a username using [REG username].");
                msg = scan.nextLine();
                check = msg.split(delims);
            }
        }

        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);

        if(!client.start())     // test if can start client
            return;

        //ChatMsg initial = new ChatMsg(1, msg.substring(4));
        //client.sendMessage(initial);

        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            msg = scan.nextLine();
            // break into command and message parts
            String[] command = msg.split(delims);
            if(!command[0].equals("EXIT") && command.length < 2) {
                System.out.println("Error code 2: Unknown message format.");
                System.out.println("Use commands: REG, MESG, PMSG followed by input");
                break;
            }
            // exit if message is EXIT
            else if (command[0].equals("EXIT")) {
                // break to do the disconnect
                ChatMsg chat = new ChatMsg(0, msg);
                client.sendMessage(chat);
                break;
            }
            // register username
            else if (command[0].equals("REG")) {
                ChatMsg chat = new ChatMsg(1, msg.substring(4));
                client.sendMessage(chat);
            }
            // send message
            else if (command[0].equals("MESG")) {  //default MESG
                ChatMsg chat = new ChatMsg(2, msg.substring(5));
                client.sendMessage(chat);
            }
            // send private message
            else if (command[0].equals("PMSG")) {
                if(command.length != 3) {
                    System.out.println("Usage: PMSG user message");
                }
                else {
                    ChatMsg chat = new ChatMsg(3, msg.substring(5));
                    client.sendMessage(chat);
                }
            }
            // otherwise error
            else
                System.out.println("Error code 2: Unknown message format.");
        }
        // done, disconnect
        client.disconnect();
    }

    // waits for the message from the server
    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                } catch (IOException e) {
                    display("Server has close the connection: " + e);
                    break;
                }
                // shouldn't trigger since using Strings
                catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}

