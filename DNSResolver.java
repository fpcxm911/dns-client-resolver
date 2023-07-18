import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DNSResolver {
    private int port;
    private static final int MAX_DNS_PACKET_SIZE = 512;
    private ArrayList<DNSRecord> nsList = new ArrayList<>();
    private static final String HINT_FILE_PATH = "./named.root";
    private static final int TIMEOUT = 500;

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
            SocketAddress clientSocketAddress = clientRequestPacket.getSocketAddress();

            // parsing client request
            byte[] clientRequestBytes = clientRequestPacket.getData();
            DNSMessage clientMessage = new DNSMessage(clientRequestBytes);
            System.out.println("Request received");
            DNSRequest clientRequest = parseClientQueryAndPrint(clientMessage);
            DatagramPacket finalResponsePacket = resolve(clientRequest);
            finalResponsePacket.setSocketAddress(clientSocketAddress);
            socket.send(finalResponsePacket);
            System.out.println("Client DNS Query Resolved");

        }
    }

    private DNSRequest parseClientQueryAndPrint(DNSMessage clientRequest) {
        String queryDomainName = clientRequest.getQueryDomainName();
        QueryType queryType = clientRequest.getQueryType();
        System.out.println("====>Client Request<====");
        System.out.println("Domain:\t" + queryDomainName);
        System.out.println("QueryType:\t" + queryType);
        return new DNSRequest(queryDomainName, queryType);
    }

    private DatagramPacket resolve(DNSRequest clientDnsRequest) {
        byte[] requestBytes = clientDnsRequest.getRequest();
        byte[] responseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT);

            InetAddress inetaddress = InetAddress.getByAddress(getIpBytesFromIpString("1.1.1.1"));
            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, inetaddress, 53);
            socket.send(requestPacket);
            socket.receive(responsePacket);
            socket.close();
            
            // TODO 18/07 iteratively call root server in nslist until get a response
            // for (int i = 0; i < nsList.size(); i++) {
            //     DNSRecord nameServer = nsList.get(i);
            //     if (nameServer.getQueryType() == QueryType.NS) continue;
            //     String nsDomainIpString = nameServer.getDomainIP();
            // }

        } catch (SocketException e) {
            System.out.println("ERROR during resolving: " + e.getMessage());
        } catch (Exception e) {
            System.out.printf("wired stuff happened...\n" + e.getMessage());
        }


        return responsePacket;

    }

    private byte[] getIpBytesFromIpString(String ipString) {
        String[] ipComponents = ipString.split("\\.");
        byte[] ipBytes = new byte[4];
        for (int i = 0; i < ipComponents.length; i++) {
            int ipValue = Integer.parseInt(ipComponents[i]);
            ipBytes[i] = (byte) ipValue;
        }
        return ipBytes;
    }


}
