package catche;

import java.util.Date;


public class CatchControl {
	public static Date cardChangeHistoryCacheTime ;
	public static Date numberChangeHistoryCacheTime ;
	private static String cardChangeHistory;
	private static String numberChangeHistory;
	public static Date getCardChangeHistoryCacheTime() {
		return cardChangeHistoryCacheTime;
	}
	public static void setCardChangeHistoryCacheTime(Date cardChangeHistoryCacheTime) {
		CatchControl.cardChangeHistoryCacheTime = cardChangeHistoryCacheTime;
	}
	public static Date getNumberChangeHistoryCacheTime() {
		return numberChangeHistoryCacheTime;
	}
	public static void setNumberChangeHistoryCacheTime(
			Date numberChangeHistoryCacheTime) {
		CatchControl.numberChangeHistoryCacheTime = numberChangeHistoryCacheTime;
	}
	public static String getCardChangeHistory() {
		return cardChangeHistory;
	}
	public static void setCardChangeHistory(String cardChangeHistory) {
		CatchControl.cardChangeHistory = cardChangeHistory;
	}
	public static String getNumberChangeHistory() {
		return numberChangeHistory;
	}
	public static void setNumberChangeHistory(String numberChangeHistory) {
		CatchControl.numberChangeHistory = numberChangeHistory;
	}

	
}
