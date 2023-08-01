import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class DNSMessage {
    private final byte[] response;
    private byte[] ID;
    private final byte[] questionAndHeaderBytes;
    private final byte[] answerSectionBytes;
    private boolean QR, AA, TC, RD, RA;
    private int RCode, QDCount, ANCount, NSCount, ARCount;
    private DNSRecord[] answerRecords;
    private DNSRecord[] authorityRecords;
    private DNSRecord[] additionalRecords;
    private QueryType queryType = QueryType.A;
    private boolean noRecords = false;
    private String queryDomainName;
    private final int MAX_DNS_PACKET_SIZE = 512;

    public DNSMessage(byte[] messageBytes) {
        // contructor DNSMessage for DNS request
        this.response = messageBytes;
        this.parseHeader();
        // parse Question section
        int startOfAnswerIndex = this.parseQuestion();
        // now offset at end of question section and start of answer section
        // deep copy question section into class questionBytes
        this.questionAndHeaderBytes = new byte[startOfAnswerIndex];
        System.arraycopy(response, 0, this.questionAndHeaderBytes, 0, startOfAnswerIndex);
        // deep copy answer section into class answerBytes
        this.answerSectionBytes = new byte[this.response.length-startOfAnswerIndex];
        System.arraycopy(response, startOfAnswerIndex, this.answerSectionBytes, 
            0, this.response.length-startOfAnswerIndex);
    }

    public DNSMessage(byte[] messageBytes, int requestSize) {
        // construct the DNSMessage from Response Message Bytes
        // not throwing any exception
        this.response = messageBytes;

        this.parseHeader();
        // parse Question section
        int startOfAnswerIndex = this.parseQuestion();
        // now offset at end of question section and start of answer section
        // deep copy question section into class questionBytes
        this.questionAndHeaderBytes = new byte[startOfAnswerIndex];
        System.arraycopy(response, 0, this.questionAndHeaderBytes, 0, startOfAnswerIndex);
        // deep copy answer section into class answerBytes
        this.answerSectionBytes = new byte[this.response.length-startOfAnswerIndex];
        System.arraycopy(response, startOfAnswerIndex, this.answerSectionBytes, 
            0, this.response.length-startOfAnswerIndex);


        answerRecords = new DNSRecord[ANCount];
        int offSet = requestSize;
        for (int i = 0; i < ANCount && offSet < MAX_DNS_PACKET_SIZE && offSet > 0; i++) {
            answerRecords[i] = this.parseAnswer(offSet);
            offSet += answerRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE || offSet < 0) {
            this.TC = true;
            return;
        }

        this.authorityRecords = new DNSRecord[NSCount];
        for (int i = 0; i < NSCount && offSet < MAX_DNS_PACKET_SIZE; i++) {
            authorityRecords[i] = this.parseAnswer(offSet);
            offSet += authorityRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE) {
            this.TC = true;
            return;
        }

        additionalRecords = new DNSRecord[ARCount];
        for (int i = 0; i < ARCount && offSet < MAX_DNS_PACKET_SIZE; i++) {
            additionalRecords[i] = this.parseAnswer(offSet);
            offSet += additionalRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE) {
            this.TC = true;
        }
    }

    public DNSMessage(byte[] messageBytes, int requestSize, QueryType queryType) {
        // construct the DNSMessage from Response Message Bytes
        // throwing exception if query type is not response or response is not the query
        // type
        this.response = messageBytes;
        this.queryType = queryType;

        this.parseHeader();
        // parse Question section
        int startOfAnswerIndex = this.parseQuestion();
        // now offset at end of question section and start of answer section
        // deep copy question section into class questionBytes
        this.questionAndHeaderBytes = new byte[startOfAnswerIndex];
        System.arraycopy(response, 0, this.questionAndHeaderBytes, 0, startOfAnswerIndex);
        // deep copy answer section into class answerBytes
        this.answerSectionBytes = new byte[this.response.length-startOfAnswerIndex];
        System.arraycopy(response, startOfAnswerIndex, this.answerSectionBytes, 
            0, this.response.length-startOfAnswerIndex);


        answerRecords = new DNSRecord[ANCount];
        int offSet = requestSize;
        for (int i = 0; i < ANCount && offSet < MAX_DNS_PACKET_SIZE; i++) {
            answerRecords[i] = this.parseAnswer(offSet);
            offSet += answerRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE) {
            this.TC = true;
            return;
        }


        this.authorityRecords = new DNSRecord[NSCount];
        for (int i = 0; i < NSCount && offSet < MAX_DNS_PACKET_SIZE; i++) {
            authorityRecords[i] = this.parseAnswer(offSet);
            offSet += authorityRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE) {
            this.TC = true;
            return;
        }

        additionalRecords = new DNSRecord[ARCount];
        for (int i = 0; i < ARCount && offSet < MAX_DNS_PACKET_SIZE; i++) {
            additionalRecords[i] = this.parseAnswer(offSet);
            offSet += additionalRecords[i].getByteLength();
        }
        if (offSet >= MAX_DNS_PACKET_SIZE) {
            this.TC = true;
        }
        try {
            this.checkRCodeForErrors();
        } catch (NameErrorException e) {
            noRecords = true;
        }

    }

    public void outputResponse() throws NullPointerException {

        switch (this.RCode) {
            case 0:
                // No error
                break;
            case 1:
                System.out.println("Format error: the name server was unable to interpret the query");
                return;
            case 2:
                System.out.println(
                        "Server failure: the name server was unable to process this query due to a problem with the name server");
                return;
            case 3:
                System.out.println(
                        "Name error: domain name in the query does not exist.");
                return;
            case 4:
                System.out.println(
                        "Not implemented: the name server does not support the requested kind of query");
                return;
            case 5:
                System.out.println(
                        "Refused: the name server refuses to perform the requested operation for policy reasons");
                return;
        }

        if (this.ANCount == 0 || noRecords) {
            System.out.println("No " + this.queryType + " Record found.");
            return;
        }

        System.out.println("***Answer Section (" + this.ANCount + " Answer Records)***");

        for (DNSRecord r : answerRecords) {
            r.outputRecord();
        }

        System.out.println();

        if (this.NSCount > 0) {
            System.out.println("***Authroity Section (" + this.NSCount + " Authority Records)***");
            for (DNSRecord r : authorityRecords) {
                r.outputRecord();
            }
        }

        System.out.println();
        if (this.ARCount > 0) {
            System.out.println("***Additional Records Section***");
            for (DNSRecord r : additionalRecords) {
                r.outputRecord();
            }
        }
    }

    public void checkRCodeForErrors() {
        switch (this.RCode) {
            case 0:
                // No error
                break;
            case 1:
                throw new FormatErrorException("Format error: the name server was unable to interpret the query");
            case 2:
                throw new ServerFailureException(
                        "Server failure: the name server was unable to process this query due to a problem with the name server");
            case 3:
                throw new NameErrorException(
                        "Name error: domain name in the query does not exist.");
            case 4:
                throw new NotImplementedException(
                        "Not implemented: the name server does not support the requested kind of query");
            case 5:
                throw new RefusedException(
                        "Refused: the name server refuses to perform the requested operation for policy reasons");
        }
    }

    private void parseHeader() {
        // ID
        byte[] ID = new byte[2];
        ID[0] = response[0];
        ID[1] = response[1];
        this.ID = ID;

        // QR
        this.QR = getBit(response[2], 7) == 1;

        // AA
        this.AA = getBit(response[2], 2) == 1;

        // TC
        this.TC = getBit(response[2], 1) == 1;

        // RD
        this.RD = getBit(response[2], 0) == 1;

        // RA
        this.RA = getBit(response[3], 7) == 1;

        // RCODE
        this.RCode = response[3] & 0x0F;

        // QDCount
        byte[] QDCount = { response[4], response[5] };
        ByteBuffer wrapped = ByteBuffer.wrap(QDCount);
        this.QDCount = wrapped.getShort();

        // ANCount
        byte[] ANCount = { response[6], response[7] };
        wrapped = ByteBuffer.wrap(ANCount);
        this.ANCount = wrapped.getShort();

        // NSCount
        byte[] NSCount = { response[8], response[9] };
        wrapped = ByteBuffer.wrap(NSCount);
        this.NSCount = wrapped.getShort();

        // ARCount
        byte[] ARCount = { response[10], response[11] };
        wrapped = ByteBuffer.wrap(ARCount);
        this.ARCount = wrapped.getShort();
    }

    /**
     * Parses the question section of the message and returns the offset at the start of the answer section in the bytes[].
     *
     * @return  the offset at the start of the answer section in the bytes[]
     */
    private int parseQuestion() {
        // get the QueryDomainName and QueryType for class variable
        // return the offset at the start of answer section in the bytes[]

        int offset = 12; // question section starts from 12 bytes in the message

        // get QName (domain main) in question section of the message
        offset += parseQuestionQName();

        // get QType
        byte[] qTypeBytes = new byte[] { response[offset], response[offset + 1] };
        int qtypeValue = (qTypeBytes[1] & 0xFF);
        setQueryTypeFromValue(qtypeValue);

        // get QClass
        offset += 2;
        byte[] qClassBytes = new byte[] {response[offset], response[offset+1]};
        // this.QClass =
        offset += 2;
        return offset;
    }

    private void setQueryTypeFromValue(int qtypeValue) {
        switch (qtypeValue) {
            case 1:
                this.queryType = QueryType.A;
                break;
            case 2:
                this.queryType = QueryType.NS;
                break;
            case 5:
                this.queryType = QueryType.CNAME;
                break;
            case 12:
                this.queryType = QueryType.PTR;
                break;
            case 15:
                this.queryType = QueryType.MX;
                break;
            default:
                this.queryType = QueryType.OTHER;
        }
    }

    public static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(b);
        }
        return sb.toString();
    }

    private int parseQuestionQName() {
        int countByte = 0;
        StringBuilder sb = new StringBuilder();
        while (!Byte.toString(response[countByte + 12]).equals("0")) {
            int byteValue = Integer.parseInt(Byte.toString(response[countByte + 12]));
            if (byteValue > 0 && byteValue < 15) {
                sb.append('.');
            } else {
                sb.append((char) byteValue);
            }
            countByte++;
        }
        sb.deleteCharAt(0);
        this.queryDomainName = sb.toString();
        countByte++;
        return countByte;
    }

    private DNSRecord parseAnswer(int index) {
        DNSRecord result = new DNSRecord(this.AA, this.TC);

        String domain = "";
        int countByte = index;

        rDataEntry domainResult = getDomainFromIndex(countByte);
        countByte += domainResult.getBytes();
        domain = domainResult.getDomain();

        // Name
        result.setName(domain);

        // TYPE
        byte[] RRTypeByte = new byte[2];
        RRTypeByte[0] = response[countByte];
        RRTypeByte[1] = response[countByte + 1];

        // TODO tempo fix for ptr
        if ((RRTypeByte[0]&0xff)==0 && (RRTypeByte[1]&0xff)==0) {
            countByte++;
            RRTypeByte[0] = response[countByte];
            RRTypeByte[1] = response[countByte + 1];
        }
        //////
        
        result.setQueryType(getQTYPEFromByteArray(RRTypeByte));

        countByte += 2;
        // CLASS
        byte[] ans_class = new byte[2];
        ans_class[0] = response[countByte];
        ans_class[1] = response[countByte + 1];

        result.setQueryClass(ans_class);

        countByte += 2;
        // TTL
        byte[] TTL = { response[countByte], response[countByte + 1], response[countByte + 2], response[countByte + 3] };
        ByteBuffer wrapped = ByteBuffer.wrap(TTL);
        result.setTimeToLive(wrapped.getInt());

        countByte += 4;
        // RDLength
        byte[] RDLength = { response[countByte], response[countByte + 1] };
        wrapped = ByteBuffer.wrap(RDLength);
        int rdLength = wrapped.getShort();
        result.setRdLength(rdLength);

        countByte += 2;
        // RDATA
        switch (result.getQueryType()) {
            case A:
                result.setDomainIP(parseATypeRDATA(rdLength, countByte));
                break;
            case NS:
                result.setDomainIP(parseNSTypeRDATA(rdLength, countByte));
                break;
            case MX:
                result.setDomainIP(parseMXTypeRDATA(rdLength, countByte, result));
                break;
            case CNAME:
                result.setDomainIP(parseCNAMETypeRDATA(rdLength, countByte));
                break;
            case PTR:
                result.setDomainIP(parsePTRTypeRDATA(rdLength, countByte));
                break;
            case OTHER:
                break;
        }
        // System.out.println("parsing queryType: "+result.getQueryType());
        // System.out.println("domainIP just set: "+result.getDomainIP());
        // System.out.println("countByte: "+countByte);
        // System.out.println("rdLength: "+rdLength);
        // System.out.println("index: "+index);

        result.setByteLength(countByte + rdLength - index);
        return result;
    }

    private String parseATypeRDATA(int rdLength, int countByte) {
        String address = "";
        byte[] byteAddress = { response[countByte], response[countByte + 1], response[countByte + 2],
                response[countByte + 3] };
        try {
            InetAddress inetaddress = InetAddress.getByAddress(byteAddress);
            address = inetaddress.toString().substring(1);
        } catch (UnknownHostException e) {
            // e.printStackTrace();
        }
        return address;

    }

    private String parseNSTypeRDATA(int rdLength, int countByte) {
        rDataEntry result = getDomainFromIndex(countByte);
        String nameServer = result.getDomain();

        return nameServer;
    }
    private String parsePTRTypeRDATA(int rdLength, int countByte) {
        rDataEntry result = getDomainFromIndex(countByte);
        String nameServer = result.getDomain();
        return nameServer;
    }

    private String parseMXTypeRDATA(int rdLength, int countByte, DNSRecord record) {
        byte[] mxPreference = { this.response[countByte], this.response[countByte + 1] };
        ByteBuffer buf = ByteBuffer.wrap(mxPreference);
        record.setMxPreference(buf.getShort());
        return getDomainFromIndex(countByte + 2).getDomain();
    }

    private String parseCNAMETypeRDATA(int rdLength, int countByte) {
        rDataEntry result = getDomainFromIndex(countByte);
        String cname = result.getDomain();

        return cname;
    }

    private void validateQueryTypeIsResponse() {
        if (!this.QR) {
            throw new RuntimeException("ERROR\tInvalid response from server: Message is not a response");
        }
    }

    // private void validateResponseQuestionType() {
    // // Question starts at byte 13 (indexed at 11)
    // int index = 12;

    // while (this.response[index] != 0) {
    // index++;
    // }
    // byte[] qType = { this.response[index + 1], this.response[index + 2] };

    // // if (this.getQTYPEFromByteArray(qType) != this.queryType) {
    // // throw new RuntimeException("ERROR\tResponse query type does not match
    // request query type");
    // // }
    // }

    private rDataEntry getDomainFromIndex(int index) {
        rDataEntry result = new rDataEntry();
        int wordSize = response[index];
        String domain = "";
        boolean start = true;
        int count = 0;
        while (wordSize != 0) {
            if (!start) {
                domain += ".";
            }
            if ((wordSize & 0xC0) == 0xC0) {
                byte[] offset = { (byte) (response[index] & 0x3F), response[index + 1] };
                ByteBuffer wrapped = ByteBuffer.wrap(offset);
                domain += getDomainFromIndex(wrapped.getShort()).getDomain();
                index += 2;
                count += 2;
                wordSize = 0;
            } else {
                domain += getWordFromIndex(index);
                index += wordSize + 1;
                count += wordSize + 1;
                wordSize = response[index];
            }
            start = false;
            
        }
        // System.out.println("word size = 0 at index: "+index);
        // System.out.println("domain: "+domain);
        // System.out.println("count: "+count);

        result.setDomain(domain);
        result.setBytes(count);
        return result;
    }

    private String getWordFromIndex(int index) {
        StringBuilder sb = new StringBuilder();
        int wordSize = response[index];
        for (int i = 0; i < wordSize; i++) {
            sb.append((char) response[index + i + 1]);
        }
        return sb.toString();
    }

    private int getBit(byte b, int position) {
        return (b >> position) & 1;
    }

    private QueryType getQTYPEFromByteArray(byte[] qType) {
        if (qType[0] == 0) {
            if (qType[1] == 1) {
                return QueryType.A;
            } else if (qType[1] == 2) {
                return QueryType.NS;
            } else if (qType[1] == 15) {
                return QueryType.MX;
            } else if (qType[1] == 5) {
                return QueryType.CNAME;
            } else if (qType[1] == 12) {
                return QueryType.PTR;
            } else {
                return QueryType.OTHER;
            }
        } else {
            return QueryType.OTHER;
        }
    }

    public QueryType getQueryType() {
        return this.queryType;
    }

    public String getQueryDomainName() {
        return this.queryDomainName;
    }

    public boolean getRD() {
        return this.RD;
    }

    public int getRCode() {
        return this.RCode;
    }

    public int getNSCount() {
        return this.NSCount;
    }

    public int getANCOUNT() {
        return this.ANCount;
    }

    public int getARCount() {
        return this.ARCount;
    }

    public DNSRecord[] getAdditionalRecords() {
        return this.additionalRecords;
    }

    public DNSRecord[] getAnswerRecords() {
        return this.answerRecords;
    }

    public DNSRecord[] getAuthorityRecords() {
        return this.authorityRecords;
    }

    public byte[] getQuestionAndHeaderBytes() {
        return this.questionAndHeaderBytes;
    }

    public byte[] getAnswerSectionBytes() {
        return this.answerSectionBytes;
    }
}
