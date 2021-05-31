package storage;

import rr_field_codes.RRType;

import java.util.*;

public class Storage {
    private final Map<String, List<String>> nameWithIP;  // domain name <- IP
    private final Map<String, RRsByType> ipWithRecords;  // IP <- RR

    public Storage() {  // Интересно, что будет с не полностью разрешенными доменными именами
        nameWithIP = new HashMap<>();
        ipWithRecords = new HashMap<>();
    }

    public void setDataForName(String name, String ip, RRType type, List<Record> data) {
        if (nameWithIP.containsKey(name)){
            if (!nameWithIP.get(name).contains(ip))
                ipWithRecords.put(
                        ip,
                        new RRsByType(
                                new ArrayList<>(Collections.emptyList()),
                                new ArrayList<>(Collections.emptyList()),
                                new ArrayList<>(Collections.emptyList()),
                                new ArrayList<>(Collections.emptyList())
                        )
                );
            nameWithIP.get(name).add(ip);
        } else {
            nameWithIP.put(name, new ArrayList<>(Collections.singletonList(ip)));
            ipWithRecords.put(
                    ip,
                    new RRsByType(
                            new ArrayList<>(Collections.emptyList()),
                            new ArrayList<>(Collections.emptyList()),
                            new ArrayList<>(Collections.emptyList()),
                            new ArrayList<>(Collections.emptyList())
                    )
            );
        }
        setDataForIP(ip, data, type);
    }

    private void setDataForIP(String ip, List<Record> data, RRType type) {
        if (data == null) return;
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

    public List<Record> getRRsByName(String name, RRType type) {
        if (!nameWithIP.containsKey(name)) return new ArrayList<>(Collections.emptyList());
        List<Record> result = new ArrayList<>();
        for (String ip : nameWithIP.get(name)) {
                List<Record> records = getRRsByIP(ip, type);
            assert records != null;
            if (records.size() == 0) continue;
                result.addAll(records);
            }

        return result;
    }

    public List<Record> getRRsByIP(String ip, RRType type) {
        if (!ipWithRecords.containsKey(ip)) return new ArrayList<>(Collections.emptyList());
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
         return new ArrayList<>(Collections.emptyList());
    }

    private static class RRsByType {
        private final List<Record> typeA;
        private final List<Record> typeAAAA;
        private final List<Record> typeNS;
        private final List<Record> typePTR;

        private RRsByType(List<Record> ofA, List<Record> ofAAAA,
                          List<Record> ofNS, List<Record> ofPTR) {
            typeA = ofA;
            typeAAAA = ofAAAA;
            typeNS = ofNS;
            typePTR = ofPTR;
        }
    }
}
