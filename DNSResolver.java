import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.List;


public class DNSResolver {
    private QueryType queryType;
    private int port;
    private static final int MAX_DNS_PACKET_SIZE = 1024;
    private NameServer[] nsList;
    private static final String HINT_FILE_PATH = "./named.root";

    public DNSResolver(String[] args) {
        this.readHintFile();
        try {
            this.parseInputArguments(args);

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
                System.out.println(line);
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
            System.out.println("Request received");
            // TODO send request iteratively to dns servers
            // TODO parsing dns response
            // TODO when reach desired result, return the dns reponse to client


        }
    }



}
