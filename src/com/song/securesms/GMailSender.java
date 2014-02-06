/*
 * This java file is for sending keys via email in the background. 
 * If private key test is needed, please modify the 'testDectypt' to false.
 * Otherwise the private key will be sent to my email account.
 * 
 */

package com.song.securesms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import android.content.Context;
import android.os.Environment;

/**
 * Created by song_jin on 6/20/13.
 */
public class GMailSender extends javax.mail.Authenticator{
	private String user;
	private String password;
    private Context context;

	static{
		Security.addProvider(new com.song.securesms.JSSEProvider());
	}
	
	protected PasswordAuthentication getPasswordAuthentication() {   
        return new PasswordAuthentication(user, password);   
    }

    public GMailSender(Context context) {
        this.context = context;
    }

    public void sendMail(String recipients, String phoneNum, String messageBody) throws Exception {
        final  String username = "dswuser1@gmail.com";
        final  String password = "hailtopitt123";
        String messageSubject = phoneNum;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator()
                {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(username, password);
                    }
                });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(recipients));
        message.setSubject(messageSubject);
        message.setText(messageBody);
        Transport.send(message);
    }

	public void sendMailWithPubKey(String recipients, String phoneNum, RSAPublicKeySpec pub) throws Exception {
        //Email out the public key to the system.
        final  String username = "dswuser1@gmail.com";
        final  String password = "hailtopitt123";

        if(!phoneNum.startsWith("1")){phoneNum="1"+phoneNum;}
        String messageBody = pub.getModulus().toString()+"\n"+pub.getPublicExponent().toString()+"\n"+phoneNum;
        String messageSubject="public key";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
             new javax.mail.Authenticator()
             {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(username, password);
                }
             });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(recipients));
        message.setSubject(messageSubject);
        
        // add both text and attachment to the email
        MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setText(messageBody);
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(mbp1);
        mp.addBodyPart(addAttachment("public.key"));
        
        message.setContent(mp);
        Transport.send(message);
     }
      
	
	public final BodyPart addAttachment(String filename) throws Exception {
			BodyPart messageBodyPart = new MimeBodyPart(); 
            File file = new File(context.getFilesDir(), filename);
		    DataSource source = new FileDataSource(file); 
		    messageBodyPart.setDataHandler(new DataHandler(source)); 
		    messageBodyPart.setFileName(filename); 
	    return messageBodyPart;
	} 

	
	public class ByteArrayDataSource implements DataSource {   
		private byte[] data;   
        private String type;
        
        public ByteArrayDataSource(byte[] data, String type) {   
            super();   
            this.data = data;   
            this.type = type;   
        }   
        
        public ByteArrayDataSource(byte[] data) {   
            super();   
            this.data = data;   
        }   
        
        public void setType(String type) {   
            this.type = type;   
        }   
        
		@Override
		public String getContentType() {
			if (type == null)   
                return "application/octet-stream";   
            else  
                return type;   
		}
		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(data);   
		}
		@Override
		public String getName() {
			return "ByteArrayDataSource";  
		}
		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new IOException("Not Supported"); 
		}        
	}
}
