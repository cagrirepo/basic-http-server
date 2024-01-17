package dersler.basic.http.server;

import java.io.*;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.InvokerTransformer;

public class UnsafeSerializationExample {

    public static void main(String[] args) {
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");

        // Uyarı: Bu kod güvenli değildir ve öğretici amaçlıdır.
        try {
            // Güvenli olmayan Transformer nesnesi oluşturuluyor
            Transformer transformer = new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"calc.exe"});

            transformer.transform(null);
            
            // Nesneyi serialize etme
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(transformer);
            objectOutputStream.close();

            // Nesneyi deserialize etme
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Transformer deserializedTransformer = (Transformer) objectInputStream.readObject();
            objectInputStream.close();

            // Deserialized nesnenin metodu çağrılıyor
            deserializedTransformer.transform(null);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
