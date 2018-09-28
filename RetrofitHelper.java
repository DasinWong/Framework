import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public class RetrofitHelper {

    private static final String FILE_LIST_KEY = "fileList"; //多文件上传字段
    private static final int TIMEOUT = 10; //超时时间
    private static volatile OkHttpClient mOkHttpClient;
    private static volatile OkHttpClient mDownloadClient;

    private RetrofitHelper() {
    }

    private static OkHttpClient getOkHttpClient() {
        if (mOkHttpClient == null) {
            synchronized (RetrofitHelper.class) {
                mOkHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .build();
            }
        }
        return mOkHttpClient;
    }

    private static Retrofit getDownloadRetrofit(DownloadListener listener) {
        if (mDownloadClient == null) {
            synchronized (RetrofitHelper.class) {
                mDownloadClient = new OkHttpClient.Builder()
                        .addInterceptor(new DownloadInterceptor(listener))
                        .retryOnConnectionFailure(true)
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .build();
            }
        }
        return new Retrofit.Builder()
                .baseUrl("http://www.dispensable.com/")
                .client(mDownloadClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    private static class StringConverter implements Converter<ResponseBody, String> {

        public static final StringConverter INSTANCE = new StringConverter();

        @Override
        public String convert(ResponseBody value) throws IOException {
            return value.string();
        }
    }

    /**
     * String类型转换工厂
     */
    public static class StringConverterFactory extends Converter.Factory {

        private static final StringConverterFactory INSTANCE = new StringConverterFactory();

        private static StringConverterFactory create() {
            return INSTANCE;
        }

        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (type == String.class) {
                return StringConverter.INSTANCE;
            }
            return null;
        }
    }

    /**
     * 通用的Get请求接口
     */
    private interface GetInterface {
        @GET("{path}")
        Observable<String> doGet(@Path("path") String path, @QueryMap Map<String, String> params);
    }

    /**
     * 通用的Post请求接口
     */
    private interface PostInterface {
        @FormUrlEncoded
        @POST("{path}")
        Observable<String> doPost(@Path("path") String path, @FieldMap Map<String, String> params);
    }

    /**
     * 通用的Post请求接口（Json数据传输）
     */
    private interface PostJsonInterface {
        @POST("{path}")
        @Headers("Content-Type:application/json; charset=utf-8")
        Observable<String> doPost(@Path("path") String path, @Body RequestBody json);
    }

    /**
     * 通用的文件上传接口
     */
    private interface PostFileInterface {
        @POST("{path}")
        @Multipart
        Observable<String> doPostFiles(@Path("path") String path, @PartMap Map<String, RequestBody> params, @Part List<MultipartBody.Part> files);
    }

    /**
     * 通用的文件下载接口
     */
    private interface DownloadInterface {
        @Streaming
        @GET
        Observable<ResponseBody> download(@Url String url);
    }

    /**
     * 请求回调接口
     */
    public interface RetrofitListener {

        void onSuccess(String result);

        void onError(Throwable e);
    }

    /**
     * 下载回调接口
     */
    public interface DownloadListener {

        void onLoading(int progress);

        void onSuccess();

        void onError(Throwable e);
    }

    /**
     * Get请求
     *
     * @param path   请求路径
     * @param params 请求参数Map形式
     */
    public static void get(String baseUrl, String path, Map<String, String> params, final RetrofitListener listener) {
        new Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(baseUrl)
                .addConverterFactory(StringConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(GetInterface.class)
                .doGet(path, params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        listener.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * Post请求
     *
     * @param path   请求路径
     * @param params 请求参数Map形式
     */
    public static void post(String baseUrl, String path, Map<String, String> params, final RetrofitListener listener) {
        new Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(baseUrl)
                .addConverterFactory(StringConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(PostInterface.class)
                .doPost(path, params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        listener.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * Post请求（Json数据传输）
     *
     * @param path   请求路径
     * @param params 请求参数Map形式
     */
    public static void postJson(String baseUrl, String path, Map<String, String> params, final RetrofitListener listener) {
        String jsonParams = new JSONObject(params).toString();
        new Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(baseUrl)
                .addConverterFactory(StringConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(PostJsonInterface.class)
                .doPost(path, RequestBody.create(MediaType.parse("Content-Type, application/json"), jsonParams))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        listener.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 多文件上传
     *
     * @param path   请求路径
     * @param params 请求参数Map形式
     * @param files  文件集合
     */
    public static void postFiles(String baseUrl, String path, Map<String, String> params, List<File> files, final RetrofitListener listener) {
        MediaType paramType = MediaType.parse("text/plain");
        MediaType fileType = MediaType.parse("multipart/form-data");
        Map<String, RequestBody> paramMap = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramMap.put(entry.getKey(), RequestBody.create(paramType, entry.getValue()));
        }
        List<MultipartBody.Part> partList = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            partList.add(MultipartBody.Part.createFormData(FILE_LIST_KEY, file.getName(), RequestBody.create(fileType, file)));
        }
        new Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(baseUrl)
                .addConverterFactory(StringConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(PostFileInterface.class)
                .doPostFiles(path, paramMap, partList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        listener.onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private static class DownloadResponseBody extends ResponseBody {

        private ResponseBody responseBody;

        private DownloadListener downloadListener;

        private BufferedSource bufferedSource;

        public DownloadResponseBody(ResponseBody responseBody, DownloadListener downloadListener) {
            this.responseBody = responseBody;
            this.downloadListener = downloadListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    if (null != downloadListener) {
                        if (bytesRead != -1) {
                            downloadListener.onLoading((int) (totalBytesRead * 100 / responseBody.contentLength()));
                        }

                    }
                    return bytesRead;
                }
            };
        }
    }

    /**
     * 下载拦截器
     */
    private static class DownloadInterceptor implements Interceptor {

        private DownloadListener downloadListener;

        public DownloadInterceptor(DownloadListener downloadListener) {
            this.downloadListener = downloadListener;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            return response.newBuilder().body(
                    new DownloadResponseBody(response.body(), downloadListener)).build();
        }
    }

    private static void writeFile(InputStream inputString, String filePath, DownloadListener listener) {

        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);

            byte[] b = new byte[1024];

            int len;
            while ((len = inputString.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            inputString.close();
            fos.close();
            listener.onSuccess();
        } catch (FileNotFoundException e) {
            listener.onError(e);
        } catch (IOException e) {
            listener.onError(e);
        }
    }

    /**
     * 文件下载
     *
     * @param url 文件动态地址(全路径)
     */
    public static void download(String fileUrl, final String filePath, final DownloadListener listener) {
        DownloadInterface service = getDownloadRetrofit(listener).create(DownloadInterface.class);
        service.download(fileUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<ResponseBody, InputStream>() {
                    @Override
                    public InputStream apply(ResponseBody responseBody) throws Exception {
                        return responseBody.byteStream();
                    }
                })
                .subscribe(new Observer<InputStream>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(InputStream inputStream) {
                        writeFile(inputStream, filePath, listener);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}