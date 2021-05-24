import ParsedDNSPacket.ParsedDNSPacket;
import ParsedDNSPacket.ParsedHeader;
import ParsedDNSPacket.ParsedQuestionSection;
import ParsedDNSPacket.ParsedRR;
import RRFieldCodes.RRClass;
import RRFieldCodes.RRType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class DNSPacketParser {
    private static int currentIndex = 0;

    public static ParsedDNSPacket parse(byte[] packet) {
        ParsedHeader header = parseHeader(packet);
        currentIndex += 12;
        List<ParsedQuestionSection> question = parseQuestionPart(packet, header.getQdCount());
        List<ParsedRR> answer = parseRRs(packet, header.getAnCount());
        List<ParsedRR> authority = parseRRs(packet, header.getNsCount());
        List<ParsedRR> additional = parseRRs(packet, header.getArCount());
        currentIndex = 0; // сбрасываем для будущих запусков

        return new ParsedDNSPacket(header, question, answer, authority, additional);
    }

    private static ParsedHeader parseHeader(byte[] packet) {
        return new ParsedHeader(
                (char) ((char) (packet[0] << 8) + packet[1]),
                (packet[2] & -0b10000000) == -128,
                (byte) ((packet[2] & 0b01111111) >> 4),
                (packet[2] & 0b100) == 4,
                (packet[2] & 0b10) == 2,
                (packet[2] & 0b1) == 1,
                (packet[3] & -0b10000000) == -128,
                (byte) (packet[3] & 0b1111),
                (char) ((char) (packet[4] << 8) + packet[5]),
                (char) ((char) (packet[6] << 8) + packet[7]),
                (char) ((char) (packet[8] << 8) + packet[9]),
                (char) ((char) (packet[10] << 8) + packet[11])
        );
    }

    private static List<ParsedQuestionSection> parseQuestionPart(byte[] fragment, int qdCount) {
        List<ParsedQuestionSection> sections = new ArrayList<>();
        for (int _i = 0; _i < qdCount; _i++){
            sections.add(
                    new ParsedQuestionSection(
                            parseByQNameRules(fragment),
                            (char) ((char) (fragment[currentIndex++] << 8) +
                                    fragment[currentIndex++]),
                            (char) ((char) (fragment[currentIndex++] << 8) +
                                    fragment[currentIndex++])
                    )
            );
        }

        return sections;
    }

    private static String parseByQNameRules(byte[] fragment) {
        StringBuilder resBuilder = new StringBuilder();
        currentIndex = writeInStringBuilderAndGetIndex(resBuilder, fragment, currentIndex);

        return resBuilder.toString();
    }

    private static int writeInStringBuilderAndGetIndex(StringBuilder sb, byte[] fragment, int index) {
        byte length;
        while (fragment[index] != 0) {
            length = fragment[index];
            index++;
            byte[] bytes = Arrays.copyOfRange(fragment, index, index + length);
            index += length;
            sb.append(new String(bytes, StandardCharsets.US_ASCII)).append(".");  // Punycode maybe
        }
        index++;

        return index;
    }

    private static List<ParsedRR> parseRRs(byte[] fragment, int count){
        List<ParsedRR> sections = new ArrayList<>();
        for (int _i = 0; _i < count; _i++) {
            String name = parseNamePart(fragment);
            short type = (short) ((fragment[currentIndex] << 8) + fragment[++currentIndex]);
            short clazz = (short) ((fragment[++currentIndex] << 8) + fragment[++currentIndex]);
            long ttl = (fragment[++currentIndex] << 24) + (fragment[++currentIndex] << 16 )+
                    (fragment[++currentIndex] << 8) + fragment[++currentIndex];
            char rdLength = (char) ((fragment[++currentIndex] << 8) + fragment[++currentIndex]);
            String rData = new String(Arrays.copyOfRange(fragment, ++currentIndex, currentIndex + rdLength),
                    StandardCharsets.US_ASCII);

            sections.add(new ParsedRR(name, type, clazz, ttl, rdLength, rData));
        }

        return sections;
    }

    private static String parseNamePart(byte[] fragment) { // это для сжатия
        StringBuilder resBuilder = new StringBuilder();
        while (fragment[currentIndex] != 0 && !isNameFinish(fragment, currentIndex)) {
            if ((fragment[currentIndex] & -0b1000000) == -0b1000000) {  // нашли указатель
                var v1 = (fragment[currentIndex] & 0b00_111111) << 8;
                var v2 = fragment[++currentIndex];
                var ref = v1 + v2;
                writeInStringBuilderAndGetIndex(
                        resBuilder,
                        fragment,
                         ref);
                currentIndex++;
            } else {
                resBuilder.append(parseByQNameRules(fragment));
            }
        }
        //currentIndex++;

        return resBuilder.toString();
    }

    private static boolean isNameFinish(byte[] fragment, int index) {
        return fragment[index] == 0 && (index + 1 >= fragment.length || fragment[index + 1] >> 1 == 0);
    }

//    private static String parseRData(short type, short clazz) {
//        if (clazz != RRClass.IN.getValue()) System.out.println("Unexpected class of resource record");
//        if (type == (short) RRType.A.getValue()) {
//
//        } else if (type == (short) RRType.AAAA.getValue()) {
//
//        } else if (type == (short) RRType.NS.getValue()) {
//
//        } else if (type == (short) RRType.PTR.getValue()) {
//
//        }
//    }
}
