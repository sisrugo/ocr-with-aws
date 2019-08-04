import Messages.Task;
import Services.AWSService;
import com.amazonaws.services.sqs.model.Message;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by sharo on 11/13/2018.
 */

public class Manager {

    public static final String WORKER_BUCKET_NAME = "workerbucketdistributed";
    public static final String MANAGER_QUEUE_NAME="InputTasksQueue";
    public static final String APP_BUCKET_NAME = "appbucketdistributed";
    public static final String APP_RESULT_BUCKET = "resultimagestextbucketdistributed";

    public static void main(String [ ] args){

        //connecting to sqs service
        AWSService.connectEC2();
        //connecting to ec2 service  - for creating workers
        AWSService.connectS3();
        //conncecting to s3 service - to download the file
        AWSService.connectSqs();

        //create manger task queue and bucket for applications input and output
        AWSService.createBucket(APP_BUCKET_NAME);
        AWSService.createBucket(APP_RESULT_BUCKET);
        AWSService.createQueue(MANAGER_QUEUE_NAME);

        // initilize counter for applications ID's
        int appId = 0;

        //create executer for applications tasks
        ExecutorService pool = Executors.newFixedThreadPool(10);

        //listening to managerMessages per app
        //for every task in the task queue do execTask
        List<Message> managerMessages = null;
        while(true){
            managerMessages = AWSService.receiveMessageFromQueue(MANAGER_QUEUE_NAME);
            for (Message task: managerMessages) {
                Task appTask = new Task(task.getBody());
                appId = appId + 1;
                AppTasker appTasker = new AppTasker(appId,appTask);
                pool.execute(appTasker);
                AWSService.deleteMessageFromQueue(managerMessages, MANAGER_QUEUE_NAME, 1);
            }
        }
    }
}