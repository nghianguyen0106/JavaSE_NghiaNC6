package dummy;

public class SMTP {
    private String host;
    private int port;
    private String username;
    private String password;

    public SMTP(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public SMTP(String host, String port, String username, String password) {
        this(host, parsePort(port), username, password);
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            return 587;
        }
    }

    public void send(String to, String subject, String content) {
        System.out.println("[SMTP] Sending email to: " + to + " | Subject: " + subject);
    }
}
