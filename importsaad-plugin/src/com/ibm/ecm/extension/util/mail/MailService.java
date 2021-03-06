package com.ibm.ecm.extension.util.mail;

import java.util.List;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
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
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import com.excelecm.common.util.CommonUtils;

import com.filenet.api.core.Document;
import com.ibm.json.java.JSONObject;
import com.sun.mail.util.MailSSLSocketFactory;
import com.ibm.ecm.extension.service.ContentService;

public class MailService {
	
	java.util.Properties emailProps = new java.util.Properties();
	private static String emailFrom;
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
		
        // avoid cert check
        try {
            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            emailProps.put("mail.smtp.ssl.socketFactory", sf);
        } catch (Exception e) {
        }		
		
		emailFrom = mailSettings.get("emailFrom").toString();
		userName = emailFrom;
		if (mailSettings.containsKey("emailUser"))
			userName = mailSettings.get("emailUser").toString();
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
		message.setFrom(new InternetAddress(emailFrom, (!CommonUtils.isEmtpy(fromAlias) ? fromAlias : alias)));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		if (!CommonUtils.isEmtpy(cc)) {
			message.setRecipients(Message.RecipientType.CC, 
					InternetAddress.parse(cc));
		}
		if (!CommonUtils.isEmtpy(bcc)) {
			message.setRecipients(Message.RecipientType.BCC, 
					InternetAddress.parse(bcc));
		}			
		message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "Q"));
		
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
	
	public void sendTemplateMail(String host, String to, String cc, String bcc, String fromAlias, String subject, Document template, List<String> templateNames, List<String> templateValues, List<Document> atts) throws Exception {
		
        // Get session
        Session session = Session.getDefaultInstance(emailProps, null);
        
        // Create message
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(emailFrom, (!CommonUtils.isEmtpy(fromAlias) ? fromAlias : alias)));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		if (!CommonUtils.isEmtpy(cc))
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		if (!CommonUtils.isEmtpy(bcc)) {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
		}		
		message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "Q"));	
		
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

        // Get transport and connect
        Transport transport = session.getTransport("smtp");
        transport.connect(host, userName, password);
        
        // send message
        transport.sendMessage(message, message.getAllRecipients());
        
        // close transport
        transport.close();
	}	
	
	public void sendEmail(String to, String fromAlias, String cc, String bcc, String subject, String body, List<String> atts, List<String> embeddedImages) throws Exception {
		Session session = Session.getInstance(emailProps, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		});
		 
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(emailFrom, (!CommonUtils.isEmtpy(fromAlias) ? fromAlias : alias)));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		if (!CommonUtils.isEmtpy(cc))
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		if (!CommonUtils.isEmtpy(bcc)) {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
		}		
		message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "Q"));		

		// Create related multipart
		Multipart multipart = new MimeMultipart("related");
		
		// Set html body part
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(body, "text/html");
		multipart.addBodyPart(htmlPart);
		
		// Set inline images
		String[] embeddedImagesArr = embeddedImages.toArray(new String[0]);
        if (!CommonUtils.isArrayEmpty(embeddedImagesArr)) {
	        for (int i = 0; i < embeddedImagesArr.length; i++) {
	        	if (!CommonUtils.isEmtpy(embeddedImagesArr[i])) {
	    	        BodyPart imgPart = new MimeBodyPart();
	        		imgPart = getImagePart(embeddedImagesArr[i]);
			        multipart.addBodyPart(imgPart);
	        	}
	        }
        }		        

        // Set attachments
        String[] attArr = atts.toArray(new String[0]);
        if (!CommonUtils.isArrayEmpty(attArr)) {
			for (int i = 0; i < attArr.length; i++) {
				if (!CommonUtils.isEmtpy(attArr[i])) {
					BodyPart attPart = new MimeBodyPart();
					attPart = getAttachmentPart(attArr[i]);
				    multipart.addBodyPart(attPart);
				}
			}	     
        }		

        // Set message multipart
	    message.setContent(multipart);

		// Send message
		Transport.send(message); 		
	}	
	
	public void sendEmail(String host, String to, String fromAlias, String cc, String bcc, String subject, String body, List<String> atts, List<String> embeddedImages) throws Exception {
        // Get session
        Session session = Session.getDefaultInstance(emailProps, null);
        
        // Create message
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(emailFrom, (!CommonUtils.isEmtpy(fromAlias) ? fromAlias : alias)));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		if (!CommonUtils.isEmtpy(cc))
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		if (!CommonUtils.isEmtpy(bcc)) {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
		}		
		message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "Q"));	
		
		// Create related multipart
		Multipart multipart = new MimeMultipart("related");
		
		// Set html body part
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(body, "text/html");
		multipart.addBodyPart(htmlPart);
		
		// Set inline images
		String[] embeddedImagesArr = embeddedImages.toArray(new String[0]);
        if (!CommonUtils.isArrayEmpty(embeddedImagesArr)) {
	        for (int i = 0; i < embeddedImagesArr.length; i++) {
	        	if (!CommonUtils.isEmtpy(embeddedImagesArr[i])) {
	    	        BodyPart imgPart = new MimeBodyPart();
	        		imgPart = getImagePart(embeddedImagesArr[i]);
			        multipart.addBodyPart(imgPart);
	        	}
	        }
        }		        

        // Set attachments
        String[] attArr = atts.toArray(new String[0]);
        if (!CommonUtils.isArrayEmpty(attArr)) {
			for (int i = 0; i < attArr.length; i++) {
				if (!CommonUtils.isEmtpy(attArr[i])) {
					BodyPart attPart = new MimeBodyPart();
					attPart = getAttachmentPart(attArr[i]);
				    multipart.addBodyPart(attPart);
				}
			}	     
        }		

        // Set message multipart
	    message.setContent(multipart);		
        
        // Get transport and connect
        Transport transport = session.getTransport("smtp");
        transport.connect(host, userName, password);
        
        // send message
        transport.sendMessage(message, message.getAllRecipients());
        
        // close transport
        transport.close();
	}		
	
    private BodyPart getP8AttachmentPart(Document att) throws Exception {	
		BodyPart attPart = new MimeBodyPart();
		com.filenet.api.core.ContentTransfer ct = ContentService.getContentTransfer(att, 0);
	    DataSource source = new ByteArrayDataSource((InputStream)ct.accessContentStream(), ct.get_ContentType());
	    attPart.setDataHandler(new DataHandler(source));
	    attPart.setFileName(ct.get_RetrievalName());
	    return attPart;
	} 	
	
    private BodyPart getAttachmentPart(String attPath) throws Exception	{	
		BodyPart attPart = new MimeBodyPart();
		DataSource source = new FileDataSource(attPath);
		attPart.setDataHandler(new DataHandler(source));
		attPart.setFileName(attPath);
		return attPart;
	}    
    
    private BodyPart getImagePart(String imagepath) throws Exception {
    	BodyPart imgPart = new MimeBodyPart();  
        DataSource source = new FileDataSource(imagepath);
        imgPart.setDataHandler(new DataHandler(source));
        imgPart.setHeader("Content-ID","<" + CommonUtils.getFileName(imagepath, true) + ">");
        return imgPart;
    } 	
	
    private String getTemplateContent(Document template, String[] templateNames, String[] templateValues) throws Exception {
    	String body = IOUtils.toString(ContentService.getContentTransfer(template, 0).accessContentStream());
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

}