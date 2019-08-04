package Messages;

/**
 * Created by sharo on 11/13/2018.
 */
public class ImageTask {
    private String imageUrl;

    public ImageTask(String imageUrl){
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        String str = this.getImageUrl();
        return str;
    }
}