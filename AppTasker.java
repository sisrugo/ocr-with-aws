import Messages.DoneImageTask;
import Messages.DoneTask;
import Messages.ImageTask;
import Messages.Task;
import Services.AWSService;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.List;

/**
 * Created by sharo on 11/23/2018.
 */
public class AppTasker implements Runnable {

    private int appId;
    private Task appTask;
    private static final String IMAGE_QUEUE_NAME_PREFIX = "imagetaskQueue";
    private static final String DONE_IMAGE_QUEUE_NAME_PREFIX = "doneimagetaskQueue";

    public AppTasker(int appId, Task appTask){
        this.appId = appId;
        this.appTask = appTask;
    }

    public void run() {
        //create image task queue done image task queue for the app images
        AWSService.createQueue(IMAGE_QUEUE_NAME_PREFIX +appId);
        AWSService.createQueue(DONE_IMAGE_QUEUE_NAME_PREFIX +appId);

        // get parameters from current task
        int numOfTaskPerWorker = appTask.getN();
        String resultQueueUrl = appTask.getResultQueueUrl();

        //download the images url file from s3 (the url in currentTask.getUrlFileInS3)
        InputStream content = AWSService.downloadObject(Manager.APP_BUCKET_NAME, appTask.getUrlFileInS3());


        // create workers as node in ec2 and send for each the following parameters:
        // 1. image task url
        // 2. done image task utl
        // 3. number of image tasks to execute (no forget about the reminder)
        String [] workerArgs = new String [3];
        workerArgs[0] = AWSService.getQueue(IMAGE_QUEUE_NAME_PREFIX +appId);
        workerArgs[1] = AWSService.getQueue(DONE_IMAGE_QUEUE_NAME_PREFIX +appId);
        workerArgs[2] = String.valueOf(numOfTaskPerWorker);

        // count the lines and save it in numberOfImages
        int numberOfImages = 0;
        int tempNumImages = 0 ;
        //for any line (image url) in the file create image task and send to images task queue
        String imageQueueUrl = AWSService.getQueue(IMAGE_QUEUE_NAME_PREFIX + appId);
        List<Instance> instances = null;
        try{
            // Construct BufferedReader from FileReader
            BufferedReader br = new BufferedReader(new InputStreamReader(content));
            String imageUrl = null;

            while ((imageUrl = br.readLine()) != null) {
                //handler the imageUrl
                numberOfImages++;
                tempNumImages++;
                ImageTask lineTask = new ImageTask(imageUrl);
                AWSService.sendMessageToQueue(imageQueueUrl,lineTask.toString() );

                if (tempNumImages % numOfTaskPerWorker == 0){
                    tempNumImages = 0;
                    if(instances == null)
                        instances = AWSService.createInstanceWithUserData(1,"worker.jar", Manager.WORKER_BUCKET_NAME, workerArgs);
                    else
                        instances.addAll(AWSService.createInstanceWithUserData(1,"worker.jar", Manager.WORKER_BUCKET_NAME, workerArgs));
                }

            }
            br.close();

            //add worker for reminder
            if (tempNumImages > 0){
                workerArgs[2] = String.valueOf(tempNumImages);
                instances.addAll(AWSService.createInstanceWithUserData(1,"worker.jar", Manager.WORKER_BUCKET_NAME, workerArgs));
            }

        }catch (IOException e){
            System.out.println("Error: invalid url file");
        }

        // declare String output variable
        String output = "";

        //listening to the done image task until amount doneImageTasks = numImagetasks
        List<Message> messages = null;
        int countMessages = 0 ;
        while(countMessages < numberOfImages){
            messages = AWSService.receiveMessageFromQueue(DONE_IMAGE_QUEUE_NAME_PREFIX +appId);
            if(messages.size() > 0){
                countMessages = countMessages + messages.size();
                output = creatOutputFromMessages(output, messages);
                AWSService.deleteMessageFromQueue(messages, DONE_IMAGE_QUEUE_NAME_PREFIX +appId, messages.size());
            }
        }

        String resultFile = "txtFiles/result" + appId + ".txt";
        try {  // create file with output string
            FileUtils.writeStringToFile(new File(resultFile), output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add the file in s3 with the same path as in local
        AWSService.uploadObject(Manager.APP_RESULT_BUCKET, resultFile);

        // create done task object with url of output file
        DoneTask doneTask = new DoneTask(resultFile, Manager.APP_RESULT_BUCKET);

        // send done task object to doneImageQueueUrl
        AWSService.sendMessageToQueue(resultQueueUrl, doneTask.toString());

        //shut down all workers
        AWSService.shutDownInstances(instances);

        //delete image task and done image tasks queues
        AWSService.deleteQueue(IMAGE_QUEUE_NAME_PREFIX + appId);
        AWSService.deleteQueue(DONE_IMAGE_QUEUE_NAME_PREFIX + appId);
    }

    private static String creatOutputFromMessages(String output, List<Message> messages){
        //for every doneImageTask add it in output variable:

        for (Message msg: messages){

            String ReceiveMes = msg.getBody();

            //parse string to done task obj
            DoneImageTask doneTask= new DoneImageTask(ReceiveMes);
            // urlimag + "\n" + text + "\n"
            output = output + doneTask.getImageUrl() + "\n"
                    + doneTask.getText() + "\n";
        }
        return output;
    }
}
