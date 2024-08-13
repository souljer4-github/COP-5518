import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Sender {
    private static final int BUFFER_SIZE = 10; // Transport header + payload = 10 bytes
    private DatagramSocket socket;
    private int sequenceNumber = 0;
    private String serverAddress;
    private int serverPort;

    public Sender(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public int createSocket() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(2000); // Set timeout for 2 seconds
        } catch (SocketException ex) {
            System.err.println("Unable to create and bind socket: " + ex.toString());
            return -1;
        }
        return 0;
    }

    public void send(String message) {
        int messageLength = message.length();
        int totalSegments = (int) Math.ceil((double) messageLength / (BUFFER_SIZE - 1)); // -1 for sequence number

        for (int i = 0; i < totalSegments; i++) {
            int start = i * (BUFFER_SIZE - 1);
            int end = Math.min(start + (BUFFER_SIZE - 1), messageLength);
            String segment = message.substring(start, end);
            String packet = sequenceNumber + segment;
            boolean ackReceived = false;

            while (!ackReceived) {
                System.out.println("Sending packet: " + packet);
                sendPacket(packet);
                try {
                    String ack = receiveAck();
                    if (ack.equals(String.valueOf(sequenceNumber))) {
                        System.out.println("ACK received: " + ack);
                        ackReceived = true;
                        sequenceNumber = 1 - sequenceNumber; // Toggle between 0 and 1
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout, resending packet...");
                } catch (IOException e) {
                    System.err.println("Error receiving ACK: " + e.toString());
                }
            }
        }
    }

    private void sendPacket(String packet) {
        try {
            byte[] buffer = packet.getBytes();
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, address, serverPort);
            socket.send(datagramPacket);
        } catch (IOException ex) {
            System.err.println("Unable to send message: " + ex.toString());
        }
    }

    private String receiveAck() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagramPacket);
        return new String(buffer, 0, datagramPacket.getLength()).trim();
    }

    public void closeSocket() {
        socket.close();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Sender <serverAddress> <serverPort>");
            return;
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Sender sender = new Sender(serverAddress, serverPort);
        if (sender.createSocket() < 0) {
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a message: ");
        String message = scanner.nextLine();
        sender.send(message);
        sender.closeSocket();
    }
}
