/**
 * Created by John on 9/19/2015.
 */
import java.io.*;
// class to create message objects to send through streams
public class ChatMsg implements Serializable {

    protected static final long serialVersionUID = 10000000L;

    private int type;
    private String message;

    // constructor
    ChatMsg(int type, String msg) {
        this.type = type;
        this.message = msg;
    }

    // getters
    int getType() {
        return type;
    }

    String getMessage() {
        return message;
    }
}