package Messages;

/**
 * Created by sharo on 11/13/2018.
 */
public class DoneTask {
    private String urlFileInS3;
    private String bucketName;

    public DoneTask(String message){
        String[] attributes = message.split("!");
        this.urlFileInS3 = attributes[0];
        this.bucketName = attributes[1];
    }


    public DoneTask(String urlFileInS3, String bucketName){
        this.urlFileInS3 = urlFileInS3;
        this.bucketName = bucketName;
    }

    public String getUrlFileInS3() {
        return urlFileInS3;
    }

    public String getBucketName() {
        return bucketName;
    }
    @Override
    public String toString() {
        String str = this.getUrlFileInS3() +"!"
                + this.getBucketName();

        return str;
    }
}