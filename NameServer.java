public class NameServer {
    private String ns;
    private byte[] nsIPBytes = new byte[4];

    public String getNs() {
        return this.ns;
    }

    public void setNs(String ns, String ipString) {
        this.ns = ns;
        this.nsIPBytes = ipStringToByteArray(ipString);
    }

    public byte[] getNsIPBytes() {
        return this.nsIPBytes;
    }

    public void setNsIPBytes(byte[] nsIPBytes) {
        this.nsIPBytes = nsIPBytes;
    }

    private byte[] ipStringToByteArray(String ipString) {
        byte[] ipBytes = new byte[4];
        String[] ipComponents = ipString.split("\\.");

        for (int i = 0; i < ipComponents.length; i++) {
            int ipValue = Integer.parseInt(ipComponents[i]);
            if (ipValue < 0 || ipValue > 255) {
                throw new NumberFormatException("ERROR: Incorrect IP Address numbers must be between 0 and 255.");
            }
            ipBytes[i] = (byte) ipValue;
        }
        return ipBytes;
    }
}
