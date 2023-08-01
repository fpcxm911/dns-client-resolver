import java.nio.ByteBuffer;
import java.util.Random;

public class DNSRequest {

	private final String domain;
	private final QueryType qtype;
	private final boolean RDFlag;

	// public DNSRequest(String domain, QueryType type) {
	// 	this.domain = domain;
	// 	this.qtype = type;
	// }
	public DNSRequest(String domain, QueryType type, boolean RDFlag) {
		if (type == QueryType.PTR && !domain.contains("in-addr")) {
			this.domain = generatePTRQueryDomain(domain);
			// System.out.println("new ptr domain " + this.domain);
		} else {
			this.domain = domain;
		}
		this.qtype = type;
		this.RDFlag = RDFlag;
	}
	public byte[] getRequestBytes() {
		int qNameLength = getQNameLength();
		// 12 bytes for header, 4 bytes for QTYPE + QCLASS, 1 byte for end of QNAME, qNameLength for QNAME
		ByteBuffer request = ByteBuffer.allocate(12 + 5 + qNameLength);
		request.put(createRequestHeader()); // 12 bytes header section
		request.put(createQuestionSection(qNameLength)); // get Question Section
		return request.array();
	}

	private byte[] createRequestHeader() {
		ByteBuffer header = ByteBuffer.allocate(12);
		byte[] randomID = new byte[2];
		new Random().nextBytes(randomID);
		header.put(randomID);
		// QR Opcode AA TC RD 
		// all set as 0
		if (RDFlag) {
			header.put((byte) 0x01);
		} else {
			header.put((byte) 0x00);
		}
		// RA Z RCode
		header.put((byte) 0x00);
		
		// QDCOUNT set as 1 to show it has 1 question
		header.put((byte) 0x00);
		header.put((byte) 0x01);
		
		// ANCOUNT NSCOUNT ARCOUNT will be all 0s
		return header.array();
	}
	
	private int getQNameLength() {
		int byteLength = 0;
		String[] items = domain.split("\\.");
		for (int i = 0; i < items.length; i++) {
			// 1 byte length for the number value and then another for each character
			// www.unsw.edu.au = 3, w, w, w, 4, u, n, s, w, 3, e, d, u, 2, a, u = 16 bytes
			byteLength += items[i].length() + 1;
		}
		return byteLength;
	}
	
	// private byte[] creatPTRQuestionSection(int qNameLength) {
	// 	ByteBuffer question = ByteBuffer.allocate(qNameLength + 5);
		
	// 	// first calculate how many bytes needed so we know the size of the array
	// 	String[] items = domain.split("\\.");
	// 	reverseArray(items);
		
	// 	for (int i = 0; i < items.length; i++) {
	// 		question.put((byte) items[i].length());
	// 		for (int j = 0; j < items[i].length(); j++) {
	// 			question.put((byte) ((int) items[i].charAt(j)));
	// 		}
	// 	}

	// 	question.put((byte) 0x00);


	// 	// Add Query Type
	// 	question.put(hexStringToByteArray("000" + hexValueFromQueryType(qtype)));
		
	// 	// Add Query Class IN
	// 	question.put((byte) 0x00);
	// 	question.put((byte) 0x01);

	// 	return question.array();
	// }
	private void reverseArray(String[] items) {
		int start = 0;
		int end = items.length -1;
		while (start < end) {
			String temp = items[start];
			items[start] = items[end];
			items[end] = temp;

			start++;
			end--;
		}
	}
	private String generatePTRQueryDomain(String queryString) {
		StringBuilder sb = new StringBuilder();
		String[] items = queryString.split("\\.");
		reverseArray(items);
		
		for (String item : items) {
			sb.append(item);
			sb.append('.');
		}
		sb.append("in-addr.arpa");
		return sb.toString();
	}
	private byte[] createQuestionSection(int qNameLength) {
		ByteBuffer question = ByteBuffer.allocate(qNameLength + 5);
		

		String[] items = domain.split("\\.");
		
		for (int i = 0; i < items.length; i++) {
			question.put((byte) items[i].length());
			for (int j = 0; j < items[i].length(); j++) {
				question.put((byte) ((int) items[i].charAt(j)));
			}
		}

		question.put((byte) 0x00);


		// Add Query Type
		question.put(hexStringToByteArray("000" + hexValueFromQueryType(qtype)));
		
		// Add Query Class IN
		question.put((byte) 0x00);
		question.put((byte) 0x01);

		return question.array();
	}

	private char hexValueFromQueryType(QueryType type) {
		switch (type) {
			case A:
				return '1';
			case NS:
				return '2';
			case CNAME:
				return '5';
			case PTR:
				return 'c';
			case MX:
				return 'f';
			default:
				return 'F';
		}

	}

	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
	public QueryType getQueryType() {
		return this.qtype;
	}
	public boolean getRDFlag() {
		return this.getRDFlag();
	}
}
