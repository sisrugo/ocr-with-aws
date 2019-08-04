package Messages;

/**
 * Created by sharo on 11/13/2018.
 */
public class DoneImageTask {
    private String imageUrl;
    private String text;

    public DoneImageTask(String message){
        String[] attributes = message.split("!");
        this.imageUrl = attributes[0];
        if(attributes.length > 1)
            this.text = attributes[1];
    }

    public DoneImageTask(String imageUrl, String text) {
        this.imageUrl = imageUrl;
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        String str = this.getImageUrl() +"!"
                + this.getText();
        return str;
    }
}