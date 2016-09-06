package entities;

/**
 * Created by AnhQuan on 8/31/2016.
 */
public class Response {
    int status;
    Object message;

    public Response(int status, Object message) {
        this.status = status;
        this.message = message;
    }

    public Response() {
    }
}
