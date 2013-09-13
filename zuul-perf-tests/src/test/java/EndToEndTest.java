import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author HWEB
 */
public class EndToEndTest {

    @Test
    public void testCall() throws Exception {
        String responseBody = sendRequest("http://localhost:9091");
        assertEquals("OK", responseBody);
    }

    public String sendRequest(final String uri) throws Exception {
        URL url = new URL(uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseMessage();
    }

}
