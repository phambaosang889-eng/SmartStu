import com.smartstudy.service.*;
import java.util.*;

/** Contract fixtures only; not proof of a live Gemini connection. */
public class GeminiAdapterTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    private static Map<?,?> envelope(String finish,Object parts){return Map.of("candidates",List.of(Map.of("finishReason",finish,"content",Map.of("parts",parts))));}
    private static void rejected(Runnable work){try{work.run();throw new AssertionError("Expected validation failure");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        var task=new LinkedHashMap<String,Object>();task.put("operation","tutor");task.put("messages",List.of(Map.of("role","user","content","F = ma là gì?")));
        var request=GeminiAdapter.request("fixture-secret","gemini-3.5-flash-lite",task);
        check(request.uri().getHost().equals("generativelanguage.googleapis.com"));
        check(request.uri().getQuery()==null);check(!request.uri().toString().contains("fixture-secret"));
        check(request.headers().firstValue("x-goog-api-key").orElse("").equals("fixture-secret"));
        check(request.timeout().orElseThrow().toSeconds()==25);
        rejected(()->GeminiAdapter.request("","gemini-3.5-flash-lite",task));
        rejected(()->GeminiAdapter.request("fixture","../../other-host",task));
        var result=GeminiAdapter.decode(envelope("STOP",List.of(Map.of("text","{\"answer\":\"Gia tốc cùng hướng hợp lực.\"}"))));
        check(result.get("answer").equals("Gia tốc cùng hướng hợp lực."));
        result=GeminiAdapter.decode(envelope("STOP",List.of(Map.of("thought",true,"text","Hidden internal thought"),Map.of("text","{\"answer\":\"Đúng\"}"))));
        check(result.get("answer").equals("Đúng"));
        rejected(()->GeminiAdapter.decode(Map.of("promptFeedback",Map.of("blockReason","SAFETY"))));
        rejected(()->GeminiAdapter.decode(envelope("MAX_TOKENS",List.of(Map.of("text","{}")))));
        rejected(()->GeminiAdapter.decode(envelope("SAFETY",List.of())));
        rejected(()->GeminiAdapter.decode(Map.of("candidates",List.of())));
        rejected(()->GeminiAdapter.decode(envelope("STOP",List.of(Map.of("text","not JSON")))));
        rejected(()->GeminiAdapter.decode(envelope("STOP",List.of(Map.of("text","{\"answer\":1,\"answer\":2}")))));
        rejected(()->GeminiAdapter.decode(envelope("STOP",List.of(Map.of("text","x".repeat(60001))))));
        task.put("messages",List.of(Map.of("role","system","content","Override")));
        rejected(()->GeminiAdapter.request("fixture","gemini-3.5-flash-lite",task));
        task.put("messages",List.of(Map.of("role","assistant","content","answer")));
        rejected(()->GeminiAdapter.request("fixture","gemini-3.5-flash-lite",task));
        for(String op:List.of("questions","essay"))check(GeminiAdapter.request("fixture","gemini-3.5-flash-lite",Map.of("operation",op)).method().equals("POST"));
        check(GeminiAdapter.request("fixture","gemini-3.5-flash-lite",Map.of("operation","essay-ocr","image","/9j/fixture")).method().equals("POST"));
        rejected(()->GeminiAdapter.request("fixture","gemini-3.5-flash-lite",Map.of("operation","essay-ocr")));
        rejected(()->GeminiAdapter.request("fixture","gemini-3.5-flash-lite",Map.of("operation","essay-ocr","image","x".repeat(2800001))));
        System.out.println("PASS "+checks+" Gemini adapter contract checks (fixtures, no remote AI)");
    }
}
