package github.kituin.chatimage.tool;

import com.madgag.gif.fmsware.GifDecoder;
import github.kituin.chatimage.exception.InvalidChatImageUrlException;

import java.io.*;
import java.util.concurrent.CompletableFuture;

import static github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static github.kituin.chatimage.tool.HttpUtils.CACHE_MAP;

public class ChatImageUrl {
    private String originalUrl;
    private String httpUrl;

    private UrlMethod urlMethod;
    private String fileUrl;


    public ChatImageUrl(String url) throws InvalidChatImageUrlException {
        this.originalUrl = url;
        init();
    }

    private void init() throws InvalidChatImageUrlException {
        File folder = new File(CONFIG.cachePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (this.originalUrl.startsWith("http://") || this.originalUrl.startsWith("https://")) {
            this.urlMethod = UrlMethod.HTTP;
            this.httpUrl = this.originalUrl;
            if (!CACHE_MAP.containsKey(this.httpUrl)) {
                boolean f = HttpUtils.getInputStream(this.httpUrl);
                if (!f) {
                    throw new InvalidChatImageUrlException("Invalid HTTP URL",
                            InvalidChatImageUrlException.InvalidUrlMode.HttpNotFound);
                }
            }

        } else if (this.originalUrl.startsWith("file:///")) {
            this.urlMethod = UrlMethod.FILE;
            this.fileUrl = this.originalUrl
                    .replace("\\", "\\\\")
                    .replace("file:///", "");
            File file = new File(this.fileUrl);
            if (file.exists()) {
                try {
                    if ("gif".equals(fileUrl.substring(fileUrl.length() - 3))) {
                        loadGif(new FileInputStream(file),this.fileUrl);
                    } else {
                        ChatImageFrame frame = new ChatImageFrame(new FileInputStream(file));
                        CACHE_MAP.put(this.fileUrl, frame);
                    }

                } catch (IOException e) {
                    throw new InvalidChatImageUrlException("file open error",
                            InvalidChatImageUrlException.InvalidUrlMode.FileNotFound);
                }
            } else {
                CACHE_MAP.put(this.fileUrl, new ChatImageFrame(ChatImageFrame.FrameError.FILE_NOT_FOUND));
            }

        } else {
            throw new InvalidChatImageUrlException(originalUrl + "<- this url is invalid, Please Recheck",
                    InvalidChatImageUrlException.InvalidUrlMode.NotFound);
        }
    }


    public String getOriginalUrl() {
        return this.originalUrl;
    }

    public UrlMethod getUrlMethod() {
        return this.urlMethod;
    }

    public String getUrl() {
        switch (this.urlMethod) {

            case FILE:
                return this.fileUrl;
            case HTTP:
                return this.httpUrl;
            default:
                return this.originalUrl;
        }

    }

    @Override
    public String toString() {
        return this.originalUrl;
    }
    public static void loadGif(InputStream is, String url){
        CompletableFuture.supplyAsync(() -> {
            try {
                GifDecoder gd = new GifDecoder();
                int status = gd.read(is);
                if (status != GifDecoder.STATUS_OK) {
                    return null;
                }
                ChatImageFrame frame = new ChatImageFrame(gd.getFrame(0));
                for (int i = 1; i < gd.getFrameCount(); i++) {
                    frame.append(new ChatImageFrame(gd.getFrame(i)));
                }
                CACHE_MAP.put(url, frame);

            } catch (IOException ignored) {
                CACHE_MAP.put(url, new ChatImageFrame(ChatImageFrame.FrameError.FILE_LOAD_ERROR));
            }
            return null;
        });
    }
    /**
     * Url的类型
     */
    public enum UrlMethod {
        /**
         * 本地文件 格式
         */
        FILE,
        /**
         * http(s)格式
         */
        HTTP,
    }

}
