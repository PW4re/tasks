import org.apache.commons.lang.ArrayUtils;
import parse_and_build.DNSPacketCrafter;
import parsed_dns_packet.ParsedDNSPacket;
import parsed_dns_packet.ParsedHeader;
import parsed_dns_packet.ParsedQuestionSection;
import parse_and_build.DNSPacketParser;
import parsed_dns_packet.ParsedRR;
import rr_field_codes.RRSemantics;
import rr_field_codes.RRType;
import storage.Record;
import storage.Storage;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class CachingDNSServer {
    private static final int SERVER_PORT = 53;
    private static InetAddress DNS_IP;

    private static Sender sender;
    private static Receiver receiver;
    private static final Storage storage = new Storage();

    // qname <- packets
    private static final Map<String, List<DatagramPacket>> openRequests = Collections.synchronizedMap(new HashMap<>());

    static  {
        try {
            DNS_IP = InetAddress.getByName("8.8.8.8");
        } catch (UnknownHostException e) {
            System.out.println("Работать не буду:((");
            DNS_IP = InetAddress.getLoopbackAddress();
        }
    }

    public static void main(String[] args) throws IOException {  // обязательно обработать
//        byte a = -0b10000000;
//        System.out.println(String.format("%8s", Integer.toBinaryString(-0b10000000 + 2)).replace(' ', '0'));
//        System.out.println(-0b10000000 + 2);

        System.out.println("My address: " + InetAddress.getLocalHost());

        try (DatagramSocket generalSocket = new DatagramSocket(SERVER_PORT, InetAddress.getLocalHost())) {
            receiver = new Receiver(generalSocket);
            sender = new Sender(generalSocket);
            while (true) {
                DatagramPacket pack = receiver.recv();
                ParsedDNSPacket parsedPack = DNSPacketParser.parse(pack.getData());
                if (!parsedPack.getHeader().getQR()) {  // определили, что это запрос от клиента
                    List<Record> answersFromStorage = findRRsInStorage(parsedPack, pack);
                    if (answersFromStorage.size() > 0) {
                        System.out.println("From RAM");
                        sender.sendTo(buildPacket(parsedPack, answersFromStorage), pack.getAddress(), pack.getPort());
                    }
                } else {
                    for (DatagramPacket clientPacket : findAllRequestsByAnswer(parsedPack)) {
                        sender.sendTo(pack.getData(), clientPacket.getAddress(), clientPacket.getPort());
                        setDataToStorage(DNSPacketParser.parse(clientPacket.getData()));
                        System.out.println("set data");
                    }
                }
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static byte[] buildPacket(ParsedDNSPacket request, List<Record> answers) throws IOException {
        List<ParsedRR> answerSection = answers.stream()
                .filter(r -> r.getSemantics() == RRSemantics.ANSWER)
                .map(Record::getRecord)
                .collect(Collectors.toList());
        List<ParsedRR> authoritySection = answers.stream()
                .filter(r -> r.getSemantics() == RRSemantics.AUTHORITY)
                .map(Record::getRecord)
                .collect(Collectors.toList());
        List<ParsedRR> additionalSection = answers.stream()
                .filter(r -> r.getSemantics() == RRSemantics.ADDITIONAL)
                .map(Record::getRecord)
                .collect(Collectors.toList());
        ParsedHeader newHeader = new ParsedHeader(
                request.getHeader().getId(),
                true,
                request.getHeader().getOpcode(),
                request.getHeader().getAA(),
                request.getHeader().getTC(),
                request.getHeader().getRD(),
                request.getHeader().getRA(),
                request.getHeader().get_rCode(),
                (char) request.getQuestion().size(),
                (char) answerSection.size(),
                (char) authoritySection.size(),
                (char) additionalSection.size()
        );
        ParsedDNSPacket pack = new ParsedDNSPacket(
                newHeader,
                request.getQuestion(),
                answerSection,
                authoritySection,
                additionalSection
        );
        byte[] packet = DNSPacketCrafter.craftAnswer(pack);

        System.out.println(DNSPacketParser.parse(packet));
        return packet;
    }

    private static List<Record> findRRsInStorage(ParsedDNSPacket request, DatagramPacket rawRequest)
            throws UnknownHostException {
        List<Record> result = new ArrayList<>();
        for (int i = 0; i < request.getQuestion().size(); i++){
            String qName = request.getQuestion().get(i).get_qName();
            char qType = request.getQuestion().get(i).get_qType();
            List<Record> answer = findAnswerInStorage(qName, RRType.getFromValue(qType));
            if (answer.size() > 0) result.addAll(answer);
            else {
                addRequestToOpens(rawRequest, request);
                sender.sendTo(rawRequest.getData(), DNS_IP, SERVER_PORT);
            }
        }

        return result;
    }

    private static List<Record> findAnswerInStorage(String qName, RRType qType) {
        switch (qType) {
            case A:
                return storage.getRRsByName(qName, RRType.A);
            case AAAA:
                return storage.getRRsByName(qName, RRType.AAAA);
            case NS:
                return storage.getRRsByName(qName, RRType.NS);
            case PTR:
                return storage.getRRsByIP(qName, RRType.PTR);
            default:
                return Collections.emptyList();
        }
    }

    private static void addRequestToOpens(DatagramPacket packet, ParsedDNSPacket parsedPacket) {
        for (int i = 0; i < parsedPacket.getQuestion().size(); i++) {
            String qName = parsedPacket.getQuestion().get(i).get_qName();
            if (!openRequests.containsKey(qName)) {
                openRequests.put(qName, new ArrayList<>());
                openRequests.get(qName).add(packet);
            } else openRequests.get(qName).add(packet);
        }
    }

    private static List<DatagramPacket> findAllRequestsByAnswer(ParsedDNSPacket parsedAnswer) {
        List<DatagramPacket> result = new ArrayList<>();
        for (ParsedQuestionSection question : parsedAnswer.getQuestion()) {
            for (String qName : openRequests.keySet()) {
                if (qName.equals(question.get_qName())) {
                    result.addAll(openRequests.get(qName));
                    openRequests.remove(qName);
                    //System.out.println(qName);
                }
            }
        }

        return result;
    }

    private static void setDataToStorage(ParsedDNSPacket parsedPack) {
        for (int i = 0; i < parsedPack.getAnswer().size(); i++) {
            storage.setDataForName(
                    parsedPack.getAnswer().get(i).getName(),
                    parsedPack.getAnswer().get(i).get_rData(),
                    RRType.getFromValue(parsedPack.getAnswer().get(i).getType()),
                    parsedPack.getAnswer().stream()
                            .map(rec -> new Record(rec, RRSemantics.ANSWER)).collect(Collectors.toList())
            );
        }

        for (int i = 0; i < parsedPack.getAuthority().size(); i++) {
            storage.setDataForName(
                    parsedPack.getAuthority().get(i).getName(),
                    parsedPack.getAuthority().get(i).get_rData(),
                    RRType.getFromValue(parsedPack.getAuthority().get(i).getType()),
                    parsedPack.getAuthority().stream()
                            .map(rec -> new Record(rec, RRSemantics.AUTHORITY)).collect(Collectors.toList())
            );
        }

        for (int i = 0; i < parsedPack.getAdditional().size(); i++) {
            storage.setDataForName(
                    parsedPack.getAdditional().get(i).getName(),
                    parsedPack.getAdditional().get(i).get_rData(),
                    RRType.getFromValue(parsedPack.getAdditional().get(i).getType()),
                    parsedPack.getAdditional().stream()
                            .map(rec -> new Record(rec, RRSemantics.ADDITIONAL)).collect(Collectors.toList())
            );
        }
    }

//    private static byte[] doSurvey(byte[] request) throws UnknownHostException {
//        sender.sendTo(request, InetAddress.getByName(DNS_IP), SERVER_PORT);
//        byte[] ourAnswer = receiver.recv().getData();
//        System.out.println(DNSPacketParser.parse(ourAnswer));
//
//        return ourAnswer;
//    }

    private static class Sender {
        private final DatagramSocket socket;

        private Sender(DatagramSocket socket) {
            this.socket = socket;
        }

        protected void sendTo(byte[] packet, InetAddress addr, int port) throws UnknownHostException {
            // Очень хотелось разрешать не рекурсивно, но для этого нужно писать парсинг SOA
            // packet[2] &= 0b1111111_0_11111111;  // RD := false
            DatagramPacket dataPack = new DatagramPacket(packet, packet.length, addr, port);
            dataPack.setAddress(addr);
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
