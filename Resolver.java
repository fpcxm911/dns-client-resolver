public class Resolver {
    public static void main(String[] args) {
        try {
            DNSResolver resolver = new DNSResolver(args);
            resolver.run();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
