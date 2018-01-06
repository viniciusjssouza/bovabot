import java.text.DecimalFormat

interface Ranking {
    def Ranking rank()
    def String asText()
}

abstract class BaseRanking implements Ranking {
    protected def List<Fundamentus> fundamentuses
    protected def size

    def BaseRanking(List<Fundamentus> fundamentuses, int size) {
        this.fundamentuses = new ArrayList<Fundamentus>(fundamentuses)
        this.size = size
        this.rank()
    }

    protected abstract BigDecimal getComparisonValue(Fundamentus fundamentus)

    Ranking rank() {
        this.fundamentuses = fundamentuses.sort {
            a,b -> (getComparisonValue(b) <=> getComparisonValue(a))
        }.subList(0, this.size)
        return this
    }

    String asText() {
        def formatter = DecimalFormat.getNumberInstance(Locale.getDefault())
        def result = new StringBuilder()
        for (f in this.fundamentuses) {
            def ratio = formatter.format(getComparisonValue(f))
            result.append("${f.toString()}\t Ratio: ${ratio}\n")
        }
        return result.toString()
    }
}

class AssetsRanking extends BaseRanking {

    def AssetsRanking(List<Fundamentus> fundamentuses, int size = 20) {
        super(fundamentuses, size)
    }

    def BigDecimal getComparisonValue(Fundamentus fundamentus) {
        return fundamentus.getAssetsMarketValueRatio()
    }

    String asText() {
        return "======== Ranking V. Mercado por Patrimônio ========\n${super.asText()}"
    }
}

class RevenueRanking extends BaseRanking {

    def RevenueRanking(List<Fundamentus> fundamentuses, size = 20) {
        super(fundamentuses, size)
    }

    def BigDecimal getComparisonValue(Fundamentus fundamentus) {
        return fundamentus.getRevenueMarketValueRatio()
    }

    String asText() {
        return "======== Ranking V. Mercado por Lucro 12 Meses ========\n${super.asText()}"
    }
}

class MixedRanking extends BaseRanking {

    def MixedRanking(List<Fundamentus> fundamentuses, size = 20) {
        super(fundamentuses, size)
    }

    def BigDecimal getComparisonValue(Fundamentus fundamentus) {
        return fundamentus.getMixedRatio()
    }

    String asText() {
        return "======== Ranking Misto ========\n${super.asText()}"
    }
}