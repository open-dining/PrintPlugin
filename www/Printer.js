/*jslint indent: 2 */
/*global window, cordova */
"use strict";

var printer = {
	printOrder: function(order, printConfig, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'Printer',
			'printOrder',
			[order, printConfig]
		)
	},
	findPrinters: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'Printer',
			'findPrinters',
			[]
		)
	},
	sendToPrintConnect: function(zplBody, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'Printer',
			'sendToPrintConnect',
			[zplBody]
		)
	}
};

module.exports = printer;
