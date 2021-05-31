package network.messenger.runner;

import network.messenger.controller.Controller;

import javax.swing.*;

public class MessengerRunner {
    public static void run() {
        var frame = new Controller();
        SwingUtilities.invokeLater(frame);
    }
}
