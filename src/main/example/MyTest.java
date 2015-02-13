import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

public class MyTest extends WebSocketClient {

    public MyTest(URI uri, Draft d) {
        super(uri);
    }

    public static void main(String[] args) throws Exception {
        WebSocketClient client = new MyTest(new URI(
                "wss://localhost:443/connect"), new Draft_17());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null,
                new TrustManager[] { new LocalSSLTrustManager() }, null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        client.setSocket(factory.createSocket());
        client.connect();

        Gson gson = new Gson();
        RandomJSON rjson = new RandomJSON();

        while (true) {
            Thread.sleep(1 * 1000);
            Object o = rjson.createRandomObject(1, 150, 0.0);
            client.send(gson.toJson(o));
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Open: " + handshakedata.toString());
    }

    @Override
    public void onMessage(String message) {
        System.out.println("receiving: " + message);
        send(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed: " + code + " " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("error: " + ex.toString());
    }
}

class LocalSSLTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] cert, String authType)
            throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] cert, String authType)
            throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

class RandomJSON {
    private static Random random = new Random(0l);

    private String memberName(int i) {
        return "member" + i;
    }

    private double complexObjectProbality(int depth) {
        return 0.05 / depth * depth;
    }

    public Object createRandomObject(int depth, int memberCount, double rnd) {
        Object value;
        if (rnd < complexObjectProbality(depth)) {
            if (random.nextBoolean()) {
                Map m = new HashMap();
                for (int i = 0; i < memberCount; i++) {
                    m.put(memberName(i),
                            createRandomObject(depth + 1, memberCount / 2,
                                    random.nextDouble()));
                }
                value = m;
            } else {
                List l = new ArrayList(memberCount);
                for (int i = 0; i < memberCount; i++) {
                    l.add(createRandomObject(depth + 1, memberCount / 2,
                            random.nextDouble()));
                }
                value = l;
            }
        } else {
            rnd = random.nextDouble();

            if (rnd < .75) {
                int len = (int) Math.sqrt(memberCount);
                StringBuilder sb = new StringBuilder(len);
                for (int i = 0; i < len; i++) {
                    char c;
                    do {
                        c = (char) random.nextInt(65536);
                    } while (Character.isISOControl(c));
                    sb.append(c);
                }

                value = sb.toString();
            } else if (rnd < .9) {
                value = random.nextLong();
            } else {
                switch (random.nextInt(3)) {
                default:
                case 0:
                    value = true;
                    break;
                case 1:
                    value = false;
                    break;
                case 2:
                    value = null;
                    break;
                }
            }

        }
        return value;
    }
}