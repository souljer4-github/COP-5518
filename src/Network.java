import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class Network {
    private static final int BUFFER_SIZE = 54;
    private DatagramSocket socket;
    private int lostPercent;
    private int delayedPercent;
    private int errorPercent;
    private Random random = new Random();

    public Network(int port, int lostPercent, int delayedPercent, int errorPercent) {
        this.lostPercent = lostPercent;
        this.delayedPercent = delayedPercent;
        this.errorPercent = errorPercent;

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            System.err.println("Unable to create and bind socket: " + ex.getMessage());
        }
    }

    public void run() {
        while (true) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("Packet received: " + new String(packet.getData(), 0, packet.getLength()));

                if (simulateLoss()) {
                    System.out.println("Packet lost");
                    continue;
                }

                if (simulateDelay()) {
                    System.out.println("Packet delayed");
                    new Thread(new DelayedSender(packet)).start();
                } else {
                    if (simulateError()) {
                        System.out.println("Packet corrupted");
                        corruptPacket(packet);
                    }
                    forwardPacket(packet);
                }

            } catch (IOException ex) {
                System.err.println("Unable to receive message: " + ex.getMessage());
            }
        }
    }

    private boolean simulateLoss() {
        return random.nextInt(100) < lostPercent;
    }

    private boolean simulateDelay() {
        return random.nextInt(100) < delayedPercent;
    }

    private boolean simulateError() {
        return random.nextInt(100) < errorPercent;
    }

    private void corruptPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        data[0] = (byte) ~data[0]; // Corrupt the first byte (for example)
    }

    private void forwardPacket(DatagramPacket packet) {
        try {
            InetAddress address = InetAddress.getByName("127.0.0.1");
            int port = packet.getPort() == 5000 ? 5001 : 5000; // Toggle port between sender and receiver

            DatagramPacket newPacket = new DatagramPacket(packet.getData(), packet.getLength(), address, port);
            socket.send(newPacket);
            System.out.println("Packet forwarded: " + new String(packet.getData(), 0, packet.getLength()) + " to " + address.getHostAddress() + ":" + port);
        } catch (IOException ex) {
            System.err.println("Unable to forward packet: " + ex.getMessage());
        }
    }

    private class DelayedSender implements Runnable {
        private DatagramPacket packet;

        public DelayedSender(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1500 + random.nextInt(500)); // Delay between 1.5 and 2 seconds
                forwardPacket(packet);
            } catch (InterruptedException ex) {
                System.err.println("Thread interrupted: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: Network <port> <lostPercent> <delayedPercent> <errorPercent>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int lostPercent = Integer.parseInt(args[1]);
        int delayedPercent = Integer.parseInt(args[2]);
        int errorPercent = Integer.parseInt(args[3]);

        Network network = new Network(port, lostPercent, delayedPercent, errorPercent);
        network.run();
    }
}
