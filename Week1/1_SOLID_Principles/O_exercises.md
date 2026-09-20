
```Java
public class ReportGenerator {
    
    public void generate(String format, List<OrderData> data) {
        if (format.equals("PDF")) {
            // Code PDF
        } else if (format.equals("EXCEL")) {
            // Code Excel
        } else if (format.equals("JSON")) {
            // Code JSON
        }
    }
}
```

### **Hãy trả lời**

1. Lỡ thêm định dạng mới `CSV` thì cần sửa gì?
Phải chính sửa if và thêm case mới, vi phạm SRP và OCP.

2. Hãy refactor dùng interface.

```Java
public interface ReportFormater {
    void format(List<OrderData> data);
}

public class PDFFormater implements ReportFormater{
    @Override
    public void format(List<OrderData> data){
        // code PDF
    }
}

public class ExcelFormater implements ReportFormater{
    @Override
    public void format(List<OrderData> data){
        // code Excel
    }
}

public class JSONFormater implements ReportFormater{
    @Override
    public void format(List<OrderData> data){
        // code JSON
    }
}

public class ReportGenerator {
    public void generate(ReportFormater formater, List<OrderData> data){
        formater.format(data);
    }
}

```
3. Lợi ích của thiết kế mới là gì?
- Dễ test, dễ mở rộng, dễ bảo trì.
