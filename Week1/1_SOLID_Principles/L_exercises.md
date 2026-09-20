
```Java
public abstract class Vehicle {
    public abstract void startEngine();
    public abstract void accelerate();
    public abstract void chargeBattery();
}

public class Car extends Vehicle {
    @Override
    public void startEngine() { /* Petrol engine */ }
    
    @Override
    public void accelerate() { /* ... */ }
    
    @Override
    public void chargeBattery() {
        throw new UnsupportedOperationException("Car has no battery");
    }
}

public class ElectricCar extends Vehicle {
    @Override
    public void startEngine() {
        throw new UnsupportedOperationException("Electric car has no gas engine");
    }
    
    @Override
    public void accelerate() { /* ... */ }
    
    @Override
    public void chargeBattery() { /* ... */ }
}
```

### **Hãy trả lời**

1. Class này vi phạm LSP ở đâu?
- Car phải thực thi cả chargeBattery nhưng lại ném ra Exception, tương tự với ElectricCar

2. Hãy refactor bằng interface.
```Java

public interface Vehicle {
    void accelerate();
}

public interface FuelVehicle {
    void startEngine();
}

public interface ElectricVehicle {
    void chargeBattery();
}

public class Car implements Vehicle, FuelVehicle {
    @override
    public void startEngine() { /* ... */ }
    
    @override
    public void accelerate() { /* ... */ }
}

public class ElectricCar implements Vehicle, ElectricVehicle {
    @override
    public void chargeBattery() { /* ... */ };
    
    @override
    public void accelerate() { /* ... */ }
}


```
3. Giải thích lợi ích của thiết kế mới.
- Dễ mở rộng, dễ bảo trì, dễ test ( như thêm loại xe mới mà không sợ ảnh hưởng đến hệ thống hiện tại)
