package network.messenger.controller;

import network.messenger.messengergui.MessengerFrame;

public class Controller implements Runnable {
    private MessengerFrame messengerFrame;

    public Controller() {
        messengerFrame = new MessengerFrame();


    }

    @Override
    public void run() {
        messengerFrame.setVisible(true);
    }
}
