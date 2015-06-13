define([
		"dojo/_base/declare",
		"dojo/html",
		"dijit/Dialog",
		"dijit/form/Button",
		"dijit/_TemplatedMixin",
		"dijit/_WidgetsInTemplateMixin",
		"dojo/text!./templates/MessagesDialog.html"
	],
	function(declare, html, Dialog, Button, _TemplatedMixin, _WidgetsInTemplateMixin, template) {

	/**
	 * @name importSaadPluginDojo.MessagesDialog
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.MessagesDialog", [ Dialog, _TemplatedMixin, _WidgetsInTemplateMixin], {
	/** @lends importSaadPluginDojo.MessagesDialog.prototype */
	
		templateString: template,
		widgetsInTemplate: true,
	
		showMessage: function(title, message, img, w) {
			this.msgDialogTitle.innerHTML = title;
			this.msgDialogInfo.innerHTML = message;
			if (img != null)
				this.msgDialogImage.innerHTML = img;
			if (w != null)
				dojo.byId("msgDialogTable").width = w;			
			this.show();
		}
	
	});
});
