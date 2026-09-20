Bài tập:
```java
public class UserService {
    
    public void createUser(String name, String email, String password) {
        // Validate email
        if (!email.contains("@")) {
            throw new Exception("Invalid email");
        }
        
        // Hash password
        String hashedPassword = hashPassword(password);
        
        // Save to database
        User user = new User(name, email, hashedPassword);
        saveToDatabase(user);
        
        // Send welcome email
        sendWelcomeEmail(email);
        
        // Log activity
        logger.info("User created: " + email);
        
        // Update user count
        incrementUserCount();
    }
}
```
1. Class này có bao nhiêu trách nhiệm? 2. Nó sẽ cần thay đổi trong những trường hợp nào?
- Có 6 trách nhiệm:
    - Validate email format
        - Sẽ cần thay đổi trong trường hợp: Cập nhật format email, vd: tên email không chứa số, cần check thêm domain, ...
    - Create new user's information
        - Sẽ cần thay đổi trong trường hợp: Thêm thông tin đăng ký cho user, vd: username, sdt, địa chỉ, ...
    - Save user's information to database
        - Sẽ cần thay đổi trong trường hợp: Cập nhật DB
    - Hash password
        - Sẽ cần thay đổi trong trường hợp: Thay đổi thuật toán hash password
    - Send welcome email
        - Sẽ cần thay đổi trong trường hợp: Cập nhật/ thêm phương thức gửi mail, hoặc cập nhật template email
    - Log activity
        - Sẽ cần thay đổi trong trường hợp: Cập nhật/ thêm phương thức log
    - Update user count
        - Sẽ cần thay đổi trong trường hợp: Cập nhật/ thêm phương thức đếm user

3. Hãy refactor thành các class nhỏ hơn.

```java
// package Interface
public interface EmailService {
    void sendEmail(String to, String subject, String body);
}

public interface UserRepository {
    void save(User user);
}

public interface PasswordHasher {
    String hashPassword(String password);
}

public interface EmailValidator {
    boolean isValid(String email);
}

public interface Logger {
    void log(String message);
}

// package Repository
public class DefaultUserRepository implements UserRepository {
    @Override
    public void save(User user) {
    }
}

// package Validator
public class UserEmailValidator implements EmailValidator {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    @Override
    public boolean isValid(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }
}

// package Service
public class DefaultEmailService implements EmailService {
    public void sendEmail(String to, String subject, String body) {
    }
}

public class UserRegistration {
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserEmailValidator  userEmailValidator;
    
    public UserRegistration(EmailService emailServ, UserRepository userRes, UserEmailValidator userEmailVald) {
        this.emailServ = emailServ;
        this.userRes = userRes;
        this.userEmailVald = userEmailVald;
    }
    
    public void registerUser(String name, String email, String password) {
        if (!userEmailVald.isValid(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        String hashedPassword = hashPassword(password);

        User user = new User(name, email, hashedPassword);
        userRes.save(user);
        emailServ.sendEmail(email, "Welcome", "Thank you for registering");
    }
}

```
