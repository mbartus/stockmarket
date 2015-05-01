
package pkg.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.MarketHistory;
import pkg.market.api.IObserver;
import pkg.market.api.PriceSetter;
import pkg.util.OrderUtility;

import java.util.Map.Entry;

import pkg.trader.Trader;

public class OrderBook implements Comparator<Order> {
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;
	ArrayList<Order> stockList;

	public OrderBook(Market market) {
		this.market = market;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
		stockList = new ArrayList<Order>();
	}

	public void addToOrderBook(Order order) {
		if (order instanceof BuyOrder) {
			if (buyOrders.get(order.getStockSymbol()) != null) {
				stockList = buyOrders.get(order.getStockSymbol());
				stockList.add(order);
			} else {
				stockList = new ArrayList<Order>();
				stockList.add(order);
			}
			buyOrders.put(order.getStockSymbol(), stockList);
		} else {
			if (sellOrders.get(order.getStockSymbol()) != null) {
				stockList = sellOrders.get(order.getStockSymbol());
				stockList.add(order);
			} else {
				stockList = new ArrayList<Order>();
				stockList.add(order);
			}
			sellOrders.put(order.getStockSymbol(), stockList);
		}

	}

	public int compare(Order order1, Order order2) {
		if (order1.getPrice() == order2.getPrice()) {
			return 0;
		}
		return order1.getPrice() > order2.getPrice() ? 1 : -1;
	}

	public void trade()  {
		String tempSymbol = "";
		@SuppressWarnings("unchecked")
		ArrayList<Order> temp = (ArrayList<Order>) stockList.clone();
		Iterator<Order> it = temp.iterator();
		while (it.hasNext()){
			Order stockSymbol = it.next();	
			if(tempSymbol != stockSymbol.getStockSymbol()){
				tempSymbol = stockSymbol.getStockSymbol();
				ArrayList<Order> buyList = buyOrders.get(tempSymbol);
				ArrayList<Order> sellList = sellOrders.get(tempSymbol);
				HashMap<Double, Double> buyNumHash = new HashMap<Double, Double>();
				HashMap<Double, Double> sellNumHash = new HashMap<Double, Double>();
				ArrayList<Double> priceList = new ArrayList<Double>();
				double buyNum, sellNum, maxNumberOfTrades, maxPrice;
				PriceSetter priceSet = new PriceSetter();
				priceSet.registerObserver(m.getMarketHistory());
				m.getMarketHistory().setSubject(priceSet);
				Collections.sort(buyList, this);
				Collections.sort(sellList, this);
				Collections.reverse(sellList);
				
				// cycle through each buyList
				for (int i = 0; i < buyList.size() && i < sellList.size(); i++){
					if (buyList.get(i).getPrice() >= sellList.get(i).getPrice()){
						priceList.add(buyList.get(i).getPrice());
					}
					else {
						priceList.add(sellList.get(i).getPrice());
					}
					if (buyList.get(i) != null){
						buyNum += buyList.get(i).getSize();
						buyNumHash.put(buyList.get(i).getPrice(), buyNum);
					}
					if (sellList.get(i) != null){
						sellNum += sellList.get(i).getSize();
						sellNumHash.put(sellList.get(i).getPrice(), sellNum);
					}
				}
				// cycle through each pricelist
				for (int j = 0; j < priceList.size(); j++){
					buyNum = buyNumHash.get(priceList.get(j));
					if(sellNumHash.containsKey(priceList.get(j))){
						sellNum = sellNumHash.get(priceList.get(j));
					}
					if(buyNum > sellNum){
						if (sellNum > maxNumberOfTrades){
							maxNumberOfTrades = sellNum;
							maxPrice = priceList.get(j);
						}
					}
					else {
						if (buyNum > maxNumberOfTrades){
							maxNumberOfTrades = buyNum;
							maxPrice = priceList.get(j);

						}
					}
				}

				// set new price
				priceSet.setNewPrice(m, buyList.get(0).getStockSymbol(), maxPrice);
				
				try{
					for(int k = 0; k < buyList.size(); k++){
						if (buyList.get(k).getPrice() >= maxPrice){
							Trader trader = buyList.get(k).getTrader();
							if (trader != null)
							trader.tradePerformed(buyList.get(k), maxPrice);
						} else if (buyList.get(k).isMarketOrder){
							Trader trader = buyList.get(k).getTrader();
							if (trader != null)
							trader.tradePerformed(buyList.get(k), maxPrice);
							
						}
						
					}
					for (int k = 0;  k < sellList.size(); k ++){
						if (sellList.get(k).getPrice() <= maxPrice){
							Trader trader = sellList.get(k).getTrader();
							if (trader != null)
							trader.tradePerformed(sellList.get(k), maxPrice);
						} else if (sellList.get(k).isMarketOrder){
							Trader trader = sellList.get(k).getTrader();
							if (trader != null)
							trader.tradePerformed(sellList.get(k), maxPrice);
							
						}
					}
				}catch(StockMarketExpection e){
					e.printStackTrace();
				}
			}
		}
	}
}



