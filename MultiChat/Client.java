package MultiChat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client implements Runnable {

    private static Socket clientSocket = null;
    private static ObjectOutputStream os = null;
    private static ObjectInputStream is = null;
    private static BufferedReader inputLine = null;
    private static BufferedInputStream bis = null;
    private static boolean closed = false;
    //test test

    public static void main(String[] args) {

        Scanner s = new Scanner(System.in);

        System.out.println("Please enter Server IP:");
        String ip;
        ip = s.nextLine();

        System.out.println("Please enter port connect to the server:");
        int portNumber;
        portNumber = s.nextInt();

        try {
            clientSocket = new Socket(ip, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            is = new ObjectInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            System.err.println("Unknown " + ip);
        } catch (IOException e) {
            System.err.println("No Server found. Please ensure that the Server program is running and try again.");
        }


        if (clientSocket != null && os != null && is != null) {
            try {

                // Create a thread to read from the server.
                 Thread t = new Thread(new Client());
                 t.start();

                while (!closed) {

                    // Read input from MultiChat.Client

                    String msg = inputLine.readLine().trim();

                    // Check the input for private messages or files

                    if ((msg.split("::").length > 1))
                    {
                        if (msg.split("::")[1].toLowerCase().startsWith("sendfile"))
                        {
                            File sfile = new File((msg.split("::")[1]).split(" ",2)[1]);

                            if (!sfile.exists())
                            {
                                System.out.println("File Doesn't exist!!");
                                continue;
                            }

                            byte [] mybytearray  = new byte [(int)sfile.length()];
                            FileInputStream fis = new FileInputStream(sfile);
                            bis = new BufferedInputStream(fis);
                            while (bis.read(mybytearray,0,mybytearray.length)>=0)
                            {
                                bis.read(mybytearray,0,mybytearray.length);
                            }
                            os.writeObject(msg);
                            os.writeObject(mybytearray);
                            os.flush();

                        }
                        else {
                            os.writeObject(msg);
                            os.flush();
                        }
                    }

                    // Check the input for broadcast files

                    else if (msg.toLowerCase().startsWith("sendfile"))
                    {

                        File sfile = new File(msg.split(" ",2)[1]);

                        if (!sfile.exists())
                        {
                            System.out.println("File Doesn't exist!!");
                            continue;
                        }

                        byte [] mybytearray  = new byte [(int)sfile.length()];
                        FileInputStream fis = new FileInputStream(sfile);
                        bis = new BufferedInputStream(fis);
                        while (bis.read(mybytearray,0,mybytearray.length)>=0)
                        {
                            bis.read(mybytearray,0,mybytearray.length);
                        }
                        os.writeObject(msg);
                        os.writeObject(mybytearray);
                        os.flush();
                    }
                    //Check the input for broadcast messages

                    else
                    {
                        os.writeObject(msg);
                        os.flush();
                    }
                }
                os.close();
                is.close();
                clientSocket.close();
            } catch (IOException e)
            {
                System.err.println(e);
            }
        }
    }

     //Create a thread to read from the server.
    public void run() {
        //reading until receive Bye from the server

        String responseLine;
        String filename = null;
        byte[] ipfile = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        File directory_name = null;
        String full_path;
        String dir_name = "Received_Files";

        try {


            while ((responseLine = (String) is.readObject()) != null)  {

                //Directory creation

                if (responseLine.equals("Directory Created"))
                {
                    //Creating Receiving Folder

                    directory_name = new File(dir_name);

                    if (!directory_name.exists())
                    {
                        directory_name.mkdir();

                        System.out.println("New Receiving file directory for this client created!!");
                    }
                    else
                    {
                        System.out.println("Receiving file directory for this client already exists!!");
                    }
                }

                //Checking for incoming files

                else if (responseLine.startsWith("Sending_File"))
                {
                    try
                    {
                        filename = responseLine.split(":")[1];
                        full_path = directory_name.getAbsolutePath()+"/"+filename;
                        ipfile = (byte[]) is.readObject();
                        fos = new FileOutputStream(full_path);
                        bos = new BufferedOutputStream(fos);
                        bos.write(ipfile);
                        bos.flush();
                        //System.out.println("File Received.");
                    }
                    finally
                    {
                        if (fos != null) fos.close();
                        if (bos != null) bos.close();
                    }
                }

                // Checking for incoming messages
                else {
                    System.out.println(responseLine);
                }
                if (responseLine.contains("*** Bye"))
                    break;
            }

            closed = true;
            System.exit(0);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Server Process Stopped Unexpectedly!!");

        }
    }
}

