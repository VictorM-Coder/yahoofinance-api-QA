package yahoofinance.histquotes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yahoofinance.utils.*;
import yahoofinance.YahooFinance;

/**
 *
 * @author Stijn Strickx
 */
public class HistQuotesRequest {
    private static final Logger log = LoggerFactory.getLogger(HistQuotesRequest.class);
    private final String symbol;
    private final Calendar from;
    private final Calendar to;
    private final Interval interval;
    public static final Calendar DEFAULT_FROM = Calendar.getInstance();

    static {
        DEFAULT_FROM.add(Calendar.YEAR, -1);
    }
    public static final Calendar DEFAULT_TO = Calendar.getInstance();
    public static final Interval DEFAULT_INTERVAL = Interval.MONTHLY;

    public HistQuotesRequest(String symbol) {
        this(symbol, DEFAULT_INTERVAL);
    }

    public HistQuotesRequest(String symbol, Interval interval) {
        this(symbol, DEFAULT_FROM, DEFAULT_TO, interval);
    }

    public HistQuotesRequest(String symbol, Calendar from, Calendar to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotesRequest(String symbol, Calendar from, Calendar to, Interval interval) {
        this.symbol = symbol;
        this.from = this.cleanHistCalendar(from);
        this.to = this.cleanHistCalendar(to);
        this.interval = interval;
    }

    public HistQuotesRequest(String symbol, Date from, Date to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotesRequest(String symbol, Date from, Date to, Interval interval) {
        this(symbol, interval);
        this.from.setTime(from);
        this.to.setTime(to);
        this.cleanHistCalendar(this.from);
        this.cleanHistCalendar(this.to);
    }
    
    /**
     * Put everything smaller than days at 0
     * @param cal calendar to be cleaned
     */
    private Calendar cleanHistCalendar(Calendar cal) {
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        return cal;
    }

    public List<HistoricalQuote> getResult() throws IOException {
        List<HistoricalQuote> result = new ArrayList<HistoricalQuote>();
        if(this.from.after(this.to)) {
            log.warn("Unable to retrieve historical quotes. "
                    + "From-date should not be after to-date. From: "
                    + this.from.getTime() + ", to: " + this.to.getTime());
            return result;
        }
        BufferedReader br = getBufferedReader();
        br.readLine(); // skip the first line
        // Parse CSV
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            log.info("Parsing CSV line: " + Utils.unescape(line));
            HistoricalQuote quote = RequestUtils.parseCSVLine(line, this.symbol);
            result.add(quote);
        }
        return result;
    }

    private BufferedReader getBufferedReader() throws IOException {
        String url = getUrl();
        return RequestUtils.getBufferedReader(url, getUrlConnection(url).getInputStream());
    }

    private static URLConnection getUrlConnection(String url) throws IOException {
        URL request = new URL(url);
        RedirectableRequest redirectableRequest = new RedirectableRequest(request, 5);
        redirectableRequest.setConnectTimeout(YahooFinance.CONNECTION_TIMEOUT);
        redirectableRequest.setReadTimeout(YahooFinance.CONNECTION_TIMEOUT);
        return redirectableRequest.openConnection();
    }

    private String getUrl() {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("s", this.symbol);
        params.put("a", String.valueOf(this.from.get(Calendar.MONTH)));
        params.put("b", String.valueOf(this.from.get(Calendar.DAY_OF_MONTH)));
        params.put("c", String.valueOf(this.from.get(Calendar.YEAR)));
        params.put("d", String.valueOf(this.to.get(Calendar.MONTH)));
        params.put("e", String.valueOf(this.to.get(Calendar.DAY_OF_MONTH)));
        params.put("f", String.valueOf(this.to.get(Calendar.YEAR)));
        params.put("g", this.interval.getTag());
        params.put("ignore", ".csv");

        return YahooFinance.HISTQUOTES_BASE_URL + "?" + Utils.getURLParameters(params);
    }
}
