package before.dummy;

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

    public void send(String to, String subject, String content) {
        System.out.println("[SMTP] Sending email to: " + to + " | Subject: " + subject);
    }
}
