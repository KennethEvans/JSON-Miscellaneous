package misc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/*
 * Created on Nov 18, 2011
 * By Kenneth Evans, Jr.
 */

public class TestJson
{
//    private static String DEFAULT_IN_FILE = "C:/Users/evans/Documents/DAZ 3D/Studio4/Studio 4 Content/weights/DAZ 3D/Genesis/Base/Morphs/Kenneth Evans/KE Test/CloneVictoria4.KE.dsf";
    private static String DEFAULT_IN_FILE = "C:/Scratch/AAA/github-repos-2018-12-22.json";
    public static void read(String fileName) {
        try {
            JsonReader reader = new JsonReader(new FileReader(fileName));
            prettyprint(reader);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static String tabs(int level) {
        String tabs = "";
        for(int i = 0; i < level; i++) {
            tabs += " ";
        }
        return tabs;
    }

    static void prettyprint(JsonReader reader, JsonWriter writer)
        throws IOException {
        while(true) {
            JsonToken token = reader.peek();
            switch(token) {
            case BEGIN_ARRAY:
                reader.beginArray();
                writer.beginArray();
                break;
            case END_ARRAY:
                reader.endArray();
                writer.endArray();
                break;
            case BEGIN_OBJECT:
                reader.beginObject();
                writer.beginObject();
                break;
            case END_OBJECT:
                reader.endObject();
                writer.endObject();
                break;
            case NAME:
                String name = reader.nextName();
                writer.name(name);
                break;
            case STRING:
                String s = reader.nextString();
                writer.value(s);
                break;
            case NUMBER:
                String n = reader.nextString();
                writer.value(new BigDecimal(n));
                break;
            case BOOLEAN:
                boolean b = reader.nextBoolean();
                writer.value(b);
                break;
            case NULL:
                reader.nextNull();
                writer.nullValue();
                break;
            case END_DOCUMENT:
                return;
            }
        }
    }

    static void prettyprint(JsonReader reader) throws IOException {
        int level = 0;
        while(true) {
            JsonToken token = reader.peek();
            switch(token) {
            case BEGIN_ARRAY:
                reader.beginArray();
                System.out.println(tabs(level) + "BEGIN ARRAY");
                level++;
                break;
            case END_ARRAY:
                level--;
                reader.endArray();
                System.out.println(tabs(level) + "END ARRAY");
                break;
            case BEGIN_OBJECT:
                reader.beginObject();
                System.out.println(tabs(level) + "BEGIN OBJECT");
                level++;
                break;
            case END_OBJECT:
                level--;
                reader.endObject();
                System.out.println(tabs(level) + "END OBJECT");
                break;
            case NAME:
                String name = reader.nextName();
                System.out.println(tabs(level) + "NAME=" + name);
                break;
            case STRING:
                String s = reader.nextString();
                System.out.println(tabs(level) + "STRING=" + s);
                break;
            case NUMBER:
                String n = reader.nextString();
                System.out.println(tabs(level) + "NUMBER=" + new BigDecimal(n));
                break;
            case BOOLEAN:
                boolean b = reader.nextBoolean();
                System.out.println(tabs(level) + "BOOLEAN=" + b);
                break;
            case NULL:
                reader.nextNull();
                System.out.println(tabs(level) + "NULL");
                break;
            case END_DOCUMENT:
                return;
            }
        }
    }

    public static void write(String fileName) {
        JsonWriter writer;
        try {
            writer = new JsonWriter(new FileWriter(fileName));

            writer.beginObject(); // {
            writer.name("name").value("mkyong"); // "name" : "mkyong"
            writer.name("age").value(29); // "age" : 29

            writer.name("messages"); // "messages" :
            writer.beginArray(); // [
            writer.value("msg 1"); // "msg 1"
            writer.value("msg 2"); // "msg 2"
            writer.value("msg 3"); // "msg 3"
            writer.endArray(); // ]

            writer.endObject(); // }
            writer.close();

            System.out.println("Done");

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        read(DEFAULT_IN_FILE);
    }

}
