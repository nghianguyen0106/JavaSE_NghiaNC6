## **Bài tập**

Java

```Java
public interface Animal {
    void eat();
    void run();
    void fly();
    void swim();
}

public class Dog implements Animal {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void run() { /* ... */ }
    
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Dog cannot fly");
    }
    
    @Override
    public void swim() { /* ... */ }
}

public class Bird implements Animal {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void run() {
        throw new UnsupportedOperationException("Bird cannot run");
    }
    
    @Override
    public void fly() { /* ... */ }
    
    @Override
    public void swim() {
        throw new UnsupportedOperationException("Bird cannot swim");
    }
}
```

### **Hãy trả lời**

1. Interface này vi phạm ISP ở đâu?
- Dog buộc phải dùng fly(), Bird buộc phải dùng run(), swim() trong khi không cần thiết

2. Hãy tách thành các interface nhỏ hơn.

```Java
interface 
public interface Animal {
    void eat();
}

public interface canRun {
    void run();
}

public interface canFly {
    void fly();
}

public interface canSwim {
    void swim();
}

public class Dog implements Animal, canRun, canSwim {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void run() { /* ... */ }
    
    @Override
    public void swim() { /* ... */ }
}

public class Bird implements Animal, canFly {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void fly() { /* ... */ }
    
}
```

3. Giải thích lợi ích.
- Không có class nào phải implements method không cần thiết
- Dễ dàng mở rộng hệ thống nếu sau này có thêm các loài động vật mới
- Các interface trở nên dễ test hơn vì chúng chỉ có một trách nhiệm (SRP)
