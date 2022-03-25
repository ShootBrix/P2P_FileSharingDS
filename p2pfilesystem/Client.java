package p2pfilesystem;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    Socket serverSocket;
    Socket clientSupplierSocket;
    ObjectInputStream ois;
    ObjectOutputStream oos;
    String directoryPath = null;
    int localPeerid;

    public Client() throws IOException {
        System.out.println("Welcome Client:  ");
        serverSocket = new Socket("localhost", 7799);

        ois = new ObjectInputStream(serverSocket.getInputStream());
        oos = new ObjectOutputStream(serverSocket.getOutputStream());

        System.out.println("Connection has been established with the server");
    }

    // Ask for port and directory from the client and start Client as a receiver and a supplier
    public void run() {
        try {
            BufferedReader buffRead = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter full path of the directory that contains your files(upload from and download to):  ");
            directoryPath = buffRead.readLine();

            System.out.println("Enter the Peer ID for this directory:");
            localPeerid = Integer.parseInt(buffRead.readLine());

            sendFileInfoListToServer();
            System.out.println("The complete list of files sent: ");

            new ClientServerSupplier(localPeerid, directoryPath).start(); //Thread for suppling files

            runClientReceiver(); // function for recieving files
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error in establishing the Connection between the Client and the Server!!! \n Please try again.");
        }
    }

    // function for gathering file info for the server to maintain
    private void sendFileInfoListToServer() {
        try {
            File folder = new File(directoryPath);
            File[] listofFiles = folder.listFiles();
            FileInfo currentFile;
            File file;
            ArrayList<FileInfo> arrList = new ArrayList();
            for (File listofFile : listofFiles) {
                currentFile = new FileInfo();
                file = listofFile;
                currentFile.fileName = getFileName(file.getName());
                currentFile.chunkNumber = getChunkNumber(file.getName());
                currentFile.peerid = localPeerid;
                currentFile.originalHash = getOriginalHash(file);
                arrList.add(currentFile);
            }
            oos.writeObject(arrList); // send to the server
            oos.flush();
        } catch (Exception e) {
            System.out.println("Error in establishing the Connection between the Client and the Server!! " + e);
            System.out.println("Please try again");
        }
    }

    // two functions to get file names and chunck numbers.
    private String getFileName(String name) {
        //System.out.println("File Name = " + name);
        String temp = name.split("\\.")[0];
        //System.out.println("Name = " + temp);
        return temp;
    }

    private int getChunkNumber(String name) {
        String temp = name.split("\\.")[1];
        //System.out.println("Chunk = " + temp);
        return Integer.parseInt(temp);
    }

    // Alwasy listen to user input and take care of download requests
    private void runClientReceiver() {
        while (true) {
            try {
                BufferedReader buffRead = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter the desired file name that you want to download from the list of the files available in the Server:");
                String fileNameToDownload = buffRead.readLine();

                oos.writeObject(fileNameToDownload);
                oos.flush();

                System.out.println("Waiting for the reply from Server...!!");
                ArrayList<FileInfo> fileInfos = getFileInfoList();
                for (int i = 0; i < fileInfos.size(); i++) {
                    int peerid = fileInfos.get(i).peerid;
                    int chunkNumber = fileInfos.get(i).chunkNumber;
                    int originalHash = fileInfos.get(i).originalHash;
                    if (peerid != localPeerid) { // check that I'm not downloading from myself
                        if (!isFileAlreadyExist(fileNameToDownload, chunkNumber)) { // check that I'm not downloading a file I already have
                            receiveFile(peerid, fileNameToDownload, chunkNumber, originalHash);
                        } else {
                            System.out.println("Already has this file");
                        }
                    } else {
                        System.out.println("Already has this file");
                    }
                }
                if (fileInfos.isEmpty()) {
                    System.out.println("There are no files on server");
                } else {
                    sendFileInfoListToServer(); //update server
                }
            } catch (IOException e) {
                System.out.println("Error on run to download files: " + e);
            }
        }
    }

    // Get file info from server
    @SuppressWarnings("unchecked")
    private ArrayList<FileInfo> getFileInfoList() {
        ArrayList<FileInfo> peersFiles = new ArrayList();
        try {
            peersFiles = (ArrayList<FileInfo>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error on receive file info: " + e.getMessage());
        }
        return peersFiles;
    }

    // Download file to client's designated folder
    public void receiveFile(int clientSupplierPeerid, String fileNamedwld, int chunkNumber, int originalHash) {
        try {
            clientSupplierSocket = new Socket("localhost", clientSupplierPeerid);
            ObjectOutputStream clientAsServerOOS = new ObjectOutputStream(clientSupplierSocket.getOutputStream());
            ObjectInputStream clientAsServerOIS = new ObjectInputStream(clientSupplierSocket.getInputStream());

            // send file name request
            clientAsServerOOS.writeObject(fileNamedwld);
            clientAsServerOOS.flush();
            // send chunk number request
            clientAsServerOOS.writeObject(chunkNumber);
            clientAsServerOOS.flush();

            int readBytes = (int) clientAsServerOIS.readObject();
            //System.out.println("Number of bytes that have been received: " + readBytes);

            byte[] full_data = new byte[readBytes];
            clientAsServerOIS.readFully(full_data);

            // Integrity Test
            int hash = Arrays.hashCode(full_data); // function that calculates hashcode
            if (hash == originalHash) { // compare to original hash from fileInfo and then continue
                writeFileToFolder(full_data, readBytes, (fileNamedwld + "." + chunkNumber + ".txt"));
                System.out.println("Number of bytes that have been received: " + readBytes);
                System.out.println("File " + fileNamedwld + " chunk " + chunkNumber + " was received successful from peer:" + clientSupplierPeerid);
            } else {
                System.out.println("Chunk number " + chunkNumber + " was not downloaded due to corruption. current hash = " + hash + " originalHash = " + originalHash);
            }
        } catch (Exception ex) {
            System.out.println("Error on file receive: " + ex);
        }
    }

    private void writeFileToFolder(byte[] b, int readBytes, String fileNamedwld) throws Exception {
        OutputStream fileOPstream = new FileOutputStream(directoryPath + "//" + fileNamedwld);
        try (BufferedOutputStream BOS = new BufferedOutputStream(fileOPstream)) {
            BOS.write(b, 0, (int) readBytes);
            //System.out.println("Requested file - "+fileNamedwld+ ", has been downloaded to your desired directory "+directoryPath);			
            BOS.flush();
        }
    }

    // Function for preventing download of an existing file
    private Boolean isFileAlreadyExist(String fileName, int fileChunk) {
        File myFile = new File(directoryPath + "//" + fileName + "." + fileChunk + ".txt");
        return (int) myFile.length() > 0;
    }

    // hash the data for integrity test
    private int getOriginalHash(File file) throws Exception {
        FileInputStream FIS = new FileInputStream(file);
        byte[] full_data = new byte[(int) file.length()];
        try (BufferedInputStream objBIS = new BufferedInputStream(FIS)) {
            objBIS.read(full_data, 0, (int) file.length());
        }
        return Arrays.hashCode(full_data);
    }
}
