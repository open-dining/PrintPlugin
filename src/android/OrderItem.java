package net.opendining.cordova;

import java.math.BigDecimal;
import java.util.List;

public class OrderItem {
	public int quantity;
	public String category;
	public String size;
	public String name;
	public String notes;
	public BigDecimal total_price;

	public List<ItemOption> options;
}