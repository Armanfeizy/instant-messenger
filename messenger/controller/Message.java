package network.messenger.controller;

import network.socketserver.client.AbstractClient;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private byte[] data;

    public Message(Object msg) throws IOException {
        if (msg instanceof File)
            data = Utils.convertFileToByteArray((File) msg);
        else
            data = Utils.serializeObject(msg);
    }

    public Message() throws IOException {
        this(null);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(Object newMsg) throws IOException {
        this.data = Utils.serializeObject(newMsg);
    }

    public byte[][] breakIntoParts(int numOfParts)  {
        if (numOfParts > 127)
            numOfParts = 127;
        if (numOfParts <= 0) {
            var res = new byte[1][data.length + 1];
            System.arraycopy(data, 0, res[0], 1, data.length);
            return res;
        }
        int partLen = data.length / numOfParts;
        int lastPartLen = data.length - numOfParts * partLen;
        byte[][] res = new byte[(lastPartLen != 0 ? 1 : 0) + numOfParts][];
        for (byte i = 0; i < numOfParts; i++) {
            byte[] dataSection = new byte[1 + partLen];
            dataSection[0] = i;
            System.arraycopy(data, i * partLen, dataSection, 1, partLen);
            res[i] = dataSection;
        }
        if (lastPartLen != 0) {
            res[numOfParts] = new byte[1 + lastPartLen];
            res[numOfParts][0] = (byte) numOfParts;
            System.arraycopy(data, numOfParts * partLen, res[numOfParts], 1, lastPartLen);
        }
        return res;
    }

    public byte[][] breakIntoParts() {
        return breakIntoParts(data.length / 127);
    }

    public void sendTo(AbstractClient client) {
        try {
            for (var p : breakIntoParts())
                client.sendObject(p);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static byte[] receiveFrom(AbstractClient client, int numOfParts) {
        byte[] fileBytes;
        try {
            int fileSize = 0;
            byte[][] parts = new byte[numOfParts][];
            for (int i = 0; i < numOfParts; i++) {
                var obj = client.readObject();
                if (obj instanceof String) {
                    System.err.println("FF:==>    " + obj);
                    continue;
                }
                var p = (byte[]) obj;
                parts[p[0]] = Arrays.copyOfRange(p, 1, p.length);
                fileSize += p.length - 1;
            }
            int accumulator = 0;
            fileBytes = new byte[fileSize];
            for (var p : parts) {
                System.arraycopy(p, 0, fileBytes, accumulator, p.length);
                accumulator += p.length;
            }
        } catch (Exception e) {
            System.err.println("Problem in receiving file.");
            e.printStackTrace();
            return null;
        }
        return fileBytes;
    }
}
