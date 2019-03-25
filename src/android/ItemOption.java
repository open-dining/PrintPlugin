package net.opendining.cordova;

public class ItemOption {
	public String name;
	public String group_name;
	public String price;
	public int quantity;
	public String selected_value;
	public String level;
	public boolean is_default_level;
	public String option_name;

	public String getDisplay(boolean optionNameOnly) {
		String displayName = name;

		if (optionNameOnly) {
			displayName = (level != null && !level.isEmpty() && !is_default_level) ? level + " " + option_name : option_name;
		}

		if (quantity > 1) {
			return quantity + " " + displayName;
		}

		return displayName;
	}
}