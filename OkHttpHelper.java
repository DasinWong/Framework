import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpHelper {

    private static final String POST_FILE_KEY = "fileList"; //请求参数中文件的key
    private static final int MAX_LOAD_TIMES = 5; //下载超时重连次数限制
    private static final int CONN_TIMEOUT = 10;  //连接超时时间
    private static final int WRITE_TIMEOUT = 10; //写入超时时间
    private static final int READ_TIMEOUT = 10;  //读取超时时间
    private static OkHttpClient client = null;

    private OkHttpHelper() {
    }

    private static OkHttpClient getClient() {
        if (client == null) {
            synchronized (OkHttpHelper.class) {
                if (client == null)
                    client = new OkHttpClient.Builder()
                            .connectTimeout(CONN_TIMEOUT, TimeUnit.SECONDS)
                            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                            .build();
            }
        }
        return client;
    }

    /**
     * 异步回调接口
     */
    public interface OnCallListener {

        void onSuccess(String result);

        void onError(Exception e);
    }

    /**
     * 同步 get
     */
    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = getClient().newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * 异步 get
     */
    public static void get(String url, final OnCallListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = getClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                listener.onSuccess(response.body().string());
            }
        });
    }

    /**
     * 同步 post [map 形式]
     */
    public static String post(String url, Map<String, String> params) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getClient().newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * 异步 post [map 形式]
     */
    public static void post(String url, Map<String, String> params, final OnCallListener listener) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                listener.onSuccess(response.body().string());
            }
        });
    }

    /**
     * 同步 post [json 形式]
     */
    public static String post(String url, String json) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json))
                .build();
        Call call = getClient().newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * 异步 post [json 形式]
     */
    public static void post(String url, String json, final OnCallListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json))
                .build();
        Call call = getClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                listener.onSuccess(response.body().string());
            }
        });
    }

    /**
     * 同步 post [多文件上传]
     */
    public static String postFiles(String url, Map<String, String> params, List<File> fileList) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (params != null) {
            for (String key : params.keySet()) {
                builder.addFormDataPart(key, params.get(key));
            }
        }
        if (fileList != null && fileList.size() > 0) {
            for (File file : fileList) {
                RequestBody requestBody = RequestBody.create(MediaType.parse(getMimeType(file)), file);
                builder.addFormDataPart(POST_FILE_KEY, file.getName(), requestBody);
            }
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getClient().newCall(request);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * 异步 post [多文件上传]
     */
    public static void postFiles(String url, Map<String, String> params, List<File> fileList, final OnCallListener listener) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (params != null) {
            for (String key : params.keySet()) {
                builder.addFormDataPart(key, params.get(key));
            }
        }
        if (fileList != null && fileList.size() > 0) {
            for (File file : fileList) {
                RequestBody requestBody = RequestBody.create(MediaType.parse(getMimeType(file)), file);
                builder.addFormDataPart(POST_FILE_KEY, file.getName(), requestBody);
            }
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                listener.onSuccess(response.body().string());
            }
        });
    }

    /**
     * 下载回调接口
     */
    public interface OnDownloadListener {
        void onSuccess(String result);

        void onError(Exception e);

        void onLoading(int progress);
    }

    /**
     * 下载文件
     */
    public static void downloadFile(String fileUrl, final String filePath, final OnDownloadListener listener) {
        Request request = new Request.Builder()
                .url(fileUrl)
                .build();
        Call call = getClient().newCall(request);
        call.enqueue(new Callback() {
            int loadTimes = 0; //重新连接次数

            @Override
            public void onFailure(Call call, IOException e) {
                //超时重新连接
                if (e.getCause().equals(SocketTimeoutException.class) && loadTimes < MAX_LOAD_TIMES) {
                    loadTimes++;
                    getClient().newCall(call.request()).enqueue(this);
                } else {
                    listener.onError(e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                long contentLength = response.body().contentLength();
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(new File(filePath));
                    byte[] buffer = new byte[2048];
                    int len, sum = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / contentLength * 100);
                        listener.onLoading(progress);
                    }
                    fileOutputStream.flush();
                    listener.onSuccess(response.body().string());
                } catch (IOException e) {
                    listener.onError(e);
                } finally {
                    try {
                        if (inputStream != null)
                            inputStream.close();
                        if (fileOutputStream != null)
                            fileOutputStream.close();
                    } catch (Exception e) {
                        listener.onError(e);
                    }

                }
            }
        });
    }

    /**
     * 工具方法：通过文件获取文件类型
     */
    private static String getMimeType(final File file) {
        String extension = getExtension(file);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * 工具方法：通过文件获取文件后缀
     */
    private static String getExtension(final File file) {
        String extension = "";
        String name = file.getName();
        final int idx = name.lastIndexOf(".");
        if (idx > 0) {
            extension = name.substring(idx + 1);
        }
        return extension;
    }
}
