package awslambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Map;
import java.util.logging.Logger;

public class SnsLambdaFunction implements RequestHandler<SNSEvent, String> {

    private static final Logger logger = Logger.getLogger(SnsLambdaFunction.class.getName());

    @Override
    public String handleRequest(SNSEvent event, Context context) {
        // Fetch the secret name from environment variables
        String secretName = System.getenv("EMAIL_CREDENTIALS_SECRET_NAME");
        if (secretName == null || secretName.isEmpty()) {
            throw new RuntimeException("EMAIL_CREDENTIALS_SECRET_NAME environment variable is not set");
        }

        // Fetch email credentials from Secrets Manager
        Map<String, String> credentials = fetchEmailCredentials(secretName);
        String mailgunApiKey = credentials.get("MAILGUN_API_KEY");
        String mailgunDomainName = credentials.get("MAILGUN_DOMAIN_NAME");
        String fromEmail = "noreply@" + mailgunDomainName;

        // Process the SNS event
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            SNSEvent.SNS sns = record.getSNS();
            String message = sns.getMessage();
            JsonObject jsonPayload = JsonParser.parseString(message).getAsJsonObject();
            String email = jsonPayload.get("email").getAsString();
            String activationLink = jsonPayload.get("activationLink").getAsString();

            try {
                sendVerificationEmail(email, activationLink, mailgunApiKey, mailgunDomainName, fromEmail);
                logger.info("Verification email sent to: " + email);
            } catch (Exception e) {
                logger.severe("Error sending verification email: " + e.getMessage());
                return "Error sending email";
            }
        }
        return "Success";
    }

    private void sendVerificationEmail(String recipient, String activationLink, String mailgunApiKey, String mailgunDomainName, String fromEmail) {
        MailgunMessagesApi mailgunMessagesApi = MailgunClient.config(mailgunApiKey)
                .createApi(MailgunMessagesApi.class);

        String subject = "Verify Your Email Address";
        String htmlBody = "<p>Dear User,</p>"
                + "<p>Please click on the following link to verify your email address.</p>"
                + "<a href=\"" + activationLink + "\">Verify Email</a>"
                + "<p>Thanks</p>";

        String textBody = "Dear User,\n\n"
                + "Please click on the following link to verify your email address.\n"
                + activationLink
                + "\n\nThanks";

        Message message = Message.builder()
                .from(fromEmail)
                .to(recipient)
                .subject(subject)
                .text(textBody)
                .html(htmlBody)
                .build();

        try {
            mailgunMessagesApi.sendMessage(mailgunDomainName, message);
        } catch (Exception e) {
            logger.severe("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private Map<String, String> fetchEmailCredentials(String secretName) {
        try {
            SecretsManagerClient secretsClient = SecretsManagerClient.create();
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse secretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
            String secretString = secretValueResponse.secretString();
            logger.info("Retrieved secret: " + secretString);

            logger.info("Successfully fetched email credentials from SecretsManager.");
            return new Gson().fromJson(secretString, Map.class);

        } catch (Exception e) {
            logger.severe("Failed to fetch email credentials: " + e.getMessage());
            throw new RuntimeException("Failed to fetch email credentials", e);
        }
    }
}
