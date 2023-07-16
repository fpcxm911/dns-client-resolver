public class Client {
    public static void main(String args[]) throws Exception {
        try {
            DNSClient client = new DNSClient(args);
            client.makeRequest();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}


