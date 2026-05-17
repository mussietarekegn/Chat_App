import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class ChatClient extends JFrame {

    private JTextPane chatArea;
    private JTextField inputField;
    private PrintWriter out;

    // Attachment state
    private File pendingImageFile = null;
    private JLabel attachmentLabel;
    private JButton cancelAttachButton;

    public ChatClient() {
        setTitle("Chat Client");
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

        connectToServer();

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

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        handleIncoming(msg);
                    }
                } catch (Exception e) {
                    appendText("Disconnected from server\n");
                }
            }).start();

        } catch (Exception e) {
            appendText("Cannot connect to server\n");
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
        if (out == null) return;
        String msg = inputField.getText();

        if (pendingImageFile != null) {
            try {
                BufferedImage img = ImageIO.read(pendingImageFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                out.println("IMAGE:" + base64);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cancelAttachment();
        }

        if (!msg.trim().isEmpty()) {
            out.println("Client: " + msg);
            inputField.setText("");
        }
    }

    private void handleIncoming(String msg) {
        try {
            if (msg.startsWith("IMAGE:")) {
                String base64 = msg.substring(6);
                byte[] bytes = Base64.getDecoder().decode(base64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));

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

                // FIX: Move cursor to the very end before adding the image
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                chatArea.insertComponent(label);

                // Add click listener to the image component
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        new ImageZoomer(img);
                    }
                });

                appendText("\n");
            } else {
                appendText(msg + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendText(String text) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), text, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inner class for image zooming
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

            // Set a larger size but not full window
            setSize(400, 400);
            setLocationRelativeTo(ChatClient.this);
            setVisible(true);
        }
    }

    public static void main(String[] args) {
        new ChatClient();
    }
}