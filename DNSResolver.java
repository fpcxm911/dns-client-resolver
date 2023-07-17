import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.List;

public class DNSResolver {
    public QueryType queryType;
    private int port;
    public static final int MAX_DNS_PACKET_SIZE = 512;

    public DNSResolver(String[] args) {
        try {
            this.parseInputArguments(args);
            this.readHintFile();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new IllegalArgumentException(
                "Usage: Resolver <port>"
            );
        }

    }

    private void readHintFile() {
        // TODO read named.root
        // ignore lines start with a semi-colon
        // blank lines, and type AAAA record
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

    public void run() throws Exception {
        DatagramSocket socket = new DatagramSocket(port);
        while (true) {
            // create a datagrampacket to store client request from udp socket
            DatagramPacket clientRequestPacket = new DatagramPacket(new byte[MAX_DNS_PACKET_SIZE], MAX_DNS_PACKET_SIZE);
            
            // receive request from client
            socket.receive(clientRequestPacket);
            // parsing client request
            byte[] clientRequestBytes = clientRequestPacket.getData();
            DNSMessage clientRequest = new DNSMessage(clientRequestBytes);
            clientRequest.outputQuestion();
            System.out.println("Request received");
            // TODO send request iteratively to dns servers
            // TODO parsing dns response
            // TODO when reach desired result, return the dns reponse to client


        }
    }



}
