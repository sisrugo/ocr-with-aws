import Services.AWSService;
import Messages.DoneTask;
import Messages.Task;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import j2html.tags.ContainerTag;
import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.List;
import static j2html.TagCreator.*;


/**
 * Created by sharo on 11/13/2018.
 */
public class Application {

    private static final String MANAGER_BUCKET_NAME = "managerbucketdistributed"; // where the jar is in s3
    private static final String RESULT_QUEUE_NAME = "resultaskQueue"; //queue for the application to accept result message

    public static void main(String[] args) {
        //args[0] input file name (url)
        String inputFilename = args[0];
        //args[1] output html file name
        String outputFileName = args[1];
        //args[2] n (number tasks per worker)
        int n = Integer.parseInt(args[2]);

        //connecting ec2 service
        AWSService.connectEC2();

        //connecting s3 service
        AWSService.connectS3();

        //connecting sqs service
        AWSService.connectSqs();

        //check if the noe manager is active- check the input task of manager exists
        String managerQueueUrl="";
        try{
            managerQueueUrl = AWSService.getQueue(Manager.MANAGER_QUEUE_NAME);
        }catch (QueueDoesNotExistException e){
            //create manager with ec2
            AWSService.createInstanceWithUserData(1,"manager.jar",MANAGER_BUCKET_NAME,null);
        }

        //busy wait until the manager is created
        boolean flag = false;
        while (!flag){
            try{
                managerQueueUrl = AWSService.getQueue(Manager.MANAGER_QUEUE_NAME);
                flag = true;
                break;
            }catch(QueueDoesNotExistException e){
                continue;
            }
        }

        //upload the file to Manager's app bucket
        if (AWSService.doesObjectExistInTheBucket(Manager.APP_BUCKET_NAME , inputFilename)){
            System.out.println("input file name already exists in the Manager. \n Please enter different name for your input file name.");
            System.exit(-1);
        }else{
            AWSService.uploadObject(Manager.APP_BUCKET_NAME, inputFilename);
        }

        String resultQueueName = inputFilename.substring(0, inputFilename.length() - 4);
        resultQueueName = RESULT_QUEUE_NAME + resultQueueName.toLowerCase();

        //create done task queue
        String resultQueueAppUrl= AWSService.createQueue(resultQueueName) ;

        //create task object with url(bucket, key) file in s3, n and done queue url
        Task message = new Task(inputFilename, n, resultQueueAppUrl);

        //parse task object to string
        String msgString = message.toString();

        //send it to tasks queue that belongs to Manager
        AWSService.sendMessageToQueue(managerQueueUrl, msgString);

        //listening to done task queue
        List<Message> messages = null;
        while(messages == null || messages.size() <= 0) {
            try {
                Thread.currentThread().sleep(10000);
            }catch (Exception e){}

            messages = AWSService.receiveMessageFromQueue(resultQueueAppUrl);
        }

        String ReceiveMes = messages.get(0).getBody();

        //parse string to done task obj
        DoneTask doneTask= new DoneTask(ReceiveMes);

        //download output file from s3
        InputStream content = AWSService.downloadObject(doneTask.getBucketName(), doneTask.getUrlFileInS3());

        try {
            FileUtils.writeStringToFile(new File(outputFileName), createHtmlFile(content));
        }catch (IOException e){
            System.out.println("Error: can not create html5 file");
        }

        //delete done task from the queue
        AWSService.deleteMessageFromQueue(messages, resultQueueName,1 );

        //delete application input file
        //AWSService.deleteFile(Manager.APP_BUCKET_NAME, doneTask.getUrlFileInS3());
        AWSService.deleteFile(Manager.APP_BUCKET_NAME,inputFilename);

        //delete application result queue
        AWSService.deleteQueue(resultQueueName);
    }

    private static String createHtmlFile(InputStream content)throws IOException{
        ContainerTag html = html().attr("lang", "en").with(
                head().with(
                        meta().withCharset("utf-8"),
                        title("Output file")
                ),
                body().with(createContent(content))
        );
        return document().render() + html.render();
    }

    private static ContainerTag createContent(InputStream content)throws IOException{
        ContainerTag lineBreakDiv = div();
        BufferedReader br = new BufferedReader(new InputStreamReader(content));
        String imageUrl = null;
        String txt = "";
        while ((imageUrl = br.readLine()) != null) {
            txt = br.readLine();
            lineBreakDiv.with(img().withSrc(imageUrl),div().with(p(txt)));
        }
        br.close();
        return lineBreakDiv;
    }
}