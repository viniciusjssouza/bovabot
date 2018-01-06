import java.text.SimpleDateFormat

/**
 * Created by vinicius on 26/12/14.
 */
class Main {

    static void main(String[] args) {
        def db = new DBConnection()
        def companyRepository = new CompanyRepository()

        def fundamentusUpdater = new FundamentusUpdater(db, companyRepository.companies)
        def fundamentuses = fundamentusUpdater.update()

        println(new AssetsRanking(fundamentuses).asText())
        println(new RevenueRanking(fundamentuses).asText())
        println(new MixedRanking(fundamentuses).asText())
    }
}
