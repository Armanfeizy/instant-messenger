package network.messenger.messengergui;

import insomnia.view.IconFactory;
import network.messenger.controller.Message;

import static network.socketserver.client.ClientTypes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class MessagePanel extends JPanel {
    private JPanel messagesPanel;
    private JTextField messageTextField;
    private JButton sendMessageButton;
    private ClientUserMessenger client;
    private final JPanel commandHistoryPanel;
    private String anotherClientName;
    private boolean isSelected;
    private boolean isGroup;
    private boolean unreadMessageExistence;
    private JButton chooseFileButton;

    public MessagePanel(ClientUserMessenger client, JPanel commandHistoryPanel, String anotherClientName) {
        this.client = client;
        this.anotherClientName = anotherClientName;
        this.commandHistoryPanel = commandHistoryPanel;
        isSelected = false;
        isGroup = false;
        unreadMessageExistence = false;
        setLayout(new BorderLayout());
        handleContentPanel();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setAnotherClientName(String newName) {
        this.anotherClientName = newName;
    }

    public String getAnotherClientName() {
        return anotherClientName;
    }

    public JPanel getMessagesPanel() {
        return messagesPanel;
    }

    private void handleContentPanel() {
        handleMessagesPanel();
        handleMessageTextField();
    }

    private void handleChooseFileButton() {
        chooseFileButton = new JButton("File");
        var fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addActionListener(ev -> {
            try {
                var selectedFile = fileChooser.getSelectedFile();
                var fileMsg = new Message(selectedFile);
                messageTextField.setText(selectedFile.getName() + "$$" +
                        fileMsg.breakIntoParts().length + "%%" + selectedFile.length() + "->upLo " + anotherClientName);
                sendMessageButton.doClick();
                fileMsg.sendTo(client);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        chooseFileButton.addActionListener(e -> fileChooser.showOpenDialog(messagesPanel));
        chooseFileButton.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
    }

    private void handleMessageTextField() {
        JPanel wrapper = new JPanel(new BorderLayout());
        handleSendMessageButton();
        handleChooseFileButton();

        messageTextField = new JTextField();
        messageTextField.setBorder(IconFactory.textBubbleBorder(
                Color.BLUE, 2, 5, 0, true));
        messageTextField.addActionListener(e -> sendMessageButton.doClick());
        messageTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    messageTextField.setText(client.getCommand(true));
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    messageTextField.setText(client.getCommand(false));
                }
            }
        });

        wrapper.add(sendMessageButton, BorderLayout.EAST);
        wrapper.add(messageTextField, BorderLayout.CENTER);
        wrapper.add(chooseFileButton, BorderLayout.WEST);

        add(wrapper, BorderLayout.SOUTH);
    }

    public void setClient(ClientUserMessenger client) {
        this.client = client;
    }

    private void handleSendMessageButton() {
        AtomicInteger commandCounter = new AtomicInteger();
        sendMessageButton = new JButton("Send");
        sendMessageButton.setIcon(IconFactory.createTriangleIcon(15, Color.GREEN));
        sendMessageButton.setBorder(IconFactory.textBubbleBorder(
                Color.MAGENTA.darker(), 2, 5, 0, true));
        sendMessageButton.addActionListener(e -> {
            if (messageTextField.getText().isEmpty())
                return;
            final var command = messageTextField.getText() + (anotherClientName.equals("Server") ||
                    messageTextField.getText().contains("->upLo") ? "" :  "->send " + anotherClientName);
            if (command.replace(" ", "").length() == 0)
                return;
            var p = new JTextArea(null, messageTextField.getText(), 2, 0);
            p.setBackground(Color.DARK_GRAY);
            p.setLineWrap(true);
            p.setWrapStyleWord(true);
            p.setColumns(Math.min(command.length() , 50));
            p.setEditable(false);
            p.setOpaque(true);
            p.setBackground(Color.DARK_GRAY.darker());
            p.setBorder(IconFactory.textBubbleBorder(Color.BLUE, 4, 30, 10, false));

            var wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            wrapper.add(p);
            wrapper.setMaximumSize(new Dimension(
                    Integer.MAX_VALUE, 120 + messageTextField.getText().length() / 5));

            messagesPanel.add(wrapper);

            wrapper.revalidate();

            ///////////////////
            var l = new JLabel("#" + (commandCounter.get() + 1) + ": " + command);
            l.setOpaque(true);
            l.setBackground(commandCounter.getAndIncrement() % 2 == 0 ?
                    Color.GRAY : Color.GRAY.darker());
            l.setForeground(commandCounter.get() % 2 == 0 ?
                    Color.GRAY : Color.GRAY.darker());
            wrapper = new JPanel(new BorderLayout());
            wrapper.add(l, BorderLayout.CENTER);
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            commandHistoryPanel.add(wrapper);
            commandHistoryPanel.revalidate();
            ///////////////////

            messageTextField.setText("");

            try {
                client.sendObject(command);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            client.pushCommand(command);
        });
    }

    private void handleMessagesPanel() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        JScrollPane messagesPanelScrollPane = new JScrollPane(messagesPanel);
        messagesPanelScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(messagesPanelScrollPane, BorderLayout.CENTER);
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public boolean isUnreadMessageExistence() {
        return unreadMessageExistence;
    }

    public void setUnreadMessageExistence(boolean unreadMessageExistence) {
        this.unreadMessageExistence = unreadMessageExistence;
    }
}
