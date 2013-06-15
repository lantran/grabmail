import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.sun.mail.util.BASE64DecoderStream;

public class Grabber {
	public static void main(String[] args) throws Exception {
		final String username = "uname";
		final String password = "pass";

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props,
				new javax.mail.Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
		Store store = session.getStore("pop3s");
		store.connect("pop.gmail.com", username, password);

		Folder f = store.getDefaultFolder().getFolder("INBOX");
		f.open(Folder.READ_ONLY);
		Message[] ms = f.getMessages();
		PrintWriter printWriter;
		for (Message m : ms) {
			File file = new File(username + "/"
					+ m.getFrom()[0].toString().replaceAll(" <.*>$", "") + " "
					+ m.getSentDate().getTime() + ".md");
			file.setLastModified(m.getSentDate().getTime());

			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(
					file)));

			printWriter.write("## Sent: " + m.getSentDate().toString());
			
			printWriter.write("## From:\n");
			for (Address s : m.getFrom()) {
				printWriter.write("* " + s.toString() + "\n");
			}
			printWriter.write("\n");

			printWriter.write("## To:\n");
			for (Address s : m.getAllRecipients()) {
				printWriter.write("* " + s.toString() + "\n");
			}
			printWriter.write("\n");

			printWriter.write("## Subject:\n" + m.getSubject() + "\n");

			printWriter.write("## Body: \n");
			for (String s : contentToString(m.getContent())) {
				printWriter.write(s);
			}
			printWriter.write("\n");

			printWriter.close();
			printWriter.flush();
		}
	}

	// returns: [[<type>, <content>]]
	private static ArrayList<String> contentToString(Object o)
			throws MessagingException, IOException {
		ArrayList<String> rtn = new ArrayList<String>();

		if (o instanceof MimeMultipart) {
			MimeMultipart mm = (MimeMultipart) o;
			for (int i = 0; i < mm.getCount(); i++) {
				Object temp = mm.getBodyPart(i).getContent();
				if (temp instanceof BASE64DecoderStream) {
					BASE64DecoderStream b64Stream = (BASE64DecoderStream) temp;
					rtn.add("### Type: "
							+ mm.getBodyPart(i).getContentType()
							+ "\n"
							+ new String(Base64.encodeBase64(IOUtils
									.toByteArray(b64Stream)), "UTF-8") + "\n");
				} else if (temp instanceof String) {
					if (mm.getBodyPart(i).getContentType()
							.startsWith("text/html")) {
						rtn.add("### Type: "
								+ mm.getBodyPart(i).getContentType()
								+ "\n    "
								+ ((String) temp).replace('\u00A0', ' ')
										.replace("\n", "\n    ") + "\n");
					} else {
						rtn.add("### Type: "
								+ mm.getBodyPart(i).getContentType() + "\n"
								+ ((String) temp).replace('\u00A0', ' ') + "\n");
					}
				} else if (temp instanceof MimeMultipart) {
					rtn.addAll(contentToString(temp));
				} else {
					// Discover things I've missed
					System.out.println(temp.getClass());
				}
			}
		} else if (o instanceof String) {
			rtn.add("## Body: \n ### Type: text/plain\n" + (String) o + "\n");
		} else {
			// Discover things I've missed
			System.out.println(o.getClass());
		}

		return rtn;
	}
}
