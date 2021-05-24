import ParsedDNSPacket.ParsedDNSPacket;

import java.io.IOException;
import java.net.*;

public class CachingDNSServer {
    private static final int SERVER_PORT = 53;


    public static void main(String[] args) throws UnknownHostException {  // обязательно обработать
        System.out.println(InetAddress.getLocalHost());
        try (DatagramSocket generalSocket = new DatagramSocket(SERVER_PORT, InetAddress.getLocalHost())) {
            Receiver r = new Receiver(generalSocket);
            Sender s = new Sender("8.8.8.8", generalSocket);
            while (true) {
                var pack = r.recv();
                s.send(pack.getData());
                System.out.println(pack.getAddress().getHostAddress());
                System.out.println(DNSPacketParser.parse(pack.getData()));
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }


    private static class Sender {
        private final String defaultDnsIp;
        private final DatagramSocket socket;

        private Sender(String ip, DatagramSocket socket) {
            defaultDnsIp = ip;
            this.socket = socket;
        }

        protected void send(byte[] packet) throws UnknownHostException {
            // packet[2] &= 0b1111111_0_11111111;  // RD := false
            DatagramPacket dataPack = new DatagramPacket(packet, packet.length, InetAddress.getByName("8.8.8.8"), SERVER_PORT);
            dataPack.setAddress(InetAddress.getByName(defaultDnsIp));
            try {
                socket.send(dataPack);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Receiver {
        private final byte[] buffer;
        private final DatagramSocket socket;

        protected Receiver(DatagramSocket socket) {
            buffer = new byte[512];
            this.socket = socket;
        }

        protected DatagramPacket recv() {
            DatagramPacket pack = null;
            try {
                pack = new DatagramPacket(buffer, buffer.length);
                socket.receive(pack);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return pack;
        }
    }
}
