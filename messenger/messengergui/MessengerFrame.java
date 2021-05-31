package network.messenger.messengergui;

import guitools.MainFrame;
import insomnia.view.IconFactory;
import network.Main;
import utils.ExceptionHandler;
import utils.Utils;
import utils.managers.TTManager;
import utils.managers.Updater;

import static network.socketserver.client.ClientTypes.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class MessengerFrame extends MainFrame {
    // animation.render3D.j3ds.j3d2.Main Frame
    public final JFrame mainFrame;
    
    // clients private pages handling
    private ArrayList<MessagePanel> messagePanels;
    private String selectedUser;

    // certification
    private JDialog certificationDialog;
    private JDialog signUpDialog;

    // logical fields
    private String nameOfClient;
    private String passOfClient;

    // The only client using this user interface
    private ClientUserMessenger client;

    // Left Panel Components
    private JTextField findUserTextField;
    private JPanel onlineUsersPanel;
    private ArrayList<JLabel> usersLabel;

    // Right Panel Components
    private JPanel commandsHistoryPanel;

    // Constructor
    public MessengerFrame() {
        super("9726028-AUT-Messenger");
        mainFrame = this;
        init();
        handleCertificationPanel();
    }

    // handle certification
    private void handleCertificationPanel() {
        // certification panel
        JPanel certificationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        var userName = new JTextField();
        certificationPanel.add(userName);
        userName.setPreferredSize(new Dimension(350, 50));

        var password = new JPasswordField();
        certificationPanel.add(password);
        password.setPreferredSize(new Dimension(350, 50));

        var logIn = new JButton("Login");
        certificationPanel.add(logIn);
        logIn.setPreferredSize(new Dimension(350, 50));
        password.addActionListener(e -> logIn.doClick());

        logIn.addActionListener(e -> {
            nameOfClient = userName.getText();
            passOfClient = new String(password.getPassword());
            userName.setText("");
            password.setText("");
            client.setName(nameOfClient);
            client.setPass(passOfClient);
            try {
                client.sendObject(nameOfClient + ":" + passOfClient + "->cert");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        certificationDialog = new JDialog(mainFrame, "Certification");

        var signUp = new JButton("Sign Up");
        signUp.setPreferredSize(new Dimension(350, 50));
        signUp.addActionListener(e -> {
            certificationDialog.setVisible(false);
            signUpDialog = new JDialog(certificationDialog, "Sign Up");
            signUpDialog.setLocationRelativeTo(certificationPanel);
            signUpDialog.setAlwaysOnTop(true);
            signUpDialog.setSize(400, 250);
            signUpDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
            signUpDialog.setVisible(true);
            signUpDialog.setResizable(false);
            signUpDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    certificationDialog.setVisible(true);
                }
            });
            var newUserName = new JTextField();
            signUpDialog.add(newUserName);
            newUserName.setPreferredSize(new Dimension(350, 50));

            var newPassword = new JPasswordField();
            signUpDialog.add(newPassword);
            newPassword.setPreferredSize(new Dimension(350, 50));

            var newSignUp = new JButton("Sign Up");
            signUpDialog.add(newSignUp);
            newSignUp.setPreferredSize(new Dimension(350, 50));
            newPassword.addActionListener(ev -> newSignUp.doClick());

            newSignUp.addActionListener(ev -> {
                nameOfClient = newUserName.getText();
                passOfClient = new String(newPassword.getPassword());
                userName.setText("");
                password.setText("");
                client.setName(nameOfClient);
                client.setPass(passOfClient);
                try {
                    client.sendObject(nameOfClient + ":" + passOfClient + "->sign");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });

            signUpDialog.setVisible(true);
        });
        certificationPanel.add(signUp);

        setEnabled(false);

        certificationDialog.setLocationRelativeTo(mainFrame);
        certificationDialog.setAlwaysOnTop(true);
        certificationDialog.setSize(400, 300);
        certificationDialog.setLayout(new BorderLayout());
        certificationDialog.add(certificationPanel, BorderLayout.CENTER);
        certificationDialog.setVisible(true);
        certificationDialog.setResizable(false);
        certificationDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(-1);
            }
        });
    }

    // initialization
    private void init() {
        nameOfClient = "Unidentified";

        splitterRight.setDividerLocation(0.2);
        splitterLeft.setDividerLocation(0.6);
        splitterDown.setDividerLocation(0.075);
        splitterUp.setDividerLocation(0.95);

        var l = new JLabel("Please Choose a Person To Chat", JLabel.CENTER);
        l.setOpaque(true);
        l.setBackground(Color.DARK_GRAY);
        contentPanel.add(l, BorderLayout.CENTER);

        handleNorthPanel();
        handleSouthPanel();
        handleLeftPanel();
        handleRightPanel();

        usersLabel = new ArrayList<>();
        messagePanels = new ArrayList<>();

        new Thread(this::handleClient).start();
        handleMainFrame();
        handleUpdater();
    }

    // MainFrame
    private void handleMainFrame() {
        splitterDown.setDividerSize(1);
        splitterUp.setDividerSize(1);
        splitterRight.setDividerSize(1);
        splitterLeft.setDividerSize(1);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        try {
                            client.stopDataTransferring();
                            client.getSocket().close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
        );
    }

    // Handle Client
    private void handleClient() {
        try {
            client = new ClientUserMessenger("localhost", 3000, nameOfClient) {
                private String destinationPath;

                @Override
                public void useComponent(JComponent c) {
                    var wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));

                    wrapper.add(c);
                    wrapper.setBorder(IconFactory.
                            textBubbleBorder(Color.RED, 4, 30, 10, true));
                    wrapper.revalidate();
                    wrapper.setPreferredSize(new Dimension(400, 400));
                    wrapper.setMaximumSize(new Dimension(400, 400));
                    wrapper.setMinimumSize(new Dimension(400, 400));
                    Objects.requireNonNull(getMessagePanel(selectedUser)).add(wrapper);
                    Objects.requireNonNull(getMessagePanel(selectedUser)).revalidate();
                }

                @Override
                public void useText(String text) {
                    String group = null;
                    if (text.contains("%%")) {
                        group = text.substring(0, text.indexOf("%%"));
                        text = text.substring(text.indexOf("%%") + 2);
                    }
                    var p = new JTextArea(null, text, 2, 0);
                    p.setLineWrap(true);
                    p.setWrapStyleWord(true);
                    p.setColumns(Math.min(text.length() , 50));
                    p.setEditable(false);
                    p.setOpaque(true);
                    p.setBackground(Color.DARK_GRAY.darker());
                    p.setBorder(IconFactory.textBubbleBorder(
                            Color.RED, 4, 30, 10, true));

                    var wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    wrapper.add(p);
//                    wrapper.setMaximumSize(new Dimension(
//                            Integer.MAX_VALUE, 120 + text.length() / 5));

                    if (!text.contains(":"))
                        text = "Server: " + text;

                    if (group != null) {
                        var mp = Objects.requireNonNull(getGroupPanel(group));
                        mp.getMessagesPanel().add(wrapper);
                        if (!mp.isSelected())
                            mp.setUnreadMessageExistence(true);
                        mp.revalidate();
                    } else {
                        var mp = Objects.requireNonNull(getMessagePanel(text.substring(0, text.indexOf(":"))));
                        mp.getMessagesPanel().add(wrapper);
                        mp.getMessagesPanel().revalidate();
                        if (!mp.isSelected())
                            mp.setUnreadMessageExistence(true);
                    }
                    revalidate();
                    wrapper.revalidate();
                }

                @Override
                public void useUsersSituations(String[] usersSituations) {
                    var diff = usersSituations.length - usersLabel.size();
                    if (diff != 0)
                        while (diff-- > 0) {
                            var ss = usersSituations[usersSituations.length-diff-1].split("=");
                            if (ss.length == 1 || ss[0] == null || ss[0].equals("null") || ss[0].equals("Unidentified"))
                                continue;
                            createNewUserLabel(ss[0]);
                        }

                    for (String usersSituation : usersSituations) {
                        var ss = usersSituation.split("=");
                        if (ss.length == 1 || ss[0] == null || ss[0].equals("null") || ss[0].equals("Unidentified"))
                            continue;
                        ss[0] = ss[0].equals(nameOfClient) ? "Server" : ss[0];
                        if (getUserLabel(ss[0]) == null)
                            createNewUserLabel(ss[0]);
                        var l = getUserLabel(ss[0]);
//                        Objects.requireNonNull(getMessagePanel(ss[0])).setAnotherClientName(ss[0]);
//                        if (messagePanels.get(i).isSelected())
//                            selectedUser = ss[0];
                        assert l != null;
                        l.setToolTipText(ss[1]);
                        l.setText(ss[0].equals(nameOfClient) ? "Server" : ss[0]);
                        l.setIcon(IconFactory.createCircleIcon(15, ss[1].equals("online") ? Color.GREEN : Color.RED));
                    }
                }

                @Override
                public void certificationAction(boolean certificated) {
                    if (certificated) {
                        certificationDialog.setVisible(false);
                        setEnabled(true);
                        repaint();
                        var self = getUserLabel(nameOfClient);
                        if (self != null)
                            onlineUsersPanel.remove(self);
                    } else {
                        JOptionPane.showMessageDialog(certificationDialog, "Wrong username password combination.");
                    }
                }

                @Override
                public void signUpAction(boolean done) {
                    if (done) {
                        JOptionPane.showMessageDialog(certificationDialog, "Account Added");
                        certificationDialog.setVisible(true);
                        signUpDialog.setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(certificationDialog, "This Name Existed");
                    }
                }

                @Override
                public void createNewGroup(String info) {
                    createNewUserLabel(info);
                }

                @Override
                public void useFileInfo(String fileInfo) {
                    var ss = fileInfo.split(":");
                    var text = ss[0] + ":  " + "sent file: " + ss[3] + "\n" + "size: " + ss[2];
                    var p = new JTextArea(null, text, 2, 0);
                    p.setLineWrap(true);
                    p.setWrapStyleWord(true);
                    p.setColumns(Math.min(text.length() , 50));
                    p.setEditable(false);
                    p.setOpaque(true);
                    p.setBackground(Color.DARK_GRAY.darker());
                    p.setBorder(IconFactory.textBubbleBorder(
                            Color.RED, 4, 30, 10, true));

                    var downloadButton = new JButton("Download");
                    var fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileChooser.addActionListener(e -> {
                        try {
                            destinationPath = fileChooser.getSelectedFile().getAbsolutePath();
                            sendObject(ss[3] + "->down");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    });
                    downloadButton.addActionListener(e -> fileChooser.showOpenDialog(mainFrame));

                    var wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    wrapper.add(p);

                    // for images only
                    var wrapperInner = new JPanel(new GridLayout(0, 1));
                    if (ss[3].endsWith(".jpg") || ss[3].endsWith(".png") || ss[3].endsWith(".jpeg")) {
                        var previewButton = new JButton("Preview");
                        previewButton.addActionListener(e -> {
                            var dialog = new JDialog(mainFrame);
                            dialog.setLayout(new BorderLayout());
                            dialog.setLocationByPlatform(true);
                            try {
                                destinationPath = ".\\";
                                sendObject(ss[3] + "->down");
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            File file;
                            do {
                                file = new File(".\\" + ss[3]);
                                Utils.sleep(100.0);
                            } while (!file.exists());
                            var img = new ImageIcon(IconFactory.getImage(".\\" + ss[3], true));
                            var label = new JLabel(img, JLabel.CENTER);
                            dialog.add(label);
                            dialog.addWindowListener(new WindowAdapter() {
                                @Override
                                public void windowClosing(WindowEvent e) {
                                    //noinspection ResultOfMethodCallIgnored
                                    new File(".\\" + ss[3]).delete();
                                }
                            });
                            dialog.setResizable(false);
                            dialog.setVisible(true);
                            dialog.setSize(img.getIconWidth(), img.getIconHeight());
                            dialog.pack();
                        });
                        wrapperInner.add(previewButton);
                    }
                    wrapperInner.add(downloadButton);

                    wrapper.add(wrapperInner);
                    var mp = Objects.requireNonNull(getMessagePanel(text.substring(0, text.indexOf(":"))));
                    mp.getMessagesPanel().add(wrapper);
                    mp.revalidate();
                    if (!mp.isSelected())
                        mp.setUnreadMessageExistence(true);

                    revalidate();
                    wrapper.revalidate();
                }

                @Override
                public void useFileByteArray(byte[] fileBytes, String fileName) {
                    Utils.writeByteArrayToFile(destinationPath + "\\" + fileName, fileBytes);
                }
            };
        } catch (Exception e) {
            ExceptionHandler.handle(e, ExceptionHandler.PRINT_STACK_TRACE);
            Utils.sleep(1000.0);
            handleClient();
        }
    }

    // Major Panel
    private void handleNorthPanel() {
        setNorthPanel(northPanel);
    }

    // Updater Thread
    private void handleUpdater() {
        var l = new JLabel("", JLabel.LEFT);
        l.setOpaque(true);
        l.setBackground(Color.DARK_GRAY);
        l.setFont(new Font("serif", Font.ITALIC, 16));
        var ll = new JLabel("", JLabel.CENTER);
        ll.setOpaque(true);
        ll.setFont(new Font("serif", Font.BOLD, 18));
        new Updater(() -> {
            try {
                var factor = (float) Math.abs((Math.sin(TTManager.secondsAfterStart() / 20) % 0.3));
                l.setText(
                        "  network.network.packetsniffer.uniprojsniffer.packetsniffer.sniffer1.client id: " + client.getId() + "  <=>  " +
                        "host address: " + client.getSocket().getInetAddress().getHostAddress() + "  <=>  " +
                        "port: " + client.getSocket().getPort() + "  <=>  " +
                        "selected user: " + selectedUser + "  <=>  " +
                        "is connected to the network.network.packetsniffer.uniprojsniffer.packetsniffer.sniffer1.server: " + client.isConnected()
                );
                ll.setText("network.network.packetsniffer.uniprojsniffer.packetsniffer.sniffer1.client: " + nameOfClient + "   selected user: " + selectedUser);
                ll.setForeground(new Color(factor + 0.7f, factor, factor));
                l.setForeground(new Color(factor + 0.5f, 3 * factor + 0.1f, factor + 0.5f));
                l.setBackground(new Color(factor, factor / 2, factor / 6));

                for (var mp : messagePanels) {
                    JLabel label = mp.isGroup() ? getGroupLabel(mp.getAnotherClientName()) : getUserLabel(mp.getAnotherClientName());
                    assert label != null;
                    if (mp.isUnreadMessageExistence() && !mp.isSelected()) {
                        label.setFont(new Font("serif", Font.BOLD, 16));
                    } else {
                        label.setFont(new Font("serif", Font.PLAIN, 12));
                    }
                    label.revalidate();
                }
            } catch (Exception e) {
                ExceptionHandler.handle(e, ExceptionHandler.IGNORE);
            }
        }, true).setDelay(1000);
        northPanel.add(ll);
        southPanel.add(l);
    }

    // Major Panel
    private void handleSouthPanel() {
        setSouthPanel(southPanel);
    }

    // Major Panel
    private void handleLeftPanel() {
        leftPanel.setLayout(new BorderLayout());

        handleFindUserTextField();
        handleCreateGroupButton();

        setLeftPanel(leftPanel);
    }

    // Major Panel
    private void handleRightPanel() {
        handleOnlineUsersPanel();
        handleHistoryPanel();

        setRightPanel(rightPanel);
    }

    // Inside Left Panel
    private void handleFindUserTextField() {
        findUserTextField = new JTextField();
        findUserTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        findUserTextField.setBorder(IconFactory.textBubbleBorder(
                Color.DARK_GRAY, 3, 5, 0, true));

        findUserTextField = new JTextField("Filter");
        findUserTextField.setToolTipText("Filter (Search)");
        findUserTextField.setForeground(Color.GRAY);
        findUserTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                for (Component component : onlineUsersPanel.getComponents()) {
                    component.setVisible(((JLabel) component).getText().toLowerCase().
                            contains(findUserTextField.getText().toLowerCase()));

                    if (findUserTextField.getText().isEmpty())
                        component.setVisible(true);
                }

                onlineUsersPanel.revalidate();
                findUserTextField.repaint();
            }
        });

        findUserTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (findUserTextField.getText().equals("Filter")) {
                    findUserTextField.setText("");
                    findUserTextField.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (findUserTextField.getText().isEmpty()) {
                    findUserTextField.setText("Filter");
                    findUserTextField.setForeground(Color.GRAY);
                }
            }
        });

        leftPanel.add(findUserTextField, BorderLayout.NORTH);
    }

    // Inside Left Panel
    private MouseAdapter clickOnUserLabelAction() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Component component : onlineUsersPanel.getComponents())
                    if (!component.equals(e.getSource())) {
                        component.setBackground(Color.DARK_GRAY.darker());
                        component.setEnabled(true);
                        ((JLabel) component).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                    }

                var l = (JLabel) e.getSource();
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, Color.RED),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                selectedUser = l.getText().contains(":") ?
                        l.getText().substring(0, l.getText().indexOf(":")) : l.getText();
                contentPanel.removeAll();
                for (var mp : messagePanels)
                    mp.setSelected(false);

                if (l.getText().contains(":")) {
                    contentPanel.add(Objects.requireNonNull(getGroupPanel(selectedUser)), BorderLayout.CENTER);
                    Objects.requireNonNull(getGroupPanel(selectedUser)).setSelected(true);
                    Objects.requireNonNull(getGroupPanel(selectedUser)).setUnreadMessageExistence(false);
                    Objects.requireNonNull(getGroupPanel(selectedUser)).revalidate();
                } else {
                    contentPanel.add(Objects.requireNonNull(getMessagePanel(selectedUser)), BorderLayout.CENTER);
                    Objects.requireNonNull(getMessagePanel(selectedUser)).setSelected(true);
                    Objects.requireNonNull(getMessagePanel(selectedUser)).setUnreadMessageExistence(false);
                    Objects.requireNonNull(getMessagePanel(selectedUser)).revalidate();
                }
                contentPanel.revalidate();
                contentPanel.repaint();
                revalidate();
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ((JLabel) e.getSource()).setBackground(Color.DARK_GRAY);
                ((JLabel) e.getSource()).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, Color.RED),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8))
                );
            }

            @Override
            public void mouseExited(MouseEvent e) {
                MessagePanel mp = getMessagePanel(((JLabel) e.getSource()).getText());
                if (mp == null) {
                    mp = getGroupPanel(((JLabel) e.getSource()).getText() + ":");
                }
                if (mp == null)
                    return;
                if (!mp.isSelected()) {
                    ((JLabel) e.getSource()).setBackground(Color.DARK_GRAY.darker());
                    ((JLabel) e.getSource()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                }
            }
        };
    }

    // Inside Left Panel
    private void createNewUserLabel(String name) {
        if (name == null)
            return;
        if (name.equals(nameOfClient))
            name = "Server";
        if (getUserLabel(name) != null || name.equals("Unidentified") || name.equals("null"))
            return;
        var l = new JLabel(name, JLabel.LEFT);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8), null));
        l.setIcon(IconFactory.createCircleIcon(15, Color.GREEN));
        l.addMouseListener(clickOnUserLabelAction());
        var msgPanel = new MessagePanel(client, commandsHistoryPanel, name);
        messagePanels.add(msgPanel);
        usersLabel.add(l);
        if (name.contains(":"))
            msgPanel.setGroup(true);
        if (name.contains(":") && getGroupLabel(name) != null)
            return;
        if (getUserLabel(name) == null) {
            onlineUsersPanel.add(l);
            onlineUsersPanel.revalidate();
        }
    }

    // Inside left Panel
    private void handleOnlineUsersPanel() {
        onlineUsersPanel = new JPanel();
        onlineUsersPanel.setLayout(new BoxLayout(onlineUsersPanel, BoxLayout.Y_AXIS));

        leftPanel.add(onlineUsersPanel, BorderLayout.CENTER);
    }

    // Inside Left Panel
    private void handleCreateGroupButton() {
        JButton createGroupButton = new JButton("+");
        createGroupButton.setToolTipText("Create a new Group");
        createGroupButton.setPreferredSize(new Dimension(60, 40));

        createGroupButton.addActionListener(e -> {
            var dialog = new JDialog(mainFrame, "New Group");
            dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
            ArrayList<JCheckBox> checkBoxes = new ArrayList<>(usersLabel.size());
            for (var l : usersLabel) {
                if (l.getText().equals(nameOfClient) || l.getText().contains(",") || l.getText().equals("Server"))
                    continue;
                var checkBox = new JCheckBox(l.getText());
                dialog.add(checkBox);
                checkBoxes.add(checkBox);
            }
            var nameOfGroupTextField = new JTextField();
            nameOfGroupTextField.setOpaque(true);
            nameOfGroupTextField.setBackground(Color.DARK_GRAY);
            nameOfGroupTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            dialog.add(nameOfGroupTextField);
            var createButton = new JButton("Create new Group");
            createButton.addActionListener(ev -> {
                if (nameOfGroupTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame, "You should set a name for the group.");
                    return;
                }
                var sb = new StringBuilder(nameOfGroupTextField.getText()).append(":");
                for (var c : checkBoxes)
                    if (c.isSelected())
                        sb.append(c.getText()).append(",");
                if (sb.substring(sb.indexOf(":") + 1).length() == 0) {
                    JOptionPane.showMessageDialog(dialog, "You should choose at least one user.");
                    return;
                }
                var name = sb.substring(0, sb.length() - 1);
                if (getGroupLabel(name) != null) {
                    JOptionPane.showMessageDialog(mainFrame, "The name of this group already exist.");
                    return;
                }
                try {
                    client.sendObject(name + "->goro");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                createNewUserLabel(name);
                dialog.setVisible(false);
            });
            var wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
            wrapper.add(createButton);
            dialog.add(wrapper);
            dialog.setLocationRelativeTo(mainFrame);
            dialog.setSize(300, 400);
            dialog.setVisible(true);
        });

        var wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wrapper.add(createGroupButton);

        leftPanel.add(wrapper, BorderLayout.SOUTH);
    }

    // Inside left panel
    private JLabel getUserLabel(String nameOfClient) {
        for (var c : onlineUsersPanel.getComponents())
            if (c instanceof JLabel && ((JLabel) c).getText().equals(nameOfClient))
                return (JLabel) c;
        return null;
    }

    // Inside Left Panel
    private JLabel getGroupLabel(String groupInfo) {
        var nameOfGroup = groupInfo.substring(0, groupInfo.indexOf(":"));
        for (var c : onlineUsersPanel.getComponents())
            if (c instanceof JLabel) {
                var l = (JLabel) c;
                if (l.getText().contains(":"))
                    if (l.getText().substring(0, l.getText().indexOf(":")).equals(nameOfGroup))
                        return l;
            }
        return null;
    }

    // Inside Right Panel
    private void handleHistoryPanel() {
        commandsHistoryPanel = new JPanel();
        commandsHistoryPanel.setLayout(new BoxLayout(commandsHistoryPanel, BoxLayout.Y_AXIS));
        var sp = new JScrollPane(commandsHistoryPanel);
        sp.setAutoscrolls(true);
        sp.getVerticalScrollBar().setUnitIncrement(15);
        rightPanel.add(sp, BorderLayout.CENTER);
    }

    // central panel
    private MessagePanel getMessagePanel(String nameOfOtherClient) {
        if (nameOfOtherClient.contains(":"))
            nameOfOtherClient = nameOfOtherClient.substring(0, nameOfOtherClient.indexOf(":"));
        for (var mp : messagePanels)
            if (mp.getAnotherClientName().equals(nameOfOtherClient))
                return mp;
        return null;
    }

    private MessagePanel getGroupPanel(String groupName) {
        for (var mp : messagePanels)
            if (mp.isGroup() && mp.getAnotherClientName().substring(0, mp.getAnotherClientName().indexOf(":"))
                    .equals(groupName))
                return mp;
        return null;
    }
}
