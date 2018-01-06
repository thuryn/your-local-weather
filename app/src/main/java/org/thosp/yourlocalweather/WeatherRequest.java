package org.thosp.yourlocalweather;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherRequest
{
    private static final String TAG = "WeatherRequest";

    private class AsyncGetRequest extends Thread {
        static final String USER_AGENT = "User-Agent";
        static final String USER_AGENT_TEMPLATE = "UnifiedNlp/%s (Linux; Android %s)";
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final URL url;
        private final Context context;
        private byte[] result;

        private AsyncGetRequest(URL url, Context context) {
            this.url = url;
            this.context = context;
        }

        @Override
        public void run() {
            synchronized (done) {
                try {
                    Log.d(TAG, "Requesting " + url);
                    appendLog(context, TAG, "weather url request:" + url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty(USER_AGENT, String.format(USER_AGENT_TEMPLATE, BuildConfig.VERSION_NAME, Build.VERSION.RELEASE));
                    connection.setDoInput(true);
                    InputStream inputStream = connection.getInputStream();
                    result = readStreamToEnd(inputStream);
                } catch (Exception e) {
                    Log.w(TAG, e);
                    }
                done.set(true);
                done.notifyAll();
            }
        }

        AsyncGetRequest asyncStart() {
            start();
            return this;
        }

        byte[] retrieveAllBytes() {
            if (!done.get()) {
                synchronized (done) {
                    while (!done.get()) {
                        try {
                            done.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
            return result;
        }

        String retrieveString() {
            byte[] result = retrieveAllBytes();
            if (result == null) {
                return "";
            }
            return new String(retrieveAllBytes());
        }

        private byte[] readStreamToEnd(InputStream is) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (is != null) {
                byte[] buff = new byte[1024];
                while (true) {
                    int nb = is.read(buff);
                    if (nb < 0) {
                        break;
                    }
                    bos.write(buff, 0, nb);
                }
                is.close();
            }
            return bos.toByteArray();
        }
    }
        
    String getWeatherByte(URL url, Context context) throws IOException {
        return new AsyncGetRequest(url, context).asyncStart().retrieveString();
    }

    public String getItems(String lat, String lon, String units, String lang, Context context) throws IOException {
        return getWeatherByte(Utils.getWeatherForecastUrl(Constants.WEATHER_ENDPOINT,
                                                          lat.replace(',', '.'),
                                                          lon.replace(',', '.'),
                                                          units,
                                                          lang), context);
    }
}
