/**
 * Created by vinicius on 26/12/14.
 */
class DateUtils {

    static Calendar getHistoryStart(start) {
        def date = GregorianCalendar.getInstance();
        date.set(Calendar.YEAR, 2006);
        return date;
    }
}
