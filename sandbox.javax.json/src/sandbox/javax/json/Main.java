package sandbox.javax.json;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;

public class Main {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) {
        JsonProvider provider = JsonProvider.provider();
        System.out.println(provider);

        JsonObject jsonObject = readJson();
        System.out.println("JsonObject --> " + jsonObject);
        if (jsonObject != null) {
            writeJson(jsonObject);
        }

        JsonObject jsonObject2 = buildJson();
        System.out.println("jsonObject2 --> " + jsonObject2);
        System.out.println("jsonObject == jsonObject2 ? " + Objects.equals(jsonObject, jsonObject2));
    }

    static JsonObject readJson() {
        JsonReaderFactory readerFactory = Json.createReaderFactory(new HashMap<>());
        try (JsonReader reader = readerFactory.createReader(new BufferedReader(new FileReader("person.txt")))) {
            JsonObject jsonObject = reader.readObject();
            return jsonObject;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static JsonObject buildJson() {
        JsonBuilderFactory factory = Json.createBuilderFactory(new HashMap<>());
        JsonObject jsonObject = factory.createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Smith")
                .add("age", 25)
                .add("address",
                        factory.createObjectBuilder()
                            .add("streetAddress", "21 2nd Street")
                            .add("city", "New York")
                            .add("state", "NY")
                            .add("postalCode", 10021))
                .add("phoneNumbers",
                        factory.createArrayBuilder()
                                .add(factory.createObjectBuilder()
                                        .add("type", "home")
                                        .add("number", "212 555-1234"))
                                .add(factory.createObjectBuilder()
                                        .add("type", "fax")
                                        .add("number", "646 555-4567")))
                .build();
        return jsonObject;
    }

    static void writeJson(JsonObject jsonObject) {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("javax.json.stream.JsonGenerator.prettyPrinting", Boolean.valueOf(true));
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);

        StringWriter sw = new StringWriter();
        try (JsonWriter writer = writerFactory.createWriter(sw)) {
            writer.write(jsonObject);
        }
        System.out.println(sw.toString());
    }
}