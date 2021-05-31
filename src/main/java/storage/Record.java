package storage;

import parsed_dns_packet.ParsedRR;
import rr_field_codes.RRSemantics;

public class Record {
    private final RRSemantics semantics;
    private final ParsedRR record;

    public Record(ParsedRR record, RRSemantics semantics) {
        this.record    = record;
        this.semantics = semantics;
    }

    public RRSemantics getSemantics() { return semantics; }

    public ParsedRR getRecord() { return record; }
}
