import java.net.DatagramPacket;
import java.util.List;

public class DNSSlistSearch {
    private List<DNSRecord> slist;
    private DatagramPacket packet;

    public DNSSlistSearch(List<DNSRecord> slist, DatagramPacket packet) {
        this.slist = slist;
        this.packet = packet;
    }

    public List<DNSRecord> getSlist() {
        return this.slist;
    }

    public void setSlist(List<DNSRecord> slist) {
        this.slist = slist;
    }

    public DatagramPacket getPacket() {
        return this.packet;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

}
