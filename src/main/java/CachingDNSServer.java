import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class CachingDNSServer {
    private static final int SERVER_PORT = 53;
    private static final String DNS_IP = "199.7.83.42";  // ICANN root server
    private static final byte[] THIS_IP = new byte[] { (byte) 192, (byte) 168, 1, 3 };


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {  // обязательно обработать
//        System.out.println("My address: " + InetAddress.getLocalHost());
//        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open();
//        server.bind(new InetSocketAddress(InetAddress.getLocalHost(), SERVER_PORT));
//        var a = server.accept();
//        ByteBuffer buff = ByteBuffer.allocate(512);
//        var chan = a.get();
//        System.out.println(chan.isOpen());
//        chan.write(buff);
//        System.out.println(Arrays.toString(buff.array()));

        try (DatagramSocket generalSocket = new DatagramSocket(SERVER_PORT, InetAddress.getLocalHost())) {
            Receiver r = new Receiver(generalSocket);
            Sender s = new Sender(DNS_IP, generalSocket);
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
            DatagramPacket dataPack = new DatagramPacket(packet, packet.length, InetAddress.getByName(DNS_IP), SERVER_PORT);
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
