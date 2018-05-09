public class S3ClientBuilder {

    final private Regions region = Regions.US_WEST_1;

    private AmazonS3 amazonS3Client;

    public S3ClientBuilder() {
        amazonS3Client = generateClient();
    }

    public void listBuckets() {
        List<Bucket> buckets = amazonS3Client.listBuckets();
        System.out.println("Your Amazon S3 buckets are:");
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }
    }

    private AmazonS3 generateClient() {
    
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
    
        String proxyHost = Optional.ofNullable(System.getenv("PROXY_HOST")).ifPresent(conf::setProxyHost);
        String proxyPort = Optional.ofNullable(System.getenv("PROXY_PORT")).ifPresent(port -> conf.setProxyPort(Integer.parseInt(port)));

        return AmazonS3ClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(getTemporaryCredentials(clientConfiguration, region)))
              .withClientConfiguration(clientConfiguration)
              .withRegion(region)
              .build();
    }

    private BasicSessionCredentials getTemporaryCredentials(ClientConfiguration conf, Regions region)  {
        
        final String ROLE_ARN = Optional.of(System.getenv("ROLE_ARN")).orElse("NO ROLE FOUND");

        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
            .withClientConfiguration(conf)
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .withRegion(region)
            .build();

        System.out.println("Sts client built.");

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
            .withRoleArn(ROLE_ARN)
            .withDurationSeconds(3600)
            .withRoleSessionName("cerved-s3-connect");
            /** NECESSARI PER MFA 
            //.withSerialNumber(ID_ARN_DEVICE_MFA)
            //.withTokenCode(TOKEN_GENERATO_DA_DEVICE_MFA)
            */

        AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);

        System.out.println("AssumeRole done.");

        BasicSessionCredentials temporaryCredentials =
            new BasicSessionCredentials(
                assumeResult.getCredentials().getAccessKeyId(),
                assumeResult.getCredentials().getSecretAccessKey(),
                assumeResult.getCredentials().getSessionToken());

        System.out.println("Temporary credentials created.");

        return temporaryCredentials;
    }
}
