public class DNSRecord {
	private int timeToLive, rdLength, mxPreference;
	private String name, domain;
	private byte[] queryClass;
	private QueryType queryType;
	private boolean auth;
	private boolean truncated;
	private int byteLength;

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

	public DNSRecord(boolean auth, boolean truncated) {
		this.auth = auth;
		this.truncated = truncated;
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
			default:
				break;
		}
	}

	private void outputATypeRecords() {
		String authString = this.auth ? "authoritative" : "not authoritative";
		String tcString = this.truncated ? "truncated" : "not truncated";
		System.out.println("A\t" + this.domain + "\t" + authString + "\t" + tcString);
	}

	private void outputNSTypeRecords() {
		String authString = this.auth ? "authoritative" : "not authoritative";
		String tcString = this.truncated ? "truncated" : "not truncated";
		System.out.println("NS\t" + this.domain + "\t" + authString + "\t" + tcString);
	}

	private void outputMXTypeRecords() {
		String authString = this.auth ? "authoritative" : "not authoritative";
		String tcString = this.truncated ? "truncated" : "not truncated";
		System.out.println("MX\t" + this.domain + "\t" + authString + "\t" + tcString);
	}

	private void outputCNameTypeRecords() {
		String authString = this.auth ? "authoritative" : "not authoritative";
		String tcString = this.truncated ? "truncated" : "not truncated";
		System.out.println("CNAME\t" + this.domain + "\t" + authString + "\t" + tcString);
	}

	private void outputPTRTypeRecords() {
		// TODO
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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
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
