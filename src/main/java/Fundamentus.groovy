import groovy.transform.Canonical

import java.text.DecimalFormat

@Canonical
class Fundamentus {
    String company
    BigDecimal marketValue;
    BigDecimal netAssets;
    BigDecimal netRevenue;

    BigDecimal getAssetsMarketValueRatio() {
        return (netAssets ?: 0).div(marketValue)
    }

    BigDecimal getRevenueMarketValueRatio() {
        return (netRevenue ?: 0).div(marketValue)
    }

    BigDecimal getMixedRatio() {
        return BigDecimal.valueOf(200).multiply(getRevenueMarketValueRatio())+getAssetsMarketValueRatio()
    }

    @Override
    String toString() {
        def formatter = DecimalFormat.getNumberInstance(Locale.getDefault())
        return String.format("%s - Valor Mercado: %s \tPatr. Liquido: %s\t Lucro 12M: %s\n",
                company, formatter.format(marketValue), formatter.format(netAssets), formatter.format(netRevenue))
    }
}