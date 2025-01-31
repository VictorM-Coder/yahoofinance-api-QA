package yahoofinance.histquotes2;

import yahoofinance.utils.*;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stijn Strickx
 */
public class HistQuotes2Request {


    private static final Logger log = LoggerFactory.getLogger(HistQuotes2Request.class);
    private final String symbol;

    private final Calendar from;
    private final Calendar to;

    private final QueryInterval interval;

    public static final Calendar DEFAULT_FROM = Calendar.getInstance();

    static {
        DEFAULT_FROM.add(Calendar.YEAR, -1);
    }
    public static final Calendar DEFAULT_TO = Calendar.getInstance();
    public static final QueryInterval DEFAULT_INTERVAL = QueryInterval.MONTHLY;

    public HistQuotes2Request(String symbol) {
        this(symbol, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, QueryInterval interval) {
        this(symbol, DEFAULT_FROM, DEFAULT_TO, interval);
    }


    public HistQuotes2Request(String symbol, Calendar from, Calendar to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, Calendar from, Calendar to, QueryInterval interval) {
        this.symbol = symbol;
        this.from = this.cleanHistCalendar(from);
        this.to = this.cleanHistCalendar(to);
        this.interval = interval;
    }

    public HistQuotes2Request(String symbol, Date from, Date to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, Date from, Date to, QueryInterval interval) {
        this(symbol, interval);
        this.from.setTime(from);
        this.to.setTime(to);
        this.cleanHistCalendar(this.from);
        this.cleanHistCalendar(this.to);
    }

    // Constructors to support the old Interval
    public HistQuotes2Request(String symbol, Interval interval) {
        this(symbol, DEFAULT_FROM, DEFAULT_TO, interval);
    }

    public HistQuotes2Request(String symbol, Calendar from, Calendar to, Interval interval) {
        this(symbol, from, to, IntervalMapper.get(interval));
    }

    public HistQuotes2Request(String symbol, Date from, Date to, Interval interval) {
        this(symbol, from, to, IntervalMapper.get(interval));
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
        Map<String, String> params = configureParams();
        BufferedReader br = RequestUtils.getBufferedReaderByParams(params, symbol);
        br.readLine(); // skip the first line
        // Parse CSV
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            log.info("Parsing CSV line: " + Utils.unescape(line));
            HistoricalQuote quote = RequestUtils.parseCSVLine(line, this.symbol);
            result.add(quote);
        }
        return result;
    }

    private Map<String, String> configureParams() throws IOException {
        Map<String, String> params = RequestUtils.configureParamsPeriodsAndInterval(from, to, DEFAULT_INTERVAL);
        params.put("crumb", CrumbManager.getCrumb());
        return params;
    }

}
