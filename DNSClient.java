import java.net.*;
import java.util.Arrays;
import java.util.List;

public class DNSClient {

    public QueryType queryType = QueryType.A;
    public static final int MAX_DNS_PACKET_SIZE = 512;
    private int timeout = 5000;
    private byte[] resolverIPBytes = new byte[4];
    String resolverIPString;
    private String domainName;
    private int port;

    public DNSClient(String[] args) {
        try {
            this.parseInputArguments(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new IllegalArgumentException(
                "Usage: Client <resolver_ip> <resolver_port> <domain_name> [type=A] [timeout=5]"
            );
        }
    }

    public void makeRequest() {
        System.out.println("DNSClient sending request for " + domainName);
        System.out.println("Server: " + resolverIPString);
        System.out.println("Request type: " + queryType);
        pollRequest();
    }

    private void pollRequest() {
        try {
            // Create Datagram socket and request object(s)
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            InetAddress inetaddress = InetAddress.getByAddress(resolverIPBytes);
            DNSResquest request = new DNSResquest(domainName, queryType);

            byte[] requestBytes = request.getRequest();
            byte[] responseBytes = new byte[1024];

            DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, inetaddress, port);
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);

            // Send packet and time response
            long startTime = System.currentTimeMillis();
            socket.send(requestPacket);
            socket.receive(responsePacket);
            long endTime = System.currentTimeMillis();
            socket.close();

            System.out.println("Response received after " + (endTime - startTime) + " ms");

            DNSMessage response = new DNSMessage(responsePacket.getData(), requestBytes.length, queryType);
            response.outputResponse();

        } catch (SocketException e) {
            System.out.println("ERROR: Could not create socket");
        } catch (UnknownHostException e) {
            System.out.println("ERROR: Unknown host");
        } catch (SocketTimeoutException e) {
            System.out.println("ERROR: Socket Timeout");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void parseInputArguments(String args[]) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.size() < 3 || argsList.size() > 5) {
            throw new IllegalArgumentException("ERROR: Incorrect number of input arguments");
        }
        resolverIPString = argsList.get(0);
        String[] ipComponents = resolverIPString.split("\\.");

        for (int i = 0; i < ipComponents.length; i++) {
            int ipValue = Integer.parseInt(ipComponents[i]);
            if (ipValue < 0 || ipValue > 255) {
                throw new NumberFormatException("ERROR: Incorrect IP Address numbers must be between 0 and 255.");
            }
            resolverIPBytes[i] = (byte) ipValue;
        }

        port = Integer.parseInt(argsList.get(1));
        domainName = argsList.get(2);

        if (argsList.size() == 4) { // one enhanced argument
            String enhancedArg = argsList.get(3);
            if (enhancedArg.matches("[a-zA-Z]+")) { // type specified
                parseQType(enhancedArg.toLowerCase());
            } else if (enhancedArg.matches("\\d+")) { // timeout specified
                timeout = Integer.parseInt(enhancedArg);
            }
        } else if (argsList.size() == 5) { // two enhanced arguments
            String enhancedArg1 = argsList.get(3);
            String enhancedArg2 = argsList.get(4);
            if ((enhancedArg1.matches("[a-zA-Z]+") && enhancedArg2.matches("[a-zA-Z]+")) ||
                    (enhancedArg1.matches("\\d+") && enhancedArg2.matches("\\d+"))) {
                throw new IllegalArgumentException("ERROR: illegal enhanced arguments");
            }
            if (enhancedArg1.matches("[a-zA-Z]+")) { // type specified
                parseQType(enhancedArg1.toLowerCase());
            } else if (enhancedArg1.matches("\\d+")) { // timeout specified
                timeout = Integer.parseInt(enhancedArg1);
            }
            if (enhancedArg2.matches("[a-zA-Z]+")) { // type specified
                parseQType(enhancedArg2.toLowerCase());
            } else if (enhancedArg2.matches("\\d+")) { // timeout specified
                timeout = Integer.parseInt(enhancedArg2);
            }
        }

    }

    private void parseQType(String type) {
        switch (type) {
            case "a":
                queryType = QueryType.A;
                break;
            case "mx":
                queryType = QueryType.MX;
                break;
            case "cname":
                queryType = QueryType.CNAME;
                break;
            case "ns":
                queryType = QueryType.NS;
                break;
            case "ptr":
                queryType = QueryType.PTR;
                break;
            default:
                throw new IllegalArgumentException("ERROR: Invalid query type");
        }
    }

}
