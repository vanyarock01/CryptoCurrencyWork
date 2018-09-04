import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AbstractSendRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import static java.lang.StrictMath.abs;

class SendMessage extends AbstractSendRequest<SendMessage> {

    public SendMessage(Object chatId, String text) {
        super(chatId);
        add("text", text);
    }

    public SendMessage parseMode(ParseMode parseMode) {
        return add("parse_mode", parseMode.name());
    }

    public SendMessage disableWebPagePreview(boolean disableWebPagePreview) {
        return add("disable_web_page_preview", disableWebPagePreview);
    }

}

class Staff {
    public double high;
    public double low;
    public double avg;
    public double vol;
    public double vol_cur;
    public double last;
    public double buy;
    public double sell;
    public long   updated;


    Staff(JSONObject arg) {
        this.high       = ((Number)arg.get("avg")).doubleValue();
        this.low        = ((Number)arg.get("low")).doubleValue();
        this.avg        = ((Number)arg.get("avg")).doubleValue();
        this.vol        = ((Number)arg.get("vol")).doubleValue();
        this.vol_cur    = ((Number)arg.get("vol_cur")).doubleValue();
        this.last       = ((Number)arg.get("last")).doubleValue();
        this.buy        = ((Number)arg.get("buy")).doubleValue();
        this.sell       = ((Number)arg.get("sell")).doubleValue();
        this.updated    = ((Number)arg.get("updated")).longValue();
    }
}

class StaffDiffer {
    public double last_buy;
    public double current_buy;
    public double percent_buy;
    public double last_sell;
    public double current_sell;
    public double percent_sell;

    StaffDiffer(double last, double current, double percent, double last_, double current_, double percent_ ) {
        this.last_buy    = last;
        this.current_buy = current;
        this.percent_buy = percent;

        this.last_sell    = last_;
        this.current_sell = current_;
        this.percent_sell = percent_;
    }
}

class Router {
    public String token;
    public String chat_id;

    Router(String token, String chat_id) {
        this.token   = token;
        this.chat_id = chat_id;
    }
    String MakeRequest(String text) {
        String pattern = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
        return String.format(pattern, this.token, this.chat_id, text);
    }
}

public class main {

    static StringBuffer GetRequest(String link) throws IOException {

        URL obj = new URL(link);

        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setUseCaches(false);
        connection.setRequestMethod  ("GET");

        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            return null;
        }
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response;
    }

    static StaffDiffer GettingPairSatistic(String pair) throws IOException, InterruptedException {

        String link = "https://yobit.io/api/3/ticker/" + pair;
        StringBuffer tmp;
        do {
            tmp = GetRequest(link);
        } while(tmp == null);

        String first = tmp.toString();

        JSONObject one  = (JSONObject) JSONValue.parse(first);
        JSONObject list = (JSONObject) one.get(pair);
        Staff ltc_one = new Staff(list);

        String second;
        JSONObject two;
        Staff ltc_two;

        while (true) {
            do {
                tmp = GetRequest(link);
                if (tmp.toString().substring(0, 4) == "Ddos") {
                    TimeUnit.SECONDS.sleep(60);
                    tmp = null;
                }
            } while(tmp == null);

            System.out.println(tmp.toString());
            two     = (JSONObject) JSONValue.parse(tmp.toString());
            list    = (JSONObject) two.get(pair);
            ltc_two = new Staff(list);

            System.out.println(ltc_two.buy);

            if ((ltc_one.buy != ltc_two.buy) && abs((ltc_two.buy - ltc_one.buy) / ltc_two.buy * 100) >= 3) break;
            TimeUnit.SECONDS.sleep(3);
        }

        //System.out.println((ltc_two.buy - ltc_one.buy) / ltc_two.buy * 100);

        double percent_buy  = (ltc_two.buy  - ltc_one.buy ) / ltc_two.buy  * 100;
        double percent_sell = (ltc_two.sell - ltc_one.sell) / ltc_two.sell * 100;

        return new StaffDiffer(ltc_one.buy, ltc_two.buy, percent_buy, ltc_one.sell, ltc_two.sell, percent_sell);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String token = "************************";
        String id    = "@***********************";

        Router tower = new Router(token, id);
        URL url;

        //String       link = tower.MakeRequest("HUI");
        //StringBuffer tmp  = GetRequest(link);

        String       pair = "liza_rur";

        while (true) {
            StaffDiffer total = GettingPairSatistic(pair);

            String text = "Pair: "           + "{ " + pair                               + " }" + "\n" +
                          "Current buy:\n"   + "{ " + String.valueOf(total.current_buy)  + " }" + "\n" +
                          "Last    buy:\n"   + "{ " + String.valueOf(total.last_buy)     + " }" + "\n" +
                          "Change  by:\n"    + "{ " + String.valueOf(total.percent_buy).substring(0,4)  + " }" + " percent\n" +
                          "Current sell:\n"  + "{ " + String.valueOf(total.current_sell) + " }" + "\n" +
                          "Last    sell:\n"  + "{ " + String.valueOf(total.last_sell)    + " }" + "\n" +
                          "Change  sell:\n"  + "{ " + String.valueOf(total.percent_sell).substring(0,4) + " }" + " percent\n";

            System.out.println(URLEncoder.encode(text));

            String link = tower.MakeRequest(URLEncoder.encode(text));
            StringBuffer tmp;

            do {
                tmp = GetRequest(link);
            }while(tmp == null);

            TimeUnit.SECONDS.sleep(60);
        }
    }
}
