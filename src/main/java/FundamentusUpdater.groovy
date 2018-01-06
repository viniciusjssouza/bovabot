import geb.Browser
import groovy.transform.Canonical
import org.apache.log4j.Logger
import org.openqa.selenium.By

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate

/**
 * Created by vinicius on 17/07/16.
 */
class FundamentusUpdater {
    static final Logger log = Logger.getLogger(FundamentusUpdater.class)
    static final String QUERY_URL = "http://www.fundamentus.com.br/detalhes.php?papel=%s"

    String[] companies
    Browser browser
    DBConnection dbConnection;

    FundamentusUpdater(dbConnection, companies) {
        this.companies = companies;
        this.dbConnection = dbConnection;
    }

    List<Fundamentus> update(int rankingSize = 30) {
        List<Fundamentus> result = []
        this.browser = new Browser()

        int currentMonth = LocalDate.now().monthValue
        int currentYear = LocalDate.now().year

        for (company in companies) {
            Optional<Fundamentus> f = dbConnection.getFundamentus(company, currentMonth, currentYear)
            if (!f.isPresent()) {
                f = this.getFundamentusFromWeb(company, currentMonth, currentYear)
            }
            if (f.isPresent())
                result.add(f.get());
        }
        return result
    }

    Optional<Fundamentus> getFundamentusFromWeb(String stock, int month, int year) {
        try {
            log.info("Updating fundamentus for '${stock}'")
            this.browser.go(String.format(QUERY_URL, stock))
            Fundamentus f = new Fundamentus(company: stock)
            def numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"))

            def marketValueHtml = this.browser.$(".data.w3 .txt").getAt(1).text()
            f.marketValue = parseNumber(marketValueHtml)

            def netAssetsHtml = this.browser.$("table").getAt(3)
                    .find(By.cssSelector("tr")).getAt(3)
                    .find(By.cssSelector("td")).getAt(3)
                    .find(By.cssSelector("span")).text()
            f.netAssets = parseNumber(netAssetsHtml)

            def netRevenueHtml = this.browser.$("table").getAt(4)
                    .find(By.cssSelector("tr")).getAt(4)
                    .find(By.cssSelector("td")).getAt(1)
                    .find(By.cssSelector("span")).text()
            f.netRevenue = parseNumber(netRevenueHtml)

            if (f.marketValue == null || f.netAssets == null || f.netRevenue == null) {
                log.warn("Invalid values found for '${stock}'. It will be skipped")
                return Optional.empty()
            } else {
                this.save(f, String.format("%d%d", month, year))
                return new Optional<Fundamentus>(f)
            }
        }catch (Exception e) {
            log.error("Error while getting data for stock ${stock}", e)
            return new Optional<Fundamentus>()
        }
    }

    private static def parseNumber(strValue) {
        def numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"))
        if (strValue != null) {
            return new BigDecimal(numberFormat.parse(strValue).doubleValue())
        } else {
            return null
        }
    }

    def save(Fundamentus fundamentus, String month) {
        log.info("Saving information for stock ${fundamentus.company}")
        this.dbConnection.connection.executeInsert(
        """INSERT INTO fundamentus(company, month, marketValue, netAssets, netRevenue) 
               VALUES (?, ?, ?, ?, ?)""",
            [fundamentus.company, month, fundamentus.marketValue, fundamentus.netAssets, fundamentus.netRevenue]
        )
    }
}
