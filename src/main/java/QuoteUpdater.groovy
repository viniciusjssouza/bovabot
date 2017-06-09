import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder

import java.text.SimpleDateFormat

/**
 * Created by vinicius on 26/12/14.
 */
class QuoteUpdater {
    def static final SITE_URL = "http://real-chart.finance.yahoo.com/table.csv?s=%s.SA&a=%s&b=%d&c=%d&d=%s&e=%d&f=%d&g=d&ignore=.csv";

    def final DBConnection db;
    def final CompanyRepository companyRepository;

    QuoteUpdater(dbConnection, companyRepository) {
        this.db = dbConnection
        this.companyRepository = companyRepository
    }

    def updateCompaniesHistory(Date end) {
        def begin = DateUtils.getHistoryStart(end)
        for (company in companyRepository.companies) {
            def lastUpdate = db.getLastUpdateDate(company);
            if (lastUpdate != null) {

                begin = new GregorianCalendar()
                begin.setTime(lastUpdate)
                begin.add(Calendar.DAY_OF_MONTH, 1)
            }

            def now = new GregorianCalendar();
            now.setTime(end)
            // must have one month of distance... If no, this will lead to an error on yahoo finance webservice.
            now.add(Calendar.MONTH, -1);
            if (now.after(begin)) {
                now.setTime(end)
                printf("Updating quote %s since %s........\n", company, lastUpdate.toString())
                this.getCsvFile(company, begin, now)
            } else {
                printf("Nothing to update for quote %s\n", company);
            }
        }
    }

    private getCsvFile(company, begin, now) {
        def beginMonth = String.valueOf(begin.get(Calendar.MONTH))
        if (beginMonth.length() == 1) {
            beginMonth = "0" + beginMonth;
        }

        def beginDay = begin.get(Calendar.DAY_OF_MONTH)
        def beginYear = begin.get(Calendar.YEAR)

        def endMonth = String.valueOf(now.get(Calendar.MONTH))
        if (endMonth.length() == 1) {
            endMonth = "0" + endMonth;
        }
        def endDay = now.get(Calendar.DAY_OF_MONTH)
        def endYear = now.get(Calendar.YEAR)

        db.connection.withBatch(100, 'INSERT INTO history(company, history_date, value) VALUES (?, ?, ?)' ) { ps ->
            String url = String.format(SITE_URL, company, beginMonth, beginDay, beginYear,
                    endMonth, endDay, endYear)
            println('Acessing ' + url)
            def http = new HTTPBuilder(url)
            try {
                def StringReader response = http.get(contentType: ContentType.TEXT);
                def first = true;
                def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
                response.eachLine { line ->
                    if (!first) {
                        def columns = line.split("\\,")
                        def date = dateFormatter.parse(columns[0])
                        def value = Double.valueOf(columns[6])
                        def values = [company, date, value]
                        println('Inserting ' + values)
                        ps.addBatch(values)
                    } else {
                        first = false;
                    }
                }
            } catch(ex) {
                ex.printStackTrace();
            }
        }
    }
}
