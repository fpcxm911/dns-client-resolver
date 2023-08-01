public class Client {
    public static void main(String[] args) throws Exception {
        try {
            DNSClient client = new DNSClient(args);
            client.makeRequest();
        } catch (Exception e) {
            // e.printStackTrace();

            System.out.println("ERROR: Unable to resolve domain query.");
            if (e.getMessage() != null) {

                System.out.println(e.getMessage());
            }
        }
    }
}
