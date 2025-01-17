package mobi.maptrek;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.InputStream;

import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.visorando.VisoRandoJson;
import mobi.maptrek.io.GPXManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VisoRandoApiCaller extends AsyncTask<String, Void, String> {
    private Context context;
    private Exception exception;
    private double latitude;
    private double longitude;
    private int hikeId;

    public VisoRandoApiCaller(Context context, double latitude, double longitude){
        this.context = context;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hikeId = 0;
    }

    protected String doInBackground(String... urls) {
        try {
            OkHttpClient client = new OkHttpClient();
            String bbox =  String.valueOf(longitude - 0.1)
                + "%2C"
                + String.valueOf(longitude + 0.1)
                + "%2C"
                + String.valueOf(latitude - 0.1)
                + "%2C"
                + String.valueOf(latitude + 0.1);
            try {
                String visoRandoUrlSearch = "https://www.visorando.com/en/index.php" +
                        "?component=rando&task=searchCircuitV2&geolocation=0&metaData=&minDuration=0&maxDuration=720&minDifficulty=1&maxDifficulty=5&loc=&retourDepart=0" +
                        "&bbox=" + bbox +
                        "&format=geojson";
                Request requestSearch = new Request.Builder()
                        .url(visoRandoUrlSearch)
                        .addHeader("Accept", "application/json;charset=UTF-8")
                        .addHeader("x-requested-with", "XMLHttpRequest")
                        .get()
                        .build();
                Response responseSearch = client
                        .newCall(requestSearch)
                        .execute();
                String searchResult = responseSearch.body().string();

                Gson g = new Gson();
                VisoRandoJson visoRandoJson = g.fromJson(searchResult, VisoRandoJson.class);
                if (visoRandoJson.geojson.features.size() > 0) {
                    hikeId = visoRandoJson.geojson.features.get(0).properties.id;

                    String visoRandoUrl = String.format(
                            "https://www.visorando.com/en/index.php?component=user&task=redirectToContent&from=gpxRando&idRandonnee=%d",
                            hikeId);

                    Request request = new Request.Builder()
                            .url(visoRandoUrl)
                            .get()
                            .build();
                    Response response = client.newCall(request).execute();
                    String php = response.body().string();
                    int indexClickHere = php.indexOf("click here");
                    String line = php.substring(indexClickHere - 150, indexClickHere);
                    int indexHref = line.indexOf("href");
                    String gpxUrl = line.substring(indexHref + 6, line.length() - 2);

                    Request requestGpx = new Request.Builder()
                            .url(gpxUrl)
                            .get()
                            .build();
                    Response responseGpx = client.newCall(requestGpx).execute();
                    String gpx = responseGpx.body().string();

                    File dir = MapTrek.getApplication().getExternalDir("data");
                    File mytempFile = new File(dir, hikeId+".gpx");
                    try {
                        Files.write(mytempFile.toPath(),  gpx.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        // Handle exception
                    }

                    return gpx;
                } else {
                    Log.d("VISORANDO: nothing found", String.valueOf(visoRandoJson.geojson.features.size()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            this.exception = e;

            return "";
        } finally {
        }
        return "";
    }

    protected void onPostExecute(String response) {
        Log.d("POST EXECUTE VISORANDO",response);
    }
}