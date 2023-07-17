public class Resolver {
    public static void main(String[] args) {
        try {
            DNSResolver resolver = new DNSResolver(args);
            resolver.receiveAndResolve();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
