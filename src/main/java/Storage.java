import ParsedDNSPacket.ParsedRR;
import RRFieldCodes.RRType;

import java.util.*;

public class Storage {
    private final Map<String, List<String>> nameWithIP;  // domain name <- IP
    private final Map<String, RRsByType> ipWithRecords;  // IP <- RR

    public Storage() {  // Интересно, что будет с не полностью разрешенными доменными именами
        nameWithIP = new HashMap<>();
        ipWithRecords = new HashMap<>();
    }

    public void setDataForName(String name, String ip, RRType type, List<ParsedRR> data) {
        if (nameWithIP.containsKey(name)){
            nameWithIP.get(name).add(ip);
        } else {
            nameWithIP.put(name, Collections.singletonList(ip));
            ipWithRecords.put(ip, new RRsByType(Collections.emptyList(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));
        }
        setDataForIP(ip, data, type);
    }

    private void setDataForIP(String ip, List<ParsedRR> data, RRType type) {
        switch (type) {
            case A:
                ipWithRecords.get(ip).typeA.addAll(data);
                break;
            case AAAA:
                ipWithRecords.get(ip).typeAAAA.addAll(data);
                break;
            case NS:
                ipWithRecords.get(ip).typeNS.addAll(data);
                break;
            case PTR:
                ipWithRecords.get(ip).typePTR.addAll(data);
                break;
            default:
                System.out.println("Unexpected type");
        }
    }

    public List<ParsedRR> getRRsByName(String name, RRType type) {
        if (!nameWithIP.containsKey(name)) return Collections.emptyList();
        List<ParsedRR> result = new ArrayList<>();
        for (String ip : nameWithIP.get(name)) {
                List<ParsedRR> records = getRRsByIP(ip, type);
            assert records != null;
            if (records.size() == 0) continue;
                result.addAll(records);
            }

        return result;
    }

    public List<ParsedRR> getRRsByIP(String ip, RRType type) {
        if (!ipWithRecords.containsKey(ip)) return Collections.emptyList();
         switch (type) {
             case A:
                 return ipWithRecords.get(ip).typeA;
             case AAAA:
                 return ipWithRecords.get(ip).typeAAAA;
             case NS:
                 return ipWithRecords.get(ip).typeNS;
             case PTR:
                 return ipWithRecords.get(ip).typePTR;
             default:
                 // error mb?
         }
         return Collections.emptyList();
    }

    private static class RRsByType {
        private final List<ParsedRR> typeA;
        private final List<ParsedRR> typeAAAA;
        private final List<ParsedRR> typeNS;
        private final List<ParsedRR> typePTR;

        private RRsByType(List<ParsedRR> ofA, List<ParsedRR> ofAAAA,
                          List<ParsedRR> ofNS, List<ParsedRR> ofPTR) {
            typeA = ofA;
            typeAAAA = ofAAAA;
            typeNS = ofNS;
            typePTR = ofPTR;
        }
    }
}
