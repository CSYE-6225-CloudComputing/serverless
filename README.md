# Serverless Function Project

## Overview
This project contains a serverless function designed to handle email verification through an SNS event trigger. The function is implemented in Java and runs on AWS Lambda, providing seamless scalability and cost-efficiency.

## Key Features
- **Event-driven execution**: The function is triggered by Amazon SNS events.
- **Scalable**: Automatically scales to handle concurrent executions.
- **Low maintenance**: No need for server provisioning or management.
- **Secure**: Built-in integration with AWS IAM and environment variable encryption.

## Prerequisites
- Java 21
- AWS account configured with credentials
- Maven installed

## Installation
1. Clone the repository:

   ```
2. Build the project using Maven:
   ```bash
   mvn clean package
   ```

## Usage
- **Trigger**: The function is triggered by an SNS event.
- **Input**: The function expects an SNS event containing a JSON payload with `email` and `activationLink` fields.
- **Output**: Returns "Success" if the email is sent successfully or an error message if it fails.

## Environment Variables
Ensure the following environment variables are configured for secure operations:
- `MAIL_GUN_API_KEY`: Your Mailgun API key.
- `MAIL_GUN_DOMAIN_NAME`: The domain name for your Mailgun account.


