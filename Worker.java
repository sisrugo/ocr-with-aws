/**
 * Created by sharo on 11/13/2018.
 */
import Messages.DoneImageTask;
import Messages.ImageTask;
import Services.AWSService;
import com.amazonaws.services.sqs.model.Message;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;


public class Worker {

    private static ITesseract tesInstance;

    public static void main(String[] args) {
        tesInstance = new Tesseract();
        // args[0] url for images task queue
        String imageTaskUrlQueue = args[0];
        // args[1] url for done images task queue
        String doneImageTaskUrlQueue = args[1];
        // args[2] n
        int n = Integer.parseInt(args[2]);

        // connect to sqs service
        AWSService.connectSqs();

        String textResult = "";

        for (int i = 0; i < n; i++) {
            // recive message from the imageTaskUrlQueue
            List<Message> receiveMessages = AWSService.receiveMessageFromQueue(imageTaskUrlQueue);

            //read the message
            String receiveMes = receiveMessages.get(0).getBody();

            // parse to imageTask class
            ImageTask imageTask = new ImageTask(receiveMes);

            String imageUrlStr = imageTask.getImageUrl();

            // execute OCR algorithm
            textResult = execOcr(imageUrlStr);

            //remove new lines and tabs
            textResult = replaceNewLineWithSpace(textResult);

            //create done image task object
            DoneImageTask doneImageTask = new DoneImageTask(imageUrlStr, textResult);

            //parse done image task  to string
            String message = doneImageTask.toString();

            //send done image task to doneImageTaskUrlQueue
            AWSService.sendMessageToQueue(doneImageTaskUrlQueue, message);
            System.out.println("done with " + i + "images");

            //delete message from imageTaskUrlQueue
            AWSService.deleteMessageFromQueue(receiveMessages, imageTaskUrlQueue, 1);
        }
    }

    private static String execOcr(String imageUrlStr) {
        String result = "no result from ocr lib";
        try{
            URL url = new URL(imageUrlStr);
            BufferedImage imageFile = ImageIO.read(url);
            if(imageFile == null){
                System.out.println("Inter - image File is null!");
            }else{
                result = tesInstance.doOCR(imageFile);
            }
        }catch (net.sourceforge.tess4j.TesseractException e){
            System.out.println("Inter - doOCR exception happends");
        }catch ( IOException e ){
            System.out.println("Inter - ImageIO.read exception happends");
        }
        return result;
    }

    private static String replaceNewLineWithSpace(String text) {
        return text.replaceAll("[\\t\\n\\r]+", " ");
    }

}