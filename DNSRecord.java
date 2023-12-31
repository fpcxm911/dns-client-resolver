public class DNSRecord {
	private int timeToLive, rdLength, mxPreference;
	private String name, domainIP;
	private byte[] queryClass;
	private QueryType queryType;
	private boolean auth;
	private boolean truncated;
	private int byteLength;

	public DNSRecord(boolean auth, boolean truncated) {
		this.auth = auth;
		this.truncated = truncated;
	}
	public DNSRecord(String name, String domainIP, QueryType queryType) {
		this.name = name;
		this.domainIP = domainIP;
		this.queryType = queryType;
	}
	public boolean getAuth() {
		return this.auth;
	}

	public boolean isTruncated() {
		return this.truncated;
	}

	public boolean getTC() {
		return this.truncated;
	}

	public void setTruncated(boolean TC) {
		this.truncated = TC;
	}


	public void outputRecord() {
		switch (this.queryType) {
			case A:
				this.outputATypeRecords();
				break;
			case NS:
				this.outputNSTypeRecords();
				break;
			case MX:
				this.outputMXTypeRecords();
				break;
			case CNAME:
				this.outputCNameTypeRecords();
				break;
			case PTR:
				this.outputPTRTypeRecords();
				break;
			default:
				break;
		}
	}

	private void outputATypeRecords() {
		String authString = this.auth ? "authoritative" : "non-authoritative";
		String tcString = this.truncated ? "truncated" : "not-truncated";
		String resultLine = ("A\t" + this.name + "\t"+ this.domainIP + "\t" + authString + "\t" + tcString);
		System.out.println(formatColumns(resultLine));
	}

	private void outputNSTypeRecords() {
		String authString = this.auth ? "authoritative" : "non-authoritative";
		String tcString = this.truncated ? "truncated" : "not-truncated";
		String resultLine = ("NS\t" + this.name + "\t" + this.domainIP + "\t" + authString + "\t" + tcString);
		System.out.println(formatColumns(resultLine));
	}	

	private void outputMXTypeRecords() {
		String authString = this.auth ? "authoritative" : "non-authoritative";
		String tcString = this.truncated ? "truncated" : "not-truncated";
		String resultLine = "MX\t" + this.name + "\t" + this.domainIP + "\t" + authString + "\t" + tcString;
		System.out.println(formatColumns(resultLine));
	}

	private void outputCNameTypeRecords() {
		String authString = this.auth ? "authoritative" : "non-authoritative";
		String tcString = this.truncated ? "truncated" : "not-truncated";
		String resultLine = ("CNAME\t" + this.name + "\t" + this.domainIP + "\t" + authString + "\t" + tcString);
		System.out.println(formatColumns(resultLine));
	}
	
	private void outputPTRTypeRecords() {
		// TODO Enhanced for PTR records
		String authString = this.auth ? "authoritative" : "non-authoritative";
		String tcString = this.truncated ? "truncated" : "not-truncated";
		String resultLine = ("PTR\t" + this.domainIP + "\t" + authString + "\t" + tcString + "\t" + tcString);
		System.out.println(formatColumns(resultLine));
	}
	private static String formatColumns(String line) {
		String[] columns = line.split("\\s+");
		return String.format("%-5s %-18s %-25s %-19s %-13s", columns[0], columns[1], columns[2], columns[3], columns[4]);
	}


	public int getByteLength() {
		return byteLength;
	}

	public void setByteLength(int byteLength) {
		this.byteLength = byteLength;
	}

	public int getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	public int getRdLength() {
		return rdLength;
	}

	public void setRdLength(int rdLength) {
		this.rdLength = rdLength;
	}

	public int getMxPreference() {
		return mxPreference;
	}

	public void setMxPreference(int mxPreference) {
		this.mxPreference = mxPreference;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomainIP() {
		return domainIP;
	}

	public void setDomainIP(String domain) {
		this.domainIP = domain;
	}

	public byte[] getQueryClass() {
		return queryClass;
	}

	public void setQueryClass(byte[] queryClass) {
		this.queryClass = queryClass;
	}

	public QueryType getQueryType() {
		return queryType;
	}

	public void setQueryType(QueryType queryType) {
		this.queryType = queryType;
	}

	public boolean isAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}
}
