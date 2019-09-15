package MultiChat;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.util.Scanner;

public class Server {

    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;

    public static ArrayList<clientThread> clients = new ArrayList<clientThread>();

    public static void main(String args[]) {

        Scanner s = new Scanner(System.in);
        //HOST
        System.out.println("Please Enter IP:");
        String ip;
        ip = s.nextLine();

        //PORT
        System.out.println("Please Enter server port:");
        int portNumber;
        portNumber = s.nextInt();


        try{
            InetAddress addr = InetAddress.getByName(ip);
            serverSocket = new ServerSocket(portNumber,0,addr);
            System.out.println("Server is running");
        } catch(Exception e){
            System.out.println(e);
        }

        int clientNum = 1;
        while (true) {
            try {

                clientSocket = serverSocket.accept();
                clientThread curr_client =  new clientThread(clientSocket, clients);
                clients.add(curr_client);
                curr_client.start();
                System.out.println("Client "  + clientNum + " is connected!");
                clientNum++;

            } catch (IOException e) {

                System.out.println("Client could not be connected");
            }
        }

    }
}

// clientHandler
class clientThread extends Thread {

    private String clientName = null;
    private ObjectInputStream is = null;
    private ObjectOutputStream os = null;
    private Socket clientSocket = null;
    private final ArrayList<clientThread> clients;
    public clientThread(Socket clientSocket, ArrayList<clientThread> clients) {

        this.clientSocket = clientSocket;
        this.clients = clients;

    }

    public void run() {

        ArrayList<clientThread> clients = this.clients;

        try {

            //Create input and output streams for this client.

            is = new ObjectInputStream(clientSocket.getInputStream());
            os = new ObjectOutputStream(clientSocket.getOutputStream());

            String name;
            while (true) {

                synchronized(this)
                {
                    this.os.writeObject("Please enter your name :");
                    this.os.flush();
                    name = ((String) this.is.readObject()).trim();

                    //the string does not occur
                    if ((name.indexOf('@') == -1) || (name.indexOf('!') == -1)) {
                        break;
                    } else {
                        this.os.writeObject("Username should not contain '@' or '!' characters.");
                        this.os.flush();
                    }
                }
            }

            System.out.println("Client Name is " + name);

            this.os.writeObject("*** Welcome " + name + " to our chat room ***\nEnter /quit to leave the chat room");
            this.os.flush();

            this.os.writeObject("Directory Created");
            this.os.flush();
            synchronized(this)
            {

                for (clientThread curr_client : clients)
                {
                    if (curr_client != null && curr_client == this) {
                        clientName = "@" + name;
                        break;
                    }
                }

                for (clientThread curr_client : clients) {
                    if (curr_client != null && curr_client != this) {
                        curr_client.os.writeObject(name + " has joined");
                        curr_client.os.flush();
                    }
                }
            }

            // Start the conversation

            while (true) {

                //this.os.writeObject("Please Enter command:");
                //this.os.flush();

                String line = (String) is.readObject();

                if (line.startsWith("/quit")) {
                    break;
                }

                if(line.equals("Who is online?")) {
                    for(clientThread curr_client : clients){
                        this.os.writeObject(curr_client.clientName);
                        this.os.flush();
                    }
                }

                else if (line.startsWith("@")) {
                    if(line.contains("::")){
                        unicast(line,name);
                    }
                    else{
                        this.os.writeObject("Please enter valid format :: ");
                        this.os.flush();
                    }
                }

                // If the message is blocked from a given client.

                else if(line.startsWith("!")) {
                    if(line.contains("::")){
                        blockcast(line,name);
                    }
                    else{
                        this.os.writeObject("Please enter valid format :: ");
                        this.os.flush();
                    }

                }

                else {
                    broadcast(line,name);
                }
            }

            // Terminate the Session for a particluar user

            this.os.writeObject("*** Bye " + name + " ***");
            this.os.flush();
            System.out.println(name + " disconnected.");
            clients.remove(this);

            synchronized(this) {

                if (!clients.isEmpty()) {
                    for (clientThread curr_client : clients) {

                        if (curr_client != null && curr_client != this && curr_client.clientName != null) {
                            curr_client.os.writeObject("*** The user " + name + " disconnected ***");
                            curr_client.os.flush();
                        }
                    }
                }
            }

            this.is.close();
            this.os.close();
            clientSocket.close();

        } catch (IOException e) {
            System.out.println("User Session terminated");

        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found");
        }
    }

    /*** Transfer message or files to all the client except a particular client connected to the server ***/

    void blockcast(String line, String name) throws IOException, ClassNotFoundException {

        String[] words = line.split("::", 2);

        // Transferring a File to all the clients except a particular client

        if (words[1].split(" ")[0].toLowerCase().equals("sendfile"))
        {
            byte[] file_data = (byte[]) is.readObject();

            synchronized(this) {
                for (clientThread curr_client : clients) {
                    if (curr_client != null && curr_client != this && curr_client.clientName != null
                            && !curr_client.clientName.equals("@"+words[0].substring(1)))
                    {
                        curr_client.os.writeObject(name + "(private): Send to you File");
                        curr_client.os.writeObject("Sending_File:"+words[1].split(" ",2)[1].substring(words[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
                        curr_client.os.writeObject(file_data);
                        curr_client.os.flush();
                    }
                }
                System.out.println("File sent by "+ this.clientName.substring(1) + " to everyone except " + words[0].substring(1));
            }
        }

        // Transferring a message to all the clients except a particular client

        else
        {
            if (words.length > 1 && words[1] != null) {
                words[1] = words[1].trim();
                if (!words[1].isEmpty()) {
                    synchronized (this){
                        for (clientThread curr_client : clients) {
                            if (curr_client != null && curr_client != this && curr_client.clientName != null
                                    && !curr_client.clientName.equals("@"+words[0].substring(1))) {
                                curr_client.os.writeObject(name + "(private): " + words[1]);
                                curr_client.os.flush();


                            }
                        }
                        System.out.println("Message sent by "+ this.clientName.substring(1) + " to everyone except " + words[0].substring(1));
                    }
                }
            }
        }
    }

    /*** Transfer message or files to all the client connected to the server ***/

    void broadcast(String line, String name) throws IOException, ClassNotFoundException {
        // Transferring a File to all the clients

        if (line.split("\\s")[0].toLowerCase().equals("sendfile"))
        {
            byte[] file_data = (byte[]) is.readObject();
            synchronized(this){
                for (clientThread curr_client : clients) {

                    if (curr_client != null && curr_client.clientName != null && curr_client.clientName!=this.clientName) {
                        curr_client.os.writeObject(name + "(public): Send to you File");
                        curr_client.os.writeObject("Sending_File:"+line.split("\\s",2)[1].substring(line.split("\\s",2)[1].lastIndexOf(File.separator)+1));
                        curr_client.os.writeObject(file_data);
                        curr_client.os.flush();
                    }
                }

                //this.os.writeObject("Broadcast file sent successfully");
                //this.os.flush();
                System.out.println("Broadcast file sent by " + this.clientName.substring(1));
            }
        }

        else
        {
            // Transferring a message to all the clients
            synchronized(this){

                for (clientThread curr_client : clients) {

                    if (curr_client != null && curr_client.clientName != null && curr_client.clientName!=this.clientName) {
                        curr_client.os.writeObject(name + "(public): " + line);
                        curr_client.os.flush();
                    }
                }
                System.out.println("Broadcast message sent by " + this.clientName.substring(1));
            }
        }
    }
    /*** Transfer message or files to a particular client connected to the server ***/

    void unicast(String line, String name) throws IOException, ClassNotFoundException {

        String[] words = line.split("::", 2);

        // Transferring File to a particular client.

        if (words[1].split(" ")[0].toLowerCase().equals("sendfile"))
        {
            byte[] file_data = (byte[]) is.readObject();
            int cnt = 0;

            for (clientThread curr_client : clients) {
                if (curr_client != null && curr_client != this && curr_client.clientName != null
                        && curr_client.clientName.equals(words[0]))
                {
                    curr_client.os.writeObject(name + "(private): Send to you File");
                    curr_client.os.writeObject("Sending_File:"+words[1].split(" ",2)[1].substring(words[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
                    curr_client.os.writeObject(file_data);
                    curr_client.os.flush();
                    System.out.println(this.clientName.substring(1) + " transferred a private file to client "+ curr_client.clientName.substring(1));
                    cnt++;
                    break;
                }
            }
            if(cnt == 0){
                this.os.writeObject("This person is not online! Write: Who is online?");
                this.os.flush();
            }
        }

        // Transferring message to a particular client

        else
        {
            if (words.length > 1 && words[1] != null) {
                words[1] = words[1].trim();

                if (!words[1].isEmpty()) {

                    int cnt = 0;
                    for (clientThread curr_client : clients) {
                        if (curr_client != null && curr_client != this && curr_client.clientName != null
                                && curr_client.clientName.equals(words[0])) {
                            curr_client.os.writeObject(name + "(private): " + words[1]);
                            curr_client.os.flush();
                            cnt++;
                            System.out.println(this.clientName.substring(1) + " transferred a private message to client "+ curr_client.clientName.substring(1));
                            break;
                        }
                    }

                    if(cnt == 0){
                        this.os.writeObject("This person is not online! Write: Who is online?");
                        this.os.flush();

                    }
                }
            }
        }
    }


}
