import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter out;

    public ChatClient() {

        setTitle("Chat Client");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton emojiButton = new JButton("😀");

        bottomPanel.add(emojiButton, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        connectToServer();

        inputField.addActionListener(e -> sendMessage());

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

        setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 12345);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        chatArea.append(msg + "\n");
                    }
                } catch (IOException e) {
                    chatArea.append("Disconnected from server\n");
                }
            }).start();

        } catch (IOException e) {
            chatArea.append("Cannot connect to server\n");
        }
    }

    private void sendMessage() {
        String msg = inputField.getText();

        if (!msg.isEmpty()) {
            out.println(msg);
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        new ChatClient();
    }
}