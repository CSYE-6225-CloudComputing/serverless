package awslambda;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;


import java.util.logging.Logger;

public class SnsLambdaFunction implements RequestHandler<SNSEvent, String> {

    private static final Logger logger = Logger.getLogger(SnsLambdaFunction.class.getName());
    private static final String FROM_EMAIL = "noreply@" + System.getenv("MAIL_GUN_DOMAIN_NAME");
    private static final String MAILGUN_API_KEY = System.getenv("MAIL_GUN_API_KEY");
    private static final String MAILGUN_DOMAIN_NAME = System.getenv("MAIL_GUN_DOMAIN_NAME");

    @Override
    public String handleRequest(SNSEvent event, Context context) {
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            SNSEvent.SNS sns = record.getSNS();
            String message = sns.getMessage();

            // Decode and parse the JSON payload
            JsonObject jsonPayload = JsonParser.parseString(message).getAsJsonObject();
            String email = jsonPayload.get("email").getAsString();
            String activationLink = jsonPayload.get("activationLink").getAsString();
          //  String tokenId = jsonPayload.get("tokenId").getAsString();

            // Send the email using Mailgun
            try {
                sendVerificationEmail(email, activationLink);
                logger.info("Verification email sent to: " + email);
            } catch (Exception e) {
                logger.severe("Error sending verification email: " + e.getMessage());
                
                return "Error sending email";
            }
        }
        return "Success";
    }

    private void sendVerificationEmail(String recipient, String activationLink) {
        MailgunMessagesApi mailgunMessagesApi = MailgunClient.config(MAILGUN_API_KEY)
                .createApi(MailgunMessagesApi.class);

        String subject = "Verify Your Email Address";
        String body = "Please click on the following link to verify your email address. "
                + "This link will expire in 2 minutes: " + activationLink;
        logger.info("i am here on 67");
        Message message = Message.builder()
                .from(FROM_EMAIL)
                .to(recipient)
                .subject(subject)
                .text(body)
                .build();

        logger.info("i have sendt mail mow5");        

        try {
            mailgunMessagesApi.sendMessage(MAILGUN_DOMAIN_NAME, message);
            logger.info("Email successfully sent to: " + recipient);
        } catch (Exception e) {
            logger.severe("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    
}
