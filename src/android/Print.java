package net.opendining.cordova;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;

import android.util.Log;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Looper;

import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.os.Parcel;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;

import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.FilterOption;

public class Print extends CordovaPlugin implements ReceiveListener {
	public static final String ACTION_PRINT_ORDER = "printOrder";
	public static final String ACTION_FIND_PRINTERS = "findPrinters";
	public static final String ACTION_SEND_TO_PRINT_CONNECT = "sendToPrintConnect";

	private static final String TAG = "TestPrinter";
	private static final String TAG_ERROR = "TestPrinter ERROR";
	private static final String TAG_STACK_TRACE = "Stack Trace";

	private Context mContext = null;
	private Printer mPrinter = null;
	private PrintConfig printConfig = null;
	private JSONObject errors = new JSONObject();
	private JSONArray printerList = new JSONArray();

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Log.d(TAG, "--Initializing--");
		super.initialize(cordova, webView);
		mContext = this.cordova.getActivity();
	}

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		try {
			if (action.equals(ACTION_PRINT_ORDER)) {
				printOrder(args, callbackContext);
				return true;
			}

			if (action.equals(ACTION_FIND_PRINTERS)) {
				findPrinters(args, callbackContext);
				return true;
			}

			if (action.equals(ACTION_SEND_TO_PRINT_CONNECT)) {
				sendToPrintConnect(args, callbackContext);
				return true;
			}

			callbackContext.error("Invalid action");
			return false;

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}
	}

	private void findPrinters(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		stopDiscovery();
		Log.d(TAG, "--findPrinters--");
		FilterOption filterOption = new FilterOption();
		filterOption.setDeviceType(Discovery.TYPE_PRINTER);

		// Starts searching
		try {
			Discovery.start(mContext, filterOption, mDiscoveryListener);
		} catch (Epos2Exception e) {
			logException(e, "Discovery.start.");
		} catch (Exception e) {
			logException(e, "Discovery.start.");
		}

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "--success--");
				callbackContext.success(printerList);
				stopDiscovery();
			}
		}, 5000);
	}

	private DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
		@Override
		public void onDiscovery(final DeviceInfo deviceInfo) {
			Log.d(TAG, "--onDiscovery--");
			Log.d("getDeviceName", deviceInfo.getDeviceName());
			Log.d("getTarget", deviceInfo.getTarget());

			try {
				JSONObject printer = new JSONObject();
				printer.put("manufacturer", "epson");
				printer.put("model", deviceInfo.getDeviceName());

				if (!deviceInfo.getIpAddress().isEmpty()) {
					printer.put("deviceType", "network");
					printer.put("deviceName", deviceInfo.getIpAddress());
				}
				else if (!deviceInfo.getBdAddress().isEmpty()) {
					printer.put("deviceType", "bluetooth");
					printer.put("deviceName", deviceInfo.getBdAddress());
				}
				else {
					printer.put("deviceType", "unknown");
					printer.put("deviceName", deviceInfo.getTarget());
				}

				printerList.put(printer);
			}
			catch (JSONException e) {
			}
		}
	};

	private void stopDiscovery() {
		try {
			Discovery.stop();
		}
		catch (Epos2Exception e) {
			Log.d(TAG_ERROR, "Discovery stop error.");
		}
	}

	@Override
	public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public synchronized void run() {
				displayPrinterWarnings(status);

				new Thread(new Runnable() {
					@Override
					public void run() {
						disconnectPrinter();
					}
				}).start();
			}
		});
	}

	private void printOrder(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		final String orderString = args.getJSONObject(0).toString();
		final String printerString = args.getJSONObject(1).toString();
		Log.d(TAG, "--PrintOrder--");
		Log.d(TAG, orderString);
		Log.d(TAG, printerString);

		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				Log.d(TAG, "--Run--");

				Gson gson = new Gson();
				Order order = gson.fromJson(orderString, Order.class);
				printConfig = gson.fromJson(printerString, PrintConfig.class);

				if (!runPrintOrder(order)) {
					callbackContext.error(errors);
				} else {
					callbackContext.success();
				}
			}
		});
	}

	private boolean runPrintOrder(Order order) {
		if (!initializeObject()) {
			return false;
		}

		if (!createReceiptData(order)) {
			finalizeObject();
			return false;
		}

		if (!printData()) {
			finalizeObject();
			return false;
		}

		return true;
	}

	private boolean createReceiptData(Order order) {
		Log.d(TAG, "--CreateReceiptData--");

		try {
			StringBuilder builder = new StringBuilder();

			// Header

			mPrinter.addFeedLine(2);
			mPrinter.addTextAlign(Printer.ALIGN_CENTER);
			mPrinter.addTextSize(2, 2);
			builder.append("Online Order\n\n");

			mPrinter.addText(builder.toString());
			builder.delete(0, builder.length());

			if (order.isAdvance()) {
				mPrinter.addTextStyle(Printer.TRUE, Printer.FALSE, Printer.TRUE, Printer.PARAM_UNSPECIFIED);
				builder.append("!! ADVANCE ORDER !!\n\n");

				mPrinter.addText(builder.toString());
				builder.delete(0, builder.length());

				mPrinter.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.FALSE, Printer.PARAM_UNSPECIFIED);
			}

			builder.append(order.display_type + "\n\n");

			if (!"0".equals(order.paid) && !"No".equals(order.paid))
				builder.append("Prepaid\n\n");

			mPrinter.addText(builder.toString());
			builder.delete(0, builder.length());

			mPrinter.addTextSize(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);
			mPrinter.addTextAlign(Printer.ALIGN_LEFT);

			// Body

			builder.append("Order Number: " + order.short_id + "\n");
			builder.append("Customer: " + order.customer_name + "\n");
			builder.append("Phone: " + order.customer_phone_display + "\n");
			builder.append("Email: " + order.email + "\n");
			builder.append("Payment: " + order.payment_type + "\n");

			if (order.customer_address != null && !order.customer_address.isEmpty())
				builder.append("Address: " + order.customer_address + "\n");

			if (order.marketplace_proper_name != null && !order.marketplace_proper_name.isEmpty())
				builder.append("Marketplace Provider: " + order.marketplace_proper_name + "\n");

			if (order.delivery_id != null && !order.delivery_id.isEmpty())
				builder.append("Delivery Provider ID: " + order.delivery_id + "\n");

			if (order.special_instructions != null && !order.special_instructions.isEmpty())
				builder.append("Delivery Instructions: " + order.special_instructions + "\n");

			if (order.all_fields != null && !order.all_fields.isEmpty())
				builder.append(order.all_fields);

			if (order.submit_time_display != null && !order.submit_time_display.isEmpty())
				builder.append("Submitted: " + order.submit_time_display + "\n");

			builder.append("Due: " + order.getDueDisplayString() + "\n");

			if (order.courier_due != null && !order.courier_due.isEmpty())
				builder.append("Courier Delivery Due: " + order.courier_due + "\n");

			mPrinter.addText(builder.toString());
			builder.delete(0, builder.length());

			mPrinter.addFeedLine(2);

			// Items

			if (order.items != null) {
				for (OrderItem item : order.items) {
					String itemName = item.quantity > 0 ?
						item.quantity + " " + item.name : item.name;

					// bold the item name
					mPrinter.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.TRUE, Printer.PARAM_UNSPECIFIED);

					builder.append(itemName + "\n");
					mPrinter.addText(builder.toString());
					builder.delete(0, builder.length());

					mPrinter.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.FALSE, Printer.PARAM_UNSPECIFIED);

					if (item.size != null && !item.size.isEmpty())
						builder.append("Size: " + item.size + "\n");

					if (item.total_price != null && item.total_price.compareTo(BigDecimal.ZERO) > 0)
						builder.append("Item Total: " + formatMoney(item.total_price) + "\n");

					if (item.options != null)
						for (ItemOption option : item.options)
							builder.append("    " + option.getDisplay(printConfig.optionNameOnly) + "\n");

					if (item.notes != null && !item.notes.isEmpty())
						builder.append("Notes: " + item.notes + "\n");

					if (item.rewards != null)
						for (Reward reward : item.rewards)
							builder.append("Reward: " + reward.name + ": -" + formatMoney(reward.amount) + "\n");

					if (item.comps != null)
						for (Reward comp : item.comps)
							builder.append("Comp: " + comp.name + ": -" + formatMoney(comp.amount) + "\n");

					mPrinter.addText(builder.toString());
					builder.delete(0, builder.length());

					mPrinter.addFeedLine(2);
				}
			}

			// Footer

			mPrinter.addFeedLine(1);

			builder.append("Subtotal: " + formatMoney(order.subtotal) + "\n");

			if (order.rewards != null)
				for (Reward reward : order.rewards)
					builder.append("Reward: " + reward.name + ": -" + formatMoney(reward.amount) + "\n");

			if (order.comps != null)
				for (Reward comp : order.comps)
					builder.append("Comp: " + comp.name + ": -" + formatMoney(comp.amount) + "\n");

			if (order.active_coupons != null)
				for (OrderCoupon coupon : order.active_coupons)
					builder.append(coupon.name + ": -" + formatMoney(coupon.discount) + "\n");

			if (order.tax != null && order.tax.compareTo(BigDecimal.ZERO) > 0)
				builder.append("Tax: " + formatMoney(order.tax) + "\n");

			if (order.active_taxes != null)
				for (Tax tax : order.active_taxes)
					builder.append(tax.getDisplay() + ": " + formatMoney(tax.amount) + "\n");

			if (order.tip != null && order.tip.compareTo(BigDecimal.ZERO) > 0)
				builder.append("Tip: " + formatMoney(order.tip) + "\n");

			if (order.delivery_fee != null && order.delivery_fee.compareTo(BigDecimal.ZERO) > 0)
				builder.append("Delivery Fee: " + formatMoney(order.delivery_fee) + "\n");

			if (order.fee != null && order.fee.compareTo(BigDecimal.ZERO) > 0)
				builder.append("Order Fee: " + formatMoney(order.fee) + "\n");

			builder.append("Total: " + formatMoney(order.total_amount) + "\n");

			mPrinter.addText(builder.toString());
			builder.delete(0, builder.length());

			if (!"0".equals(order.paid) && !"No".equals(order.paid)) {
				mPrinter.addFeedLine(3);
				builder.append("x ___________________________\n");

				if (order.signature_message != null && !order.signature_message.isEmpty())
					builder.append(order.signature_message + "\n");

				mPrinter.addText(builder.toString());
				builder.delete(0, builder.length());
			}

			mPrinter.addFeedLine(3);
			mPrinter.addCut(Printer.PARAM_DEFAULT);

		} catch (Epos2Exception e) {
			logException(e, "Could not create receipt text.");
			return false;
		} catch (Exception e) {
			logException(e, "Could not create receipt text.");
			return false;
		}

		return true;
	}

	private boolean connectPrinter() {
		boolean isBeginTransaction = false;

		if (mPrinter == null) {
			return false;
		}

		String target = "";

		switch (printConfig.deviceType) {
			case "network":
				target = "TCP:";
				break;
			case "bluetooth":
				target = "BT:";
				break;
		}

		target += printConfig.deviceName;

		try {
			mPrinter.connect(target, Printer.PARAM_DEFAULT);
		} catch (Epos2Exception e) {
			logException(e, "Connect.");
			return false;
		} catch (Exception e) {
			logException(e, "Connect.");
			return false;
		}

		try {
			mPrinter.beginTransaction();
			isBeginTransaction = true;
		} catch (Epos2Exception e) {
			logException(e, "Begin Transaction.");
		} catch (Exception e) {
			logException(e, "Begin Transaction.");
		}

		if (isBeginTransaction == false) {
			try {
				mPrinter.disconnect();
			} catch (Epos2Exception e) {
				logException(e, "Disconnect.");
			} catch (Exception e) {
				logException(e, "Disconnect.");
			}
			return false;
		}

		return true;
	}

	private boolean initializeObject() {
		Log.d(TAG, "--initializeObject--");
		try {
			int series = getPrinterSeries(printConfig.model);
			mPrinter = new Printer(series, Printer.MODEL_ANK, mContext);
		} catch (Epos2Exception e) {
			logException(e, "Initialize Printer.");
			return false;
		} catch (Exception e) {
			logException(e, "Initialize Printer.");
			return false;
		}

		mPrinter.setReceiveEventListener(this);

		return true;
	}

	private void finalizeObject() {
		Log.d(TAG, "--finalizeObject--");
		if (mPrinter == null) {
			return;
		}

		mPrinter.clearCommandBuffer();

		mPrinter.setReceiveEventListener(null);

		mPrinter = null;
	}

	private void disconnectPrinter() {
		Log.d(TAG, "--disconnectPrinter--");
		if (mPrinter == null) {
			return;
		}

		try {
			mPrinter.endTransaction();
			Log.d(TAG, "endTransaction");
		} catch (final Epos2Exception e) {
			logException(e, "End Transaction.");
		} catch (Exception e) {
			logException(e, "End Transaction.");
		}

		try {
			mPrinter.disconnect();
			Log.d(TAG, "disconnect");
		} catch (final Epos2Exception e) {
			logException(e, "Disconnect.");
		} catch (Exception e) {
			logException(e, "Disconnect.");
		}

		finalizeObject();
	}

	private boolean printData() {
		Log.d(TAG, "--printData--");

		if (mPrinter == null) {
			return false;
		}

		if (!connectPrinter()) {
			return false;
		}

		PrinterStatusInfo status = mPrinter.getStatus();

		displayPrinterWarnings(status);

		if (!isPrintable(status)) {
			Log.d(TAG, "Printer is not connected.");

			try {
				mPrinter.disconnect();
			} catch (Epos2Exception e) {
				logException(e, "Disconnect.");
			} catch (Exception e) {
				logException(e, "Disconnect.");
			}
			return false;
		}

		try {
			mPrinter.sendData(Printer.PARAM_DEFAULT);
		} catch (Epos2Exception e) {
			try {
				mPrinter.disconnect();
			} catch (Exception ex) {
				logException(e, "Disconnect.");
			}
			return false;
		}

		return true;
	}

	private boolean isPrintable(PrinterStatusInfo status) {
		if (status == null) {
			return false;
		}

		if (status.getConnection() == Printer.FALSE) {
			return false;
		} else if (status.getOnline() == Printer.FALSE) {
			return false;
		}

		return true;
	}

	private String getError(int errStatus) {
		if (errStatus == Epos2Exception.ERR_PARAM)
			return "ERR_PARAM: Check the value specified in the parameter.";

		if (errStatus == Epos2Exception.ERR_CONNECT)
			return "ERR_CONNECT: Run disconnect for the appropriate class and then connect to restore communication.";

		if (errStatus == Epos2Exception.ERR_TIMEOUT)
			return "ERR_TIMEOUT: Set the timeout period to longer than the time required for printing.";

		if (errStatus == Epos2Exception.ERR_MEMORY)
			return "ERR_MEMORY: Close unnecessary applications.";

		if (errStatus == Epos2Exception.ERR_ILLEGAL)
			return "ERR_ILLEGAL: The function was used in an illegal way.";

		if (errStatus == Epos2Exception.ERR_PROCESSING)
			return "ERR_PROCESSING: Check if the process overlaps with another process in time.";

		if (errStatus == Epos2Exception.ERR_NOT_FOUND)
			return "ERR_NOT_FOUND: Check if the connection type and/or IP address are correct.";

		if (errStatus == Epos2Exception.ERR_IN_USE)
			return "ERR_IN_USE: Stop using the device from another application.";

		if (errStatus == Epos2Exception.ERR_TYPE_INVALID)
			return "ERR_TYPE_INVALID: Check the class of the connected device and connect to it with the correct device class. Check the system configuration, and specify the appropriate connection method.";

		if (errStatus == Epos2Exception.ERR_DISCONNECT)
			return "ERR_DISCONNECT: Check connection with the device.";

		if (errStatus == Epos2Exception.ERR_ALREADY_OPENED)
			return "ERR_ALREADY_OPENED: Finish communication with the communication box.";

		if (errStatus == Epos2Exception.ERR_ALREADY_USED)
			return "ERR_ALREADY_USED: Specify a different member ID.";

		return "ERR_FAILURE: Check for a problem with the execution environment.";
	}

	private void displayPrinterWarnings(PrinterStatusInfo status) {
		String warningsMsg = "";

		if (status == null) {
			return;
		}

		if (status.getPaper() == Printer.PAPER_NEAR_END) {
			warningsMsg += "Roll paper is nearly end.\n";
		}

		if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_1) {
			warningsMsg += "Battery level of printer is low.\n";
		}

		if (!warningsMsg.isEmpty()) {
			show(warningsMsg, mContext);
		}
	}

	private static void show(String msg, Context context) {
		if (Looper.myLooper() == null) {
			Looper.prepare();
		}

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setMessage(msg);
		alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				return;
			}
		});
		alertDialog.create();
		alertDialog.show();
	}

	private String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	private void logException(Epos2Exception exception, String message) {
		int status = exception.getErrorStatus();
		String errorMessage = message + " " + getError(status);

		try {
			errors.put(Integer.toString(status), errorMessage);
		} catch (JSONException e) {

		}

		Log.d(TAG_ERROR, errorMessage);
		Log.d(TAG, getStackTrace(exception));
	}

	private void logException(Exception exception, String message) {
		String errorMessage = message + " " + getStackTrace(exception);

		try {
			errors.put("internal", errorMessage);
		} catch (JSONException e) {}

		Log.d(TAG_ERROR, errorMessage);
	}

	private String formatMoney(BigDecimal bd) {
		return new DecimalFormat("0.00").format(bd);
	}

	private int getPrinterSeries(String target) {
		int series = Printer.TM_T88;

		switch (target) {
			case "TM-T88V":
				series = Printer.TM_T88;
				break;
			case "TM-T70":
			case "TM-T70II":
				series = Printer.TM_T70;
				break;
			case "TM-U220":
				series = Printer.TM_U220;
				break;
			case "TM-P20":
				series = Printer.TM_P20;
				break;
			case "TM-P60":
				series = Printer.TM_P60;
				break;
			case "TM-P60II":
				series = Printer.TM_P60II;
				break;
			case "TM-P80":
				series = Printer.TM_P80;
				break;
			case "TM-T20":
			case "TM-T20II":
				series = Printer.TM_T20;
				break;
			case "TM-T81II":
				series = Printer.TM_T81;
				break;
			case "TM-T82":
			case "TM-T82II":
				series = Printer.TM_T82;
				break;
			case "TM-T90II":
				series = Printer.TM_T90;
				break;
			case "TM-M10":
				series = Printer.TM_M10;
				break;
			case "TM-M30":
				series = Printer.TM_M30;
				break;
			case "TM-M30II":
				series = Printer.TM_M30II;
				break;
			case "TM-M50":
				series = Printer.TM_M50;
				break;
			default:
				break;
		}

		return series;
	}

	// As of version code 95 (version name 1.2.95), the PrintConnect app must be triggered in the foreground
	private static final long NEW_PRINT_CONNECT_VERSION = 95;

	private void sendToPrintConnect(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		final String zplString = args.getString(0);
		byte[] passthroughBytes = null;
		boolean requiresForeground = false;

		try {
			passthroughBytes = zplString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Handle exception
		}

		try {
			PackageManager packageManager = this.mContext.getPackageManager();
			PackageInfo info = packageManager.getPackageInfo("com.zebra.printconnect", 0);
			long versionCode = info.getLongVersionCode();
			if (versionCode >= NEW_PRINT_CONNECT_VERSION) {
				requiresForeground = true;
			}
		} catch (PackageManager.NameNotFoundException e) {
			callbackContext.error(e.getMessage());
		}

		Intent intent = new Intent();
		intent.setComponent(new ComponentName("com.zebra.printconnect",
				"com.zebra.printconnect.print.PassthroughService"));
		intent.putExtra("com.zebra.printconnect.PrintService.PASSTHROUGH_DATA", passthroughBytes);
		intent.putExtra("com.zebra.printconnect.PrintService.RESULT_RECEIVER",
				buildIPCSafeReceiver(new ResultReceiver(null) {
					@Override
					protected void onReceiveResult(int resultCode, Bundle resultData) {
						if (resultCode == 0) { // Result code 0 indicates success
							callbackContext.success();
						} else {
							// Handle unsuccessful print
							// Error message (null on successful print)
							String errorMessage = resultData.getString("com.zebra.printconnect.PrintService.ERROR_MESSAGE");
							callbackContext.error(errorMessage);
						}
					}
				}));

		if (requiresForeground) {
			mContext.startForegroundService(intent);
		} else {
			mContext.startService(intent);
		}
	}

	// This method makes your ResultReceiver safe for inter-process communication
	private ResultReceiver buildIPCSafeReceiver(ResultReceiver actualReceiver) {
		Parcel parcel = Parcel.obtain();
		actualReceiver.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
		parcel.recycle();
		return receiverForSending;
	}

}
