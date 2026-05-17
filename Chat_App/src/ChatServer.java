import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

public class ChatServer extends JFrame {

    private static Set<PrintWriter> clients = new HashSet<>();
    private static ChatServer instance;

    private JTextPane chatArea;
    private JTextField inputField;

    private File pendingImageFile = null;
    private JLabel attachmentLabel;
    private JButton cancelAttachButton;

    private static Connection conn = null;

    public ChatServer() {
        instance = this;
        setTitle("Server Chat");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout());

        JPanel attachmentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        attachmentLabel = new JLabel("");
        cancelAttachButton = new JButton("✖ Cancel Image");
        cancelAttachButton.setForeground(Color.RED);
        cancelAttachButton.setVisible(false);
        cancelAttachButton.addActionListener(e -> cancelAttachment());

        attachmentPanel.add(attachmentLabel);
        attachmentPanel.add(cancelAttachButton);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton emojiButton = new JButton("😀");
        JButton imageButton = new JButton("📷");

        JPanel leftControls = new JPanel();
        leftControls.add(emojiButton);
        leftControls.add(imageButton);

        inputPanel.add(leftControls, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        bottomContainer.add(attachmentPanel, BorderLayout.NORTH);
        bottomContainer.add(inputPanel, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        emojiButton.addActionListener(e -> {
            String[] emojis = {"😀", "😂", "❤️", "👍", "😎", "😭"};
            String selected = (String) JOptionPane.showInputDialog(
                    this, "Choose Emoji", "Emoji Picker",
                    JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]
            );
            if (selected != null) {
                inputField.setText(inputField.getText() + selected);
            }
        });

        imageButton.addActionListener(e -> attachImage());

        setVisible(true);
    }

    // UPDATED: Now loads connection data securely from the config file
    private static void initDatabase() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            // Read the properties file
            props.load(input);

            String url = props.getProperty("db.url");
            String username = props.getProperty("db.username");
            String password = props.getProperty("db.password");

            conn = DriverManager.getConnection(url, username, password);
            Statement stat = conn.createStatement();

            stat.executeUpdate("CREATE TABLE IF NOT EXISTS chat_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "message LONGTEXT)");

            System.out.println("MySQL Database connected successfully securely.");
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: config.properties file is missing! Create it in your project root folder.");
        } catch (Exception e) {
            System.out.println("MySQL Connection failed!");
            e.printStackTrace();
        }
    }

    private static void saveToDatabase(String msg) {
        if (conn == null) return;
        try {
            String sql = "INSERT INTO chat_logs (message) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, msg);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadLocalHistory() {
        if (conn == null) return;
        try {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT message FROM chat_logs ORDER BY id ASC");

            while (rs.next()) {
                String msg = rs.getString("message");
                if (msg.startsWith("IMAGE:")) {
                    String base64 = msg.substring(6);
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    displayImage(img);
                } else {
                    appendText(msg + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatHistory(PrintWriter clientOut) {
        if (conn == null) return;
        try {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT message FROM chat_logs ORDER BY id ASC");

            while (rs.next()) {
                String oldMsg = rs.getString("message");
                clientOut.println(oldMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void attachImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pendingImageFile = chooser.getSelectedFile();
            attachmentLabel.setText("Attached: " + pendingImageFile.getName());
            cancelAttachButton.setVisible(true);
        }
    }

    private void cancelAttachment() {
        pendingImageFile = null;
        attachmentLabel.setText("");
        cancelAttachButton.setVisible(false);
    }

    private void sendMessage() {
        String msg = inputField.getText();

        if (pendingImageFile != null) {
            try {
                BufferedImage img = ImageIO.read(pendingImageFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                String rawImageProtocol = "IMAGE:" + base64;
                broadcast(rawImageProtocol);
                saveToDatabase(rawImageProtocol);
                displayImage(img);

            } catch (Exception e) {
                e.printStackTrace();
            }
            cancelAttachment();
        }

        if (!msg.trim().isEmpty()) {
            String formattedMsg = "Server: " + msg;
            broadcast(formattedMsg);
            saveToDatabase(formattedMsg);
            appendText(formattedMsg + "\n");
            inputField.setText("");
        }
    }

    public static void broadcast(String msg) {
        synchronized (clients) {
            for (PrintWriter w : clients) {
                w.println(msg);
            }
        }
    }

    public void displayImage(BufferedImage img) {
        SwingUtilities.invokeLater(() -> {
            try {
                int maxDim = 150;
                int width = img.getWidth();
                int height = img.getHeight();

                if (width > height) {
                    height = (height * maxDim) / width;
                    width = maxDim;
                } else {
                    width = (width * maxDim) / height;
                    height = maxDim;
                }

                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                JLabel label = new JLabel(icon);

                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                chatArea.insertComponent(label);

                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        new ImageZoomer(img);
                    }
                });

                StyledDocument doc = chatArea.getStyledDocument();
                doc.insertString(doc.getLength(), "\n", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = chatArea.getStyledDocument();
                doc.insertString(doc.getLength(), text, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    class ImageZoomer extends JDialog {
        public ImageZoomer(BufferedImage image) {
            setTitle("Image Viewer");
            setModal(true);
            setLayout(new BorderLayout());

            ImageIcon icon = new ImageIcon(image);
            JLabel label = new JLabel(icon);
            JScrollPane scrollPane = new JScrollPane(label);
            add(scrollPane, BorderLayout.CENTER);

            JButton closeButton = new JButton("Close (X)");
            closeButton.addActionListener(e -> dispose());
            add(closeButton, BorderLayout.SOUTH);

            setSize(400, 400);
            setLocationRelativeTo(ChatServer.this);
            setVisible(true);
        }
    }

    public static void main(String[] args) {
        initDatabase();

        ChatServer gui = new ChatServer();
        gui.loadLocalHistory();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            gui.appendText("Server running...\n");

            while (true) {
                Socket socket = serverSocket.accept();
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                gui.sendChatHistory(out);

                synchronized (clients) {
                    clients.add(out);
                }
                gui.appendText("Client connected\n");

                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            if (msg.startsWith("IMAGE:")) {
                                broadcast(msg);
                                saveToDatabase(msg);

                                String base64 = msg.substring(6);
                                byte[] bytes = Base64.getDecoder().decode(base64);
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                                gui.displayImage(img);
                            } else {
                                broadcast(msg);
                                saveToDatabase(msg);
                                gui.appendText(msg + "\n");
                            }
                        }
                    } catch (Exception e) {
                        synchronized (clients) {
                            clients.remove(out);
                        }
                        gui.appendText("Client disconnected\n");
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}