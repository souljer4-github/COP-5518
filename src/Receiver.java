import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Receiver {
    private static final int BUFFER_SIZE = 10;
    private DatagramSocket socket;
    private int expectedSequenceNumber = 0;
    private StringBuilder receivedMessage = new StringBuilder();

    public Receiver(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            System.err.println("Unable to create and bind socket: " + ex.getMessage());
        }
    }

    public void receive() {
        while (true) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Packet received: " + data);
                int sequenceNumber = Character.getNumericValue(data.charAt(0));
                String messageSegment = data.substring(1);

                if (sequenceNumber == expectedSequenceNumber) {
                    receivedMessage.append(messageSegment);
                    expectedSequenceNumber = 1 - expectedSequenceNumber; // Toggle between 0 and 1
                    System.out.println("Message segment received: " + messageSegment);
                }

                sendAck(packet.getAddress(), packet.getPort(), sequenceNumber);

                // Check for end of message (could use a special end-of-message marker or length)
                // For simplicity, assume message is ended by sender
                if (messageSegment.endsWith(".")) { // Assuming '.' is the end of message marker
                    System.out.println("Complete message received: " + receivedMessage.toString());
                    receivedMessage.setLength(0); // Clear the buffer
                }

            } catch (IOException ex) {
                System.err.println("Unable to receive message: " + ex.getMessage());
            }
        }
    }

    private void sendAck(InetAddress address, int port, int sequenceNumber) {
        try {
            String ack = String.valueOf(sequenceNumber);
            byte[] buffer = ack.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(ackPacket);
            System.out.println("ACK sent: " + ack + " to " + address.getHostAddress() + ":" + port);
        } catch (IOException ex) {
            System.err.println("Unable to send ACK: " + ex.getMessage());
        }
    }

    public void closeSocket() {
        socket.close();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: Receiver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Receiver receiver = new Receiver(port);
        receiver.receive();
    }
}
