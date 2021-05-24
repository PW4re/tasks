package ParsedDNSPacket;

public class ParsedRR {
    private final String name, rData;
    private final short type, clazz;
    private final char rdLength;
    private final long ttl;

    public ParsedRR(String name, short type, short clazz, long ttl, char rdLength, String rData) {
        this.name     = name;
        this.type     = type;
        this.clazz    = clazz;
        this.ttl      = ttl;
        this.rdLength = rdLength;
        this.rData    = rData;
    }

    public char getRdLength() { return rdLength; }

    public long getTtl() { return ttl; }

    public String getName() { return name; }

    public short getClazz() { return clazz; }

    public String get_rData() { return rData; }

    public short getType() { return type; }

    @Override
    public String toString() {
        return "ParsedRR{" +
                "name='" + name + '\'' +
                ", rData='" + rData + '\'' +
                ", type=" + type +
                ", clazz=" + clazz +
                ", rdLength=" + rdLength +
                ", ttl=" + ttl +
                '}';
    }
}
