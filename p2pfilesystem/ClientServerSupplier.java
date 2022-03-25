package p2pfilesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientServerSupplier extends Thread { //thread for client to client connections

    int peerPort;
    String directoryPath = null;
    ServerSocket clientSocket;
    Socket socket = null;

    public ClientServerSupplier(int peerPort, String directoryPath) throws IOException {
        this.peerPort = peerPort;
        this.directoryPath = directoryPath;
        clientSocket = new ServerSocket(peerPort);
    }

    public void run() {
        while (true) {
            try {
                socket = clientSocket.accept();
                new ClientSupplierThread(socket, directoryPath).start(); //Thread for waiting on another client reciever
            } catch (IOException e) {
                System.out.println("Error on connection client receiver " + e.getMessage());
            } catch (SecurityException e) {
                System.out.println("Security error on client connection: " + e);
            }
        }
    }
}

class ClientSupplierThread extends Thread { //thread for uploads from multiple clients

    Socket socket;
    String directoryPath;

    public ClientSupplierThread(Socket socket, String directoryPath) {
        this.socket = socket;
        this.directoryPath = directoryPath;
    }

    public void run() {
        try {
            ObjectOutputStream objOS = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objIS = new ObjectInputStream(socket.getInputStream());

            String fileName = (String) objIS.readObject();
            int fileChunk = (int) objIS.readObject();

            File myFile = new File(directoryPath + "//" + fileName + "." + fileChunk + ".txt");
            int length = (int) myFile.length();

            byte[] byte_arr = new byte[length];
            readFileFromFolder(myFile, byte_arr);

            objOS.writeObject(length); //allocate space for file
            objOS.flush();

            objOS.write(byte_arr, 0, length); //upload the file
            objOS.flush();

            System.out.println("File transfered successful");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error on file transfer: " + e);
        }
    }

    // Read the content of file
    private void readFileFromFolder(File myFile, byte[] byte_arr) {
        try {
            FileInputStream FIS = new FileInputStream(myFile);
            try (BufferedInputStream objBIS = new BufferedInputStream(FIS)) {
                objBIS.read(byte_arr, 0, (int) myFile.length());
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

}
