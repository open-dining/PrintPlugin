package net.opendining.cordova;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

public class Order {
	public String customer_name;
	public String customer_phone;
	public String customer_phone_display;
	public String email;
	public int short_id;
	public String id;
	public String order_date;
	public String eta;
	public String type;
	public String display_type;
	public String status;

	public String customer_address;
	public String address;
	public String address2;
	public String city;
	public String state;
	public String zip;
	public String special_instructions;

	public String paid;
	public String payment_type;
	public String payment_type_raw;
	public String signature_message;

	public String due_time;
	public String due_at;
	public long due_timestamp;
	public long fire_timestamp;
	public String fire_time;
	public String submit_time_display;
	public int time;
	public String courier_due;
	public int delivery_id;
	public String delivery_type;

	public BigDecimal subtotal;
	public BigDecimal delivery_fee;
	public BigDecimal fee;
	public BigDecimal tax;
	public BigDecimal tip;
	public BigDecimal total_amount;

	public String all_fields;

	public List<OrderField> fields;
	public List<Tax> active_taxes;
	public List<OrderCoupon> active_coupons;
	public List<OrderItem> items;
	public List<Reward> rewards;
	public List<Reward> comps;

	public boolean isAdvance() {
		return order_date != null && !order_date.isEmpty() && eta != null && !eta.isEmpty();
	}

	public String getDueDisplayString() {
		if (!isAdvance()) { // return ASAP display string
			String asapString = time > 0 ? " (" + time + " min) " : "";
			return "ASAP" + asapString;
		}

		Date orderDate = new Date(due_timestamp);

		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(orderDate);

		boolean sameDay = cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
							cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
							cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

		String prefix = sameDay ? "Today" : order_date;
		return prefix + " at " + eta;

	}
}