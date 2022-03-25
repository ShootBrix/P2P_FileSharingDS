package p2pfilesystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// main class, gives the initial promtp and starts the server and clients 
public class P2PFileSystem {

    public static void main(String args[]) throws Exception {

        System.out.println("Please enter your choice:");
        System.out.println("1. Run as Server");
        System.out.println("2. Run as Client");

        BufferedReader buffRead = new BufferedReader(new InputStreamReader(System.in));
        String choice = buffRead.readLine();

        if (choice.equals("1")) {
            try {
                Server s = new Server(); // Create a server object
                s.run(); // Start the server
            } catch (IOException e) {
                System.out.println("Server start error " + e.getMessage());
            }
        } else if (choice.equals("2")) {
            try {
                Client c = new Client(); // Create a Client object
                c.run(); //Start the Client
            } catch (IOException e) {
                System.out.println("Client start error " + e.getMessage());
            }
        } else {
            System.out.println("Incorrect input. Program Stopped");
        }
    }
}
