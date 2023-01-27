package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class CrptApi {

    private final Semaphore semaphore;
    private final TimeUnit timeUnit;
    private final long timeLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit, long timeLimit) {
        this.semaphore = new Semaphore(requestLimit, true);
        this.timeUnit = timeUnit;
        this.timeLimit = timeLimit;
    }

    public String createDocument(Document document, String signature) throws InterruptedException {
        semaphore.acquire();

        boolean permit = semaphore.tryAcquire(timeLimit, timeUnit);
        if (!permit) {
           Thread.sleep(timeUnit.toMillis(timeLimit));
        }

        Gson gson = new Gson();
        String jsonDocument = gson.toJson(document);
        String responseString;

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String baseUrl = "https://markirovka.demo.crpt.tech/";
            String urlCreateDocument = "api/v3/lk/documents/create";
            HttpPost httpPost = new HttpPost(baseUrl+urlCreateDocument);
            httpPost.setEntity(new StringEntity(jsonDocument));

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("type", "LP_INTRODUCE_GOODS");
            httpPost.addHeader("Authorization", "Bearer " + signature);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            responseString = "Status code: " + response.getStatusLine().getStatusCode();

        } catch (IOException e ) {
            responseString = e.toString();
        } finally {
            semaphore.release();
        }
        return responseString;
    }

    class Document {
        Description description;
        String doc_id;
        String doc_status;
        String doc_type;
        boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        String production_date;
        ProductionType production_type;
        List<Product> products;
        String reg_date;
        String reg_number;
    }

    class Description {
        String participantInn;
    }

    class Product {
        CertificateType certificate_document;
        String certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        String production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    enum ProductionType {
        OWN_PRODUCTION,
        CONTRACT_PRODUCTION
    }

    enum CertificateType {
        CONFORMITY_CERTIFICATE,
        CONFORMITY_DECLARATION
    }
}
