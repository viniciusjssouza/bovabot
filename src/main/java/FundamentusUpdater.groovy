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
        List<Fundamentus> ranking = []
        this.browser = new Browser()

        int currentMonth = LocalDate.now().monthValue
        int currentYear = LocalDate.now().year

        for (company in companies) {
            Optional<Fundamentus> f = dbConnection.getFundamentus(company, currentMonth, currentYear)
            if (!f.isPresent()) {
                f = this.getFundamentusFromWeb(company, currentMonth, currentYear)
            }
            if (f.isPresent())
                ranking.add(f.get());
        }
        ranking.sort {
            a,b -> b.successFactor.compareTo(a.successFactor)
        }
        return ranking.subList(0, Math.min(ranking.size(), rankingSize))
    }

    Optional<Fundamentus> getFundamentusFromWeb(String stock, int month, int year) {
        try {
            log.info("Updating fundamentus for '${stock}'")
            this.browser.go(String.format(QUERY_URL, stock))
            Fundamentus f = new Fundamentus(company: stock)
            def numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"))

            f.marketValue = new BigDecimal(
                numberFormat.parse(this.browser.$(".data.w3 .txt").getAt(1).text())
            )
            f.netAssets = new BigDecimal(
                    numberFormat.parse(
                            this.browser.$("table").getAt(3)
                                    .find(By.cssSelector("tr")).getAt(3)
                                    .find(By.cssSelector("td")).getAt(3)
                                    .find(By.cssSelector("span")).text()
                    )
            )
            if (f.marketValue == BigDecimal.ZERO || f.netAssets == BigDecimal.ZERO)
                throw new IllegalArgumentException("Zero value for stock ${stock}")

            this.save(f, String.format("%d%d", month, year))
            return new Optional<Fundamentus>(f)
        }catch (Exception e) {
            log.error("Error while getting data for stock ${stock}", e)
            return new Optional<Fundamentus>()
        }
    }

    def save(Fundamentus fundamentus, String month) {
        log.info("Saving information for stock ${fundamentus.company}")
        this.dbConnection.connection.executeInsert("""INSERT INTO fundamentus(company, month, marketValue,
            netAssets) VALUES (?, ?, ?, ?)""", [fundamentus.company, month, fundamentus.marketValue,
                fundamentus.netAssets])
    }

    @Canonical
    static class Fundamentus {
        String company
        BigDecimal marketValue;
        BigDecimal netAssets;

        BigDecimal getSuccessFactor() {
            return netAssets.div(marketValue)
        }

        @Override
        String toString() {
            def formatter = DecimalFormat.getNumberInstance(Locale.getDefault())
            return String.format("%s - Valor Mercado: %s \tPatr. Liquido: %s \t Fator: %.2f\n", company,
                formatter.format(marketValue), formatter.format(netAssets), getSuccessFactor())
        }
    }
}
