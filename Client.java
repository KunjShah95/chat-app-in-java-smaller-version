import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    Socket socket;
    BufferedReader br;
    PrintWriter out;
    JTextArea textArea;
    JTextField textField;
    JLabel statusLabel;
    DefaultListModel<String> userListModel;
    String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        JFrame frame = new JFrame("Chat Client");
        frame.setLayout(new BorderLayout());

        // Text area for chat messages
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setBackground(new Color(240, 240, 240));
        JScrollPane chatScrollPane = new JScrollPane(textArea);
        chatScrollPane.setPreferredSize(new Dimension(450, 300));

        // Text area for input
        JTextArea inputArea = new JTextArea(3, 20);
        inputArea.setFont(new Font("Arial", Font.PLAIN, 14));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);

        inputArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    String content = inputArea.getText().trim();
                    if (!content.isEmpty()) {
                        out.println(content);
                        inputArea.setText("");
                        if (content.equals("exit")) {
                            try {
                                socket.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    evt.consume();
                }
            }
        });

        // User list
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        // Status label
        statusLabel = new JLabel("Disconnected");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Top panel for status and user list
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(userScrollPane, BorderLayout.CENTER);

        // Bottom panel for input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);

        // Add components to frame
        frame.add(topPanel, BorderLayout.EAST);
        frame.add(chatScrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Login or Signup
        while (true) {
            String[] options = {"Login", "Signup"};
            int choice = JOptionPane.showOptionDialog(frame, "Choose an option", "Login/Signup",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == -1) {
                System.exit(0);
            }

            JPanel panel = new JPanel(new GridLayout(2, 2));
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(frame, panel, options[choice], JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Username and password cannot be empty.");
                    continue;
                }

                try {
                    socket = new Socket("127.0.0.1", 7777);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println((choice == 0 ? "login" : "signup") + ":" + username + ":" + password);
                    String response = br.readLine();
                    if (response.equals("Login successful") || response.equals("Signup successful")) {
                        statusLabel.setText("Connected as " + username);
                        break;
                    } else {
                        JOptionPane.showMessageDialog(frame, response);
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

            startReading();
    }

    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("Reader started..");
            try {
                while (true) {
                    String msg = br.readLine();
                    if (msg.equals("exit")) {
                        System.out.println("Server terminated the chat");
                        socket.close();
                        break;
                    }
                    updateTextArea(msg + "\n");
                }
            } catch (Exception e) {
                System.err.println("Error in reading: " + e.getMessage());
            e.printStackTrace();
        }
        };
        new Thread(r1).start();
    }

    public void updateTextArea(String message) {
        SwingUtilities.invokeLater(() -> textArea.append(message));
    }
}