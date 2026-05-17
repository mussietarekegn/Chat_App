import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends JFrame {

    private static Set<PrintWriter> clientWriters = new HashSet<>();

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    public ChatServer() {

        setTitle("Server Chat");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        inputField = new JTextField();
        sendButton = new JButton("Send");
        JButton emojiButton = new JButton("😀");

        bottomPanel.add(emojiButton, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());

        emojiButton.addActionListener(e -> {

            String[] emojis = {"😀", "😂", "❤️", "👍", "😎", "😭"};

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Choose Emoji",
                    "Emoji Picker",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    emojis,
                    emojis[0]
            );

            if (selected != null) {
                inputField.setText(inputField.getText() + selected);
            }
        });

        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void sendMessage() {

        String msg = inputField.getText();

        if (!msg.isEmpty()) {

            chatArea.append("Server: " + msg + "\n");

            synchronized (clientWriters) {

                for (PrintWriter writer : clientWriters) {
                    writer.println("Server: " + msg);
                }
            }

            inputField.setText("");
        }
    }

    public static void main(String[] args) {

        ChatServer serverGUI = new ChatServer();

        serverGUI.chatArea.append("Server is running...\n");

        try (ServerSocket serverSocket = new ServerSocket(12345)) {

            while (true) {

                Socket clientSocket = serverSocket.accept();

                serverGUI.chatArea.append("New client connected\n");

                new ClientHandler(clientSocket, serverGUI).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {

        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private ChatServer serverGUI;

        public ClientHandler(Socket socket, ChatServer serverGUI) {
            this.socket = socket;
            this.serverGUI = serverGUI;
        }

        public void run() {

            try {

                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;

                while ((message = in.readLine()) != null) {

                    serverGUI.chatArea.append(message + "\n");

                    synchronized (clientWriters) {

                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }

            } catch (IOException e) {

                serverGUI.chatArea.append("Client disconnected\n");
            }
        }
    }
}