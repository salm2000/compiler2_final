package edu.compiler.integration;

import static org.junit.jupiter.api.Assertions.*;
import edu.compiler.CompilerService;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductApplicationTest {
    @TempDir Path temp;
    @Test void compilesAndServesCompleteProductWorkflow()throws Exception{
        Path example=Path.of("examples","products");Path output=temp.resolve("generated");
        var result=new CompilerService().compile(example.resolve("app.py"),example.resolve("templates"),example.resolve("static"),output);
        assertTrue(result.successful(),()->result.diagnostics().toString());
        int port;try(ServerSocket socket=new ServerSocket(0)){port=socket.getLocalPort();}
        Process server=new ProcessBuilder("python",output.resolve("app.py").toString(),Integer.toString(port)).redirectErrorStream(true).start();
        try{
            HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
            URI base=URI.create("http://127.0.0.1:"+port);waitUntilReady(client,base);
            HttpResponse<String> listing=get(client,base.resolve("/"));assertEquals(200,listing.statusCode());assertTrue(listing.body().contains("Notebook"));
            assertTrue(get(client,base.resolve("/products/1")).body().contains("$12.5"));
            assertEquals(200,get(client,base.resolve("/products/new")).statusCode());
            assertEquals(200,get(client,base.resolve("/static/style.css")).statusCode());
            HttpRequest add=HttpRequest.newBuilder(base.resolve("/products")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("name=%3Cb%3EDanger%3C%2Fb%3E&price=4.5&image_url=https%3A%2F%2Fexample.com%2Fphoto.png")).build();
            assertEquals(303,client.send(add,HttpResponse.BodyHandlers.ofString()).statusCode());
            String updated=get(client,base.resolve("/")).body();assertTrue(updated.contains("&lt;b&gt;Danger&lt;/b&gt;"));assertFalse(updated.contains("<b>Danger</b>"));
            assertTrue(updated.contains("https://example.com/photo.png"));
            HttpRequest delete=HttpRequest.newBuilder(base.resolve("/products/3/delete")).POST(HttpRequest.BodyPublishers.noBody()).build();
            assertEquals(303,client.send(delete,HttpResponse.BodyHandlers.ofString()).statusCode());
            assertFalse(get(client,base.resolve("/")).body().contains("Danger"));
            assertEquals(404,get(client,base.resolve("/unknown")).statusCode());
            assertEquals(405,get(client,base.resolve("/products")).statusCode());
            HttpRequest unsupported=HttpRequest.newBuilder(base.resolve("/")).PUT(HttpRequest.BodyPublishers.noBody()).build();
            assertEquals(405,client.send(unsupported,HttpResponse.BodyHandlers.ofString()).statusCode());
        }finally{server.destroy();if(!server.waitFor(5,TimeUnit.SECONDS))server.destroyForcibly();}
    }
    private void waitUntilReady(HttpClient client,URI base)throws Exception{
        Exception last=null;for(int i=0;i<30;i++){try{if(get(client,base.resolve("/")).statusCode()==200)return;}catch(Exception error){last=error;}Thread.sleep(100);}
        throw new AssertionError("generated server did not start",last);
    }
    private HttpResponse<String> get(HttpClient client,URI uri)throws Exception{return client.send(HttpRequest.newBuilder(uri).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
}

