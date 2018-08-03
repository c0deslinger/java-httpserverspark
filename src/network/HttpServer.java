package network;

import org.apache.commons.io.IOUtils;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Service;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {

    private static HttpServer instance;

    public static HttpServer getInstance() {
        if (instance == null) {
            instance = new HttpServer();
        }
        return instance;
    }

    private HttpServer() {
        Service http = Service.ignite();
        http.port(1337);
        http.threadPool(350);
        http.internalServerError("Error : 500 internal server error");

        http.get("/testHttp", new HttpHandler());
        http.post("/testHttp", new HttpHandler());
    }
}

class HttpHandler implements Route {
    public HttpHandler() {
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            //- Servlet 3.x config
            String location = "./upload";  // the directory location where files will be stored
            long maxFileSize = 100000000;  // the maximum size allowed for uploaded files
            long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
            int fileSizeThreshold = 1024;  // the size threshold after which files will be written to disk
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
            //-/
            Map<String, String> params = new HashMap<>();
            Collection<Part> parts;
            //handling http get
            for (String key : request.queryParams()) {
                params.put(key, request.queryParams(key));
            }
            //handling http post
            if (request != null && request.contentType() != null && request.contentType().contains("multipart/form-data")) {
                parts = request.raw().getParts();

                //file upload
                File directory = new File("./upload");
                if (!directory.exists()) {
                    directory.mkdir();
                }

                for (Part part : parts) {
                    if(part.getContentType()!=null){
                        Part uploadedFile = request.raw().getPart(part.getName());
                        String filepath_local = "./upload/"
                                + (System.currentTimeMillis() / 1000L) + "_" + part.getSubmittedFileName();
                        Path filepath_output = Paths.get(filepath_local);
                        try (final InputStream in = uploadedFile.getInputStream()) {
                            Files.copy(in, filepath_output);
                            uploadedFile.delete();
                            // cleanup
                            multipartConfigElement = null;
                            parts = null;
                            uploadedFile = null;
                        } catch (Exception e) {
                            response.status(200);
                            response.body("Internal Server Error - Unable to get uploaded file");
                            return response.body();
                        }
                    }
                    else {
                        params.put(part.getName(), IOUtils.toString(request.raw()
                                .getPart(part.getName()).getInputStream(), "UTF-8"));
                    }
                }
            }
            //check param
            for(String key : params.keySet()){
                System.out.println("param: "+key+" value: "+params.get(key));
            }
            response.status(200);
            response.body("ntap!");
        } catch (Exception e) {
            e.printStackTrace();
            String err_msg = "10 : Internal Error. " + e.toString();
            response.status(200);
            response.body(err_msg);
        }
        return response.body();
    }
}