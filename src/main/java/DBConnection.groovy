import groovy.sql.Sql
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder

import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * Created by vinicius on 26/12/14.
 */
class DBConnection {

    def static final CONNECTION_URL = "jdbc:mysql://localhost:3306/bovespa"
    def static final USER_NAME = "bovabot"
    def static final PASSWORD = "griever"
    def static final DRIVER_NAME = "com.mysql.jdbc.Driver"

    def private Sql conn;

    DBConnection() {
        conn = Sql.newInstance(CONNECTION_URL, USER_NAME, PASSWORD, DRIVER_NAME)
    }

    def getLastUpdateDate(String company) {
        def result = conn.rows("""SELECT MAX(history_date) AS latest FROM history WHERE company LIKE ?""", company)
        if (result.isEmpty()) {
            return null;
        }
        return result[0]['latest']
    }

    def Optional<FundamentusUpdater.Fundamentus> getFundamentus(company, month, year) {
        def monthString = String.format("%d%d", month, year)
        def rows = conn.rows("""
            SELECT * FROM fundamentus
            WHERE company LIKE ? AND month LIKE ?
        """,  company, monthString)
        if (rows.isEmpty()) {
            return Optional.empty()
        } else {
            FundamentusUpdater.Fundamentus f = new FundamentusUpdater.Fundamentus(company:rows['company'],
                    netAssets: new BigDecimal(rows[0]['netAssets']),
                    marketValue: new BigDecimal(rows[0]['marketValue'])
            )
            return new Optional(f)
        }

    }

    def getConnection() {
        return conn;
    }

    def getCompanyStatistics(Date date) {
        def twoYearsAgo = GregorianCalendar.getInstance();
        twoYearsAgo.setTime(date);
        twoYearsAgo.add(Calendar.YEAR, -2)

        def dateFormat = new SimpleDateFormat("yyyy-MM-dd")
        def stats = conn.rows("""
            SELECT MIN(value) AS minVal, AVG(value) AS avgVal, company AS name
            FROM history h
            WHERE h.history_date >= ?
            GROUP BY company
        """, dateFormat.format(twoYearsAgo.getTime()))

        def result = [];
        for (row in stats) {
            def company = row['name']

            def latestVal = getLatestQuotation(company)

            if (latestVal == null) {
                throw new RuntimeException("Latest value not found for quote: " + company + ". Please, try to update.");
            }

            def resultRow = [
                name: company,
                minVal: row['minVal'],
                avgVal: row['avgVal'],
                current: latestVal,
                rate: latestVal / row['avgVal']
            ]
            result.add(resultRow);
        }
        result.sort {a, b -> return Double.compare(a['rate'], b['rate'])}
        return result.subList(0, 10)
    }

    def getLatestQuotation(company) {
        def url = String.format("http://www.bmfbovespa.com.br/Pregao-Online/ExecutaAcaoAjax.asp?CodigoPapel=%s", company);
        def http = new HTTPBuilder(url)
        try {
            def StringReader response = http.get(contentType: ContentType.TEXT);
            def first = true;
            def quotation = null;
            response.eachLine { line ->
                def matcher = Pattern.compile("Ultimo=\"(.*?)\" ").matcher(line)
                if (matcher.find()) {
                    quotation = Double.parseDouble(matcher.group(1).replace(",", "."));
                }
            }
            return quotation
        } catch(ex) {
            ex.printStackTrace();
        }
    }

    def close() {
        this.conn.close()
    }
}
