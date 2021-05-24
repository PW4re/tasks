import ParsedDNSPacket.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PacketCrafter {
//    public static byte[] craft(byte[] pack) {
//
//    }

//    private static byte[] craftHeader(ParsedHeader header) throws IOException {
//        OutputStream headersBytes = new ByteArrayOutputStream(12);
//        DataOutputStream stream = new DataOutputStream(headersBytes);
//        stream.writeChar(header.getId());
//
//
//    }
//
//    private static void fillSecondHeaderRow(DataOutputStream stream, ParsedHeader header) throws IOException {
//        stream.writeBoolean(header.getQR());
//        byte raw_opCode = header.getOpcode();
//        stream.writeBoolean((raw_opCode & 0b0000_1_000) == 0b0000_1_000);
//        stream.writeBoolean((raw_opCode & 0b00000_1_00) == 0b00000_1_00);
//        stream.writeBoolean((raw_opCode & 0b000000_1_0) == 0b000000_1_0);
//        stream.writeBoolean((raw_opCode & 0b0000000_1) == 0b0000000_1);
//        stream.writeBoolean(header.getAA());
//        stream.writeBoolean(header.getTC());
//        stream.writeBoolean(false);
//        stream.writeBoolean(header.getRA());
//        for (short _i = 0; _i < 3; _i++)
//            stream.writeBoolean(false);
//    }
}
