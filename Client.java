import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    Socket socket;
    BufferedReader br;
    PrintWriter out;
    String username;

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        try {
            // Login or Signup
            Scanner scanner = new Scanner(System.in);
            boolean authenticated = false;
            
            while (!authenticated) {
                System.out.println("Choose an option: (1) Login (2) Signup");
                String choice = scanner.nextLine();
                
                System.out.print("Username: ");
                username = scanner.nextLine();
                System.out.print("Password: ");
                String password = scanner.nextLine();

                if (username.isEmpty() || password.isEmpty()) {
                    System.out.println("Username and password cannot be empty.");
                    continue;
                }

                try {
                    socket = new Socket("127.0.0.1", 7777);
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Send login/signup request
                    String action = choice.equals("1") ? "login" : "signup";
                    out.println(action + ":" + username + ":" + password);
                    
                    String response = br.readLine();
                    System.out.println(response);
                    
                    if (response.equals("Login successful") || response.equals("Signup successful")) {
                        authenticated = true;
                        System.out.println("Connected as " + username);
                    } else {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Start message reading thread
            startReading();
            
            // Start message writing
            startWriting();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("Reader started..");
            try {
                while (true) {
                    String msg = br.readLine();
                    if (msg == null || msg.equals("exit")) {
                        System.out.println("Server terminated the chat");
                        socket.close();
                        break;
                    }
                    
                    // Display messages from others
                    if (!msg.startsWith(username + ": ")) {
                        System.out.println(msg);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in reading: " + e.getMessage());
                e.printStackTrace();
            }
        };
        new Thread(r1).start();
    }
    
    public void startWriting() {
        Runnable r2 = () -> {
            System.out.println("Writer started...");
            try {
                Scanner sc = new Scanner(System.in);
                while (!socket.isClosed()) {
                    String content = sc.nextLine();
                    if (!content.isEmpty()) {
                        out.println(content);
                        
                        // If user types 'exit', close the connection
                        if (content.equals("exit")) {
                            socket.close();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in writing: " + e.getMessage());
                e.printStackTrace();
            }
        };
        new Thread(r2).start();
    }
}