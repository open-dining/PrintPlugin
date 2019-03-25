package net.opendining.cordova;

import java.math.BigDecimal;
 import java.util.List;

 public class Tax {
 	public String name;
 	public String tax_id;
 	public BigDecimal amount;

 	public String getDisplay() {
 		if (tax_id == null || tax_id.isEmpty())
 			return name;

 		return name + " (" + tax_id + ")";
 	}
 }