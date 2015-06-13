package com.ibm.ecm.extension.service.mail;

import java.util.List;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.ibm.json.java.JSONObject;

public class MailService {
	
	java.util.Properties emailProps = new java.util.Properties();
	private static String userName;
	private static String alias;
	private static String password;		
	
	public MailService(JSONObject mailSettings) throws Exception {
		emailProps = new java.util.Properties();
		emailProps.put("mail.smtp.auth", true);
		emailProps.put("mail.smtp.starttls.enable", (Boolean) mailSettings.get("starttls"));
		emailProps.put("mail.smtp.host", mailSettings.get("emailHost").toString());
		emailProps.put("mail.smtp.port", ((Long) mailSettings.get("emailPort")).intValue());
		emailProps.put("mail.smtp.sendpartial", true);	
		
		userName = mailSettings.get("emailFrom").toString();
		alias = mailSettings.get("emailAlias").toString();
		password = mailSettings.get("emailPassword").toString();		
	}
	
	public void sendTemplateMail(String to, String cc, String bcc, String fromAlias, String subject, Document template, List<String> templateNames, List<String> templateValues, List<Document> atts) throws Exception {
		
		Session session = Session.getInstance(emailProps, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		});
		
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(userName, (!isEmtpy(fromAlias) ? fromAlias : alias)));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		if (!isEmtpy(cc)) {
			message.setRecipients(Message.RecipientType.CC, 
					InternetAddress.parse(cc));
		}
		if (!isEmtpy(bcc)) {
			message.setRecipients(Message.RecipientType.BCC, 
					InternetAddress.parse(bcc));
		}			
		message.setSubject(subject);
		
		// Create related multipart
		Multipart multipart = new MimeMultipart("related");
		
		// Set html body part
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(getTemplateContent(template, templateNames.toArray(new String[0]), templateValues.toArray(new String[0])), "text/html" );	
		multipart.addBodyPart(htmlPart);
		
        // Set attachments
        for (Document doc : atts) {
			BodyPart attPart = new MimeBodyPart();
			attPart = getP8AttachmentPart(doc);
			if (attPart != null)
				multipart.addBodyPart(attPart);	        	
        }		
		
        // Set message multipart
	    message.setContent(multipart);

		// Send message
		Transport.send(message);
				
	}
	
    private BodyPart getP8AttachmentPart(Document att) throws Exception {	
		BodyPart attPart = new MimeBodyPart();
		com.filenet.api.core.ContentTransfer ct = getContentTransfer(att);
	    DataSource source = new ByteArrayDataSource((InputStream)ct.accessContentStream(), ct.get_ContentType());
	    attPart.setDataHandler(new DataHandler(source));
	    attPart.setFileName(ct.get_RetrievalName());
	    return attPart;
	} 	
	
	private ContentTransfer getContentTransfer(Document doc) throws Exception {
    	ContentElementList cel = doc.get_ContentElements();
    	ContentTransfer ct = (ContentTransfer)cel.get(0);
    	return ct;
	}	
	
    private String getTemplateContent(Document template, String[] templateNames, String[] templateValues) throws Exception {
    	String body =  IOUtils.toString(getContentTransfer(template).accessContentStream());
    	return replacePlaceHolders(body, templateNames, templateValues);	
    }
    
    private String replacePlaceHolders(String value, String templateNames[], String templateValues[]) throws Exception {
    	String result = value;
		String currentPattern;
		for (int i = 0; i < templateNames.length; i++) {
			currentPattern = "#" + templateNames[i] + "#";
			result = replaceContent(result, currentPattern, templateValues[i]);
		} 
		return result;
    }  
    
	private String replaceContent(String content, String from, String to) throws Exception {
		if (from == null || to == null)
			return content;	
		return StringUtils.replace(content, from, to);
	}	    
	
	private static boolean isEmtpy(String val) throws Exception {
		if (val == null)
			return true;
		else if (val.trim().equals(""))
			return true;
		
		return false;
	}		

}
