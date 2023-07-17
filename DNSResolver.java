import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DNSResolver {
    private int port;
    private static final int MAX_DNS_PACKET_SIZE = 1024;
    private ArrayList<DNSRecord> nsList = new ArrayList<>();
    private static final String HINT_FILE_PATH = "./named.root";

    public DNSResolver(String[] args) {
        this.readHintFile();
        try {
            this.parseInputArguments(args);
            this.receiveAndResolve();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new IllegalArgumentException(
                "Usage: Resolver <port>"
            );
        }

    }

    private void readHintFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(HINT_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(";") || line.isBlank() || line.contains("AAAA")) continue;
                String[] records = line.split("\\s+");
                String name = records[0];
                QueryType qtype;
                switch(records[2]) {
                    case "NS":
                        qtype = QueryType.NS;
                        break;
                    case "A":
                        qtype = QueryType.A;
                        break;
                    default:
                        throw new RuntimeException("Something wrong when parsing named.root file");
                }
                String domainIp = records[3];
                nsList.add(new DNSRecord(name, domainIp, qtype));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseInputArguments(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.size() == 1) {
            if (argsList.get(0).matches("\\d+")) {
                port = Integer.parseInt(argsList.get(0));
            } else {
                throw new IllegalArgumentException("Error: invalid port number.");
            }
        } else {
            throw new IllegalArgumentException("Error: Wrong number of argument");
        }
    }

    public void receiveAndResolve() throws Exception {
        DatagramSocket socket = new DatagramSocket(port);
        while (true) {
            // create a datagrampacket to store client request from udp socket
            DatagramPacket clientRequestPacket = new DatagramPacket(new byte[MAX_DNS_PACKET_SIZE], MAX_DNS_PACKET_SIZE);
            
            // receive request from client
            socket.receive(clientRequestPacket);
            // parsing client request
            byte[] clientRequestBytes = clientRequestPacket.getData();
            DNSMessage clientRequest = new DNSMessage(clientRequestBytes);
            System.out.println("Request received");
            parseClientQueryAndPrint();
            resolve();


        }
    }

    private void parseClientQueryAndPrint() {
        // TODO parse client request
    }

    private void resolve() {
        // TODO resovle the dns query from client
    }



}
