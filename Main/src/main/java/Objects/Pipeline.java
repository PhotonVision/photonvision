package Objects;
import java.util.Arrays;
import java.util.List;

public class Pipeline {
    public int exposure = 50;
    public int brightness = 50;
    public String orientation = "Normal";
    public List<Integer> hue = Arrays.asList(0,100);
    public List<Integer> saturation = Arrays.asList(0,100);
    public List<Integer> value = Arrays.asList(0,100);
    public boolean erode = false;
    public boolean dilate = false;
    public List<Integer> area = Arrays.asList(0,100);
    public List<Integer> ratio = Arrays.asList(0,20);
    public List<Integer> extent = Arrays.asList(0,100);
    public boolean is_binary = false;
    public String sort_mode = "Largest";
    public String target_group = "Single";
    public String target_intersection = "Largest";
    public double M = 1;
    public double B = 0;
}
