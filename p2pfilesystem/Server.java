package p2pfilesystem;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Server {

    public static ArrayList<FileInfo> globalArray = new ArrayList<FileInfo>();
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    public Server() throws IOException {
        serverSocket = new ServerSocket(7799);
        System.out.println("Server started!!  ");
    }

    public void run() {
        while (true) {
            System.out.println("Waiting for new Clients to connect...");
            try {
                clientSocket = serverSocket.accept();
                new ServerMainProcess(clientSocket, globalArray).start();
            } catch (IOException e) {
                System.out.println("Connection error: " + e);
            } catch (SecurityException e) {
                System.out.println("Security error: " + e);
            }
        }
    }
}

// Thread for running the server
class ServerMainProcess extends Thread {

    Socket clientSocket;
    ArrayList<FileInfo> globalArray; // array to keep list of all client files
    InputStream is;
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public ServerMainProcess(Socket clientSocket, ArrayList<FileInfo> globalArray) throws IOException {
        this.clientSocket = clientSocket;
        this.globalArray = globalArray;

        is = clientSocket.getInputStream();
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        ois = new ObjectInputStream(is);
    }

    public void run() {
        readFileList();
        while (true) {
            String str = receiveCommand();
            if (str != null) {
                System.out.println("Searching for the file name: " + str);
                ArrayList<FileInfo> sendingPeers = new ArrayList<FileInfo>();
                for (int j = 0; j < globalArray.size(); j++) {
                    FileInfo fileInfo = globalArray.get(j);
                    if (fileInfo.fileName.equals(str)) {
                        sendingPeers.add(fileInfo);
                    }
                }
                if (sendingPeers.size() > 0) {
                    System.out.println("File is found");
                    send(sendingPeers);
                } else {
                    System.out.println("File is not found");
                    send("File " + str + "is not found");
                }
            } else {
                if (readFileList() == false) {
                    break;
                }
            }
        }
    }

    private boolean readFileList() {
        try {
            ArrayList<FileInfo> filesList = receiveFileInfoList();
            if (filesList != null) {
                System.out.println("All the available files from the given directory have been recieved to the Server!");
                for (int i = 0; i < filesList.size(); i++) {
                    FileInfo fileInfo = filesList.get(i);
                    if (!globalArray.contains(fileInfo)) {
                        globalArray.add(fileInfo);
                        System.out.println("Added File name=" + fileInfo.fileName + ", chunk=" + fileInfo.chunkNumber + ", from PeerId=" + fileInfo.peerid);
                    }
                }
                System.out.println("Total number of files available in the Server that are received from all the connected clients: " + globalArray.size());
                return true;
            }
        } catch (Exception e) {
            System.out.println("Common exception: " + e);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<FileInfo> receiveFileInfoList() {
        ArrayList<FileInfo> filesList = null;
        try {
            filesList = (ArrayList<FileInfo>) ois.readObject();
        } catch (ClassNotFoundException e) {
            System.out.println("Recieve File info error:" + e);
        } catch (IOException ex) {
            System.out.println("Common Error on Recieve File info: " + ex);
        }
        return filesList;
    }

    private String receiveCommand() {
        String str = null;
        try {
            str = (String) ois.readObject();
        } catch (ClassCastException e) {
            //System.out.println("Error on casting to comman: " + e);
        } catch (Exception e) {
            System.out.println("Error on command read: " + e);
        }
        return str;
    }

    private void send(ArrayList<FileInfo> sendingPeers) {
        try {
            oos.writeObject(sendingPeers);
        } catch (IOException e) {
            System.out.println("Error on send: " + e.getMessage());
        }
    }

    private void send(String answer) {
        try {
            oos.writeObject(answer);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
