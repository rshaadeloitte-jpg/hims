package com.hims.interop.application;

import com.hims.interop.application.exceptions.InvalidRequestException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HL7 v2.x minimal-segment parser (MIG-055). Pinned to the segments we actually
 * need at this slice:
 *   ADT^A01/A04/A08  -> MSH + EVN + PID + [PV1]   (register: register / update)
 *   ADT^A03          -> MSH + EVN + PID + [PV1]   (discharge)
 *   ORM^O01          -> MSH + PID + ORC + [OBR]  (order)
 *   ORU^R01          -> MSH + PID + OBR + [OBX]  (result)
 *
 * The parser is INTENTIONALLY MINIMAL: it does not try to be a full HAPI
 * parser. The deliverable is a stable, unit-testable contract for the
 * segments we ship; downstream services parse the few fields they need
 * from the Map representation and the row in t_inbound_his is the durability
 * boundary.
 *
 * Encoding: HL7 v2 standard delimiters from MSH-1/MSH-2 (e.g. |^~\&amp;). Field
 * separator is the first character of MSH-1; component separator is the first
 * character of MSH-2 (typically ^); repetition is ~; escape is \.
 *
 * The parser is PURE — no Spring, no persistence; easy to unit-test in
 * isolation.
 */
public final class Hl7V2Parser {

    private Hl7V2Parser() {}

    /** Parsed HL7 envelope. */
    public record Hl7Message(
            String messageType,         // "ADT", "ORM", "ORU"
            String triggerEvent,        // "A01", "A04", "A08", "A03", "O01", "R01"
            String controlId,           // MSH-10
            String sendingApp,          // MSH-3
            String sendingFacility,     // MSH-4
            String receivingApp,        // MSH-5
            String receivingFacility,   // MSH-6
            String timestamp,           // MSH-7 (raw; v2 is typically YYYYMMDDHHMMSS)
            String processingId,        // MSH-11 (P=production, T=training, D=debug)
            Map<String, String> evn,    // event-type segment (ADT only)
            Map<String, String> pid,    // patient identification
            Map<String, String> pv1,    // visit (visit/encounter; ADT only)
            Map<String, String> orc,    // common order (ORM only)
            Map<String, String> obr,    // observation request (ORM/ORU)
            Map<String, String> obx,    // observation/result (ORU only)
            List<String> rawSegments,   // verbatim segments for downstream
            char fieldSep,
            char componentSep,
            char repetitionSep,
            char escapeChar) {
        public String fullType() { return messageType + "^" + triggerEvent; }
    }

    /** Parse a raw HL7 v2 message. Throws InvalidRequestException on malformed input. */
    public static Hl7Message parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("BEXA003", "raw HL7 payload is required");
        }
        // HL7 uses CR as segment terminator; trim trailing CR
        String[] segments = raw.split("\\r?\n|\r");
        if (segments.length == 0 || !segments[0].startsWith("MSH")) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "HL7 payload does not start with MSH segment");
        }
        char fieldSep = segments[0].charAt(3);
        String encChars = segments[0].length() > 4 ? segments[0].substring(4, Math.min(8, segments[0].length())) : "^~\\&";
        char componentSep = encChars.isEmpty() ? '^' : encChars.charAt(0);
        char repetitionSep = encChars.length() >= 2 ? encChars.charAt(1) : '~';
        char escapeChar = encChars.length() >= 3 ? encChars.charAt(2) : '\\';
        // subcomponent separator is '&' per the HL7 standard; not all senders use it.
        char subcomponentSep = encChars.length() >= 4 ? encChars.charAt(3) : '&';

        Map<String, String> msh = splitSegment(segments[0], fieldSep, componentSep);
        if (!"MSH".equals(msh.get("0"))) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "first segment is not MSH");
        }
        String msh9 = msh.get("9");
        if (msh9 == null || !msh9.contains("^")) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "MSH-9 (message type) is required, e.g. ADT^A01");
        }
        String[] typeParts = msh9.split("\\^", -1);
        if (typeParts.length < 2) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "MSH-9 must be <messageType>^<triggerEvent>");
        }
        String messageType = typeParts[0];
        String triggerEvent = typeParts[1];

        Map<String, String> evn = new LinkedHashMap<>();
        Map<String, String> pid = new LinkedHashMap<>();
        Map<String, String> pv1 = new LinkedHashMap<>();
        Map<String, String> orc = new LinkedHashMap<>();
        Map<String, String> obr = new LinkedHashMap<>();
        Map<String, String> obx = new LinkedHashMap<>();
        List<String> rawSegments = new ArrayList<>();
        rawSegments.add(segments[0]);

        for (int i = 1; i < segments.length; i++) {
            String seg = segments[i];
            if (seg.isBlank()) continue;
            rawSegments.add(seg);
            // MSH is special: MSH-1 is the field separator itself
            Map<String, String> parsed = splitSegment(seg, fieldSep, componentSep);
            String segId = parsed.get("0");
            switch (segId) {
                case "EVN" -> evn.putAll(parsed);
                case "PID" -> pid.putAll(parsed);
                case "PV1" -> pv1.putAll(parsed);
                case "ORC" -> orc.putAll(parsed);
                case "OBR" -> obr.putAll(parsed);
                case "OBX" -> obx.putAll(parsed);
                // MSH already handled; others are captured in rawSegments
                default -> { /* no-op; preserve verbatim for downstream */ }
            }
        }

        return new Hl7Message(
                messageType, triggerEvent,
                msh.get("10"),
                msh.get("3"), msh.get("4"), msh.get("5"), msh.get("6"),
                msh.get("7"), msh.get("11"),
                evn, pid, pv1, orc, obr, obx, rawSegments,
                fieldSep, componentSep, repetitionSep, escapeChar);
    }

    /**
     * Split a single HL7 segment into a Map keyed by field index (as String for
     * stable iteration order). Field indices for MSH are off by one: the first
     * field after the segment name is MSH-2 (because MSH-1 is the field separator
     * itself). All other segments use 1-based indexing starting at the segment
     * name. We re-key MSH here so MSH-2 ends up at key "2" etc.
     */
    private static Map<String, String> splitSegment(String seg, char fieldSep, char componentSep) {
        Map<String, String> out = new LinkedHashMap<>();
        if (seg == null || seg.isEmpty()) return out;
        // We don't use String.split with a regex char to keep delimiter handling
        // explicit (HL7 separators can include backslashes and pipes).
        List<String> fields = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < seg.length(); i++) {
            if (seg.charAt(i) == fieldSep) {
                fields.add(seg.substring(start, i));
                start = i + 1;
            }
        }
        fields.add(seg.substring(start));
        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i);
            String key;
            if (i == 0) {
                key = "0";  // segment name
            } else {
                // MSH-1 is the field separator itself; everything after shifts by 1.
                // For non-MSH segments, field 1 is right after the segment name.
                if ("MSH".equals(fields.get(0))) {
                    key = String.valueOf(i);  // 1..n map to MSH-2..MSH-(n+1) per HL7 spec
                } else {
                    key = String.valueOf(i);
                }
            }
            out.put(key, f);
        }
        return out;
    }

    /** Validate that the parsed message carries the minimum segments for its type. */
    public static void validateMessageType(String raw) {
        Hl7Message m = parse(raw);
        switch (m.messageType()) {
            case "ADT" -> {
                if (m.pid().isEmpty()) {
                    throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                            "ADT messages must carry a PID segment");
                }
            }
            case "ORM" -> {
                if (m.orc().isEmpty() && m.obr().isEmpty()) {
                    throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                            "ORM messages must carry an ORC or OBR segment");
                }
            }
            case "ORU" -> {
                if (m.obr().isEmpty() || m.obx().isEmpty()) {
                    throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                            "ORU messages must carry OBR + OBX segments");
                }
            }
            default -> throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "Unsupported HL7 messageType: " + m.messageType());
        }
    }
}
