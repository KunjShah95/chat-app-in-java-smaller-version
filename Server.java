import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class Server {
    ServerSocket server;
    List<ClientHandler> clients = new ArrayList<>();
    JTextArea textArea;
    DefaultListModel<String> userListModel;
    Map<String, String> userCredentials = new HashMap<>(); // Store username and password

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }

    public Server() {
        JFrame frame = new JFrame("Server");
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userScrollPane);
        splitPane.setDividerLocation(300);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(this::startServer).start();
    }

    public void startServer() {
        try {
            server = new ServerSocket(7777);
            updateTextArea("Server is ready to accept connections\nWaiting...\n");

            while (true) {
                Socket socket = server.accept(); // establishes connection
                updateTextArea("A new client has connected\n");
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (Exception e) {
            updateTextArea("Error in server setup: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void updateTextArea(String message) {
        SwingUtilities.invokeLater(() -> textArea.append(message));
    }

    public void addUser(String username) {
        SwingUtilities.invokeLater(() -> userListModel.addElement(username));
    }

    public void removeUser(String username) {
        SwingUtilities.invokeLater(() -> userListModel.removeElement(username));
    }

    public boolean registerUser(String username, String password) {
        if (userCredentials.containsKey(username)) {
            return false; // User already exists
        }
        userCredentials.put(username, password);
        return true;
    }

    public boolean authenticateUser(String username, String password) {
        return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
    }
}

class ClientHandler implements Runnable {
    Socket socket;
    BufferedReader br;
    PrintWriter out;
    Server server;
    String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Handle login/signup
            while (true) {
                String[] credentials = br.readLine().split(":", 3);
                String action = credentials[0];
                username = credentials[1];
                String password = credentials[2];

                if ("signup".equals(action)) {
                    if (server.registerUser(username, password)) {
                        out.println("Signup successful");
                        break;
                    } else {
                        out.println("Username already exists");
                    }
                } else if ("login".equals(action)) {
                    if (server.authenticateUser(username, password)) {
                        out.println("Login successful");
                        break;
                    } else {
                        out.println("Invalid credentials");
                    }
                }
            }

            server.updateTextArea(username + " has joined the chat\n");
            server.addUser(username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = br.readLine()) != null) {
                if (msg.equals("exit")) {
                    server.updateTextArea(username + " has left the chat\n");
                    server.removeUser(username);
                    socket.close();
                    break;
                }
                String message = username + ": " + msg;
                server.updateTextArea(message + "\n");
                server.broadcastMessage(message, this);
            }
        } catch (Exception e) {
            server.updateTextArea("Error in client handler: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}