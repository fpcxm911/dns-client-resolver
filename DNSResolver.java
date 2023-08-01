import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DNSResolver {
    private int port;
    private static final int MAX_DNS_PACKET_SIZE = 512;
    private final List<DNSRecord> rootList = new ArrayList<>();
    private static final String HINT_FILE_PATH = "./named.root";
    private static final int TIMEOUT = 500;

    public DNSResolver(String[] args) {
        this.readHintFile();
        run(args);
    }
    public void run(String[] args) {
        try {
            this.parseInputArguments(args);
            this.receiveAndResolve();
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timed out during iterative query");
            run(args);
        } catch (IOException e) {
            // e.printStackTrace();
            throw new IllegalArgumentException(
                    "Usage: Resolver <port>");
        }
        
    }

    private void readHintFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(HINT_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(";") ||
                        line.isBlank() ||
                        line.contains("AAAA") ||
                        line.contains("NS"))
                    continue;
                String[] records = line.split("\\s+");
                String name = records[0];
                QueryType qtype;
                switch (records[2]) {
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
                rootList.add(new DNSRecord(name, domainIp, qtype));
            }
        } catch (IOException e) {
            System.out.println("IOException when reading named.root file");
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
            System.out.println("Usage: Resolver <port>");
            throw new IllegalArgumentException("Error: Wrong number of argument");
        }
    }

    public void receiveAndResolve() throws IOException {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            while (true) {
                // create a datagrampacket to store client request from udp socket
                DatagramPacket clientRequestPacket = new DatagramPacket(new byte[MAX_DNS_PACKET_SIZE],
                        MAX_DNS_PACKET_SIZE);

                // receive request from client
                socket.receive(clientRequestPacket);
                SocketAddress clientSocketAddress = clientRequestPacket.getSocketAddress();

                // parsing client request
                byte[] clientRequestBytes = clientRequestPacket.getData();
                DNSMessage clientMessage = new DNSMessage(clientRequestBytes);
                // System.out.println("=======>Request received<=======");
                DNSRequest clientDnsRequest = parseClientQueryAndPrint(clientMessage);
                DatagramPacket finalResponsePacket = resolve(clientDnsRequest);

                // System.out.println();
                // System.out.println("checkpoint: sending result to client: ");
                // DNSMessage finalmsg = new DNSMessage(finalResponsePacket.getData(),
                // clientDnsRequest.getRequestBytes().length,
                // clientDnsRequest.getQueryType());
                // finalmsg.outputResponse();

                finalResponsePacket.setSocketAddress(clientSocketAddress);

                socket.send(finalResponsePacket);
                System.out.println("===>Client DNS Query Resolved<===");
                System.out.println();
            }
        }
    }

    private DNSRequest parseClientQueryAndPrint(DNSMessage clientRequestMessage) {
        String queryDomainName = clientRequestMessage.getQueryDomainName();
        QueryType queryType = clientRequestMessage.getQueryType();
        System.out.println("=======>Client Request<=======");
        System.out.println("Domain:\t" + queryDomainName);
        System.out.println("QueryType:\t" + queryType);
        return new DNSRequest(queryDomainName, queryType, clientRequestMessage.getRD());
    }

    private DatagramPacket resolve(DNSRequest clientDnsRequest) throws RuntimeException, SocketTimeoutException {
        // given client DNS request, resolve the request and return final response as
        // DatagramPacket
        byte[] clientRequestBytes = clientDnsRequest.getRequestBytes();
        byte[] responseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket finalResponsePacket = new DatagramPacket(responseBytes, responseBytes.length);
        boolean finalAnswerGet = false;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);

            DatagramPacket requestPacket = new DatagramPacket(clientRequestBytes, clientRequestBytes.length);

            // query named.root servers for NS to iteratively send DNS query
            DNSSlistSearch slistResult = queryServers(new DNSSlistSearch(rootList, requestPacket));

            if (slistResult.getPacket() == null) {
                throw new SlistTimeOutException("All Root Servers timeout");
            }

            while (!finalAnswerGet && slistResult.getPacket() != null) {

                // parse the response from NS
                DNSMessage msg = new DNSMessage(slistResult.getPacket().getData(), clientRequestBytes.length);

                try {
                    msg.checkRCodeForErrors();

                    // no RCode Exception in the reponse
                    // there is answer record in response
                    if (msg.getANCOUNT() > 0) {
                        // if there is CNAME record included in Answer Section
                        // if client query about CNAME, then return response
                        // if client didn't query CNAME, reconstruct the dns query for CNAME and start
                        // query again
                        for (DNSRecord ansRecord : msg.getAnswerRecords()) {
                            if (clientDnsRequest.getQueryType() == QueryType.CNAME) {
                                finalAnswerGet = true;
                                finalResponsePacket = slistResult.getPacket();
                                return finalResponsePacket;
                            }
                            if (ansRecord.getQueryType() == QueryType.CNAME) {
                                String cName = ansRecord.getDomainIP();
                                return resolveWithCNAME(cName, clientDnsRequest);
                            }
                        }

                        // there is no CNAME record, meaning final answer get!
                        // return to client
                        finalAnswerGet = true;
                        finalResponsePacket = slistResult.getPacket();

                        // System.out.println();
                        // System.out.println("checkpoint: before returning from resolve()");
                        // DNSMessage finalmsg = new DNSMessage(finalResponsePacket.getData(),
                        // clientRequestBytes.length,
                        // clientDnsRequest.getQueryType());
                        // finalmsg.outputResponse();

                        return finalResponsePacket;
                    }

                    // check if response nominates antoher server to query iteratively
                    // NS nominated in response
                    if (msg.getNSCount() > 0) {
                        // if SOA record present in Auth NS section, return response to client
                        DNSRecord[] authRecords = msg.getAuthorityRecords();
                        for (DNSRecord r : authRecords) {
                            if (r.getQueryType() == QueryType.OTHER) {
                                finalAnswerGet = true;
                                finalResponsePacket = slistResult.getPacket();
                                return finalResponsePacket;
                            }
                        }
                        // get the list of NS type A record
                        List<DNSRecord> slist = handleNSNominatedResponse(msg);
                        // query slist using client request
                        slistResult = queryServers(new DNSSlistSearch(slist, requestPacket));
                        continue;
                    }

                } catch (ServerFailureException e) {
                    // response shows a server failure, exhaust search space before return to client
                    // return the response to client
                    if (slistResult.getSlist().isEmpty()) {
                        // list exhausted, return response to client
                        finalResponsePacket = slistResult.getPacket();
                        finalAnswerGet = true;
                        return finalResponsePacket;
                    } else {
                        // exhaust search slist
                        slistResult = queryServers(slistResult);
                    }
                } catch (
                        NotImplementedException | NameErrorException | FormatErrorException | RefusedException e) {
                    // return it back to client
                    finalResponsePacket = slistResult.getPacket();
                    finalAnswerGet = true;
                    return finalResponsePacket;
                }
            }

            if (slistResult.getPacket() == null) {
                throw new SlistTimeOutException("All Servers timeout");
            }
        } catch (NullPointerException e) {
            // e.printStackTrace();
            System.out.println("null pointer");
            System.out.println(e.getMessage());
        } catch (SocketException e) {
            // e.printStackTrace();
            System.out.println("ERROR from Socket during resolving: " + e.getMessage());
        }

        // iterative query complete, return the Response Packet to client

        // System.out.println();
        // System.out.println("checkpoint: before returning from resolve()" +
        //         finalResponsePacket.getData().length);
        // DNSMessage finalmsg = new DNSMessage(finalResponsePacket.getData(), clientRequestBytes.length,
        //         clientDnsRequest.getQueryType());
        // finalmsg.outputResponse();

        return (finalResponsePacket);

    }

    private DatagramPacket resolveWithCNAME(String cName, DNSRequest clientDnsRequest) throws SocketTimeoutException {

        byte[] cnameRequestResponseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket cnameResponse = new DatagramPacket(cnameRequestResponseBytes, cnameRequestResponseBytes.length);

        DNSRequest cNameRequest = new DNSRequest(cName, clientDnsRequest.getQueryType(), false);
        cnameResponse = resolve(cNameRequest);

        return contacteResponseAndRequest(cnameResponse, clientDnsRequest);

    }

    /**
     * Contact the response and request by concatenating the client request bytes
     * with
     * the answer bytes from the cname message. Return a DatagramPacket to be sent
     * to client.
     *
     * @param cnameResponse    the DatagramPacket containing the response from the
     *                         Type A request with canonical name
     * @param clientDnsRequest the original DNSRequest from the client
     * @return a response DatagramPacket to be sent to client with client's question
     *         section and cnameResponse's answer section
     */
    private DatagramPacket contacteResponseAndRequest(DatagramPacket cnameResponse, DNSRequest clientDnsRequest) {
        // extract original client request bytes
        byte[] clientRequestBytes = clientDnsRequest.getRequestBytes();

        // extract cnameResponse bytes
        byte[] cnameResponseBytes = cnameResponse.getData();

        // modify header section to match cnameResponse header section
        byte[] headerNquestionSection = modifyHeader(clientRequestBytes, cnameResponseBytes);

        DNSMessage cnameMessage = new DNSMessage(cnameResponseBytes);
        // get the answer section from cname reponse
        byte[] answerSectionBytes = cnameMessage.getAnswerSectionBytes();

        // concat modified question + header section, with canme answer section
        byte[] concatedByte = new byte[MAX_DNS_PACKET_SIZE];
        // int answerSectionStartIndexInCnameResponse = getAnswerIndex(answerBytes);
        
        if (answerSectionBytes.length + headerNquestionSection.length > MAX_DNS_PACKET_SIZE) {
            
            System.arraycopy(answerSectionBytes, 0, concatedByte, headerNquestionSection.length,
            MAX_DNS_PACKET_SIZE - headerNquestionSection.length);
            // set header TC to 1
            byte b = headerNquestionSection[2];
            b = (byte) ((b & 0xff) | (1 << 1));
            headerNquestionSection[2] = b;
        } else {
            System.arraycopy(answerSectionBytes, 0, concatedByte, headerNquestionSection.length,
            answerSectionBytes.length);
        }
        
        System.arraycopy(headerNquestionSection, 0, concatedByte, 0, headerNquestionSection.length);
        return new DatagramPacket(concatedByte, concatedByte.length);
    }



    /**
     * Modifies the header of the client DNS request bytes, to match cname
     * answerBytes.
     *
     * @param clientRequestBytes the byte array representing the client request
     * @param responseBytes      the byte array containing the answer
     * @return the modified byte array containing header and question ready to be
     *         sent to client
     */
    private byte[] modifyHeader(byte[] clientRequestBytes, byte[] responseBytes) {
        byte[] resultingBytes = new byte[clientRequestBytes.length];
        int offset = 0;
        // client ID stays the same
        System.arraycopy(clientRequestBytes, offset, resultingBytes, offset, 2);

        offset += 2;
        // second row of header
        // QR, Opcode, AA, TC, RD, RA, Z, RCode = answer
        System.arraycopy(responseBytes, offset, resultingBytes, offset, 2);

        offset += 2;
        // QDCOUNT = client;
        System.arraycopy(clientRequestBytes, offset, resultingBytes, offset, 2);

        offset += 2;
        // ANCOUNT = answer
        System.arraycopy(responseBytes, offset, resultingBytes, offset, 2);

        offset += 2;
        // NSCOUNT = answer
        System.arraycopy(responseBytes, offset, resultingBytes, offset, 2);

        offset += 2;
        // ARCOUNT = answer
        System.arraycopy(responseBytes, offset, resultingBytes, offset, 2);

        offset += 2;

        // Header section modified finished
        // question section = client
        System.arraycopy(clientRequestBytes, offset, resultingBytes, offset, clientRequestBytes.length - 12);

        return resultingBytes;
    }

    private List<DNSRecord> handleNSNominatedResponse(DNSMessage msg) {
        // if msg Additional Records section containes type A record
        // construct new slist and return the list
        List<DNSRecord> slist = new ArrayList<>();
        if (msg.getARCount() > 0) {
            DNSRecord[] aRecords = msg.getAdditionalRecords();
            for (DNSRecord r : aRecords) {
                if (r.getQueryType() == QueryType.A) {
                    slist.add(r);
                }
            }
            if (!slist.isEmpty()) {
                return slist;
            }
        }

        System.out.println("no additional record for NS, querying for type A RR for NS...");
        // if msg has no type A DNS record
        // query this server for type A record and return this DNSRecord
        for (DNSRecord authNSRecord : msg.getAuthorityRecords()) {
            String nsName = authNSRecord.getDomainIP();

            try {
                slist.add(getDNSRecordForNS(nsName));
                return slist;
            } catch (SocketTimeoutException|NullPointerException|UnknownHostException e) {
                continue;
            }
        }
        return slist;
    }
    
    private DNSRecord getDNSRecordForNS(String nsName) throws SocketTimeoutException,NullPointerException, UnknownHostException {
        // construct the DNS request to ask for type A RR of NS
        DNSRequest nsRequest = new DNSRequest(nsName, QueryType.A, false);
        
        byte[] nsResponseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket nsResponse = new DatagramPacket(nsResponseBytes, nsResponseBytes.length);
        // send the request for nsName
        nsResponse = resolve(nsRequest);
        
        // parse the response packet into DNSMessage
        DNSMessage nsAResponseMsg = new DNSMessage(nsResponse.getData(), nsRequest.getRequestBytes().length);

        // get the answer section
        DNSRecord[] nsARR = nsAResponseMsg.getAnswerRecords();

        if (nsARR.length == 0) {
            // System.out.println("USE Name");
            InetAddress ipAddr = InetAddress.getByName(nsName);
            return new DNSRecord(nsName, ipAddr.getHostAddress(), QueryType.A);
        }

        String nsDomainIpString = nsARR[0].getDomainIP();
        return new DNSRecord(nsName, nsDomainIpString, QueryType.A);

        
    }
    /**
     * Queries the servers in the provided DNSSlistSearch object until a response is
     * received.
     *
     * @param slistEntry the DNSSlistSearch object containing the list of servers
     *                   and the query packet
     * @return the DNSSlistSearch object with the updated server list and the
     *         response packet
     */
    private DNSSlistSearch queryServers(DNSSlistSearch slistEntry) throws SocketTimeoutException {
        // query the server in slist until a response is received
        // if
        List<DNSRecord> slist = slistEntry.getSlist();
        DatagramPacket queryPacket = slistEntry.getPacket();
        boolean responseFound = false;
        byte[] responseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket response = new DatagramPacket(responseBytes, responseBytes.length);
        if (slist.isEmpty()) {
            // no server left to query, all server timeout, return null
            return null;
        }

        for (int i = 0; i < slist.size(); i++) {
            DNSRecord nameServer = slist.get(i);
            if (nameServer.getQueryType() == QueryType.NS) {
                continue;
            }
            String nsDomainIpString = nameServer.getDomainIP();
            try {
                response = queryServer(nsDomainIpString, queryPacket);
                responseFound = true;
                return new DNSSlistSearch(slist, response);
            } catch (SocketException e) {
                // socket timeout, continue to query next server in the list
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                throw e;
            } catch (Exception e) {
                System.out.println("Other exception of socket in queryNsListserver ");
                // e.printStackTrace();
            }
        }

        // if response found from list, return response
        // otherwise return null response
        return (responseFound) ? new DNSSlistSearch(slist, response) : new DNSSlistSearch(slist, null);
    }

    private DatagramPacket queryServer(String nsDomainIpString, DatagramPacket clientQueryPacket) throws IOException {
        System.out.println("Querying server " + nsDomainIpString + " ...");
        byte[] responseBytes = new byte[MAX_DNS_PACKET_SIZE];
        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        clientQueryPacket.setAddress(InetAddress.getByName(nsDomainIpString));
        clientQueryPacket.setPort(53);
        socket.send(clientQueryPacket);
        socket.receive(responsePacket);
        socket.close();
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

    private DatagramPacket deepCopy(DatagramPacket originalPacket) {
        byte[] originalData = originalPacket.getData();
        int originalLength = originalPacket.getLength();
        InetAddress originalAddress = originalPacket.getAddress();
        int originalPort = originalPacket.getPort();

        byte[] newData = Arrays.copyOf(originalData, originalLength);
        return new DatagramPacket(newData, originalLength, originalAddress, originalPort);
    }

}
